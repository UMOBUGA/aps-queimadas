package br.unip.aps.ui;

import br.unip.aps.analysis.Contagem;
import br.unip.aps.analysis.Estatisticas;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.ui.componentes.BarrasHorizontais;
import br.unip.aps.ui.componentes.CalendarioHeatmap;
import br.unip.aps.ui.componentes.ChartCard;
import br.unip.aps.ui.componentes.DonutChart;
import br.unip.aps.ui.componentes.Graficos;
import br.unip.aps.ui.tema.GerenciadorTema;
import br.unip.aps.util.Formatos;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Tela Visao geral. */
public class VisaoGeralController implements Pagina.Controlador {
    private static final String[] MESES_EXTENSO = {"janeiro", "fevereiro", "março", "abril", "maio", "junho",
            "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"};

    private final UiContexto ctx;

    @FXML private Label mLinha, mNumero, mUnidade;
    @FXML private TextFlow mApoio;
    @FXML private FlowPane destaques;
    @FXML private ChartCard cMensal, cBiomas, cMunicipios, cHoras, cCalendario, cBiomasMes, cComparar;

    private final ToggleButton tgLinear = new ToggleButton("Linear"), tgLog = new ToggleButton("Log");
    private final HBox escala = new HBox(tgLinear, tgLog);
    private final HBox multiplos = new HBox(28);
    private final ComboBox<YearMonth> cbA = new ComboBox<>(), cbB = new ComboBox<>();
    private final GridPane gradeComparar = new GridPane();
    private Estatisticas ultimaEst;
    private List<Integer> ultimosAnos;
    private int ultimoAnoMax;

    private final AreaChart<String, Number> mensal = new AreaChart<>(new CategoryAxis(), new NumberAxis());
    private final DonutChart donut = new DonutChart();
    private final BarrasHorizontais top10 = new BarrasHorizontais();
    private final BarChart<String, Number> horas = new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final CalendarioHeatmap calendario = new CalendarioHeatmap();
    private Timeline contagem;
    private boolean pendente = true;
    private boolean visivel;
    private boolean jaAnimou;

    public VisaoGeralController(UiContexto ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        mensal.setLegendVisible(false);
        mensal.setCreateSymbols(true);
        mensal.setAnimated(false);
        mensal.setVerticalGridLinesVisible(false);
        ((NumberAxis) mensal.getYAxis()).setMinorTickVisible(false);
        Graficos.eixoLog((NumberAxis) mensal.getYAxis(), false);
        ToggleGroup g = new ToggleGroup();
        tgLinear.setToggleGroup(g);
        tgLog.setToggleGroup(g);
        tgLinear.setSelected(true);
        escala.getStyleClass().add("segmented");
        escala.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        tgLog.setTooltip(new Tooltip("Escala logarítmica: os meses calmos e o pico ficam legíveis no mesmo gráfico"));
        g.selectedToggleProperty().addListener((o, a, n) -> {
            if (n == null) a.setSelected(true);
            else if (ultimaEst != null) montarMensal(ultimaEst, ultimosAnos, ultimoAnoMax);
        });
        montarComparacao();
        horas.setLegendVisible(false);
        horas.setAnimated(false);
        horas.setVerticalGridLinesVisible(false);
        horas.setCategoryGap(5);
        Graficos.eixoLog((NumberAxis) horas.getYAxis(), false);
        cMensal.conteudo(mensal);
        cBiomas.conteudo(donut);
        cMunicipios.conteudo(top10);
        cHoras.conteudo(horas);
        cCalendario.conteudo(calendario);
        ctx.registrarGrafico("Focos por mês", cMensal);
        ctx.registrarGrafico("Top 10 municípios", cMunicipios);
        ctx.registrarGrafico("Focos por bioma", cBiomas);
        ctx.registrarGrafico("Calendário de focos por dia", cCalendario);
        ctx.registrarGrafico("Focos por hora local", cHoras);
        ctx.registrarGrafico("Focos por bioma e mês", cBiomasMes);
        for (ChartCard c : cards()) c.estado(ChartCard.Estado.CARREGANDO);
        ctx.focosFiltradosProperty().addListener((o, a, n) -> {
            pendente = true;
            if (visivel) atualizar();
        });
    }

    private List<ChartCard> cards() {
        return List.of(cMensal, cBiomas, cMunicipios, cHoras, cCalendario, cBiomasMes);
    }

    @Override
    public void aoExibir() {
        visivel = true;
        if (pendente) atualizar();
    }

    @Override
    public void aoOcultar() {
        visivel = false;
    }

    private void atualizar() {
        pendente = false;
        List<FocoIncendio> focos = ctx.focosFiltradosProperty().get();
        if (focos.isEmpty()) {
            for (ChartCard c : cards()) c.estado(ChartCard.Estado.VAZIO);
            mLinha.setText("Nenhum foco atende aos filtros atuais");
            numero(0);
            mApoio.getChildren().setAll(texto("Remova algum filtro na barra acima ou clique no mês selecionado da faixa térmica para limpar o período.", false));
            destaques.getChildren().clear();
            return;
        }
        Estatisticas est = new Estatisticas(focos);
        List<Integer> anos = new ArrayList<>(est.porAno().keySet());
        int anoMax = ctx.baseProperty().get() == null ? anos.get(anos.size() - 1)
                : ctx.baseProperty().get().anos().get(ctx.baseProperty().get().anos().size() - 1);
        montarManchete(est, anos);
        montarDestaques(est, anos, focos);
        montarMensal(est, anos, anoMax);
        montarBiomas(est);
        montarMunicipios(est);
        montarHoras(est);
        montarCalendario(focos, anos);
        montarBiomasMes(focos, est, anos, anoMax);
        atualizarOpcoesComparacao();
    }

    private void montarManchete(Estatisticas est, List<Integer> anos) {
        Map<Integer, Long> porAno = est.porAno();
        int ano = anos.get(anos.size() - 1);
        long v = porAno.get(ano);
        FiltroGlobal f = ctx.filtroProperty().get();
        String onde = f != null && f.municipio() != null ? capitalizar(f.municipio())
                : f != null && f.biomas().size() == 1 ? "o bioma " + f.biomas().iterator().next() + " em São Paulo"
                : "São Paulo";
        String quando = f != null && f.de() != null
                ? (f.de().equals(f.ate()) || f.ate() == null ? "Em " + mesExtenso(f.de()) : "Entre " + rotuloMes(f.de()) + " e " + rotuloMes(f.ate()))
                : "Em " + ano;
        mLinha.setText(quando + ", " + onde + " registrou");
        boolean periodo = f != null && f.de() != null;
        long principal = periodo ? est.total() : v;
        numero(principal);
        mUnidade.setText(principal == 1 ? "foco de incêndio" : "focos de incêndio");

        List<Text> apoio = new ArrayList<>();
        if (!periodo && anos.size() >= 2) {
            int a1 = anos.get(anos.size() - 2);
            long v1 = porAno.get(a1);
            if (v1 > 0) {
                double razao = (double) v / v1;
                if (razao >= 1.95) {
                    apoio.add(texto(Formatos.decimal(razao, 1) + " vezes", true));
                    apoio.add(texto(" o total de " + a1 + " (" + Formatos.inteiro(v1) + " focos). ", false));
                } else {
                    double var = (v - v1) * 100.0 / v1;
                    apoio.add(texto((var > 0 ? "+" : "") + Formatos.decimal(var, 1) + "%", true));
                    apoio.add(texto(" em relação a " + a1 + " (" + Formatos.inteiro(v1) + " focos). ", false));
                }
            }
        }
        Map.Entry<YearMonth, Long> pico = est.mesPico();
        if (pico != null) {
            long noAno = porAno.getOrDefault(pico.getKey().getYear(), 1L);
            apoio.add(texto("O pico foi em ", false));
            apoio.add(texto(mesExtenso(pico.getKey()), true));
            apoio.add(texto(", com " + Formatos.inteiro(pico.getValue()) + " focos: "
                    + Formatos.decimal(100.0 * pico.getValue() / noAno, 1) + "% de todo o ano.", false));
        }
        mApoio.getChildren().setAll(apoio);
    }

    private void numero(long alvo) {
        if (contagem != null) contagem.stop();
        if (jaAnimou) {
            br.unip.aps.ui.componentes.Movimento.contar(mNumero, Formatos.inteiro(alvo));
            return;
        }
        if (!GerenciadorTema.get().animacoesProperty().get() || alvo < 10) {
            mNumero.setText(Formatos.inteiro(alvo));
            jaAnimou = true;
            return;
        }
        jaAnimou = true;
        SimpleDoubleProperty p = new SimpleDoubleProperty(0);
        p.addListener((o, a, n) -> mNumero.setText(Formatos.inteiro(Math.round(n.doubleValue()))));
        contagem = new Timeline(new KeyFrame(Duration.millis(900),
                new KeyValue(p, alvo, javafx.animation.Interpolator.SPLINE(0.16, 1, 0.3, 1))));
        mNumero.setText("0");
        contagem.play();
    }

    private static Text texto(String s, boolean forte) {
        Text t = new Text(s);
        t.getStyleClass().add(forte ? "manchete-apoio-forte" : "manchete-apoio-texto");
        return t;
    }

    private void montarDestaques(Estatisticas est, List<Integer> anos, List<FocoIncendio> focos) {
        destaques.getChildren().clear();
        Set<String> m1 = new HashSet<>(), m2 = new HashSet<>();
        for (FocoIncendio f : focos) {
            if (anos.size() >= 2 && f.getAno() == anos.get(anos.size() - 2)) m1.add(f.getMunicipio());
            if (f.getAno() == anos.get(anos.size() - 1)) m2.add(f.getMunicipio());
        }
        m1.retainAll(m2);
        destaque(Formatos.inteiro(est.municipiosAfetados()), false, false,
                anos.size() >= 2 ? "municípios, " + Formatos.inteiro(m1.size()) + " nos dois anos" : "municípios afetados");

        Map.Entry<YearMonth, Long> pico = est.mesPico();
        if (pico != null) {
            destaque(Estatisticas.MESES[pico.getKey().getMonthValue() - 1] + "/" + String.valueOf(pico.getKey().getYear()).substring(2),
                    false, true, "mês de pico");
        }
        Contagem lider = est.topMunicipios(1).get(0);
        destaque(capitalizar(lider.chave()), true, false, "município líder, " + Formatos.inteiro(lider.total()) + " focos");

        Contagem bioma = est.porBioma().get(0);
        destaque(bioma.chave(), true, false, "bioma com mais focos (" + Formatos.decimal(100.0 * bioma.total() / est.total(), 1) + "%)");

        long[] h = est.porHoraLocal();
        int hp = 0;
        for (int i = 1; i < 24; i++) if (h[i] > h[hp]) hp = i;
        destaque(hp + "h", false, false, Formatos.decimal(100.0 * h[hp] / Math.max(1, est.total()), 1) + "% das detecções");
    }

    private void destaque(String valor, boolean texto, boolean alerta, String rotulo) {
        Label v = new Label(valor);
        v.getStyleClass().add("destaque-num");
        if (texto) v.getStyleClass().add("texto");
        if (alerta) v.getStyleClass().add("alerta");
        v.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        Label r = new Label(rotulo);
        r.getStyleClass().add("destaque-rot");
        r.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        VBox box = new VBox(2, v, r);
        box.setAlignment(Pos.BOTTOM_LEFT);
        box.getStyleClass().add("destaque");
        if (!destaques.getChildren().isEmpty()) box.getStyleClass().add("com-fio");
        box.setMinHeight(74);
        destaques.getChildren().add(box);
    }

    static String rotuloMes(YearMonth ym) {
        return Estatisticas.MESES[ym.getMonthValue() - 1] + "/" + ym.getYear();
    }

    private static String mesExtenso(YearMonth ym) {
        return MESES_EXTENSO[ym.getMonthValue() - 1] + " de " + ym.getYear();
    }

    static String capitalizar(String s) {
        return br.unip.aps.util.Textos.nomeProprio(s);
    }

    private void montarMensal(Estatisticas est, List<Integer> anos, int anoMax) {
        ultimaEst = est;
        ultimosAnos = anos;
        ultimoAnoMax = anoMax;
        boolean log = tgLog.isSelected();
        Graficos.eixoLog((NumberAxis) mensal.getYAxis(), log);
        mensal.getData().clear();
        cMensal.limparLegenda();
        Map<YearMonth, Long> serie = est.serieMensal();
        Map.Entry<YearMonth, Long> pico = est.mesPico();
        List<String[]> tabela = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            String[] linha = new String[anos.size() + 1];
            linha[0] = Estatisticas.MESES[m - 1];
            tabela.add(linha);
        }
        int col = 1;
        for (Integer ano : anos) {
            XYChart.Series<String, Number> s = new XYChart.Series<>();
            s.setName(String.valueOf(ano));
            long[] m = est.porMes(ano);
            long totalAno = 0;
            for (long v : m) totalAno += v;
            for (int i = 0; i < 12; i++) {
                s.getData().add(new XYChart.Data<>(Estatisticas.MESES[i], log ? Math.log10(1 + m[i]) : m[i]));
                tabela.get(i)[col] = Formatos.inteiro(m[i]);
            }
            col++;
            mensal.getData().add(s);
            Graficos.classe(s, ano == anoMax ? "serie-ano-recente" : "serie-ano-anterior");
            final long tot = totalAno;
            Graficos.tooltips(s, d -> {
                long v = m[indiceMes(d.getXValue())];
                return d.getXValue() + "/" + ano + ": " + Formatos.inteiro(v) + " focos ("
                        + Formatos.decimal(tot == 0 ? 0 : 100.0 * v / tot, 1) + "% do ano)";
            });
            if (pico != null && pico.getKey().getYear() == ano) {
                XYChart.Data<String, Number> dp = s.getData().get(pico.getKey().getMonthValue() - 1);
                if (dp.getNode() != null) dp.getNode().getStyleClass().add("ponto-pico");
                dp.nodeProperty().addListener((o, a, n) -> {
                    if (n != null) n.getStyleClass().add("ponto-pico");
                });
            }
            cMensal.adicionarLegenda(String.valueOf(ano), ano == anoMax ? "ano-recente" : "ano-anterior");
        }
        if (pico != null) {
            Label num = new Label(Formatos.inteiro(pico.getValue()));
            num.getStyleClass().add("rotulo-pico");
            Label sub = new Label("focos no pico, " + rotuloMes(pico.getKey()));
            sub.getStyleClass().add("rotulo-pico-sub");
            VBox anot = new VBox(0, num, sub);
            anot.setAlignment(Pos.TOP_RIGHT);
            HBox extra = new HBox(20, escala, anot);
            extra.setAlignment(Pos.TOP_RIGHT);
            extra.setFillHeight(false);
            cMensal.setExtra(extra);
        } else {
            cMensal.setExtra(escala);
        }
        String[] cab = new String[anos.size() + 1];
        cab[0] = "Mês";
        for (int i = 0; i < anos.size(); i++) cab[i + 1] = String.valueOf(anos.get(i));
        cMensal.setDados(cab, () -> tabela);
        cMensal.estado(serie.isEmpty() ? ChartCard.Estado.VAZIO : ChartCard.Estado.CONTEUDO);
    }

    private void montarBiomas(Estatisticas est) {
        List<DonutChart.Item> itens = new ArrayList<>();
        List<String[]> linhas = new ArrayList<>();
        for (Contagem c : est.porBioma()) {
            itens.add(new DonutChart.Item(c.chave(), c.total(), Graficos.classeBioma(c.chave())));
            linhas.add(new String[]{c.chave(), Formatos.inteiro(c.total()), Formatos.decimal(100.0 * c.total() / est.total(), 1) + "%"});
        }
        donut.setItens(itens);
        cBiomas.setDados(new String[]{"Bioma", "Focos", "%"}, () -> linhas);
        cBiomas.estado(ChartCard.Estado.CONTEUDO);
    }

    private void montarMunicipios(Estatisticas est) {
        List<Contagem> top = est.topMunicipios(10);
        List<BarrasHorizontais.Item> itens = new ArrayList<>();
        List<String[]> linhas = new ArrayList<>();
        for (int i = 0; i < top.size(); i++) {
            Contagem c = top.get(i);
            double pct = 100.0 * c.total() / est.total();
            itens.add(new BarrasHorizontais.Item(capitalizar(c.chave()), c.total(), Formatos.inteiro(c.total()),
                    i == 0 ? "lider" : "", i == 0,
                    (i + 1) + "º " + c.chave() + ": " + Formatos.inteiro(c.total()) + " focos (" + Formatos.decimal(pct, 1) + "% do total)"));
            linhas.add(new String[]{(i + 1) + "º " + c.chave(), Formatos.inteiro(c.total()), Formatos.decimal(pct, 2) + "%"});
        }
        top10.setItens(itens);
        cMunicipios.setExtra();
        cMunicipios.setDados(new String[]{"Município", "Focos", "% do total"}, () -> linhas);
        cMunicipios.estado(ChartCard.Estado.CONTEUDO);
    }

    private void montarHoras(Estatisticas est) {
        horas.getData().clear();
        long[] h = est.porHoraLocal();
        long max = 1;
        for (long v : h) max = Math.max(max, v);
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        List<String[]> linhas = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            XYChart.Data<String, Number> d = new XYChart.Data<>(String.valueOf(i), h[i]);
            double t = (double) h[i] / max;
            String classe = t >= 0.66 ? "hora-pico" : t >= 0.2 ? "hora-alta" : t >= 0.05 ? "hora-media" : "hora-baixa";
            d.nodeProperty().addListener((o, a, n) -> {
                if (n != null) n.getStyleClass().add(classe);
            });
            s.getData().add(d);
            linhas.add(new String[]{String.format("%02dh", i), Formatos.inteiro(h[i]),
                    Formatos.decimal(100.0 * h[i] / Math.max(1, est.total()), 1) + "%"});
        }
        horas.getData().add(s);
        for (XYChart.Data<String, Number> d : s.getData()) {
            Node n = d.getNode();
            if (n != null) {
                double t = d.getYValue().doubleValue() / max;
                n.getStyleClass().add(t >= 0.66 ? "hora-pico" : t >= 0.2 ? "hora-alta" : t >= 0.05 ? "hora-media" : "hora-baixa");
            }
        }
        Graficos.tooltips(s, d -> String.format("%sh–%sh: ", d.getXValue(), Integer.parseInt(d.getXValue()) + 1)
                + Formatos.inteiro(d.getYValue().longValue()) + " focos ("
                + Formatos.decimal(100.0 * d.getYValue().longValue() / Math.max(1, est.total()), 1) + "%)");
        cHoras.setExtra();
        cHoras.setDados(new String[]{"Hora local", "Focos", "%"}, () -> linhas);
        cHoras.estado(ChartCard.Estado.CONTEUDO);
    }

    private void montarCalendario(List<FocoIncendio> focos, List<Integer> anos) {
        Map<LocalDate, Long> porDia = new HashMap<>();
        for (FocoIncendio f : focos) porDia.merge(f.getData(), 1L, Long::sum);
        calendario.setDados(porDia, anos);
        Label num = new Label(Formatos.inteiro(porDia.size()));
        num.getStyleClass().add("rotulo-pico");
        Label sub = new Label("dias com focos");
        sub.getStyleClass().add("rotulo-pico-sub");
        VBox anot = new VBox(0, num, sub);
        anot.setAlignment(Pos.TOP_RIGHT);
        cCalendario.setExtra(anot);
        cCalendario.estado(ChartCard.Estado.CONTEUDO);
    }

    private static int indiceMes(String abrev) {
        for (int i = 0; i < 12; i++) if (Estatisticas.MESES[i].equals(abrev)) return i;
        return 0;
    }

    private void montarBiomasMes(List<FocoIncendio> focos, Estatisticas est, List<Integer> anos, int anoMax) {
        List<String> biomas = new ArrayList<>();
        for (Contagem c : est.porBioma()) biomas.add(c.chave());
        Map<String, long[][]> dados = new HashMap<>();
        for (String b : biomas) dados.put(b, new long[anos.size()][12]);
        for (FocoIncendio f : focos) {
            long[][] d = dados.get(f.getBioma());
            int ia = anos.indexOf(f.getAno());
            if (d != null && ia >= 0) d[ia][f.getMes() - 1]++;
        }
        long max = 1;
        for (long[][] d : dados.values()) for (long[] a : d) for (long v : a) max = Math.max(max, v);
        double bruto = max / 4.0, ordem = Math.pow(10, Math.floor(Math.log10(Math.max(1, bruto))));
        double passo = bruto <= ordem ? ordem : bruto <= 2 * ordem ? 2 * ordem : bruto <= 5 * ordem ? 5 * ordem : 10 * ordem;
        double topo = Math.ceil(max / passo) * passo;
        List<String[]> linhas = new ArrayList<>();
        multiplos.getChildren().clear();
        for (String b : biomas) {
            NumberAxis y = new NumberAxis(0, topo, passo);
            y.setMinorTickVisible(false);
            Graficos.eixoLog(y, false);
            AreaChart<String, Number> g = new AreaChart<>(new CategoryAxis(), y);
            g.setLegendVisible(false);
            g.setAnimated(false);
            g.setCreateSymbols(false);
            g.setVerticalGridLinesVisible(false);
            g.setPrefHeight(240);
            long totalBioma = 0;
            for (int ia = 0; ia < anos.size(); ia++) {
                int ano = anos.get(ia);
                long[] serie = dados.get(b)[ia];
                XYChart.Series<String, Number> s = new XYChart.Series<>();
                s.setName(String.valueOf(ano));
                for (int m = 0; m < 12; m++) {
                    s.getData().add(new XYChart.Data<>(Estatisticas.MESES[m], serie[m]));
                    linhas.add(new String[]{b, String.valueOf(ano), Estatisticas.MESES[m], Formatos.inteiro(serie[m])});
                    totalBioma += serie[m];
                }
                g.getData().add(s);
                Graficos.classe(s, ano == anoMax ? "serie-ano-recente" : "serie-ano-anterior");
                Graficos.tooltips(s, d -> b + ", " + d.getXValue() + "/" + ano + ": " + Formatos.inteiro(d.getYValue().longValue()) + " focos");
            }
            Label t = new Label(b);
            t.getStyleClass().add("multiplo-titulo");
            Label tot = new Label(Formatos.inteiro(totalBioma) + " focos");
            tot.getStyleClass().add("multiplo-total");
            HBox cab = new HBox(10, t, tot);
            cab.setAlignment(Pos.BASELINE_LEFT);
            VBox painel = new VBox(4, cab, g);
            HBox.setHgrow(painel, Priority.ALWAYS);
            painel.setMinWidth(0);
            painel.setPrefWidth(10);
            multiplos.getChildren().add(painel);
        }
        cBiomasMes.limparLegenda();
        for (Integer ano : anos) cBiomasMes.adicionarLegenda(String.valueOf(ano), ano == anoMax ? "ano-recente" : "ano-anterior");
        cBiomasMes.conteudo(multiplos);
        cBiomasMes.setDados(new String[]{"Bioma", "Ano", "Mês", "Focos"}, () -> linhas);
        cBiomasMes.estado(biomas.isEmpty() ? ChartCard.Estado.VAZIO : ChartCard.Estado.CONTEUDO);
    }

    private void montarComparacao() {
        StringConverter<YearMonth> conv = new StringConverter<>() {
            @Override
            public String toString(YearMonth ym) {
                return ym == null ? "" : mesExtenso(ym);
            }

            @Override
            public YearMonth fromString(String s) {
                return null;
            }
        };
        for (ComboBox<YearMonth> cb : List.of(cbA, cbB)) {
            cb.setConverter(conv);
            cb.setVisibleRowCount(12);
            cb.valueProperty().addListener((o, a, n) -> atualizarComparacao());
        }
        cbA.setAccessibleText("Primeiro período da comparação");
        cbB.setAccessibleText("Segundo período da comparação");
        Label x = new Label("comparado a");
        x.getStyleClass().add("t-small");
        HBox seletores = new HBox(12, cbA, x, cbB);
        seletores.setAlignment(Pos.CENTER_LEFT);
        gradeComparar.getStyleClass().add("grade-comparar");
        gradeComparar.setHgap(48);
        gradeComparar.setVgap(12);
        cComparar.conteudo(new VBox(20, seletores, gradeComparar));
        cComparar.estado(ChartCard.Estado.CARREGANDO);
    }

    private void atualizarOpcoesComparacao() {
        var base = ctx.baseProperty().get();
        if (base == null) return;
        List<YearMonth> meses = new ArrayList<>(new Estatisticas(base.getFocos()).serieMensal().keySet());
        if (meses.isEmpty() || meses.equals(cbA.getItems())) return;
        cbA.getItems().setAll(meses);
        cbB.getItems().setAll(meses);
        YearMonth ultimo = meses.get(meses.size() - 1);
        YearMonth b = YearMonth.of(ultimo.getYear(), 8);
        if (!meses.contains(b)) b = ultimo;
        YearMonth a = b.minusYears(1);
        if (!meses.contains(a)) a = meses.get(0);
        cbA.setValue(a);
        cbB.setValue(b);
        cComparar.estado(ChartCard.Estado.CONTEUDO);
    }

    private void atualizarComparacao() {
        var base = ctx.baseProperty().get();
        YearMonth a = cbA.getValue(), b = cbB.getValue();
        gradeComparar.getChildren().clear();
        if (base == null || a == null || b == null) return;
        List<FocoIncendio> fa = new ArrayList<>(), fb = new ArrayList<>();
        for (FocoIncendio f : base.getFocos()) {
            YearMonth ym = YearMonth.from(f.getDataHora());
            if (ym.equals(a)) fa.add(f);
            if (ym.equals(b)) fb.add(f);
        }
        String[] cab = {"", mesExtenso(a), mesExtenso(b), "Variação"};
        for (int i = 0; i < cab.length; i++) {
            Label l = new Label(cab[i]);
            l.getStyleClass().add("comparar-cabecalho");
            gradeComparar.add(l, i, 0);
        }
        linhaComparacao(1, "Focos", fa.size(), fb.size());
        linhaComparacao(2, "Municípios com focos", distintos(fa, true), distintos(fb, true));
        linhaComparacao(3, "Dias com focos", distintos(fa, false), distintos(fb, false));
        linhaTexto(4, "Município com mais focos", lider(fa), lider(fb));
        linhaTexto(5, "Bioma com mais focos", fa.isEmpty() ? "—" : new Estatisticas(fa).porBioma().get(0).chave(),
                fb.isEmpty() ? "—" : new Estatisticas(fb).porBioma().get(0).chave());
    }

    private static long distintos(List<FocoIncendio> focos, boolean municipio) {
        Set<Object> s = new HashSet<>();
        for (FocoIncendio f : focos) s.add(municipio ? f.getMunicipio() : f.getData());
        return s.size();
    }

    private static String lider(List<FocoIncendio> focos) {
        if (focos.isEmpty()) return "—";
        Contagem c = new Estatisticas(focos).topMunicipios(1).get(0);
        return capitalizar(c.chave()) + " (" + Formatos.inteiro(c.total()) + ")";
    }

    private void linhaComparacao(int linha, String rotulo, long va, long vb) {
        String var = va == 0 ? (vb == 0 ? "sem variação" : "novo")
                : (vb >= va ? "+" : "−") + Formatos.decimal(Math.abs(100.0 * (vb - va) / va), 0) + "%";
        Label v = new Label(var);
        v.getStyleClass().add("comparar-variacao");
        if (vb > va) v.getStyleClass().add("alta");
        else if (vb < va) v.getStyleClass().add("queda");
        gradeComparar.addRow(linha, rotulo(rotulo), valor(Formatos.inteiro(va)), valor(Formatos.inteiro(vb)), v);
    }

    private void linhaTexto(int linha, String rotulo, String a, String b) {
        Label la = valor(a), lb = valor(b);
        la.getStyleClass().add("texto");
        lb.getStyleClass().add("texto");
        gradeComparar.addRow(linha, rotulo(rotulo), la, lb);
    }

    private static Label rotulo(String s) {
        Label l = new Label(s);
        l.getStyleClass().add("comparar-rotulo");
        return l;
    }

    private static Label valor(String s) {
        Label l = new Label(s);
        l.getStyleClass().add("comparar-valor");
        return l;
    }
}
