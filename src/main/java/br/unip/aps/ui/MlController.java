package br.unip.aps.ui;

import br.unip.aps.ml.BaseMensal;
import br.unip.aps.ml.ClassificadorNivel;
import br.unip.aps.ml.ClusterizacaoHotspots;
import br.unip.aps.ml.Metricas;
import br.unip.aps.ml.NivelAtividade;
import br.unip.aps.ml.Preditor;
import br.unip.aps.ml.PrevisaoFocos;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.sorting.Ordenacoes;
import br.unip.aps.ui.componentes.BarrasHorizontais;
import br.unip.aps.ui.componentes.ChartCard;
import br.unip.aps.ui.componentes.Chip;
import br.unip.aps.ui.componentes.Feedback;
import br.unip.aps.ui.componentes.Graficos;
import br.unip.aps.ui.componentes.Icones;
import br.unip.aps.ui.componentes.KpiCard;
import br.unip.aps.ui.componentes.MatrizCalor;
import br.unip.aps.util.Formatos;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tela "Machine Learning": previsao de focos (Random Forest de regressao, treino fixo e janela
 * expansivel), classificacao do nivel de atividade (Random Forest balanceado) e hotspots
 * (DBSCAN / K-Means). As metricas sempre aparecem comparadas a um baseline.
 */
public class MlController implements Pagina.Controlador {

    private final UiContexto ctx;

    @FXML private ComboBox<Integer> cbTreino, cbTeste;
    @FXML private Spinner<Integer> spArvores, spK, spMinPts;
    @FXML private Spinner<Double> spEps;
    @FXML private Button btnMapa, btnTreinar;
    @FXML private Label lblPreparo;
    @FXML private KpiCard kMae, kRmse, kR2, kAcuracia, kF1;
    @FXML private ChartCard cPrevisao, cMatriz, cImportancia, cHotspots, cCotovelo;
    @FXML private TextFlow textoSobre;

    private final LineChart<String, Number> grafPrevisao = new LineChart<>(new CategoryAxis(), new NumberAxis());
    private final MatrizCalor matriz = new MatrizCalor();
    private final BarrasHorizontais importancia = new BarrasHorizontais();
    private final TableView<ClusterizacaoHotspots.Hotspot> tabHotspots = new TableView<>();
    private final LineChart<Number, Number> grafCotovelo = new LineChart<>(new NumberAxis(), new NumberAxis());
    private final ToggleGroup metodo = new ToggleGroup();
    private Preditor.ResultadoML resultado;

    /** @param ctx contexto injetado */
    public MlController(UiContexto ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        spArvores.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 1000, 200, 50));
        spK.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 30, 8));
        spEps.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(1, 50, 10, 1));
        spMinPts.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 200, 30, 5));
        btnTreinar.setGraphic(Icones.de(Icones.ML, 16));
        btnMapa.setGraphic(Icones.de(Icones.MAPA, 16));

        for (LineChart<?, ?> g : List.of(grafPrevisao, grafCotovelo)) {
            g.setLegendVisible(false);
            g.setAnimated(false);
            g.setVerticalGridLinesVisible(false);
            g.setCreateSymbols(true);
        }
        Graficos.eixoLog((NumberAxis) grafPrevisao.getYAxis(), false);
        NumberAxis kx = (NumberAxis) grafCotovelo.getXAxis();
        kx.setLabel("k (número de grupos)");
        kx.setTickUnit(1);
        kx.setMinorTickVisible(false);
        kx.setForceZeroInRange(false);
        kx.setAutoRanging(false);
        kx.setLowerBound(1);
        kx.setUpperBound(10);
        cPrevisao.conteudo(grafPrevisao);
        cMatriz.conteudo(matriz);
        cImportancia.conteudo(importancia);
        cHotspots.conteudo(tabHotspots);
        cCotovelo.conteudo(grafCotovelo);
        for (ChartCard c : List.of(cPrevisao, cMatriz, cImportancia, cHotspots, cCotovelo)) c.estado(ChartCard.Estado.VAZIO);
        ctx.registrarGrafico("ML: focos reais x previstos", cPrevisao);
        ctx.registrarGrafico("ML: matriz de confusão", cMatriz);
        ctx.registrarGrafico("ML: importância das variáveis", cImportancia);

        HBox seg = new HBox();
        seg.getStyleClass().add("segmented");
        for (String m : List.of("DBSCAN", "K-Means")) {
            ToggleButton t = new ToggleButton(m);
            t.setUserData(m);
            t.setToggleGroup(metodo);
            seg.getChildren().add(t);
        }
        metodo.selectToggle(metodo.getToggles().get(0));
        metodo.selectedToggleProperty().addListener((o, a, n) -> {
            if (n == null) a.setSelected(true);
            else exibirHotspots();
        });
        cHotspots.setExtra(seg);
        configurarTabela();
        for (KpiCard k : List.of(kMae, kRmse, kR2, kAcuracia, kF1)) k.valor("—").contexto("treine os modelos", KpiCard.Tendencia.NEUTRA);
        escreverSobre();

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
        BaseDeFocos atual = ctx.baseProperty().get();
        if (atual != null && atual.anos().size() >= 2) {
            List<Integer> anos = atual.anos();
            cbTreino.setItems(FXCollections.observableArrayList(anos));
            cbTeste.setItems(FXCollections.observableArrayList(anos));
            cbTreino.getSelectionModel().select(anos.size() - 2);
            cbTeste.getSelectionModel().select(anos.size() - 1);
        }
    }

    private void configurarTabela() {
        Tabelas.preparar(tabHotspots, "Treine os modelos para ver os hotspots.");
        tabHotspots.getColumns().add(Tabelas.numero("#", ClusterizacaoHotspots.Hotspot::id, String::valueOf, 44));
        tabHotspots.getColumns().add(Tabelas.numero("Focos", ClusterizacaoHotspots.Hotspot::focos, v -> Formatos.inteiro(v), 70));
        tabHotspots.getColumns().add(Tabelas.coluna("Município principal", h -> VisaoGeralController.capitalizar(h.municipioPrincipal()), 170));
        tabHotspots.getColumns().add(Tabelas.bioma("Bioma", ClusterizacaoHotspots.Hotspot::biomaPredominante, 150));
        tabHotspots.getColumns().add(Tabelas.numero("Raio (km)", ClusterizacaoHotspots.Hotspot::raioKm, v -> Formatos.decimal(v, 1), 80));
    }

    @FXML
    private void executar() {
        BaseDeFocos b = ctx.baseProperty().get();
        if (b == null) {
            Feedback.alerta("Sem dados", "Carregue os CSVs do INPE primeiro.");
            return;
        }
        Integer treino = cbTreino.getValue(), teste = cbTeste.getValue();
        if (treino == null || teste == null || treino >= teste) {
            ctx.aviso("Anos inválidos", "Escolha um ano de treino ANTERIOR ao ano de teste (validação temporal: o modelo não pode aprender com o futuro).");
            return;
        }
        Preditor.Parametros p = new Preditor.Parametros(treino, teste, spArvores.getValue(), spK.getValue(), spEps.getValue(), spMinPts.getValue(), 42L);
        ctx.executar("Treinando modelos de ML", () -> new Preditor().executar(b.getFocos(), p), this::exibir);
    }

    private void exibir(Preditor.ResultadoML r) {
        resultado = r;
        ctx.sessao().setUltimoMl(r);
        ctx.mlProperty().set(r);
        btnMapa.setDisable(false);
        lblPreparo.setText("Pré-processamento: " + Formatos.inteiro(r.municipios()) + " municípios; ordenação município → data com Merge Sort ("
                + Formatos.inteiro(r.ordenacaoPreparo().comparacoes()) + " comparações em " + Formatos.duracao(r.ordenacaoPreparo().nanos())
                + "). Treino " + r.parametros().anoTreino() + " → teste " + r.parametros().anoTeste() + ".");
        exibirMetricas(r);
        exibirPrevisao(r.regressao());
        exibirMatriz(r.classificacao());
        exibirImportancia(r.regressao());
        exibirHotspots();
        exibirCotovelo(r.cotovelo());
        Feedback.sucesso("Modelos treinados", "Concluído em " + Formatos.inteiro(r.duracaoMs()) + " ms");
    }

    private void exibirMetricas(Preditor.ResultadoML r) {
        PrevisaoFocos.Resultado reg = r.regressao();
        double ganho = (reg.persistencia().mae() - reg.modelo().mae()) / reg.persistencia().mae() * 100;
        kMae.valor(Formatos.decimal(reg.modelo().mae(), 3))
                .contexto((ganho >= 0 ? "−" : "+") + Formatos.decimal(Math.abs(ganho), 1) + "% vs persistência",
                        ganho >= 0 ? KpiCard.Tendencia.BOA : KpiCard.Tendencia.ALERTA)
                .icone(KpiCard.EstiloIcone.DESTAQUE)
                .dica("Erro absoluto médio em focos por município/mês. Persistência (repetir o mês anterior): "
                        + Formatos.decimal(reg.persistencia().mae(), 3));
        kRmse.valor(Formatos.decimal(reg.modelo().rmse(), 3))
                .contexto("janela expansível: " + Formatos.decimal(reg.janelaExpansivel().rmse(), 3), KpiCard.Tendencia.NEUTRA);
        kR2.valor(Formatos.decimal(reg.modelo().r2(), 3))
                .contexto(reg.modelo().r2() < 0.1 ? "picos fora do ano de treino" : "variância explicada", KpiCard.Tendencia.NEUTRA)
                .dica("R² perto de zero: o modelo não explica os picos extremos de 2024 (crise de agosto), ausentes no ano de treino.");
        ClassificadorNivel.Resultado cls = r.classificacao();
        kAcuracia.valor(Formatos.decimal(cls.metricas().acuracia(), 3))
                .contexto("baseline: " + Formatos.decimal(cls.baselineAcuracia(), 3), KpiCard.Tendencia.NEUTRA)
                .dica("A acurácia cai em relação ao baseline porque o modelo deixa de prever sempre a classe majoritária.");
        double dF1 = cls.metricas().f1Macro() - cls.baselineF1Macro();
        kF1.valor(Formatos.decimal(cls.metricas().f1Macro(), 3))
                .contexto((dF1 >= 0 ? "+" : "") + Formatos.decimal(dF1, 3) + " vs baseline", dF1 >= 0 ? KpiCard.Tendencia.BOA : KpiCard.Tendencia.ALERTA)
                .icone(KpiCard.EstiloIcone.DESTAQUE);
    }

    private void exibirPrevisao(PrevisaoFocos.Resultado r) {
        grafPrevisao.getData().clear();
        cPrevisao.limparLegenda();
        XYChart.Series<String, Number> real = new XYChart.Series<>(), fixo = new XYChart.Series<>(), janela = new XYChart.Series<>();
        List<String[]> linhas = new ArrayList<>();
        for (Map.Entry<YearMonth, Double> e : r.realPorMes().entrySet()) {
            String m = br.unip.aps.analysis.Estatisticas.MESES[e.getKey().getMonthValue() - 1];
            double f = r.previstoPorMes().getOrDefault(e.getKey(), 0.0), j = r.previstoJanelaPorMes().getOrDefault(e.getKey(), 0.0);
            real.getData().add(new XYChart.Data<>(m, e.getValue()));
            fixo.getData().add(new XYChart.Data<>(m, f));
            janela.getData().add(new XYChart.Data<>(m, j));
            linhas.add(new String[]{m, Formatos.inteiro(Math.round(e.getValue())), Formatos.decimal(f, 1), Formatos.decimal(j, 1)});
        }
        Map<XYChart.Series<String, Number>, String[]> meta = new LinkedHashMap<>();
        meta.put(real, new String[]{"Real", "serie-neutra"});
        meta.put(fixo, new String[]{"RF treino fixo (" + r.anoTreino() + ")", "serie-1"});
        meta.put(janela, new String[]{"RF janela expansível", "serie-3"});
        for (Map.Entry<XYChart.Series<String, Number>, String[]> e : meta.entrySet()) {
            grafPrevisao.getData().add(e.getKey());
            Graficos.classe(e.getKey(), e.getValue()[1]);
            cPrevisao.adicionarLegenda(e.getValue()[0], e.getValue()[1], "linha");
            String nome = e.getValue()[0];
            Graficos.tooltips(e.getKey(), d -> nome + " · " + d.getXValue() + ": " + Formatos.decimal(d.getYValue().doubleValue(), 1) + " focos");
        }
        cPrevisao.setDados(new String[]{"Mês", "Real", "RF fixo", "RF janela"}, () -> linhas);
        cPrevisao.estado(ChartCard.Estado.CONTEUDO);
    }

    private void exibirMatriz(ClassificadorNivel.Resultado r) {
        NivelAtividade[] n = NivelAtividade.values();
        String[] rotulos = new String[n.length];
        for (int i = 0; i < n.length; i++) rotulos[i] = n[i].toString();
        matriz.setDados(rotulos, r.metricas().matriz());
        Metricas.Classificacao m = r.metricas();
        List<String[]> linhas = new ArrayList<>();
        for (int i = 0; i < n.length; i++) {
            linhas.add(new String[]{rotulos[i], Formatos.decimal(m.precisao()[i], 3), Formatos.decimal(m.revocacao()[i], 3),
                    Formatos.decimal(m.f1()[i], 3), Formatos.inteiro(r.distribuicaoTeste()[i])});
        }
        cMatriz.setDados(new String[]{"Classe", "Precisão", "Revocação", "F1", "Suporte"}, () -> linhas);
        cMatriz.setExtra(Chip.de("F1 macro " + Formatos.decimal(m.f1Macro(), 3), Icones.GRADE, Chip.Variante.DESTAQUE));
        cMatriz.estado(ChartCard.Estado.CONTEUDO);
    }

    private void exibirImportancia(PrevisaoFocos.Resultado r) {
        double[] imp = r.importancia();
        double soma = 0;
        for (double v : imp) soma += v;
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < imp.length; i++) idx.add(i);
        Ordenacoes.ordenar(idx, (a, b) -> Double.compare(imp[b], imp[a]));
        List<BarrasHorizontais.Item> itens = new ArrayList<>();
        List<String[]> linhas = new ArrayList<>();
        for (int k = 0; k < idx.size(); k++) {
            int i = idx.get(k);
            double pct = soma == 0 ? 0 : 100 * imp[i] / soma;
            itens.add(new BarrasHorizontais.Item(BaseMensal.VARIAVEIS[i], pct, Formatos.decimal(pct, 1) + "%", k == 0 ? "" : "suave", k == 0,
                    BaseMensal.VARIAVEIS[i] + ": " + Formatos.decimal(pct, 1) + "% da importância total"));
            linhas.add(new String[]{BaseMensal.VARIAVEIS[i], Formatos.decimal(pct, 2) + "%"});
        }
        importancia.setItens(itens);
        cImportancia.setDados(new String[]{"Variável", "Importância"}, () -> linhas);
        cImportancia.estado(ChartCard.Estado.CONTEUDO);
    }

    private void exibirHotspots() {
        if (resultado == null) return;
        boolean db = "DBSCAN".equals(metodo.getSelectedToggle().getUserData());
        ClusterizacaoHotspots.Resultado c = db ? resultado.dbscan() : resultado.kMeans();
        tabHotspots.setItems(FXCollections.observableArrayList(c.hotspots()));
        cHotspots.setSubtitulo(c.metodo() + " · " + c.hotspots().size() + " grupos"
                + (c.ruido() > 0 ? " · " + Formatos.inteiro(c.ruido()) + " focos isolados (ruído)" : ""));
        cHotspots.estado(ChartCard.Estado.CONTEUDO);
    }

    private void exibirCotovelo(double[] wcss) {
        grafCotovelo.getData().clear();
        XYChart.Series<Number, Number> s = new XYChart.Series<>();
        for (int k = 1; k <= wcss.length; k++) s.getData().add(new XYChart.Data<>(k, wcss[k - 1]));
        ((NumberAxis) grafCotovelo.getXAxis()).setUpperBound(Math.max(2, wcss.length));
        grafCotovelo.getData().add(s);
        Graficos.classe(s, "serie-ano-recente");
        Graficos.tooltips(s, d -> "k = " + d.getXValue() + ": " + Formatos.decimal(d.getYValue().doubleValue(), 1) + " km²");
        cCotovelo.estado(ChartCard.Estado.CONTEUDO);
    }

    @FXML
    private void verNoMapa() {
        ctx.navegar(Pagina.MAPA);
    }

    private void escreverSobre() {
        String[][] secoes = {
                {"Da ordenação ao aprendizado. ", "A base é ordenada por município → data (Merge Sort, estável) e agregada numa única varredura linear (control break) em séries município × mês."},
                {"Random Forest. ", "Conjunto de árvores treinadas em amostras bootstrap; prevê pela média (regressão) ou voto (classificação). Comparado a baselines: persistência e média histórica."},
                {"Classes desbalanceadas. ", "88% dos município-mês de treino não têm focos; o classificador usa amostragem balanceada, trocando acurácia por F1 macro."},
                {"DBSCAN. ", "Agrupa focos densos (≥ minPts vizinhos num raio eps, em km) e isola ruído; o K-Means exige escolher k (método do cotovelo)."},
                {"Deep Learning (trabalho futuro). ", "CNNs de segmentação (U-Net) sobre imagens Sentinel-2/Landsat, com índices NBR/NDVI, poderiam mapear cicatrizes de queimada usando estes focos como rótulos fracos."}
        };
        for (String[] s : secoes) {
            Text t = new Text(s[0]);
            t.getStyleClass().add("texto-destaque");
            Text c = new Text(s[1] + "\n\n");
            c.getStyleClass().add("texto-corpo");
            textoSobre.getChildren().addAll(t, c);
        }
    }

    @Override
    public void demonstrar(Runnable concluido) {
        if (resultado == null) executar();
        concluido.run();
    }
}
