package br.unip.aps.ui;

import br.unip.aps.ApsException;
import br.unip.aps.io.DataValidationException;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.report.CodigoFonteReport;
import br.unip.aps.report.ContextoRelatorio;
import br.unip.aps.util.Formatos;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller da janela principal: barra de ferramentas (carga, download, exportacoes), abas e
 * barra de status com progresso e cancelamento.
 */
public class MainController {

    private final UiContexto ctx;

    @FXML private TabPane abas;
    @FXML private Label lblBase;
    @FXML private Label lblStatus;
    @FXML private ProgressBar barraProgresso;
    @FXML private Button btnCancelar;

    /** @param ctx contexto injetado */
    public MainController(UiContexto ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        lblStatus.textProperty().bind(ctx.statusProperty());
        barraProgresso.progressProperty().bind(ctx.progressoProperty());
        barraProgresso.visibleProperty().bind(ctx.ocupadoProperty());
        btnCancelar.visibleProperty().bind(ctx.ocupadoProperty());
        ctx.baseProperty().addListener((o, a, b) -> atualizarCabecalho(b));
        atualizarCabecalho(null);
    }

    private void atualizarCabecalho(BaseDeFocos b) {
        if (b == null) {
            lblBase.setText("Nenhum dado carregado");
            return;
        }
        StringBuilder anos = new StringBuilder();
        for (Integer a : b.anos()) anos.append(anos.isEmpty() ? "" : ", ").append(a);
        lblBase.setText(Formatos.inteiro(b.tamanho()) + " focos | " + String.join(", ", b.estados()) + " | " + anos
                + " | " + b.getFontes().size() + " arquivo(s)");
    }

    /** Carga automatica ao abrir: se nao houver CSVs, oferece baixar do INPE ou abrir arquivos. */
    void carregarInicial() {
        Task<BaseDeFocos> t = ctx.executar("Carregando dados do INPE", () -> {
            try {
                return ctx.sessao().carregar();
            } catch (DataValidationException e) {
                Platform.runLater(() -> oferecerAlternativas(e.getMessage()));
                return null;
            }
        }, b -> {
            if (b != null) ctx.baseProperty().set(b);
        });
        if (t == null) ctx.statusProperty().set("Ocupado.");
    }

    private void oferecerAlternativas(String motivo) {
        ButtonType baixar = new ButtonType("Baixar do INPE");
        ButtonType abrir = new ButtonType("Abrir CSV...");
        ButtonType fechar = new ButtonType("Agora não", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert a = new Alert(Alert.AlertType.WARNING, motivo, baixar, abrir, fechar);
        a.initOwner(ctx.stage());
        a.setHeaderText("Dados de focos não encontrados");
        a.getDialogPane().setMinWidth(560);
        Optional<ButtonType> r = a.showAndWait();
        if (r.isEmpty()) return;
        if (r.get() == baixar) baixarInpe();
        else if (r.get() == abrir) abrirCsv();
    }

    @FXML
    private void abrirCsv() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecione os CSVs de focos do INPE (ex.: 2023 e 2024)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv", "*.CSV"));
        File dir = ctx.sessao().config().diretorioDados().toFile();
        if (dir.isDirectory()) fc.setInitialDirectory(dir);
        List<File> arquivos = fc.showOpenMultipleDialog(ctx.stage());
        if (arquivos == null || arquivos.isEmpty()) return;
        List<Path> paths = new ArrayList<>();
        for (File f : arquivos) paths.add(f.toPath());
        ctx.executar("Carregando " + paths.size() + " arquivo(s)", () -> ctx.sessao().carregar(paths), ctx.baseProperty()::set);
    }

    @FXML
    private void recarregar() {
        ctx.executar("Recarregando dados", () -> ctx.sessao().carregar(), ctx.baseProperty()::set);
    }

    @FXML
    private void baixarInpe() {
        TextInputDialog d = new TextInputDialog(ctx.sessao().config().uf());
        d.initOwner(ctx.stage());
        d.setTitle("Baixar dados do INPE");
        d.setHeaderText("Arquivos anuais do satélite de referência (EstadosBr_sat_ref)\nAnos: "
                + ctx.sessao().config().anos() + " — destino: " + ctx.sessao().config().diretorioDados().toAbsolutePath());
        d.setContentText("UF (sigla):");
        Optional<String> uf = d.showAndWait();
        if (uf.isEmpty()) return;
        ctx.executar("Baixando dados do INPE (" + uf.get().toUpperCase() + ")", () -> {
            ctx.sessao().baixarDoInpe(uf.get(), ctx.sessao().config().anos());
            return ctx.sessao().carregar();
        }, ctx.baseProperty()::set);
    }

    @FXML
    private void exportarExcel() {
        exportar("xlsx");
    }

    @FXML
    private void exportarPdf() {
        exportar("pdf");
    }

    private void exportar(String tipo) {
        ContextoRelatorio rel;
        try {
            rel = ctx.sessao().contextoRelatorio();
        } catch (ApsException e) {
            ctx.aviso("Nada para exportar", e.getMessage());
            return;
        }
        if (tipo.equals("pdf")) {
            for (Map.Entry<String, BufferedImage> g : ctx.snapshotsGraficos()) rel.grafico(g.getKey(), g.getValue());
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Salvar relatório");
        Path sugestao = ctx.sessao().arquivoRelatorio("relatorio", "." + tipo);
        File dir = sugestao.toAbsolutePath().getParent().toFile();
        dir.mkdirs();
        fc.setInitialDirectory(dir);
        fc.setInitialFileName(sugestao.getFileName().toString());
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(tipo.toUpperCase(), "*." + tipo));
        File f = fc.showSaveDialog(ctx.stage());
        if (f == null) return;
        final ContextoRelatorio r = rel;
        ctx.executar("Gerando relatório " + tipo.toUpperCase(), () -> tipo.equals("pdf")
                ? ctx.sessao().exporter().exportarPdf(r, f.toPath())
                : ctx.sessao().exporter().exportarExcel(r, f.toPath()), this::oferecerAbrir);
    }

    @FXML
    private void relatorioCodigo() {
        Path dir = ctx.sessao().config().diretorioRelatorios();
        ctx.executar("Gerando relatório com as linhas de código", () -> {
            new CodigoFonteReport().gerar(Path.of("").toAbsolutePath(), dir.resolve("codigo-fonte.pdf"), dir.resolve("codigo-fonte.txt"));
            return dir.resolve("codigo-fonte.pdf");
        }, this::oferecerAbrir);
    }

    private void oferecerAbrir(Path arquivo) {
        if (ctx.confirmar("Arquivo gerado", arquivo.toAbsolutePath() + "\n\nDeseja abrir o arquivo agora?")) {
            try {
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(arquivo.toFile());
            } catch (IOException | UnsupportedOperationException e) {
                ctx.aviso("Não foi possível abrir", "Abra manualmente: " + arquivo.toAbsolutePath());
            }
        }
    }

    @FXML
    private void cancelar() {
        ctx.cancelar();
    }

    @FXML
    private void sobre() {
        ctx.info("APS — Estrutura de Dados (UNIP)",
                """
                Sistema para Análise de Performance de Algoritmos de Ordenação aplicado aos focos de \
                queimadas detectados por satélite (INPE — Programa Queimadas).

                • 10 algoritmos implementados manualmente (Strategy + Factory)
                • Contagem de comparações, trocas, atribuições e acessos
                • Benchmark com aquecimento do JIT, repetições e expoente empírico
                • Machine Learning (Smile): Random Forest e DBSCAN/K-Means
                • Mapa Leaflet/OpenStreetMap, relatórios Excel/PDF

                Dados: dataserver-coids.inpe.br (EstadosBr_sat_ref).""");
    }

    void selecionarAba(String id) {
        for (Tab t : abas.getTabs()) {
            if (id.equals(t.getId())) abas.getSelectionModel().select(t);
        }
    }
}
