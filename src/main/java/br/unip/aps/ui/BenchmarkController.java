package br.unip.aps.ui;

import br.unip.aps.benchmark.AnaliseComplexidade;
import br.unip.aps.benchmark.BenchmarkConfig;
import br.unip.aps.benchmark.BenchmarkResult;
import br.unip.aps.benchmark.BenchmarkRunner;
import br.unip.aps.io.CsvParser;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.sorting.AlgoritmoTipo;
import br.unip.aps.sorting.CenarioEntrada;
import br.unip.aps.sorting.CriterioOrdenacao;
import br.unip.aps.sorting.Ordenacoes;
import br.unip.aps.ui.componentes.ChartCard;
import br.unip.aps.ui.componentes.Feedback;
import br.unip.aps.ui.componentes.Graficos;
import br.unip.aps.ui.componentes.Icones;
import br.unip.aps.util.Formatos;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tela Benchmark. */
public class BenchmarkController implements Pagina.Controlador {
    private static final Map<AlgoritmoTipo, String> SERIE = new EnumMap<>(Map.ofEntries(
            Map.entry(AlgoritmoTipo.BUBBLE, "serie-1"), Map.entry(AlgoritmoTipo.SELECTION, "serie-2"),
            Map.entry(AlgoritmoTipo.INSERTION, "serie-3"), Map.entry(AlgoritmoTipo.SHELL, "serie-1"),
            Map.entry(AlgoritmoTipo.MERGE, "serie-2"), Map.entry(AlgoritmoTipo.QUICK, "serie-3"),
            Map.entry(AlgoritmoTipo.QUICK_2PIVOS, "serie-3 variante-tracejada"), Map.entry(AlgoritmoTipo.INTRO, "serie-3 variante-pontilhada"),
            Map.entry(AlgoritmoTipo.QUICK_3WAY, "serie-4"), Map.entry(AlgoritmoTipo.HEAP, "serie-5"),
            Map.entry(AlgoritmoTipo.TIM, "serie-6"), Map.entry(AlgoritmoTipo.RADIX, "serie-7"),
            Map.entry(AlgoritmoTipo.COUNTING, "serie-7 variante-tracejada")));

    /** Linha da tabela de vencedores. */
    public record Vencedor(String criterio, String cenario, int n, String algoritmo, double mediaMs, String segundo, String ultimo) { }

    private final UiContexto ctx;

    @FXML private FlowPane fpAlgoritmos, fpCriterios, fpCenarios;
    @FXML private TextField tfTamanhos;
    @FXML private Spinner<Integer> spAquecimentos, spRepeticoes;
    @FXML private Button btnSalvos, btnRapida, btnExecutar;
    @FXML private HBox segCriterio, segCenario, segMetrica, segEscala;
    @FXML private ChartCard cQuadraticos, cLogLineares, cVencedores, cComplexidade;
    @FXML private TableView<BenchmarkResult> tabela;
    @FXML private Label lblOrigem;
    private boolean tentouEmbarcado;

    private final Map<AlgoritmoTipo, ToggleButton> tgAlgoritmos = new EnumMap<>(AlgoritmoTipo.class);
    private final Map<CriterioOrdenacao, ToggleButton> tgCriterios = new EnumMap<>(CriterioOrdenacao.class);
    private final Map<CenarioEntrada, ToggleButton> tgCenarios = new EnumMap<>(CenarioEntrada.class);
    private final ToggleGroup gCriterio = new ToggleGroup(), gCenario = new ToggleGroup(), gMetrica = new ToggleGroup(), gEscala = new ToggleGroup();
    private final LineChart<Number, Number> grafQuad = grafico();
    private final LineChart<Number, Number> grafLog = grafico();
    private final TableView<Vencedor> tabVencedores = new TableView<>();
    private final TableView<AnaliseComplexidade.Estimativa> tabComplexidade = new TableView<>();
    private List<BenchmarkResult> resultados = List.of();

    public BenchmarkController(UiContexto ctx) {
        this.ctx = ctx;
    }

    private static LineChart<Number, Number> grafico() {
        NumberAxis x = new NumberAxis();
        x.setLabel("n (tamanho da entrada)");
        x.setForceZeroInRange(false);
        NumberAxis y = new NumberAxis();
        y.setForceZeroInRange(false);
        LineChart<Number, Number> c = new LineChart<>(x, y);
        c.setLegendVisible(false);
        c.setAnimated(false);
        c.setCreateSymbols(true);
        c.setVerticalGridLinesVisible(false);
        Graficos.eixoLog(x, false);
        return c;
    }

    @FXML
    private void initialize() {
        for (AlgoritmoTipo t : AlgoritmoTipo.values()) {
            ToggleButton b = chip(t.nome() + (t.quadratico() ? "  · O(n²)" : ""), true);
            tgAlgoritmos.put(t, b);
            fpAlgoritmos.getChildren().add(b);
        }
        for (CriterioOrdenacao c : List.of(CriterioOrdenacao.DATA, CriterioOrdenacao.BIOMA, CriterioOrdenacao.MUNICIPIO, CriterioOrdenacao.LATITUDE)) {
            ToggleButton b = chip(c.rotulo(), c != CriterioOrdenacao.LATITUDE);
            tgCriterios.put(c, b);
            fpCriterios.getChildren().add(b);
        }
        for (CenarioEntrada c : CenarioEntrada.values()) {
            ToggleButton b = chip(c.toString(), c == CenarioEntrada.ALEATORIO || c == CenarioEntrada.ORDENADO || c == CenarioEntrada.INVERSO);
            tgCenarios.put(c, b);
            fpCenarios.getChildren().add(b);
        }
        spAquecimentos.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 20, 2));
        spRepeticoes.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 5));
        btnExecutar.setGraphic(Icones.de(Icones.EXECUTAR, 16));
        btnRapida.setGraphic(Icones.de(Icones.EXPERIMENTO, 16));
        btnSalvos.setGraphic(Icones.de(Icones.ABRIR, 16));

        segmento(segMetrica, gMetrica, "Tempo médio", "Comparações", "Trocas", "Acessos");
        segmento(segEscala, gEscala, "Linear", "Log");
        gMetrica.selectToggle(gMetrica.getToggles().get(1));
        gEscala.selectToggle(gEscala.getToggles().get(1));
        for (ToggleGroup g : List.of(gCriterio, gCenario, gMetrica, gEscala)) {
            g.selectedToggleProperty().addListener((o, a, n) -> {
                if (n == null && a != null) a.setSelected(true);
                else desenhar();
            });
        }

        cQuadraticos.conteudo(grafQuad);
        cLogLineares.conteudo(grafLog);
        cVencedores.conteudo(tabVencedores);
        cComplexidade.conteudo(tabComplexidade);
        for (ChartCard c : List.of(cQuadraticos, cLogLineares, cVencedores, cComplexidade)) c.estado(ChartCard.Estado.VAZIO);
        ctx.registrarGrafico("Benchmark: algoritmos O(n²)", cQuadraticos);
        ctx.registrarGrafico("Benchmark: algoritmos O(n log n) e lineares", cLogLineares);
        configurarTabelas();
        ctx.baseProperty().addListener((o, a, b) -> aplicar(List.of()));
    }

    private static ToggleButton chip(String texto, boolean selecionado) {
        ToggleButton b = new ToggleButton(texto);
        b.getStyleClass().add("chip-toggle");
        b.setSelected(selecionado);
        return b;
    }

    private static void segmento(HBox caixa, ToggleGroup g, String... rotulos) {
        caixa.getChildren().clear();
        for (String r : rotulos) {
            ToggleButton t = new ToggleButton(r);
            t.setUserData(r);
            t.setToggleGroup(g);
            caixa.getChildren().add(t);
        }
    }

    @FXML
    private void configRapida() {
        tfTamanhos.setText("100, 500, 1000, 2000");
        spAquecimentos.getValueFactory().setValue(1);
        spRepeticoes.getValueFactory().setValue(3);
        Feedback.info("Configuração rápida aplicada", "Tamanhos até 2.000, 1 aquecimento e 3 repetições (alguns segundos).");
    }

    private BenchmarkConfig config() {
        List<AlgoritmoTipo> algs = new ArrayList<>();
        tgAlgoritmos.forEach((t, b) -> { if (b.isSelected()) algs.add(t); });
        List<CriterioOrdenacao> crits = new ArrayList<>();
        tgCriterios.forEach((t, b) -> { if (b.isSelected()) crits.add(t); });
        List<CenarioEntrada> cens = new ArrayList<>();
        tgCenarios.forEach((t, b) -> { if (b.isSelected()) cens.add(t); });
        List<Integer> tamanhos = new ArrayList<>();
        for (String p : tfTamanhos.getText().split("[,;\\s]+")) {
            if (p.isBlank()) continue;
            if (p.strip().equalsIgnoreCase("todos") || p.strip().equalsIgnoreCase("base")) {
                tamanhos.add(0);
                continue;
            }
            try {
                int v = Integer.parseInt(p.replace(".", "").strip());
                if (v < 0) throw new NumberFormatException();
                tamanhos.add(v);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("O tamanho '" + p + "' não é válido. Digite números inteiros separados por vírgula, "
                        + "ou todos para usar a base inteira. Exemplo: 100, 1000, todos");
            }
        }
        return new BenchmarkConfig(algs, crits, tamanhos, cens, spAquecimentos.getValue(), spRepeticoes.getValue(), 42L, Integer.MAX_VALUE);
    }

    @FXML
    private void executar() {
        BaseDeFocos base = ctx.baseProperty().get();
        if (base == null) {
            Feedback.alerta("Sem dados", "Carregue os CSVs do INPE antes de executar o benchmark.");
            return;
        }
        BenchmarkConfig cfg;
        try {
            cfg = config();
        } catch (IllegalArgumentException e) {
            ctx.erro("Configuração inválida", e);
            return;
        }
        int maior = 0;
        for (int t : cfg.tamanhos()) maior = Math.max(maior, t <= 0 ? base.tamanho() : Math.min(t, base.tamanho()));
        boolean quad = cfg.algoritmos().stream().anyMatch(AlgoritmoTipo::quadratico);
        if (quad && maior > 5_000 && !ctx.confirmar("Benchmark demorado",
                cfg.totalCasos() + " casos com algoritmos O(n²) até n = " + Formatos.inteiro(maior)
                        + " podem levar vários minutos (a bateria completa leva ~20 min). É possível cancelar a qualquer momento.")) {
            return;
        }
        ctx.executar("Benchmark (" + cfg.totalCasos() + " casos)",
                prog -> new BenchmarkRunner().executar(base.getFocos(), cfg, p -> prog.accept(p.fracao(), p.mensagem())),
                r -> {
                    ctx.sessao().setUltimoBenchmark(r);
                    aplicar(r);
                    origem("Medições feitas agora, ao vivo, nesta máquina (" + Formatos.inteiro(r.size()) + " casos).");
                    Feedback.sucesso("Benchmark concluído", Formatos.inteiro(r.size()) + " medições");
                });
    }

    @FXML
    private void abrirSalvos() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Abrir resultados de benchmark (CSV)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV do benchmark", "*.csv"));
        File dir = Path.of("docs", "resultados").toFile();
        if (dir.isDirectory()) fc.setInitialDirectory(dir);
        File f = fc.showOpenDialog(ctx.stage());
        if (f != null) carregarCsv(f.toPath());
    }

    @Override
    public void aoExibir() {
        if (tentouEmbarcado || ctx.sessao().ultimoBenchmark() != null) return;
        tentouEmbarcado = true;
        try (java.io.InputStream in = getClass().getResourceAsStream("/resultados/benchmark.csv")) {
            if (in == null) return;
            carregarLinhas(new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList(), null);
        } catch (IOException e) {
            java.util.logging.Logger.getLogger(getClass().getName()).warning("Resultados embarcados indisponíveis: " + e.getMessage());
        }
    }

    private void origem(String texto) {
        lblOrigem.setText(texto);
        lblOrigem.setVisible(true);
        lblOrigem.setManaged(true);
    }

    void carregarCsv(Path arquivo) {
        try {
            carregarLinhas(Files.readAllLines(arquivo, StandardCharsets.UTF_8), arquivo.getFileName().toString());
        } catch (IOException e) {
            ctx.erro("Não foi possível abrir os resultados", new IllegalArgumentException(
                    "Arquivo inválido ou em outro formato: " + e.getMessage(), e));
        }
    }

    private void carregarLinhas(List<String> linhas, String arquivo) {
        try {
            CsvParser p = new CsvParser(';');
            List<BenchmarkResult> r = new ArrayList<>();
            for (int i = 1; i < linhas.size(); i++) {
                List<String> c = p.dividir(linhas.get(i));
                if (c.size() < 16) continue;
                r.add(new BenchmarkResult(c.get(0), criterio(c.get(1)), cenario(c.get(2)), Integer.parseInt(c.get(3)),
                        Integer.parseInt(c.get(4)), dec(c.get(5)) * 1e6, dec(c.get(6)) * 1e6, (long) (dec(c.get(7)) * 1e6),
                        (long) (dec(c.get(8)) * 1e6), Long.parseLong(c.get(9)), Long.parseLong(c.get(10)), Long.parseLong(c.get(11)),
                        Long.parseLong(c.get(12)), c.get(15).startsWith("s")));
            }
            if (r.isEmpty()) throw new IllegalArgumentException("O arquivo não contém medições no formato do benchmark.");
            ctx.sessao().setUltimoBenchmark(r);
            aplicar(r);
            if (arquivo == null) {
                origem("Resultados pré-calculados: bateria completa gravada em docs/resultados (" + Formatos.inteiro(r.size())
                        + " medições). Para medir de novo nesta máquina, clique em Executar benchmark.");
            } else {
                origem("Resultados carregados de " + arquivo + " (" + Formatos.inteiro(r.size()) + " medições).");
                Feedback.sucesso("Resultados carregados", Formatos.inteiro(r.size()) + " medições de " + arquivo);
            }
        } catch (RuntimeException e) {
            ctx.erro("Não foi possível abrir os resultados", new IllegalArgumentException(
                    "Arquivo inválido ou em outro formato: " + e.getMessage(), e));
        }
    }

    private static double dec(String s) {
        return Double.parseDouble(s.replace(".", "").replace(',', '.'));
    }

    private static CriterioOrdenacao criterio(String rotulo) {
        for (CriterioOrdenacao c : CriterioOrdenacao.values()) if (c.rotulo().equals(rotulo)) return c;
        throw new IllegalArgumentException("critério desconhecido: " + rotulo);
    }

    private static CenarioEntrada cenario(String rotulo) {
        for (CenarioEntrada c : CenarioEntrada.values()) if (c.toString().equals(rotulo)) return c;
        throw new IllegalArgumentException("cenário desconhecido: " + rotulo);
    }

    private void aplicar(List<BenchmarkResult> r) {
        resultados = r;
        tabela.setItems(FXCollections.observableArrayList(r));
        List<String> crits = new ArrayList<>(), cens = new ArrayList<>();
        for (BenchmarkResult b : r) {
            if (!crits.contains(b.criterio().rotulo())) crits.add(b.criterio().rotulo());
            if (!cens.contains(b.cenario().toString())) cens.add(b.cenario().toString());
        }
        segmento(segCriterio, gCriterio, crits.toArray(String[]::new));
        segmento(segCenario, gCenario, cens.toArray(String[]::new));
        if (!gCriterio.getToggles().isEmpty()) gCriterio.selectToggle(gCriterio.getToggles().get(0));
        if (!gCenario.getToggles().isEmpty()) gCenario.selectToggle(gCenario.getToggles().get(0));
        tabVencedores.setItems(FXCollections.observableArrayList(vencedores(r)));
        ChartCard.Estado e = r.isEmpty() ? ChartCard.Estado.VAZIO : ChartCard.Estado.CONTEUDO;
        cVencedores.estado(e);
        cComplexidade.estado(e);
        desenhar();
    }

    private static List<Vencedor> vencedores(List<BenchmarkResult> r) {
        Map<String, List<BenchmarkResult>> g = new LinkedHashMap<>();
        for (BenchmarkResult b : r) g.computeIfAbsent(b.criterio().rotulo() + "|" + b.cenario(), k -> new ArrayList<>()).add(b);
        List<Vencedor> v = new ArrayList<>();
        for (List<BenchmarkResult> lista : g.values()) {
            int nmax = 0;
            for (BenchmarkResult b : lista) nmax = Math.max(nmax, b.n());
            List<BenchmarkResult> maiores = new ArrayList<>();
            for (BenchmarkResult b : lista) if (b.n() == nmax) maiores.add(b);
            Ordenacoes.ordenar(maiores, Comparator.comparingDouble(BenchmarkResult::mediaNs));
            BenchmarkResult w = maiores.get(0);
            v.add(new Vencedor(w.criterio().rotulo(), w.cenario().toString(), nmax, w.algoritmo(), w.mediaMs(),
                    maiores.size() > 1 ? maiores.get(1).algoritmo() : "—", maiores.get(maiores.size() - 1).algoritmo()));
        }
        return v;
    }

    private void desenhar() {
        if (resultados.isEmpty() || gCriterio.getSelectedToggle() == null || gCenario.getSelectedToggle() == null) {
            grafQuad.getData().clear();
            grafLog.getData().clear();
            cQuadraticos.estado(ChartCard.Estado.VAZIO);
            cLogLineares.estado(ChartCard.Estado.VAZIO);
            return;
        }
        String crit = (String) gCriterio.getSelectedToggle().getUserData();
        String cen = (String) gCenario.getSelectedToggle().getUserData();
        String metrica = (String) gMetrica.getSelectedToggle().getUserData();
        boolean log = "Log".equals(gEscala.getSelectedToggle().getUserData());
        List<AnaliseComplexidade.Estimativa> est = new ArrayList<>();
        for (AnaliseComplexidade.Estimativa e : AnaliseComplexidade.estimar(resultados)) {
            if (e.criterio().rotulo().equals(crit) && e.cenario().toString().equals(cen)) est.add(e);
        }
        tabComplexidade.setItems(FXCollections.observableArrayList(est));
        cComplexidade.setSubtitulo(crit + " · " + cen + " · custo ≈ c·nᵏ: k≈2 → O(n²); k≈1,0–1,2 → O(n log n)");
        desenhar(grafQuad, cQuadraticos, true, crit, cen, metrica, log);
        desenhar(grafLog, cLogLineares, false, crit, cen, metrica, log);
    }

    private void desenhar(LineChart<Number, Number> g, ChartCard card, boolean quadraticos, String crit, String cen,
                          String metrica, boolean log) {
        g.getData().clear();
        card.limparLegenda();
        Map<String, XYChart.Series<Number, Number>> series = new LinkedHashMap<>();
        Map<String, String> classes = new LinkedHashMap<>();
        List<Integer> ns = new ArrayList<>();
        List<String[]> linhas = new ArrayList<>();
        for (AlgoritmoTipo t : AlgoritmoTipo.values()) {
            if (t.quadratico() != quadraticos) continue;
            for (BenchmarkResult r : resultados) {
                if (!r.algoritmo().equals(t.nome()) || !r.criterio().rotulo().equals(crit) || !r.cenario().toString().equals(cen)) continue;
                double v = valor(r, metrica);
                if (log && v <= 0) continue;
                XYChart.Series<Number, Number> s = series.computeIfAbsent(t.nome(), k -> {
                    XYChart.Series<Number, Number> nova = new XYChart.Series<>();
                    nova.setName(k);
                    return nova;
                });
                classes.put(t.nome(), SERIE.get(t));
                if (log && r.n() <= 0) continue;
                s.getData().add(new XYChart.Data<>(log ? Math.log10(r.n()) : r.n(), log ? Math.log10(v) : v));
                if (!ns.contains(r.n())) ns.add(r.n());
                linhas.add(new String[]{t.nome(), Formatos.inteiro(r.n()), formatar(metrica, v)});
            }
        }
        Ordenacoes.ordenar(ns, Comparator.naturalOrder());
        for (Map.Entry<String, XYChart.Series<Number, Number>> e : series.entrySet()) {
            g.getData().add(e.getValue());
            String[] estilo = classes.get(e.getKey()).split(" ");
            for (String c : estilo) Graficos.classe(e.getValue(), c);
            String[] marca = java.util.Arrays.copyOf(estilo, estilo.length + 1);
            marca[estilo.length] = "linha";
            card.adicionarLegenda(e.getKey(), marca);
            Graficos.tooltips(e.getValue(), d -> e.getKey() + "\nn = " + Formatos.inteiro(Math.round(log ? Math.pow(10, d.getXValue().doubleValue()) : d.getXValue().doubleValue())) + "\n"
                    + metrica + ": " + formatar(metrica, log ? Math.pow(10, d.getYValue().doubleValue()) : d.getYValue().doubleValue()));
        }
        boolean teorica = !metrica.startsWith("Tempo") && !ns.isEmpty();
        if (teorica) {
            XYChart.Series<Number, Number> t = new XYChart.Series<>();
            t.setName(quadraticos ? "n²/2 (teórico)" : "n·log₂n (teórico)");
            for (int n : ns) {
                double v = quadraticos ? n * (n - 1) / 2.0 : (n <= 1 ? 0 : n * Math.log(n) / Math.log(2));
                if (!log || v > 0) t.getData().add(new XYChart.Data<>(log ? Math.log10(n) : n, log ? Math.log10(v) : v));
            }
            g.getData().add(t);
            Graficos.classe(t, "serie-teorica");
            card.adicionarLegenda(t.getName(), "tracejada");
        }
        NumberAxis y = (NumberAxis) g.getYAxis();
        NumberAxis x = (NumberAxis) g.getXAxis();
        x.setLabel(log ? "n (escala log: a inclinação da reta é o expoente k)" : "n (tamanho da entrada)");
        Graficos.eixoLog(x, log);
        x.setAutoRanging(true);
        if (log) x.setTickUnit(1);
        y.setLabel(metrica + (log ? " (escala log)" : ""));
        Graficos.eixoLog(y, log);
        if (log) {
            y.setAutoRanging(true);
            y.setTickUnit(1);
        }
        card.setDados(new String[]{"Algoritmo", "n", metrica}, () -> linhas);
        card.estado(series.isEmpty() ? ChartCard.Estado.VAZIO : ChartCard.Estado.CONTEUDO);
    }

    private static double valor(BenchmarkResult r, String metrica) {
        return switch (metrica) {
            case "Tempo médio" -> r.mediaMs();
            case "Trocas" -> r.trocas();
            case "Acessos" -> r.acessos();
            default -> r.comparacoes();
        };
    }

    private static String formatar(String metrica, double v) {
        return metrica.startsWith("Tempo") ? Formatos.decimal(v, 3) + " ms" : Formatos.inteiro(Math.round(v));
    }

    private void configurarTabelas() {
        Tabelas.preparar(tabela, "Execute o benchmark ou abra resultados salvos.");
        tabela.getColumns().add(Tabelas.coluna("Algoritmo", BenchmarkResult::algoritmo, 160));
        tabela.getColumns().add(Tabelas.coluna("Critério", (BenchmarkResult r) -> r.criterio().rotulo(), 100));
        tabela.getColumns().add(Tabelas.coluna("Cenário", (BenchmarkResult r) -> r.cenario().toString(), 150));
        tabela.getColumns().add(Tabelas.numero("n", BenchmarkResult::n, v -> Formatos.inteiro(v), 80));
        tabela.getColumns().add(Tabelas.numero("Média (ms)", BenchmarkResult::mediaMs, v -> Formatos.decimal(v, 3), 100));
        tabela.getColumns().add(Tabelas.numero("Desvio (ms)", BenchmarkResult::desvioMs, v -> Formatos.decimal(v, 3), 100));
        tabela.getColumns().add(Tabelas.numero("Comparações", BenchmarkResult::comparacoes, Formatos::inteiro, 120));
        tabela.getColumns().add(Tabelas.numero("Trocas", BenchmarkResult::trocas, Formatos::inteiro, 110));
        tabela.getColumns().add(Tabelas.numero("Acessos", BenchmarkResult::acessos, Formatos::inteiro, 120));
        tabela.getColumns().add(Tabelas.coluna("OK", (BenchmarkResult r) -> r.verificado() ? "✔" : "✘", 50));

        Tabelas.preparar(tabVencedores, "Sem medições.");
        tabVencedores.getStyleClass().add("data-table");
        tabVencedores.getColumns().add(Tabelas.coluna("Critério", Vencedor::criterio, 90));
        tabVencedores.getColumns().add(Tabelas.coluna("Cenário", Vencedor::cenario, 185));
        tabVencedores.getColumns().add(Tabelas.coluna("★ Vencedor", v -> "★ " + v.algoritmo(), 160));
        tabVencedores.getColumns().add(Tabelas.numero("Tempo (ms)", Vencedor::mediaMs, v -> Formatos.decimal(v, 3), 90));
        tabVencedores.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Vencedor v, boolean vazio) {
                super.updateItem(v, vazio);
                setTooltip(vazio || v == null ? null : new javafx.scene.control.Tooltip(
                        "n = " + Formatos.inteiro(v.n()) + " · 2º lugar: " + v.segundo() + " · mais lento: " + v.ultimo()));
            }
        });

        Tabelas.preparar(tabComplexidade, "Sem medições (são necessários ao menos 3 tamanhos).");
        tabComplexidade.getColumns().add(Tabelas.coluna("Algoritmo", AnaliseComplexidade.Estimativa::algoritmo, 170));
        tabComplexidade.getColumns().add(Tabelas.numero("k comparações", AnaliseComplexidade.Estimativa::expoenteComparacoes,
                v -> Double.isNaN(v) ? "0 comp." : Formatos.decimal(v, 2), 110));
        tabComplexidade.getColumns().add(Tabelas.numero("k tempo", AnaliseComplexidade.Estimativa::expoenteTempo, v -> Formatos.decimal(v, 2), 80));
        tabComplexidade.getColumns().add(Tabelas.coluna("Classe estimada", AnaliseComplexidade.Estimativa::classificacao, 120));
    }

    @Override
    public void demonstrar(Runnable concluido) {
        aoExibir();
        concluido.run();
    }
}
