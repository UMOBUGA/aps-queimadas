package br.unip.aps.ui;

import br.unip.aps.ApsException;
import br.unip.aps.app.Sessao;
import br.unip.aps.ml.Preditor;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.ui.componentes.Dialogos;
import br.unip.aps.ui.componentes.Feedback;
import br.unip.aps.ui.tema.GerenciadorTema;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contexto compartilhado pelos controllers do dashboard: sessao (modelo), estado observavel
 * (base carregada, filtro global, lista filtrada, resultado de ML, tela atual), execucao de
 * tarefas em background com progresso e cancelamento, dialogos estilizados e toasts.
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
    private final ObjectProperty<Preditor.ResultadoML> ml = new SimpleObjectProperty<>();
    private final ObjectProperty<FiltroGlobal> filtro = new SimpleObjectProperty<>(FiltroGlobal.VAZIO);
    private final ReadOnlyObjectWrapper<List<FocoIncendio>> focosFiltrados = new ReadOnlyObjectWrapper<>(List.of());
    private final ObjectProperty<Pagina> pagina = new SimpleObjectProperty<>(Pagina.VISAO_GERAL);
    private final StringProperty status = new SimpleStringProperty("Pronto.");
    private final DoubleProperty progresso = new SimpleDoubleProperty(0);
    private final BooleanProperty ocupado = new SimpleBooleanProperty(false);
    private final Map<String, Node> graficos = new LinkedHashMap<>();
    private Task<?> tarefaAtual;
    private MainController main;

    UiContexto(Sessao sessao, Stage stage) {
        this.sessao = sessao;
        this.stage = stage;
        base.addListener((o, a, n) -> recalcularFiltrados());
        filtro.addListener((o, a, n) -> recalcularFiltrados());
    }

    private void recalcularFiltrados() {
        BaseDeFocos b = base.get();
        FiltroGlobal f = filtro.get();
        if (b == null) {
            focosFiltrados.set(List.of());
        } else if (f == null || !f.ativo()) {
            focosFiltrados.set(b.getFocos());
        } else {
            focosFiltrados.set(List.copyOf(b.filtrar(f)));
        }
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
     * Executa uma tarefa em background, atualizando status, progresso e overlay.
     *
     * @param descricao  texto exibido no status/overlay
     * @param trabalho   trabalho (fora da thread da interface)
     * @param aoConcluir callback na thread da interface com o resultado
     * @param <T>        tipo do resultado
     * @return tarefa criada, ou {@code null} se ja houver outra em andamento
     */
    public <T> Task<T> executar(String descricao, Callable<T> trabalho, Consumer<T> aoConcluir) {
        return executar(descricao, p -> trabalho.call(), aoConcluir);
    }

    /**
     * Variante em que o trabalho recebe um reportador de progresso (0 a 1, mensagem).
     *
     * @param descricao  texto
     * @param trabalho   trabalho com progresso
     * @param aoConcluir callback com o resultado
     * @param <T>        tipo
     * @return tarefa, ou {@code null} se ocupado
     */
    public <T> Task<T> executar(String descricao, TrabalhoComProgresso<T> trabalho, Consumer<T> aoConcluir) {
        if (ocupado.get()) {
            Feedback.alerta("Aguarde a operação atual", status.get());
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
        status.set(descricao + "…");
        long t0 = System.nanoTime();
        task.setOnSucceeded(e -> {
            finalizar();
            status.set(descricao + " — concluído em " + br.unip.aps.util.Formatos.duracao(System.nanoTime() - t0));
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
                Feedback.info("Operação cancelada", descricao);
            } else {
                status.set(descricao + " — falhou.");
                erro(descricao, ex);
            }
        });
        task.setOnCancelled(e -> {
            finalizar();
            status.set(descricao + " — cancelado pelo usuário.");
            Feedback.info("Operação cancelada", descricao);
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

    /** Dispara a carga inicial dos dados (feita pela janela principal). */
    void carregarDadosIniciais(Runnable aoTerminar) {
        if (main != null) main.carregarInicial(aoTerminar);
        else aoTerminar.run();
    }

    /**
     * Mostra erro amigavel; excecoes inesperadas ganham detalhes tecnicos expansiveis.
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
        String detalhes = null;
        if (!esperado) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            detalhes = sw.toString();
        }
        Dialogos.erro(stage, titulo, esperado ? causa.getMessage()
                : "Ocorreu um erro inesperado: " + causa + "\nOs detalhes foram gravados em logs/aps-0.log.", detalhes);
    }

    /**
     * @param titulo cabecalho
     * @param msg    mensagem
     */
    public void aviso(String titulo, String msg) {
        Dialogos.aviso(stage, titulo, msg);
    }

    /**
     * @param titulo cabecalho
     * @param msg    mensagem
     */
    public void info(String titulo, String msg) {
        Dialogos.info(stage, titulo, msg);
    }

    /**
     * @param titulo cabecalho
     * @param msg    pergunta
     * @return {@code true} se o usuario confirmar
     */
    public boolean confirmar(String titulo, String msg) {
        return Dialogos.confirmar(stage, titulo, msg, "Continuar");
    }

    /**
     * Registra um grafico para inclusao (snapshot) nos relatorios PDF.
     *
     * @param titulo legenda
     * @param no     no do grafico (ou card)
     */
    public void registrarGrafico(String titulo, Node no) {
        graficos.put(titulo, no);
    }

    /** @return snapshots dos graficos ja exibidos e com dados (thread FX) */
    public List<Map.Entry<String, BufferedImage>> snapshotsGraficos() {
        List<Map.Entry<String, BufferedImage>> r = new ArrayList<>();
        for (Map.Entry<String, Node> e : graficos.entrySet()) {
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
     * Navega para uma tela.
     *
     * @param p tela
     */
    public void navegar(Pagina p) {
        pagina.set(p);
    }

    /**
     * Compatibilidade: seleciona tela pelo identificador antigo das abas.
     *
     * @param id "mapa", "ml"...
     */
    public void selecionarAba(String id) {
        for (Pagina p : Pagina.values()) if (p.name().equalsIgnoreCase(id)) navegar(p);
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
    public GerenciadorTema tema() { return GerenciadorTema.get(); }
    public ObjectProperty<BaseDeFocos> baseProperty() { return base; }
    public ObjectProperty<Preditor.ResultadoML> mlProperty() { return ml; }
    public ObjectProperty<FiltroGlobal> filtroProperty() { return filtro; }
    /** @return focos da base apos o filtro global (lista imutavel) */
    public ReadOnlyObjectProperty<List<FocoIncendio>> focosFiltradosProperty() { return focosFiltrados.getReadOnlyProperty(); }
    public ObjectProperty<Pagina> paginaProperty() { return pagina; }
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
        T executar(BiConsumer<Double, String> progresso) throws Exception;
    }
}
