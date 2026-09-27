package br.unip.aps.ui;

import br.unip.aps.benchmark.AnaliseComplexidade;
import br.unip.aps.benchmark.BenchmarkConfig;
import br.unip.aps.benchmark.BenchmarkResult;
import br.unip.aps.benchmark.BenchmarkRunner;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.sorting.AlgoritmoTipo;
import br.unip.aps.sorting.CenarioEntrada;
import br.unip.aps.sorting.CriterioOrdenacao;
import br.unip.aps.sorting.Ordenacoes;
import br.unip.aps.util.Formatos;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aba "Benchmark": configura e executa a bateria de medicoes e exibe custo x n por algoritmo,
 * com curvas teoricas n²/2 e n·log2(n) e a estimativa empirica do expoente de crescimento.
 */
public class BenchmarkController {

    private final UiContexto ctx;

    @FXML private VBox boxAlgoritmos, boxCriterios, boxCenarios;
    @FXML private TextField tfTamanhos;
    @FXML private Spinner<Integer> spAquecimentos, spRepeticoes;
    @FXML private ComboBox<CriterioOrdenacao> cbCriterio;
    @FXML private ComboBox<CenarioEntrada> cbCenario;
    @FXML private ComboBox<String> cbMetrica;
    @FXML private CheckBox chkLog, chkTeoricas;
    @FXML private LineChart<Number, Number> grafico;
    @FXML private NumberAxis eixoY;
    @FXML private TableView<BenchmarkResult> tabela;
    @FXML private TableView<AnaliseComplexidade.Estimativa> tabelaComplexidade;

    private final Map<AlgoritmoTipo, CheckBox> chkAlgoritmos = new LinkedHashMap<>();
    private final Map<CriterioOrdenacao, CheckBox> chkCriterios = new LinkedHashMap<>();
    private final Map<CenarioEntrada, CheckBox> chkCenarios = new LinkedHashMap<>();
    private List<BenchmarkResult> resultados = List.of();

    /** @param ctx contexto injetado */
    public BenchmarkController(UiContexto ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        for (AlgoritmoTipo t : AlgoritmoTipo.values()) {
            CheckBox c = new CheckBox(t.nome() + (t.quadratico() ? "  (O(n²))" : ""));
            c.setSelected(true);
            chkAlgoritmos.put(t, c);
            boxAlgoritmos.getChildren().add(c);
        }
        for (CriterioOrdenacao c : List.of(CriterioOrdenacao.DATA, CriterioOrdenacao.BIOMA, CriterioOrdenacao.MUNICIPIO,
                CriterioOrdenacao.LATITUDE)) {
            CheckBox cb = new CheckBox(c.rotulo());
            cb.setSelected(c != CriterioOrdenacao.LATITUDE);
            chkCriterios.put(c, cb);
            boxCriterios.getChildren().add(cb);
        }
        for (CenarioEntrada c : CenarioEntrada.values()) {
            CheckBox cb = new CheckBox(c.toString());
            cb.setSelected(c == CenarioEntrada.ALEATORIO || c == CenarioEntrada.ORDENADO || c == CenarioEntrada.INVERSO);
            chkCenarios.put(c, cb);
            boxCenarios.getChildren().add(cb);
        }
        spAquecimentos.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 20, 2));
        spRepeticoes.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 5));
        cbMetrica.setItems(FXCollections.observableArrayList("Tempo médio (ms)", "Tempo mínimo (ms)", "Comparações", "Trocas", "Acessos"));
        cbMetrica.getSelectionModel().select("Comparações");
        cbCriterio.valueProperty().addListener((o, a, n) -> desenhar());
        cbCenario.valueProperty().addListener((o, a, n) -> desenhar());
        cbMetrica.valueProperty().addListener((o, a, n) -> desenhar());
        chkLog.selectedProperty().addListener((o, a, n) -> desenhar());
        chkTeoricas.selectedProperty().addListener((o, a, n) -> desenhar());
        configurarTabelas();
        ctx.registrarGrafico("Benchmark: custo x n", grafico);
        ctx.baseProperty().addListener((o, a, b) -> {
            resultados = List.of();
            tabela.getItems().clear();
            tabelaComplexidade.getItems().clear();
            grafico.getData().clear();
        });
    }

    private void configurarTabelas() {
        tabela.getColumns().add(Tabelas.coluna("Algoritmo", BenchmarkResult::algoritmo, 160));
        tabela.getColumns().add(Tabelas.coluna("Critério", (BenchmarkResult r) -> r.criterio().rotulo(), 90));
        tabela.getColumns().add(Tabelas.coluna("Cenário", (BenchmarkResult r) -> r.cenario().toString(), 150));
        tabela.getColumns().add(Tabelas.numero("n", BenchmarkResult::n, v -> Formatos.inteiro(v), 70));
        tabela.getColumns().add(Tabelas.numero("Média (ms)", BenchmarkResult::mediaMs, v -> Formatos.decimal(v, 3), 90));
        tabela.getColumns().add(Tabelas.numero("Desvio (ms)", BenchmarkResult::desvioMs, v -> Formatos.decimal(v, 3), 90));
        tabela.getColumns().add(Tabelas.numero("Mín (ms)", (BenchmarkResult r) -> r.minNs() / 1e6, v -> Formatos.decimal(v, 3), 85));
        tabela.getColumns().add(Tabelas.numero("Comparações", BenchmarkResult::comparacoes, Formatos::inteiro, 115));
        tabela.getColumns().add(Tabelas.numero("Trocas", BenchmarkResult::trocas, Formatos::inteiro, 105));
        tabela.getColumns().add(Tabelas.numero("Acessos", BenchmarkResult::acessos, Formatos::inteiro, 115));
        tabela.getColumns().add(Tabelas.coluna("OK", (BenchmarkResult r) -> r.verificado() ? "✔" : "FALHA", 45));

        tabelaComplexidade.getColumns().add(Tabelas.coluna("Algoritmo", AnaliseComplexidade.Estimativa::algoritmo, 170));
        tabelaComplexidade.getColumns().add(Tabelas.coluna("Critério", (AnaliseComplexidade.Estimativa e) -> e.criterio().rotulo(), 100));
        tabelaComplexidade.getColumns().add(Tabelas.coluna("Cenário", (AnaliseComplexidade.Estimativa e) -> e.cenario().toString(), 160));
        tabelaComplexidade.getColumns().add(Tabelas.numero("k (tempo)", AnaliseComplexidade.Estimativa::expoenteTempo, v -> Formatos.decimal(v, 2), 90));
        tabelaComplexidade.getColumns().add(Tabelas.numero("k (comparações)", AnaliseComplexidade.Estimativa::expoenteComparacoes,
                v -> Double.isNaN(v) ? "—" : Formatos.decimal(v, 2), 120));
        tabelaComplexidade.getColumns().add(Tabelas.numero("R² (tempo)", AnaliseComplexidade.Estimativa::r2Tempo, v -> Formatos.decimal(v, 3), 90));
        tabelaComplexidade.getColumns().add(Tabelas.coluna("Classe estimada", AnaliseComplexidade.Estimativa::classificacao, 130));
    }

    @FXML
    private void configRapida() {
        tfTamanhos.setText("100, 500, 1000, 2000");
        spAquecimentos.getValueFactory().setValue(1);
        spRepeticoes.getValueFactory().setValue(3);
    }

    private BenchmarkConfig config() {
        List<AlgoritmoTipo> algs = new ArrayList<>();
        chkAlgoritmos.forEach((t, c) -> { if (c.isSelected()) algs.add(t); });
        List<CriterioOrdenacao> crits = new ArrayList<>();
        chkCriterios.forEach((t, c) -> { if (c.isSelected()) crits.add(t); });
        List<CenarioEntrada> cens = new ArrayList<>();
        chkCenarios.forEach((t, c) -> { if (c.isSelected()) cens.add(t); });
        List<Integer> tamanhos = new ArrayList<>();
        for (String p : tfTamanhos.getText().split("[,;\\s]+")) {
            if (p.isBlank()) continue;
            try {
                int v = Integer.parseInt(p.replace(".", "").strip());
                if (v < 0) throw new NumberFormatException();
                tamanhos.add(v);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Tamanho invalido: '" + p + "'. Use inteiros >= 0 separados por virgula.");
            }
        }
        return new BenchmarkConfig(algs, crits, tamanhos, cens, spAquecimentos.getValue(), spRepeticoes.getValue(), 42L,
                Integer.MAX_VALUE);
    }

    @FXML
    private void executar() {
        BaseDeFocos base = ctx.baseProperty().get();
        if (base == null) {
            ctx.aviso("Sem dados", "Carregue os CSVs do INPE antes de executar o benchmark.");
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
        boolean temQuadratico = false;
        for (AlgoritmoTipo t : cfg.algoritmos()) temQuadratico |= t.quadratico();
        if (temQuadratico && maior > 5_000 && !ctx.confirmar("Benchmark demorado",
                cfg.totalCasos() + " casos com algoritmos O(n²) até n = " + Formatos.inteiro(maior)
                        + " podem levar alguns minutos. Continuar? (é possível cancelar)")) {
            return;
        }
        ctx.executar("Benchmark (" + cfg.totalCasos() + " casos)",
                prog -> new BenchmarkRunner().executar(base.getFocos(), cfg, p -> prog.accept(p.fracao(), p.mensagem())),
                r -> {
                    resultados = r;
                    ctx.sessao().setUltimoBenchmark(r);
                    tabela.setItems(FXCollections.observableArrayList(r));
                    tabelaComplexidade.setItems(FXCollections.observableArrayList(AnaliseComplexidade.estimar(r)));
                    atualizarFiltros();
                });
    }

    private void atualizarFiltros() {
        List<CriterioOrdenacao> crits = new ArrayList<>();
        List<CenarioEntrada> cens = new ArrayList<>();
        for (BenchmarkResult r : resultados) {
            if (!crits.contains(r.criterio())) crits.add(r.criterio());
            if (!cens.contains(r.cenario())) cens.add(r.cenario());
        }
        cbCriterio.setItems(FXCollections.observableArrayList(crits));
        cbCenario.setItems(FXCollections.observableArrayList(cens));
        if (!crits.isEmpty()) cbCriterio.getSelectionModel().selectFirst();
        if (!cens.isEmpty()) cbCenario.getSelectionModel().selectFirst();
        desenhar();
    }

    private void desenhar() {
        grafico.getData().clear();
        if (resultados.isEmpty() || cbCriterio.getValue() == null || cbCenario.getValue() == null) return;
        String metrica = cbMetrica.getValue();
        boolean log = chkLog.isSelected();
        Map<String, XYChart.Series<Number, Number>> series = new LinkedHashMap<>();
        List<Integer> ns = new ArrayList<>();
        for (BenchmarkResult r : resultados) {
            if (r.criterio() != cbCriterio.getValue() || r.cenario() != cbCenario.getValue()) continue;
            double v = switch (metrica) {
                case "Tempo médio (ms)" -> r.mediaMs();
                case "Tempo mínimo (ms)" -> r.minNs() / 1e6;
                case "Trocas" -> r.trocas();
                case "Acessos" -> r.acessos();
                default -> r.comparacoes();
            };
            if (log && v <= 0) continue;
            series.computeIfAbsent(r.algoritmo(), k -> {
                XYChart.Series<Number, Number> s = new XYChart.Series<>();
                s.setName(k);
                return s;
            }).getData().add(new XYChart.Data<>(r.n(), log ? Math.log10(v) : v));
            if (!ns.contains(r.n())) ns.add(r.n());
        }
        Ordenacoes.ordenar(ns, Comparator.naturalOrder());
        grafico.getData().addAll(series.values());
        if (chkTeoricas.isSelected() && metrica.equals("Comparações") && !ns.isEmpty()) {
            XYChart.Series<Number, Number> n2 = new XYChart.Series<>();
            n2.setName("teórico n²/2");
            XYChart.Series<Number, Number> nlogn = new XYChart.Series<>();
            nlogn.setName("teórico n·log₂n");
            for (int n : ns) {
                double a = n * (n - 1) / 2.0, b = n <= 1 ? 0 : n * Math.log(n) / Math.log(2);
                if (!log || a > 0) n2.getData().add(new XYChart.Data<>(n, log ? Math.log10(a) : a));
                if (!log || b > 0) nlogn.getData().add(new XYChart.Data<>(n, log ? Math.log10(b) : b));
            }
            grafico.getData().add(n2);
            grafico.getData().add(nlogn);
            n2.getNode().getStyleClass().add("serie-teorica");
            nlogn.getNode().getStyleClass().add("serie-teorica");
        }
        eixoY.setLabel(metrica + (log ? " (log10)" : ""));
        grafico.setTitle(cbCriterio.getValue().rotulo() + " · " + cbCenario.getValue());
    }

    @FXML
    private void exportarCsv() {
        if (resultados.isEmpty()) {
            ctx.aviso("Nada para exportar", "Execute o benchmark primeiro.");
            return;
        }
        FileChooser fc = new FileChooser();
        Path sug = ctx.sessao().arquivoRelatorio("benchmark", ".csv");
        File dir = sug.toAbsolutePath().getParent().toFile();
        dir.mkdirs();
        fc.setInitialDirectory(dir);
        fc.setInitialFileName(sug.getFileName().toString());
        File f = fc.showSaveDialog(ctx.stage());
        if (f == null) return;
        List<BenchmarkResult> r = resultados;
        ctx.executar("Exportando benchmark", () -> ctx.sessao().exporter().exportarBenchmarkCsv(r, f.toPath()),
                p -> ctx.info("CSV gravado", p.toAbsolutePath().toString()));
    }
}
