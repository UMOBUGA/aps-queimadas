package br.unip.aps.ui;

import br.unip.aps.ApsException;
import br.unip.aps.analysis.Estatisticas;
import br.unip.aps.io.DataValidationException;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.report.CodigoFonteReport;
import br.unip.aps.report.ContextoRelatorio;
import br.unip.aps.ui.componentes.Dialogos;
import br.unip.aps.ui.componentes.EmptyState;
import br.unip.aps.ui.componentes.FaixaTermica;
import br.unip.aps.ui.componentes.Feedback;
import br.unip.aps.ui.componentes.FilterBar;
import br.unip.aps.ui.componentes.Icones;
import br.unip.aps.ui.componentes.LoadingOverlay;
import br.unip.aps.ui.componentes.SidebarItem;
import br.unip.aps.ui.componentes.Toast;
import br.unip.aps.ui.tema.GerenciadorTema;
import br.unip.aps.util.Formatos;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Controller do shell da aplicacao. */
public class MainController {
    private final UiContexto ctx;

    @FXML private StackPane raiz;
    @FXML private BorderPane shell;
    @FXML private VBox sidebar;
    @FXML private VBox topo;
    @FXML private ProgressBar progressoTopo;
    @FXML private VBox filtroHost;
    @FXML private StackPane conteudo;
    @FXML private VBox toasts;

    private final Map<Pagina, Node> nos = new EnumMap<>(Pagina.class);
    private final Map<Pagina, Object> controllers = new EnumMap<>(Pagina.class);
    private final Map<Pagina, SidebarItem> itens = new EnumMap<>(Pagina.class);
    private final ToggleGroup grupoNav = new ToggleGroup();
    private final Label titulo = new Label();
    private final Label linhaDados = new Label();
    private final FaixaTermica faixa = new FaixaTermica();
    private final FilterBar filterBar = new FilterBar();
    private final Button btnTema = new Button();
    private final MenuButton exportar = new MenuButton("Exportar");
    private final List<Label> rotulosSecao = new ArrayList<>();
    private VBox marcaTextos;
    private Button btnRecolher;
    private EmptyState vazio;
    private Pagina exibida;

    public MainController(UiContexto ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        Feedback.instalar((tipo, t, x) -> Toast.mostrar(toasts, tipo, t, x));
        toasts.setAlignment(Pos.BOTTOM_RIGHT);
        montarSidebar();
        montarTopbar();
        filtroHost.getChildren().add(filterBar);
        filterBar.filtroProperty().addListener((o, a, n) -> ctx.filtroProperty().set(n));
        ctx.focosFiltradosProperty().addListener((o, a, n) -> filterBar.setContagem(n.size()));

        vazio = new EmptyState(Icones.SEM_DADOS, "Nenhum dado carregado",
                "Carregue os CSVs de focos de queimadas do INPE (satélite de referência) para começar. "
                        + "Os arquivos de SP 2023 e 2024 ficam em data/raw.", false,
                botao("Abrir CSV…", Icones.ABRIR, "btn-primary", this::abrirCsv),
                botao("Baixar do INPE", Icones.BAIXAR, "btn-secondary", this::baixarInpe));
        LoadingOverlay overlay = new LoadingOverlay(ctx.ocupadoProperty(), ctx.statusProperty(), ctx.progressoProperty(), ctx::cancelar);
        conteudo.getChildren().addAll(vazio, overlay);

        progressoTopo.progressProperty().bind(ctx.progressoProperty().map(p -> p.doubleValue() <= 0 ? -1.0 : p.doubleValue()));
        progressoTopo.visibleProperty().bind(ctx.ocupadoProperty());

        faixa.setOnSelecionar((de, ate) -> {
            if (exibida != null && !exibida.usaFiltro()) ctx.navegar(Pagina.VISAO_GERAL);
            filterBar.periodo(de, ate);
        });
        ctx.filtroProperty().addListener((o, a, f) -> faixa.setSelecao(f == null ? null : f.de(), f == null ? null : f.ate()));
        ctx.baseProperty().addListener((o, a, b) -> {
            filterBar.setOpcoes(b);
            if (b != null) faixa.setDados(new Estatisticas(b.getFocos()).serieMensal());
            preCalcularMl(b);
            atualizarChips(b);
            exibir(ctx.paginaProperty().get());
        });
        ctx.paginaProperty().addListener((o, a, n) -> exibir(n));
        GerenciadorTema.get().temaProperty().addListener((o, a, n) -> atualizarBotaoTema());
        GerenciadorTema.get().sidebarRecolhidaProperty().addListener((o, a, n) -> aplicarRecolhimento(n));
        aplicarRecolhimento(GerenciadorTema.get().sidebarRecolhidaProperty().get());
        atualizarBotaoTema();
        atualizarChips(null);
        raiz.sceneProperty().addListener((o, a, s) -> {
            if (s != null) registrarAtalhos(s);
        });
        br.unip.aps.ui.componentes.Layout.naoEncolher(topo);
        br.unip.aps.ui.componentes.Layout.naoEncolher(filtroHost);
        exibir(Pagina.VISAO_GERAL);
    }

    private void montarSidebar() {
        StackPane marca = new StackPane(Icones.de(Icones.MARCA, 26));
        marca.getStyleClass().add("brand-mark");
        Label nome = new Label("QUEIMADAS");
        nome.getStyleClass().add("brand-title");
        Label sub = new Label("Focos de incêndio · INPE");
        sub.getStyleClass().add("brand-subtitle");
        marcaTextos = new VBox(1, nome, sub);
        HBox brand = new HBox(10, marca, marcaTextos);
        brand.getStyleClass().add("sidebar-brand");
        brand.setAlignment(Pos.CENTER_LEFT);

        VBox nav = new VBox(2);
        Label sec = new Label("Análise");
        sec.getStyleClass().add("sidebar-section");
        rotulosSecao.add(sec);
        nav.getChildren().add(sec);
        VBox rodape = new VBox(2);
        int atalho = 1;
        for (Pagina p : Pagina.values()) {
            SidebarItem it = new SidebarItem(p.titulo(), p.icone(), p.rodape() ? null : "Ctrl+" + atalho++);
            it.setToggleGroup(grupoNav);
            it.setOnAction(e -> {
                ctx.navegar(p);
                it.setSelected(true);
            });
            itens.put(p, it);
            (p.rodape() ? rodape : nav).getChildren().add(it);
        }
        btnRecolher = new Button("Recolher menu", Icones.de(Icones.RECOLHER, 18));
        btnRecolher.getStyleClass().addAll("sidebar-item", "sidebar-toggle");
        btnRecolher.setMaxWidth(Double.MAX_VALUE);
        btnRecolher.setTooltip(new Tooltip("Recolher/expandir menu (Ctrl+B)"));
        btnRecolher.setOnAction(e -> GerenciadorTema.get().sidebarRecolhidaProperty().set(
                !GerenciadorTema.get().sidebarRecolhidaProperty().get()));
        rodape.getChildren().add(btnRecolher);

        Region esp = new Region();
        VBox.setVgrow(esp, Priority.ALWAYS);
        sidebar.getChildren().addAll(brand, nav, esp, rodape);
        sidebar.setPrefWidth(240);
    }

    private void preCalcularMl(BaseDeFocos b) {
        if (b == null || b.anos().size() < 2) return;
        List<Integer> anos = b.anos();
        var p = ctx.sessao().parametrosMl(anos.get(anos.size() - 2), anos.get(anos.size() - 1));
        ctx.executarEmSegundoPlano("Pré-cálculo do ML", () -> new br.unip.aps.ml.Preditor().executar(b.getFocos(), p), r -> {
            if (ctx.baseProperty().get() == b && ctx.mlProperty().get() == null) {
                ctx.sessao().setUltimoMl(r);
                ctx.mlProperty().set(r);
            }
        });
    }

    private void aplicarRecolhimento(boolean recolhida) {
        sidebar.setPrefWidth(recolhida ? 68 : 240);
        sidebar.setMinWidth(recolhida ? 68 : 240);
        sidebar.getStyleClass().remove("recolhida");
        if (recolhida) sidebar.getStyleClass().add("recolhida");
        marcaTextos.setVisible(!recolhida);
        marcaTextos.setManaged(!recolhida);
        rotulosSecao.forEach(l -> {
            l.setVisible(!recolhida);
            l.setManaged(!recolhida);
        });
        itens.values().forEach(i -> i.setRecolhido(recolhida));
        btnRecolher.setText(recolhida ? "Expandir menu" : "Recolher menu");
        btnRecolher.setGraphic(Icones.de(recolhida ? Icones.EXPANDIR : Icones.RECOLHER, 18));
        btnRecolher.setContentDisplay(recolhida ? javafx.scene.control.ContentDisplay.GRAPHIC_ONLY : javafx.scene.control.ContentDisplay.LEFT);
    }

    private void montarTopbar() {
        titulo.getStyleClass().add("page-title");
        titulo.setMinWidth(Region.USE_PREF_SIZE);
        linhaDados.getStyleClass().add("breadcrumb");
        VBox textos = new VBox(0, titulo, linhaDados);
        Region esp = new Region();
        HBox.setHgrow(esp, Priority.ALWAYS);

        Button abrir = iconeAcao(Icones.ABRIR, "Abrir CSV (Ctrl+O)", this::abrirCsv);
        Button recarregar = iconeAcao(Icones.RECARREGAR, "Recarregar dados (Ctrl+R)", this::recarregar);
        Button baixar = iconeAcao(Icones.BAIXAR, "Baixar do INPE", this::baixarInpe);
        btnTema.getStyleClass().add("btn-icon");
        btnTema.setOnAction(e -> GerenciadorTema.get().alternar());

        MenuItem excel = new MenuItem("Relatório Excel (.xlsx)", Icones.de(Icones.EXCEL, 16));
        excel.setOnAction(e -> exportarRelatorio("xlsx"));
        MenuItem pdf = new MenuItem("Relatório PDF (com gráficos)", Icones.de(Icones.PDF, 16));
        pdf.setOnAction(e -> exportarRelatorio("pdf"));
        MenuItem codigo = new MenuItem("Relatório com as linhas de código", Icones.de(Icones.CODIGO, 16));
        codigo.setOnAction(e -> relatorioCodigo());
        exportar.getItems().setAll(excel, pdf, codigo);
        exportar.setGraphic(Icones.de(Icones.EXPORTAR, 16));
        exportar.getStyleClass().add("btn-primary");
        exportar.setTooltip(new Tooltip("Exportar relatórios (Ctrl+E)"));
        exportar.setPopupSide(Side.BOTTOM);
        exportar.setMinWidth(Region.USE_PREF_SIZE);

        HBox acoes = new HBox(4, abrir, recarregar, baixar, btnTema, exportar);
        acoes.setAlignment(Pos.CENTER_RIGHT);
        acoes.setPadding(new javafx.geometry.Insets(0, 0, 10, 0));
        HBox barra = new HBox(12, textos, esp, acoes);
        barra.getStyleClass().add("topbar");
        barra.setAlignment(Pos.BOTTOM_LEFT);
        topo.getChildren().addAll(barra, faixa);
    }

    private Button iconeAcao(String icone, String dica, Runnable acao) {
        Button b = new Button(null, Icones.de(icone, 18));
        b.getStyleClass().add("btn-icon");
        b.setTooltip(new Tooltip(dica));
        b.setAccessibleText(dica);
        b.setOnAction(e -> acao.run());
        return b;
    }

    private static Button botao(String texto, String icone, String estilo, Runnable acao) {
        Button b = new Button(texto, Icones.de(icone, 16));
        b.getStyleClass().add(estilo);
        b.setOnAction(e -> acao.run());
        return b;
    }

    private void atualizarBotaoTema() {
        boolean escuro = GerenciadorTema.get().escuro();
        btnTema.setGraphic(Icones.de(escuro ? Icones.TEMA_CLARO : Icones.TEMA_ESCURO, 18));
        btnTema.setTooltip(new Tooltip(escuro ? "Usar tema claro (Ctrl+T)" : "Usar tema escuro (Ctrl+T)"));
        btnTema.setAccessibleText("Alternar tema");
    }

    private void atualizarChips(BaseDeFocos b) {
        if (b == null) {
            linhaDados.setText("Nenhum dado carregado");
            linhaDados.setTooltip(null);
            return;
        }
        List<Integer> anos = b.anos();
        String faixaAnos = anos.size() == 1 ? String.valueOf(anos.get(0)) : anos.get(0) + "–" + anos.get(anos.size() - 1);
        List<String> estados = new ArrayList<>();
        b.estados().forEach(e -> estados.add(VisaoGeralController.capitalizar(e)));
        linhaDados.setText(String.join(", ", estados) + "  ·  " + faixaAnos + "  ·  " + Formatos.inteiro(b.tamanho()) + " focos  ·  "
                + (ctx.sessao().dadosEmbarcados() ? "base de demonstração embarcada"
                : b.getFontes().size() + (b.getFontes().size() == 1 ? " arquivo" : " arquivos")));
        List<String> nomes = new ArrayList<>();
        b.getFontes().forEach(p -> nomes.add(p.getFileName().toString()));
        linhaDados.setTooltip(new Tooltip("Satélite de referência do INPE\n" + String.join("\n", nomes)));
    }

    private void exibir(Pagina p) {
        if (p == null) return;
        SidebarItem it = itens.get(p);
        if (it != null) it.setSelected(true);
        titulo.setText(p.titulo().toUpperCase(java.util.Locale.of("pt", "BR")));
        boolean semDados = ctx.baseProperty().get() == null && !p.rodape();
        boolean comFaixa = ctx.baseProperty().get() != null && !p.rodape();
        faixa.setVisible(comFaixa);
        faixa.setManaged(comFaixa);
        filterBar.setVisible(p.usaFiltro() && !semDados);
        filterBar.setManaged(p.usaFiltro() && !semDados);

        if (exibida != null && exibida != p) {
            Node antigo = nos.get(exibida);
            if (antigo != null) antigo.setVisible(false);
            if (controllers.get(exibida) instanceof Pagina.Controlador c) c.aoOcultar();
        }
        vazio.setVisible(semDados);
        Node no = semDados ? null : carregar(p);
        if (no != null) {
            no.setVisible(true);
            if (GerenciadorTema.get().animacoesProperty().get() && exibida != p) {
                no.setOpacity(0);
                FadeTransition f = new FadeTransition(Duration.millis(200), no);
                f.setToValue(1);
                f.play();
            }
            if (controllers.get(p) instanceof Pagina.Controlador c) c.aoExibir();
        }
        exibida = p;
    }

    private Node carregar(Pagina p) {
        Node n = nos.get(p);
        if (n != null) return n;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/unip/aps/ui/" + p.fxml()));
            loader.setControllerFactory(ctx::criarController);
            Parent raizPagina = loader.load();
            br.unip.aps.ui.componentes.Layout.naoEncolher(raizPagina);
            nos.put(p, raizPagina);
            controllers.put(p, loader.getController());
            conteudo.getChildren().add(conteudo.getChildren().size() - 1, raizPagina);
            return raizPagina;
        } catch (IOException e) {
            ctx.erro("Não foi possível abrir a tela " + p.titulo(), e);
            return null;
        }
    }

    Object controllerDe(Pagina p) {
        carregar(p);
        return controllers.get(p);
    }

    private void registrarAtalhos(Scene s) {
        s.getAccelerators().put(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN), this::abrirCsv);
        s.getAccelerators().put(new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN), this::recarregar);
        s.getAccelerators().put(new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN), () -> exportar.show());
        s.getAccelerators().put(new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN), () -> GerenciadorTema.get().alternar());
        s.getAccelerators().put(new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN),
                () -> GerenciadorTema.get().sidebarRecolhidaProperty().set(!GerenciadorTema.get().sidebarRecolhidaProperty().get()));
        s.getAccelerators().put(new KeyCodeCombination(KeyCode.F1), () -> ctx.navegar(Pagina.SOBRE));
        KeyCode[] numeros = {KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3, KeyCode.DIGIT4, KeyCode.DIGIT5, KeyCode.DIGIT6};
        Pagina[] ps = Pagina.values();
        for (int i = 0; i < numeros.length; i++) {
            Pagina p = ps[i];
            s.getAccelerators().put(new KeyCodeCombination(numeros[i], KeyCombination.SHORTCUT_DOWN), () -> ctx.navegar(p));
        }
    }

    void carregarInicial(Runnable aoTerminar) {
        String[] falha = new String[1];
        var t = ctx.executar("Carregando dados do INPE", () -> {
            try {
                return ctx.sessao().carregar();
            } catch (DataValidationException e) {
                falha[0] = e.getMessage();
                return null;
            }
        }, b -> {
            aoTerminar.run();
            if (b != null) {
                ctx.baseProperty().set(b);
                Feedback.sucesso("Dados carregados", Formatos.inteiro(b.tamanho()) + " focos de " + b.getFontes().size() + " arquivo(s)");
            } else if (falha[0] != null) {
                Platform.runLater(() -> oferecerAlternativas(falha[0]));
            }
        });
        if (t == null) aoTerminar.run();
        else t.setOnFailed(e -> aoTerminar.run());
    }

    private void oferecerAlternativas(String motivo) {
        int op = Dialogos.escolher(ctx.stage(), "Dados de focos não encontrados", motivo, "Baixar do INPE", "Abrir CSV…");
        if (op == 0) baixarInpe();
        else if (op == 1) abrirCsv();
    }

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
        ctx.executar("Carregando " + paths.size() + " arquivo(s)", () -> ctx.sessao().carregar(paths), b -> {
            ctx.baseProperty().set(b);
            Feedback.sucesso("Dados carregados", Formatos.inteiro(b.tamanho()) + " focos");
        });
    }

    private void recarregar() {
        ctx.executar("Recarregando dados", () -> ctx.sessao().carregar(), b -> {
            ctx.baseProperty().set(b);
            Feedback.sucesso("Dados recarregados", Formatos.inteiro(b.tamanho()) + " focos");
        });
    }

    private void baixarInpe() {
        TextInputDialog d = new TextInputDialog(ctx.sessao().config().uf());
        d.initOwner(ctx.stage());
        d.setTitle("Baixar dados do INPE");
        d.setHeaderText("Baixar focos do satélite de referência (EstadosBr_sat_ref)\nAnos "
                + ctx.sessao().config().anos() + " → " + ctx.sessao().config().diretorioDados().toAbsolutePath());
        d.setContentText("UF (sigla):");
        d.setGraphic(Icones.de(Icones.BAIXAR, 26));
        GerenciadorTema.get().aplicar(d.getDialogPane());
        d.getDialogPane().getStyleClass().add("app-dialog");
        Optional<String> uf = d.showAndWait();
        if (uf.isEmpty()) return;
        ctx.executar("Baixando dados do INPE (" + uf.get().toUpperCase() + ")", () -> {
            ctx.sessao().baixarDoInpe(uf.get(), ctx.sessao().config().anos());
            return ctx.sessao().carregar();
        }, b -> {
            ctx.baseProperty().set(b);
            Feedback.sucesso("Download concluído", Formatos.inteiro(b.tamanho()) + " focos carregados");
        });
    }

    private void exportarRelatorio(String tipo) {
        ContextoRelatorio rel;
        try {
            rel = ctx.sessao().contextoRelatorio();
        } catch (ApsException e) {
            ctx.aviso("Nada para exportar", e.getMessage());
            return;
        }
        FiltroGlobal f = ctx.filtroProperty().get();
        if (f != null && f.ativo()) rel.filtro(f.descricao(), ctx.focosFiltradosProperty().get());
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
        File arq = fc.showSaveDialog(ctx.stage());
        if (arq == null) return;
        final ContextoRelatorio r = rel;
        ctx.executar("Gerando relatório " + tipo.toUpperCase(), () -> tipo.equals("pdf")
                ? ctx.sessao().exporter().exportarPdf(r, arq.toPath())
                : ctx.sessao().exporter().exportarExcel(r, arq.toPath()), p -> concluidoExportacao("Relatório " + tipo.toUpperCase() + " exportado", p));
    }

    private void relatorioCodigo() {
        Path dir = ctx.sessao().config().diretorioRelatorios();
        ctx.executar("Gerando relatório com as linhas de código", () -> {
            new CodigoFonteReport().gerar(Path.of("").toAbsolutePath(), dir.resolve("codigo-fonte.pdf"), dir.resolve("codigo-fonte.txt"));
            return dir.resolve("codigo-fonte.pdf");
        }, p -> concluidoExportacao("Relatório de código gerado", p));
    }

    private void concluidoExportacao(String titulo, Path arquivo) {
        Feedback.sucesso(titulo, arquivo.toAbsolutePath().toString());
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(arquivo.toFile());
        } catch (IOException | UnsupportedOperationException e) {
            Feedback.info("Abra o arquivo manualmente", arquivo.toAbsolutePath().toString());
        }
    }

    Node noDe(Pagina p) {
        return nos.get(p);
    }

    FilterBar filterBar() {
        return filterBar;
    }

    StackPane raiz() {
        return raiz;
    }
}
