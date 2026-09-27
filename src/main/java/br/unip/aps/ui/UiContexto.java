package br.unip.aps.ui;

import br.unip.aps.ApsException;
import br.unip.aps.app.Sessao;
import br.unip.aps.model.BaseDeFocos;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.Chart;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contexto compartilhado pelos controllers do dashboard: sessao (modelo), propriedades
 * observaveis (base carregada, status, progresso), execucao de tarefas em background com
 * cancelamento e dialogos de erro amigaveis.
 */
public final class UiContexto {

    private static final Logger LOG = Logger.getLogger(UiContexto.class.getName());

    private final Sessao sessao;
    private final Stage stage;
    private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "aps-worker");
        t.setDaemon(true);
        return t;
    });

    private final ObjectProperty<BaseDeFocos> base = new SimpleObjectProperty<>();
    private final ObjectProperty<br.unip.aps.ml.Preditor.ResultadoML> ml = new SimpleObjectProperty<>();
    private final StringProperty status = new SimpleStringProperty("Pronto.");
    private final DoubleProperty progresso = new SimpleDoubleProperty(0);
    private final BooleanProperty ocupado = new SimpleBooleanProperty(false);
    private final Map<String, Chart> graficos = new LinkedHashMap<>();
    private Task<?> tarefaAtual;
    private MainController main;

    UiContexto(Sessao sessao, Stage stage) {
        this.sessao = sessao;
        this.stage = stage;
    }

    /**
     * Fabrica de controllers usada pelo FXMLLoader: injeta este contexto no construtor.
     *
     * @param tipo classe do controller
     * @return instancia
     */
    Object criarController(Class<?> tipo) {
        try {
            try {
                Object c = tipo.getDeclaredConstructor(UiContexto.class).newInstance(this);
                if (c instanceof MainController m) this.main = m;
                return c;
            } catch (NoSuchMethodException e) {
                return tipo.getDeclaredConstructor().newInstance();
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Nao foi possivel criar o controller " + tipo.getName(), e);
        }
    }

    /**
     * Executa uma tarefa em background, atualizando status e barra de progresso.
     *
     * @param descricao  texto exibido na barra de status
     * @param trabalho   trabalho (roda fora da thread da interface)
     * @param aoConcluir callback na thread da interface com o resultado
     * @param <T>        tipo do resultado
     * @return tarefa criada (pode ser cancelada)
     */
    public <T> Task<T> executar(String descricao, Callable<T> trabalho, Consumer<T> aoConcluir) {
        return executar(descricao, p -> trabalho.call(), aoConcluir);
    }

    /**
     * Variante em que o trabalho recebe um reportador de progresso (0 a 1).
     *
     * @param descricao  texto
     * @param trabalho   trabalho com progresso
     * @param aoConcluir callback com o resultado
     * @param <T>        tipo
     * @return tarefa
     */
    public <T> Task<T> executar(String descricao, TrabalhoComProgresso<T> trabalho, Consumer<T> aoConcluir) {
        if (ocupado.get()) {
            aviso("Aguarde", "Ja existe uma operacao em andamento: " + status.get()
                    + "\nAguarde a conclusao ou clique em Cancelar.");
            return null;
        }
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return trabalho.executar((f, msg) -> {
                    updateProgress(f, 1.0);
                    if (msg != null) updateMessage(msg);
                });
            }
        };
        task.messageProperty().addListener((o, a, n) -> {
            if (n != null && !n.isBlank()) status.set(descricao + " — " + n);
        });
        progresso.bind(task.progressProperty());
        ocupado.set(true);
        status.set(descricao + "...");
        long t0 = System.nanoTime();
        task.setOnSucceeded(e -> {
            finalizar();
            status.set(descricao + " — concluido em " + br.unip.aps.util.Formatos.duracao(System.nanoTime() - t0));
            try {
                aoConcluir.accept(task.getValue());
            } catch (RuntimeException ex) {
                erro("Erro ao exibir o resultado", ex);
            }
        });
        task.setOnFailed(e -> {
            finalizar();
            Throwable ex = task.getException();
            if (ex instanceof CancellationException) {
                status.set(descricao + " — cancelado.");
            } else {
                status.set(descricao + " — falhou.");
                erro(descricao, ex);
            }
        });
        task.setOnCancelled(e -> {
            finalizar();
            status.set(descricao + " — cancelado pelo usuario.");
        });
        tarefaAtual = task;
        executor.submit(task);
        return task;
    }

    private void finalizar() {
        progresso.unbind();
        progresso.set(0);
        ocupado.set(false);
        tarefaAtual = null;
    }

    /** Cancela a tarefa em andamento (interrompe a thread; os algoritmos verificam a interrupcao). */
    public void cancelar() {
        if (tarefaAtual != null) tarefaAtual.cancel(true);
    }

    /** Carrega os CSVs da pasta de dados; em caso de falha oferece alternativas. */
    void carregarDadosIniciais() {
        if (main != null) main.carregarInicial();
    }

    /**
     * Exibe erro com mensagem amigavel; excecoes inesperadas incluem detalhes tecnicos expansiveis.
     *
     * @param titulo contexto
     * @param ex     excecao
     */
    public void erro(String titulo, Throwable ex) {
        Throwable causa = ex;
        while (causa.getCause() != null && !(causa instanceof ApsException) && !(causa instanceof IllegalArgumentException)) {
            causa = causa.getCause();
        }
        boolean esperado = causa instanceof ApsException || causa instanceof IllegalArgumentException
                || causa instanceof UnsupportedOperationException;
        if (!esperado) LOG.log(Level.SEVERE, titulo, ex);
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.initOwner(stage);
        a.setTitle("Erro");
        a.setHeaderText(titulo);
        a.setContentText(esperado ? causa.getMessage() : "Ocorreu um erro inesperado: " + causa
                + "\nOs detalhes foram gravados em logs/aps-0.log.");
        if (!esperado) {
            java.io.StringWriter sw = new java.io.StringWriter();
            ex.printStackTrace(new java.io.PrintWriter(sw));
            TextArea ta = new TextArea(sw.toString());
            ta.setEditable(false);
            a.getDialogPane().setExpandableContent(ta);
        }
        a.getDialogPane().setMinWidth(520);
        a.showAndWait();
    }

    /**
     * @param titulo cabecalho
     * @param msg    mensagem
     */
    public void aviso(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg);
        a.initOwner(stage);
        a.setHeaderText(titulo);
        a.getDialogPane().setMinWidth(480);
        a.showAndWait();
    }

    /**
     * @param titulo cabecalho
     * @param msg    mensagem
     */
    public void info(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.initOwner(stage);
        a.setHeaderText(titulo);
        a.getDialogPane().setMinWidth(480);
        a.showAndWait();
    }

    /**
     * @param titulo cabecalho
     * @param msg    pergunta
     * @return {@code true} se o usuario confirmar
     */
    public boolean confirmar(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.initOwner(stage);
        a.setHeaderText(titulo);
        a.getDialogPane().setMinWidth(480);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.YES;
    }

    /**
     * Registra um grafico para inclusao (snapshot) nos relatorios PDF.
     *
     * @param titulo legenda
     * @param chart  grafico
     */
    public void registrarGrafico(String titulo, Chart chart) {
        graficos.put(titulo, chart);
    }

    /** @return snapshots dos graficos visiveis e com dados (deve ser chamado na thread FX) */
    public List<Map.Entry<String, BufferedImage>> snapshotsGraficos() {
        List<Map.Entry<String, BufferedImage>> r = new ArrayList<>();
        for (Map.Entry<String, Chart> e : graficos.entrySet()) {
            BufferedImage img = snapshot(e.getValue());
            if (img != null) r.add(Map.entry(e.getKey(), img));
        }
        return r;
    }

    /**
     * @param node no a capturar
     * @return imagem, ou {@code null} se o no ainda nao foi exibido
     */
    public static BufferedImage snapshot(Node node) {
        if (node == null || node.getScene() == null || node.getLayoutBounds().getWidth() < 10) return null;
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(javafx.scene.paint.Color.WHITE);
        WritableImage img = node.snapshot(sp, null);
        return SwingFXUtils.fromFXImage(img, null);
    }

    /**
     * Seleciona uma aba do dashboard.
     *
     * @param id fx:id da aba
     */
    public void selecionarAba(String id) {
        if (main != null) main.selecionarAba(id);
    }

    /**
     * Executa na thread da interface.
     *
     * @param r acao
     */
    public static void naInterface(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }

    void encerrar() {
        cancelar();
        executor.shutdownNow();
    }

    public Sessao sessao() { return sessao; }
    public Stage stage() { return stage; }
    public ObjectProperty<BaseDeFocos> baseProperty() { return base; }
    public ObjectProperty<br.unip.aps.ml.Preditor.ResultadoML> mlProperty() { return ml; }
    public StringProperty statusProperty() { return status; }
    public DoubleProperty progressoProperty() { return progresso; }
    public BooleanProperty ocupadoProperty() { return ocupado; }

    /** Trabalho que reporta progresso. */
    @FunctionalInterface
    public interface TrabalhoComProgresso<T> {
        /**
         * @param progresso reportador (fracao 0-1, mensagem)
         * @return resultado
         * @throws Exception qualquer falha (exibida em dialogo)
         */
        T executar(java.util.function.BiConsumer<Double, String> progresso) throws Exception;
    }
}
