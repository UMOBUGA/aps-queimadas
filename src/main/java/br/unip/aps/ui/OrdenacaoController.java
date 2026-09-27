package br.unip.aps.ui;

import br.unip.aps.ApsException;
import br.unip.aps.analysis.FiltroFocos;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.sorting.AlgoritmoTipo;
import br.unip.aps.sorting.CenarioEntrada;
import br.unip.aps.sorting.Complexidade;
import br.unip.aps.sorting.CriterioOrdenacao;
import br.unip.aps.sorting.Criterios;
import br.unip.aps.sorting.Ordem;
import br.unip.aps.sorting.ResultadoOrdenacao;
import br.unip.aps.sorting.ServicoOrdenacao;
import br.unip.aps.util.Formatos;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Aba "Ordenacao": atende a solicitacao central do enunciado — ordenar e exibir os focos por
 * data, bioma, municipio (e extras), informando o numero de operacoes realizadas.
 */
public class OrdenacaoController {

    private static final String NENHUM = "(nenhum)";
    private static final String TODOS = "(todos)";

    private final UiContexto ctx;

    @FXML private ComboBox<String> cbCriterio1, cbCriterio2, cbCriterio3;
    @FXML private ComboBox<Ordem> cbOrdem1, cbOrdem2, cbOrdem3;
    @FXML private ComboBox<AlgoritmoTipo> cbAlgoritmo;
    @FXML private Label lblAlgoritmo, lblAmostra, lblResumo;
    @FXML private TextField tfAmostra, tfMunicipio;
    @FXML private CheckBox chkAleatoria, chkLog;
    @FXML private ComboBox<CenarioEntrada> cbCenario;
    @FXML private ComboBox<String> cbAno, cbBioma, cbMetrica;
    @FXML private Label lblComparacoes, lblTrocas, lblAtribuicoes, lblAcessos, lblTempo, lblVerificacao;
    @FXML private TabPane abasResultado;
    @FXML private Tab abaComparativo;
    @FXML private TableView<FocoIncendio> tabela;
    @FXML private TableView<ResultadoOrdenacao<FocoIncendio>> tabelaComparativo;
    @FXML private BarChart<String, Number> graficoComparativo;

    private List<ResultadoOrdenacao<FocoIncendio>> comparativo = List.of();

    /** @param ctx contexto injetado */
    public OrdenacaoController(UiContexto ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        for (ComboBox<Ordem> c : List.of(cbOrdem1, cbOrdem2, cbOrdem3)) {
            c.setItems(FXCollections.observableArrayList(Ordem.values()));
            c.getSelectionModel().selectFirst();
        }
        cbAlgoritmo.setItems(FXCollections.observableArrayList(AlgoritmoTipo.values()));
        cbAlgoritmo.getSelectionModel().select(AlgoritmoTipo.MERGE);
        cbAlgoritmo.valueProperty().addListener((o, a, n) -> descreverAlgoritmo());
        cbCenario.setItems(FXCollections.observableArrayList(CenarioEntrada.values()));
        cbCenario.getSelectionModel().selectFirst();
        cbMetrica.setItems(FXCollections.observableArrayList("Comparações", "Trocas", "Atribuições", "Acessos", "Tempo (ms)"));
        cbMetrica.getSelectionModel().selectFirst();
        cbMetrica.valueProperty().addListener((o, a, n) -> desenharComparativo());
        chkLog.selectedProperty().addListener((o, a, n) -> desenharComparativo());
        configurarCriterios(null);
        configurarTabelas();
        descreverAlgoritmo();
        ctx.registrarGrafico("Comparativo de algoritmos", graficoComparativo);

        ctx.baseProperty().addListener((o, a, b) -> {
            configurarCriterios(b);
            List<String> anos = new ArrayList<>(List.of(TODOS));
            List<String> biomas = new ArrayList<>(List.of(TODOS));
            if (b != null) {
                b.anos().forEach(x -> anos.add(String.valueOf(x)));
                biomas.addAll(b.biomas());
                lblAmostra.setText("Base: " + Formatos.inteiro(b.tamanho()) + " focos");
            }
            cbAno.setItems(FXCollections.observableArrayList(anos));
            cbAno.getSelectionModel().selectFirst();
            cbBioma.setItems(FXCollections.observableArrayList(biomas));
            cbBioma.getSelectionModel().selectFirst();
            tabela.getItems().clear();
            tabelaComparativo.getItems().clear();
            graficoComparativo.getData().clear();
        });
    }

    private void configurarCriterios(BaseDeFocos b) {
        List<String> nomes = new ArrayList<>();
        for (CriterioOrdenacao c : CriterioOrdenacao.values()) {
            if (!c.opcional() || (b != null && b.possuiCampo(c::valor))) nomes.add(c.rotulo());
        }
        List<String> comNenhum = new ArrayList<>(List.of(NENHUM));
        comNenhum.addAll(nomes);
        cbCriterio1.setItems(FXCollections.observableArrayList(nomes));
        cbCriterio1.getSelectionModel().select(CriterioOrdenacao.DATA.rotulo());
        cbCriterio2.setItems(FXCollections.observableArrayList(comNenhum));
        cbCriterio2.getSelectionModel().selectFirst();
        cbCriterio3.setItems(FXCollections.observableArrayList(comNenhum));
        cbCriterio3.getSelectionModel().selectFirst();
    }

    private void configurarTabelas() {
        tabela.getColumns().add(Tabelas.indice());
        tabela.getColumns().add(Tabelas.coluna("Data/hora (GMT)", (FocoIncendio f) -> f.getDataHora().format(Formatos.DATA_HORA), 130));
        tabela.getColumns().add(Tabelas.coluna("Município", FocoIncendio::getMunicipio, 230));
        tabela.getColumns().add(Tabelas.coluna("Bioma", FocoIncendio::getBioma, 120));
        tabela.getColumns().add(Tabelas.numero("Latitude", FocoIncendio::getLatitude, v -> Formatos.decimal(v, 5), 95));
        tabela.getColumns().add(Tabelas.numero("Longitude", FocoIncendio::getLongitude, v -> Formatos.decimal(v, 5), 95));
        tabela.getColumns().add(Tabelas.numero("id_bdq", FocoIncendio::getIdBdq, String::valueOf, 110));
        tabela.getColumns().add(Tabelas.coluna("foco_id", FocoIncendio::getFocoId, 260));
        tabela.setPlaceholder(new Label("Nenhum resultado ainda."));

        tabelaComparativo.getColumns().add(Tabelas.coluna("Algoritmo", (ResultadoOrdenacao<FocoIncendio> r) -> r.algoritmo(), 170));
        tabelaComparativo.getColumns().add(Tabelas.numero("Comparações", r -> r.metricas().comparacoes(), Formatos::inteiro, 120));
        tabelaComparativo.getColumns().add(Tabelas.numero("Trocas", r -> r.metricas().trocas(), Formatos::inteiro, 110));
        tabelaComparativo.getColumns().add(Tabelas.numero("Atribuições", r -> r.metricas().atribuicoes(), Formatos::inteiro, 120));
        tabelaComparativo.getColumns().add(Tabelas.numero("Acessos", r -> r.metricas().acessos(), Formatos::inteiro, 120));
        tabelaComparativo.getColumns().add(Tabelas.numero("Tempo (ms)", r -> r.metricas().millis(), v -> Formatos.decimal(v, 3), 100));
        tabelaComparativo.getColumns().add(Tabelas.coluna("Estável", r -> SortInfo.estavel(r.algoritmo()), 70));
        tabelaComparativo.getColumns().add(Tabelas.coluna("Verificado", r -> r.verificado() ? "✔" : "FALHA", 80));
    }

    private void descreverAlgoritmo() {
        AlgoritmoTipo t = cbAlgoritmo.getValue();
        if (t == null) return;
        Complexidade c = t.complexidade();
        lblAlgoritmo.setText(t.criar().descricao() + "\n\nMelhor: " + c.melhorCaso() + " · Médio: " + c.casoMedio()
                + " · Pior: " + c.piorCaso() + "\nEspaço: " + c.espaco() + " · Estável: " + (c.estavel() ? "sim" : "não")
                + (t.exigeChaveNumerica() ? "\nSó para critério numérico único (data, lat/lon, id)." : ""));
    }

    private Criterios criterios() {
        List<Criterios.Nivel> niveis = new ArrayList<>();
        niveis.add(new Criterios.Nivel(porRotulo(cbCriterio1.getValue()), cbOrdem1.getValue()));
        if (!NENHUM.equals(cbCriterio2.getValue())) niveis.add(new Criterios.Nivel(porRotulo(cbCriterio2.getValue()), cbOrdem2.getValue()));
        if (!NENHUM.equals(cbCriterio3.getValue())) niveis.add(new Criterios.Nivel(porRotulo(cbCriterio3.getValue()), cbOrdem3.getValue()));
        return Criterios.composto(niveis);
    }

    private static CriterioOrdenacao porRotulo(String r) {
        for (CriterioOrdenacao c : CriterioOrdenacao.values()) if (c.rotulo().equals(r)) return c;
        throw new IllegalArgumentException("Selecione um criterio de ordenacao.");
    }

    private List<FocoIncendio> dadosFiltrados() throws ApsException {
        BaseDeFocos b = ctx.sessao().exigirBase();
        String ano = cbAno.getValue(), bioma = cbBioma.getValue();
        FiltroFocos f = new FiltroFocos(ano == null || ano.equals(TODOS) ? Set.of() : Set.of(Integer.parseInt(ano)),
                bioma == null || bioma.equals(TODOS) ? Set.of() : Set.of(bioma), tfMunicipio.getText(), null, null);
        List<FocoIncendio> r = b.filtrar(f);
        if (r.isEmpty()) throw new ApsException("Nenhum foco atende ao filtro (" + f.descricao() + "). Ajuste os filtros.");
        return r;
    }

    private int amostra() {
        String s = tfAmostra.getText() == null ? "" : tfAmostra.getText().replace(".", "").replace(" ", "").strip();
        if (s.isEmpty()) return 0;
        try {
            int n = Integer.parseInt(s);
            if (n < 0) throw new NumberFormatException();
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Tamanho da amostra invalido: '" + tfAmostra.getText() + "'. Use um inteiro >= 0.");
        }
    }

    @FXML
    private void ordenar() {
        try {
            List<FocoIncendio> dados = dadosFiltrados();
            AlgoritmoTipo alg = cbAlgoritmo.getValue();
            Criterios crit = criterios();
            int n = amostra();
            int efetivo = n == 0 ? dados.size() : Math.min(n, dados.size());
            String aviso = ctx.sessao().servicoOrdenacao().avisoDesempenho(alg, efetivo);
            if (aviso != null && !ctx.confirmar("Algoritmo quadrático", aviso + "\n\nDeseja continuar? (é possível cancelar durante a execução)")) {
                return;
            }
            ServicoOrdenacao.Solicitacao s = new ServicoOrdenacao.Solicitacao(alg, crit, n, chkAleatoria.isSelected(),
                    cbCenario.getValue(), 42L);
            ctx.executar("Ordenando " + Formatos.inteiro(efetivo) + " focos com " + alg.nome(),
                    () -> ctx.sessao().servicoOrdenacao().ordenar(dados, s), this::exibir);
        } catch (ApsException | IllegalArgumentException e) {
            ctx.erro("Não foi possível ordenar", e);
        }
    }

    private void exibir(ResultadoOrdenacao<FocoIncendio> r) {
        ctx.sessao().setUltimaOrdenacao(r);
        tabela.setItems(FXCollections.observableArrayList(r.dados()));
        tabela.scrollTo(0);
        lblResumo.setText(r.algoritmo() + " · " + r.criterio() + " · " + r.cenario() + " · n = " + Formatos.inteiro(r.tamanho()));
        lblComparacoes.setText(Formatos.inteiro(r.metricas().comparacoes()));
        lblTrocas.setText(Formatos.inteiro(r.metricas().trocas()));
        lblAtribuicoes.setText(Formatos.inteiro(r.metricas().atribuicoes()));
        lblAcessos.setText(Formatos.inteiro(r.metricas().acessos()));
        lblTempo.setText(Formatos.duracao(r.metricas().nanos()));
        lblVerificacao.setText(r.verificado() ? "✔ ordenado" : "✘ falhou");
        abasResultado.getSelectionModel().selectFirst();
    }

    @FXML
    private void compararTodos() {
        try {
            List<FocoIncendio> dados = dadosFiltrados();
            Criterios crit = criterios();
            int n = amostra();
            CenarioEntrada cen = cbCenario.getValue();
            boolean aleatoria = chkAleatoria.isSelected();
            ctx.executar("Comparando todos os algoritmos", (progresso) -> {
                List<ResultadoOrdenacao<FocoIncendio>> r = new ArrayList<>();
                AlgoritmoTipo[] tipos = AlgoritmoTipo.values();
                for (int i = 0; i < tipos.length; i++) {
                    if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
                    AlgoritmoTipo t = tipos[i];
                    progresso.accept((double) i / tipos.length, t.nome());
                    if (t.exigeChaveNumerica() && crit.chaveNumerica() == null) continue;
                    r.add(ctx.sessao().servicoOrdenacao().ordenar(dados,
                            new ServicoOrdenacao.Solicitacao(t, crit, n, aleatoria, cen, 42L)));
                }
                return r;
            }, r -> {
                comparativo = r;
                ctx.sessao().setUltimoComparativo(r);
                tabelaComparativo.setItems(FXCollections.observableArrayList(r));
                desenharComparativo();
                abasResultado.getSelectionModel().select(abaComparativo);
                if (!r.isEmpty()) {
                    lblResumo.setText("Comparativo · " + r.get(0).criterio() + " · " + r.get(0).cenario() + " · n = "
                            + Formatos.inteiro(r.get(0).tamanho()) + " (execução única, sem aquecimento do JIT)");
                }
            });
        } catch (ApsException | IllegalArgumentException e) {
            ctx.erro("Não foi possível comparar", e);
        }
    }

    private void desenharComparativo() {
        graficoComparativo.getData().clear();
        if (comparativo.isEmpty()) return;
        String m = cbMetrica.getValue();
        boolean log = chkLog.isSelected();
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        for (ResultadoOrdenacao<FocoIncendio> r : comparativo) {
            double v = switch (m) {
                case "Trocas" -> r.metricas().trocas();
                case "Atribuições" -> r.metricas().atribuicoes();
                case "Acessos" -> r.metricas().acessos();
                case "Tempo (ms)" -> r.metricas().millis();
                default -> r.metricas().comparacoes();
            };
            s.getData().add(new XYChart.Data<>(r.algoritmo(), log ? Math.log10(Math.max(1, v)) : v));
        }
        graficoComparativo.setTitle(m + (log ? " (log10)" : ""));
        graficoComparativo.getData().add(s);
    }

    @FXML
    private void exportarCsv() {
        ResultadoOrdenacao<FocoIncendio> r = ctx.sessao().ultimaOrdenacao();
        if (r == null) {
            ctx.aviso("Nada para exportar", "Execute uma ordenação primeiro.");
            return;
        }
        FileChooser fc = new FileChooser();
        Path sug = ctx.sessao().arquivoRelatorio("dados-ordenados", ".csv");
        File dir = sug.toAbsolutePath().getParent().toFile();
        dir.mkdirs();
        fc.setInitialDirectory(dir);
        fc.setInitialFileName(sug.getFileName().toString());
        File f = fc.showSaveDialog(ctx.stage());
        if (f == null) return;
        ctx.executar("Exportando CSV", () -> ctx.sessao().exporter().exportarFocosCsv(r.dados(), f.toPath()),
                p -> ctx.info("CSV gravado", p.toAbsolutePath().toString()));
    }

    /** Consulta de propriedades por nome de algoritmo (para a tabela comparativa). */
    static final class SortInfo {
        private SortInfo() { }

        static String estavel(String nome) {
            for (AlgoritmoTipo t : AlgoritmoTipo.values()) {
                if (t.nome().equals(nome)) return t.complexidade().estavel() ? "sim" : "não";
            }
            return "?";
        }
    }
}
