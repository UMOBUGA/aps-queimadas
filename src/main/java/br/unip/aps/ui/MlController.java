package br.unip.aps.ui;

import br.unip.aps.ml.BaseMensal;
import br.unip.aps.ml.ClassificadorNivel;
import br.unip.aps.ml.ClusterizacaoHotspots;
import br.unip.aps.ml.Metricas;
import br.unip.aps.ml.NivelAtividade;
import br.unip.aps.ml.Preditor;
import br.unip.aps.ml.PrevisaoFocos;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.util.Formatos;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.List;
import java.util.Map;

/**
 * Aba "Machine Learning": previsao de focos (Random Forest de regressao), classificacao do nivel
 * de atividade (Random Forest de classificacao) e hotspots (DBSCAN / K-Means).
 */
public class MlController {

    private final UiContexto ctx;

    @FXML private ComboBox<Integer> cbTreino, cbTeste;
    @FXML private Spinner<Integer> spArvores, spK, spMinPts;
    @FXML private Spinner<Double> spEps;
    @FXML private Button btnMapa;
    @FXML private Label lblPreparo, lblRegressao, lblClassificacao, lblClusters;
    @FXML private TableView<LinhaMetrica> tabelaRegressao;
    @FXML private TableView<PrevisaoFocos.ErroPrevisao> tabelaErros;
    @FXML private LineChart<String, Number> graficoPrevisao;
    @FXML private BarChart<Number, String> graficoImportancia;
    @FXML private GridPane gridConfusao;
    @FXML private TableView<LinhaClasse> tabelaClasses;
    @FXML private ComboBox<String> cbMetodo;
    @FXML private TableView<ClusterizacaoHotspots.Hotspot> tabelaHotspots;
    @FXML private LineChart<Number, Number> graficoCotovelo;
    @FXML private TextFlow textoSobre;

    private Preditor.ResultadoML resultado;

    /** Linha da tabela de metricas de regressao. */
    public record LinhaMetrica(String modelo, Metricas.Regressao m) { }

    /** Linha da tabela de metricas por classe. */
    public record LinhaClasse(String classe, double precisao, double revocacao, double f1, int suporte) { }

    /** @param ctx contexto injetado */
    public MlController(UiContexto ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        spArvores.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 1000, 200, 50));
        spK.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 30, 8));
        spEps.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(1, 50, 10, 1));
        spMinPts.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 200, 30));
        cbMetodo.valueProperty().addListener((o, a, n) -> exibirHotspots());
        configurarTabelas();
        escreverSobre();
        ctx.registrarGrafico("ML: focos reais x previstos", graficoPrevisao);
        ctx.registrarGrafico("ML: importancia das variaveis", graficoImportancia);
        ctx.registrarGrafico("ML: metodo do cotovelo", graficoCotovelo);
        ctx.baseProperty().addListener((o, a, b) -> {
            resultado = null;
            ctx.mlProperty().set(null);
            btnMapa.setDisable(true);
            List<Integer> anos = b == null ? List.of() : b.anos();
            cbTreino.setItems(FXCollections.observableArrayList(anos));
            cbTeste.setItems(FXCollections.observableArrayList(anos));
            if (anos.size() >= 2) {
                cbTreino.getSelectionModel().select(anos.size() - 2);
                cbTeste.getSelectionModel().select(anos.size() - 1);
            }
        });
    }

    private void configurarTabelas() {
        tabelaRegressao.getColumns().add(Tabelas.coluna("Modelo", LinhaMetrica::modelo, 260));
        tabelaRegressao.getColumns().add(Tabelas.numero("MAE (focos)", (LinhaMetrica l) -> l.m().mae(), v -> Formatos.decimal(v, 3), 110));
        tabelaRegressao.getColumns().add(Tabelas.numero("RMSE (focos)", (LinhaMetrica l) -> l.m().rmse(), v -> Formatos.decimal(v, 3), 110));
        tabelaRegressao.getColumns().add(Tabelas.numero("R²", (LinhaMetrica l) -> l.m().r2(), v -> Formatos.decimal(v, 3), 90));

        tabelaErros.getColumns().add(Tabelas.coluna("Município", PrevisaoFocos.ErroPrevisao::municipio, 240));
        tabelaErros.getColumns().add(Tabelas.coluna("Mês", (PrevisaoFocos.ErroPrevisao e) -> e.mes().toString(), 90));
        tabelaErros.getColumns().add(Tabelas.numero("Real", PrevisaoFocos.ErroPrevisao::real, String::valueOf, 80));
        tabelaErros.getColumns().add(Tabelas.numero("Previsto", PrevisaoFocos.ErroPrevisao::previsto, v -> Formatos.decimal(v, 1), 90));
        tabelaErros.getColumns().add(Tabelas.numero("Erro abs.", PrevisaoFocos.ErroPrevisao::erroAbsoluto, v -> Formatos.decimal(v, 1), 90));

        tabelaClasses.getColumns().add(Tabelas.coluna("Classe", LinhaClasse::classe, 200));
        tabelaClasses.getColumns().add(Tabelas.numero("Precisão", LinhaClasse::precisao, v -> Formatos.decimal(v, 3), 100));
        tabelaClasses.getColumns().add(Tabelas.numero("Revocação", LinhaClasse::revocacao, v -> Formatos.decimal(v, 3), 100));
        tabelaClasses.getColumns().add(Tabelas.numero("F1", LinhaClasse::f1, v -> Formatos.decimal(v, 3), 90));
        tabelaClasses.getColumns().add(Tabelas.numero("Suporte (teste)", LinhaClasse::suporte, v -> Formatos.inteiro(v), 120));

        tabelaHotspots.getColumns().add(Tabelas.numero("#", ClusterizacaoHotspots.Hotspot::id, String::valueOf, 50));
        tabelaHotspots.getColumns().add(Tabelas.numero("Focos", ClusterizacaoHotspots.Hotspot::focos, v -> Formatos.inteiro(v), 80));
        tabelaHotspots.getColumns().add(Tabelas.numero("Latitude", ClusterizacaoHotspots.Hotspot::latitude, v -> Formatos.decimal(v, 4), 95));
        tabelaHotspots.getColumns().add(Tabelas.numero("Longitude", ClusterizacaoHotspots.Hotspot::longitude, v -> Formatos.decimal(v, 4), 95));
        tabelaHotspots.getColumns().add(Tabelas.numero("Raio (km)", ClusterizacaoHotspots.Hotspot::raioKm, v -> Formatos.decimal(v, 1), 85));
        tabelaHotspots.getColumns().add(Tabelas.coluna("Município principal", ClusterizacaoHotspots.Hotspot::municipioPrincipal, 220));
        tabelaHotspots.getColumns().add(Tabelas.coluna("Bioma", ClusterizacaoHotspots.Hotspot::biomaPredominante, 120));
        tabelaHotspots.getColumns().add(Tabelas.coluna("Focos por ano", (ClusterizacaoHotspots.Hotspot h) -> h.focosPorAno().toString(), 180));
    }

    @FXML
    private void executar() {
        BaseDeFocos b = ctx.baseProperty().get();
        if (b == null) {
            ctx.aviso("Sem dados", "Carregue os CSVs do INPE primeiro.");
            return;
        }
        Integer treino = cbTreino.getValue(), teste = cbTeste.getValue();
        if (treino == null || teste == null || treino >= teste) {
            ctx.aviso("Anos inválidos", "Escolha um ano de treino ANTERIOR ao ano de teste (validação temporal: "
                    + "o modelo não pode aprender com o futuro).");
            return;
        }
        Preditor.Parametros p = new Preditor.Parametros(treino, teste, spArvores.getValue(), spK.getValue(),
                spEps.getValue(), spMinPts.getValue(), 42L);
        ctx.executar("Treinando modelos de ML", () -> new Preditor().executar(b.getFocos(), p), this::exibir);
    }

    private void exibir(Preditor.ResultadoML r) {
        resultado = r;
        ctx.sessao().setUltimoMl(r);
        ctx.mlProperty().set(r);
        btnMapa.setDisable(false);
        lblPreparo.setText("Pré-processamento: " + Formatos.inteiro(r.municipios()) + " municípios; ordenação município → data com Merge Sort: "
                + Formatos.inteiro(r.ordenacaoPreparo().comparacoes()) + " comparações em " + Formatos.duracao(r.ordenacaoPreparo().nanos()) + ".");
        exibirRegressao(r.regressao());
        exibirClassificacao(r.classificacao());
        cbMetodo.setItems(FXCollections.observableArrayList(r.dbscan().metodo(), r.kMeans().metodo()));
        cbMetodo.getSelectionModel().selectFirst();
        graficoCotovelo.getData().clear();
        XYChart.Series<Number, Number> s = new XYChart.Series<>();
        for (int k = 1; k <= r.cotovelo().length; k++) s.getData().add(new XYChart.Data<>(k, r.cotovelo()[k - 1]));
        graficoCotovelo.getData().add(s);
    }

    private void exibirRegressao(PrevisaoFocos.Resultado r) {
        boolean melhor = r.modelo().mae() < r.persistencia().mae();
        lblRegressao.setText("Random Forest treinado em " + r.anoTreino() + " (" + Formatos.inteiro(r.linhasTreino())
                + " município-mês) e testado em " + r.anoTeste() + " (" + Formatos.inteiro(r.linhasTeste()) + "). "
                + (melhor ? "O modelo superou o baseline de persistência no MAE." : "O modelo NÃO superou a persistência no MAE — discuta na dissertação."));
        tabelaRegressao.setItems(FXCollections.observableArrayList(
                new LinhaMetrica("Random Forest (treino fixo em " + r.anoTreino() + ")", r.modelo()),
                new LinhaMetrica("Random Forest (janela expansível, re-treino mensal)", r.janelaExpansivel()),
                new LinhaMetrica("Baseline: persistência (mês anterior)", r.persistencia()),
                new LinhaMetrica("Baseline: média histórica do município", r.mediaHistorica())));
        graficoPrevisao.getData().clear();
        XYChart.Series<String, Number> real = new XYChart.Series<>(), prev = new XYChart.Series<>(), janela = new XYChart.Series<>();
        real.setName("Real");
        prev.setName("RF treino fixo");
        janela.setName("RF janela expansível");
        r.realPorMes().forEach((m, v) -> {
            real.getData().add(new XYChart.Data<>(m.toString(), v));
            prev.getData().add(new XYChart.Data<>(m.toString(), r.previstoPorMes().get(m)));
            janela.getData().add(new XYChart.Data<>(m.toString(), r.previstoJanelaPorMes().getOrDefault(m, 0.0)));
        });
        graficoPrevisao.getData().add(real);
        graficoPrevisao.getData().add(prev);
        graficoPrevisao.getData().add(janela);

        graficoImportancia.getData().clear();
        XYChart.Series<Number, String> imp = new XYChart.Series<>();
        for (int i = BaseMensal.VARIAVEIS.length - 1; i >= 0; i--) {
            imp.getData().add(new XYChart.Data<>(r.importancia()[i], BaseMensal.VARIAVEIS[i]));
        }
        graficoImportancia.getData().add(imp);
        tabelaErros.setItems(FXCollections.observableArrayList(r.maioresErros()));
    }

    private void exibirClassificacao(ClassificadorNivel.Resultado r) {
        Metricas.Classificacao m = r.metricas();
        lblClassificacao.setText(String.format("Acurácia %s (baseline classe majoritária %s) · F1 macro %s (baseline %s)",
                Formatos.decimal(m.acuracia(), 3), Formatos.decimal(r.baselineAcuracia(), 3),
                Formatos.decimal(m.f1Macro(), 3), Formatos.decimal(r.baselineF1Macro(), 3)));
        gridConfusao.getChildren().clear();
        NivelAtividade[] niveis = NivelAtividade.values();
        int max = 1;
        for (int[] linha : m.matriz()) for (int v : linha) max = Math.max(max, v);
        gridConfusao.add(celula("real \\ previsto", "cabecalho-matriz"), 0, 0);
        for (int j = 0; j < niveis.length; j++) gridConfusao.add(celula(niveis[j].toString(), "cabecalho-matriz"), j + 1, 0);
        for (int i = 0; i < niveis.length; i++) {
            gridConfusao.add(celula(niveis[i].toString(), "cabecalho-matriz"), 0, i + 1);
            for (int j = 0; j < niveis.length; j++) {
                Label c = celula(Formatos.inteiro(m.matriz()[i][j]), i == j ? "celula-acerto" : "celula-erro");
                double intensidade = 0.15 + 0.85 * m.matriz()[i][j] / (double) max;
                c.setOpacity(Math.max(0.35, intensidade));
                gridConfusao.add(c, j + 1, i + 1);
            }
        }
        List<LinhaClasse> linhas = new java.util.ArrayList<>();
        for (int i = 0; i < niveis.length; i++) {
            linhas.add(new LinhaClasse(niveis[i].toString(), m.precisao()[i], m.revocacao()[i], m.f1()[i], r.distribuicaoTeste()[i]));
        }
        tabelaClasses.setItems(FXCollections.observableArrayList(linhas));
    }

    private static Label celula(String texto, String estilo) {
        Label l = new Label(texto);
        l.getStyleClass().add(estilo);
        l.setMinSize(150, 34);
        return l;
    }

    private void exibirHotspots() {
        if (resultado == null || cbMetodo.getValue() == null) return;
        ClusterizacaoHotspots.Resultado c = cbMetodo.getValue().equals(resultado.dbscan().metodo()) ? resultado.dbscan() : resultado.kMeans();
        tabelaHotspots.setItems(FXCollections.observableArrayList(c.hotspots()));
        lblClusters.setText(c.hotspots().size() + " grupos" + (c.ruido() > 0 ? " · " + Formatos.inteiro(c.ruido()) + " focos isolados (ruído)" : ""));
    }

    @FXML
    private void verNoMapa() {
        ctx.selecionarAba("mapa");
    }

    private void escreverSobre() {
        Map<String, String> secoes = new java.util.LinkedHashMap<>();
        secoes.put("Por que ordenar antes de aprender?",
                "Os focos são ordenados por município → data (Merge Sort, estável). Com a base ordenada, a agregação em séries "
                        + "município × mês é uma única varredura linear (control break), sem estruturas auxiliares — é a etapa de "
                        + "pré-processamento que prepara os dados para os modelos.");
        secoes.put("Random Forest (Breiman, 2001)",
                "Conjunto de árvores de decisão treinadas em amostras bootstrap, sorteando variáveis a cada divisão; a previsão "
                        + "é a média (regressão) ou o voto (classificação) das árvores. Reduz a variância de uma árvore única e "
                        + "fornece a importância de cada variável.");
        secoes.put("Validação temporal",
                "Treino em um ano e teste no seguinte, prevendo cada mês só com informação até o mês anterior. Embaralhar séries "
                        + "temporais vazaria o futuro para o treino. Comparamos com baselines (persistência e média histórica): um "
                        + "modelo só tem valor se superá-los. Métricas: MAE, RMSE e R² (regressão); acurácia, precisão, revocação e "
                        + "F1 macro (classificação).");
        secoes.put("DBSCAN e K-Means",
                "O DBSCAN (Ester et al., 1996) agrupa focos densos (≥ minPts vizinhos em raio eps), descobre o número de grupos e "
                        + "isola ruído — adequado a hotspots de formato irregular. O K-Means exige k (método do cotovelo) e forma "
                        + "grupos esféricos. As coordenadas são projetadas em km antes do agrupamento.");
        secoes.put("Deep Learning aplicado a imagens de satélite (fundamentação)",
                "Redes neurais convolucionais (CNNs) aprendem filtros que detectam bordas, texturas e padrões espectrais em imagens. "
                        + "Para classificar áreas desmatadas ou queimadas, usam-se imagens multiespectrais (Landsat-8/9, Sentinel-2, "
                        + "CBERS-4A) recortadas em blocos (patches) e rotuladas com mapas de referência (PRODES/DETER/MapBiomas). "
                        + "Arquiteturas de segmentação semântica como a U-Net classificam cada pixel (floresta, desmatamento, cicatriz "
                        + "de queimada); bandas como NIR e SWIR e índices (NDVI, NBR) realçam a vegetação queimada. Os focos do INPE, "
                        + "ordenados e agregados por este sistema, podem servir de rótulos fracos e de variáveis temporais para "
                        + "modelos híbridos (CNN + LSTM) que prevejam a evolução de queimadas.");
        for (Map.Entry<String, String> e : secoes.entrySet()) {
            Text t = new Text(e.getKey() + "\n");
            t.setFont(Font.font("System", FontWeight.BOLD, 14));
            Text c = new Text(e.getValue() + "\n\n");
            c.setFont(Font.font("System", 13));
            textoSobre.getChildren().addAll(t, c);
        }
    }
}
