package br.unip.aps.ui;

import br.unip.aps.analysis.Contagem;
import br.unip.aps.analysis.Estatisticas;
import br.unip.aps.analysis.FiltroFocos;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.util.Formatos;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Aba "Visao geral": indicadores (KPIs) e graficos de focos por mes (2023 x 2024), bioma,
 * municipios e hora local.
 */
public class VisaoGeralController {

    private static final String TODOS = "(todos)";

    private final UiContexto ctx;

    @FXML private ComboBox<String> cbBioma;
    @FXML private TextField tfMunicipio;
    @FXML private Label lblFiltro;
    @FXML private FlowPane kpis;
    @FXML private LineChart<String, Number> graficoMensal;
    @FXML private PieChart graficoBiomas;
    @FXML private BarChart<Number, String> graficoMunicipios;
    @FXML private BarChart<String, Number> graficoHoras;

    /** @param ctx contexto injetado */
    public VisaoGeralController(UiContexto ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        ctx.baseProperty().addListener((o, a, b) -> {
            List<String> biomas = new ArrayList<>();
            biomas.add(TODOS);
            if (b != null) biomas.addAll(b.biomas());
            cbBioma.setItems(FXCollections.observableArrayList(biomas));
            cbBioma.getSelectionModel().selectFirst();
            atualizar();
        });
        ctx.registrarGrafico("Focos por mês", graficoMensal);
        ctx.registrarGrafico("Focos por bioma", graficoBiomas);
        ctx.registrarGrafico("Top 10 municípios", graficoMunicipios);
        ctx.registrarGrafico("Focos por hora local", graficoHoras);
    }

    @FXML
    private void aplicar() {
        atualizar();
    }

    @FXML
    private void limpar() {
        cbBioma.getSelectionModel().selectFirst();
        tfMunicipio.clear();
        atualizar();
    }

    private void atualizar() {
        BaseDeFocos base = ctx.baseProperty().get();
        if (base == null) return;
        String bioma = cbBioma.getValue();
        FiltroFocos filtro = new FiltroFocos(Set.of(), bioma == null || bioma.equals(TODOS) ? Set.of() : Set.of(bioma),
                tfMunicipio.getText(), null, null);
        List<FocoIncendio> focos = base.filtrar(filtro);
        lblFiltro.setText(Formatos.inteiro(focos.size()) + " focos — " + filtro.descricao());
        if (focos.isEmpty()) {
            kpis.getChildren().clear();
            graficoMensal.getData().clear();
            graficoBiomas.getData().clear();
            graficoMunicipios.getData().clear();
            graficoHoras.getData().clear();
            ctx.aviso("Filtro vazio", "Nenhum foco atende ao filtro: " + filtro.descricao());
            return;
        }
        Estatisticas est = new Estatisticas(focos);
        montarKpis(est);
        montarMensal(est);
        montarBiomas(est);
        montarMunicipios(est);
        montarHoras(est);
    }

    private void montarKpis(Estatisticas est) {
        kpis.getChildren().clear();
        kpis.getChildren().add(card("Total de focos", Formatos.inteiro(est.total()), null));
        Map<Integer, Long> porAno = est.porAno();
        List<Integer> anos = new ArrayList<>(porAno.keySet());
        for (Integer a : anos) kpis.getChildren().add(card("Focos em " + a, Formatos.inteiro(porAno.get(a)), null));
        if (anos.size() >= 2) {
            long x = porAno.get(anos.get(anos.size() - 2)), y = porAno.get(anos.get(anos.size() - 1));
            String v = x == 0 ? "—" : (y >= x ? "+" : "") + Formatos.decimal((y - x) * 100.0 / x, 1) + "%";
            kpis.getChildren().add(card("Variação " + anos.get(anos.size() - 2) + "→" + anos.get(anos.size() - 1), v,
                    y > x ? "kpi-alerta" : "kpi-ok"));
        }
        kpis.getChildren().add(card("Municípios afetados", Formatos.inteiro(est.municipiosAfetados()), null));
        Map.Entry<YearMonth, Long> pico = est.mesPico();
        if (pico != null) {
            kpis.getChildren().add(card("Mês de pico", Estatisticas.MESES[pico.getKey().getMonthValue() - 1] + "/"
                    + pico.getKey().getYear() + " (" + Formatos.inteiro(pico.getValue()) + ")", "kpi-alerta"));
        }
        List<Contagem> top = est.topMunicipios(1);
        if (!top.isEmpty()) kpis.getChildren().add(card("Município líder", top.get(0).chave() + " (" + top.get(0).total() + ")", null));
    }

    private static VBox card(String titulo, String valor, String estilo) {
        Label t = new Label(titulo);
        t.getStyleClass().add("kpi-titulo");
        Label v = new Label(valor);
        v.getStyleClass().add("kpi-valor");
        VBox box = new VBox(2, t, v);
        box.getStyleClass().add("kpi");
        if (estilo != null) box.getStyleClass().add(estilo);
        return box;
    }

    private void montarMensal(Estatisticas est) {
        graficoMensal.getData().clear();
        for (Integer ano : est.porAno().keySet()) {
            XYChart.Series<String, Number> s = new XYChart.Series<>();
            s.setName(String.valueOf(ano));
            long[] m = est.porMes(ano);
            for (int i = 0; i < 12; i++) s.getData().add(new XYChart.Data<>(Estatisticas.MESES[i], m[i]));
            graficoMensal.getData().add(s);
        }
    }

    private void montarBiomas(Estatisticas est) {
        List<PieChart.Data> dados = new ArrayList<>();
        for (Contagem c : est.porBioma()) {
            dados.add(new PieChart.Data(c.chave() + " (" + Formatos.decimal(100.0 * c.total() / est.total(), 1) + "%)", c.total()));
        }
        graficoBiomas.setData(FXCollections.observableArrayList(dados));
    }

    private void montarMunicipios(Estatisticas est) {
        graficoMunicipios.getData().clear();
        XYChart.Series<Number, String> s = new XYChart.Series<>();
        List<Contagem> top = est.topMunicipios(10);
        for (int i = top.size() - 1; i >= 0; i--) {
            s.getData().add(new XYChart.Data<>(top.get(i).total(), top.get(i).chave()));
        }
        graficoMunicipios.getData().add(s);
    }

    private void montarHoras(Estatisticas est) {
        graficoHoras.getData().clear();
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        long[] h = est.porHoraLocal();
        for (int i = 0; i < 24; i++) {
            if (h[i] > 0) s.getData().add(new XYChart.Data<>(String.format("%02dh", i), h[i]));
        }
        graficoHoras.getData().add(s);
    }
}
