package br.unip.aps.ui;

import br.unip.aps.analysis.Contagem;
import br.unip.aps.analysis.Estatisticas;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.ui.componentes.BarrasHorizontais;
import br.unip.aps.ui.componentes.CalendarioHeatmap;
import br.unip.aps.ui.componentes.ChartCard;
import br.unip.aps.ui.componentes.Chip;
import br.unip.aps.ui.componentes.DonutChart;
import br.unip.aps.ui.componentes.Graficos;
import br.unip.aps.ui.componentes.Icones;
import br.unip.aps.ui.componentes.KpiCard;
import br.unip.aps.util.Formatos;
import javafx.fxml.FXML;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

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
    private final UiContexto ctx;

    @FXML private KpiCard kTotal, kAnoAnterior, kAnoRecente, kVariacao, kMunicipios, kPico, kLider, kBioma;
    @FXML private ChartCard cMensal, cBiomas, cMunicipios, cHoras, cCalendario;

    private final AreaChart<String, Number> mensal = new AreaChart<>(new CategoryAxis(), new NumberAxis());
    private final DonutChart donut = new DonutChart();
    private final BarrasHorizontais top10 = new BarrasHorizontais();
    private final BarChart<String, Number> horas = new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final CalendarioHeatmap calendario = new CalendarioHeatmap();
    private boolean pendente = true;
    private boolean visivel;

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
        horas.setLegendVisible(false);
        horas.setAnimated(false);
        horas.setVerticalGridLinesVisible(false);
        horas.setCategoryGap(4);
        Graficos.eixoLog((NumberAxis) horas.getYAxis(), false);
        cMensal.conteudo(mensal);
        cBiomas.conteudo(donut);
        cMunicipios.conteudo(top10);
        cHoras.conteudo(horas);
        cCalendario.conteudo(calendario);
        for (ChartCard c : List.of(cMensal, cBiomas, cMunicipios, cHoras, cCalendario)) {
            ctx.registrarGrafico(c.getTitulo(), c);
            c.estado(ChartCard.Estado.CARREGANDO);
        }
        ctx.focosFiltradosProperty().addListener((o, a, n) -> {
            pendente = true;
            if (visivel) atualizar();
        });
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
            for (ChartCard c : List.of(cMensal, cBiomas, cMunicipios, cHoras, cCalendario)) c.estado(ChartCard.Estado.VAZIO);
            for (KpiCard k : List.of(kTotal, kAnoAnterior, kAnoRecente, kVariacao, kMunicipios, kPico, kLider, kBioma)) {
                k.valor("—").contexto("Sem focos para os filtros", KpiCard.Tendencia.NEUTRA);
            }
            kTotal.valor("0");
            return;
        }
        Estatisticas est = new Estatisticas(focos);
        List<Integer> anos = new ArrayList<>(est.porAno().keySet());
        int anoMax = ctx.baseProperty().get() == null ? anos.get(anos.size() - 1) : ctx.baseProperty().get().anos().get(ctx.baseProperty().get().anos().size() - 1);
        montarKpis(est, anos);
        montarMensal(est, anos, anoMax);
        montarBiomas(est);
        montarMunicipios(est);
        montarHoras(est);
        montarCalendario(focos, anos);
    }

    private void montarKpis(Estatisticas est, List<Integer> anos) {
        Map<Integer, Long> porAno = est.porAno();
        Map<YearMonth, Long> serie = est.serieMensal();
        double[] sparkTotal = serie.values().stream().mapToDouble(Long::doubleValue).toArray();
        kTotal.valor(Formatos.inteiro(est.total()))
                .contexto(anos.size() == 1 ? "Ano " + anos.get(0) : anos.get(0) + "–" + anos.get(anos.size() - 1) + " · " + serie.size() + " meses",
                        KpiCard.Tendencia.NEUTRA)
                .icone(KpiCard.EstiloIcone.DESTAQUE)
                .sparkline(sparkTotal, "spark-recente");

        if (anos.size() >= 2) {
            int a1 = anos.get(anos.size() - 2), a2 = anos.get(anos.size() - 1);
            long v1 = porAno.get(a1), v2 = porAno.get(a2);
            double var = v1 == 0 ? Double.NaN : (v2 - v1) * 100.0 / v1;
            boolean subiu = v2 > v1;
            kAnoAnterior.titulo("Focos em " + a1);
            kAnoRecente.titulo("Focos em " + a2);
            kAnoAnterior.valor(Formatos.inteiro(v1))
                    .contexto("Média de " + Formatos.inteiro(Math.round(v1 / 12.0)) + " focos/mês", KpiCard.Tendencia.NEUTRA)
                    .sparkline(mensais(est, a1), "spark-anterior");
            kAnoRecente.valor(Formatos.inteiro(v2))
                    .contexto(Double.isNaN(var) ? "sem base de comparação" : sinal(var) + " vs " + a1,
                            subiu ? KpiCard.Tendencia.ALERTA : KpiCard.Tendencia.BOA)
                    .sparkline(mensais(est, a2), "spark-recente");
            kVariacao.valor(Double.isNaN(var) ? "—" : sinal(var))
                    .contexto(Formatos.inteiro(v1) + " → " + Formatos.inteiro(v2) + " focos", KpiCard.Tendencia.NEUTRA)
                    .icone(subiu ? KpiCard.EstiloIcone.ALERTA : KpiCard.EstiloIcone.SUCESSO)
                    .dica("Aumento de focos é sinalizado como alerta (vermelho); redução como positivo (verde).");
        } else {
            for (KpiCard k : List.of(kAnoAnterior, kVariacao)) {
                k.valor("—").contexto("Selecione dois anos para comparar", KpiCard.Tendencia.NEUTRA).semSparkline();
            }
            kAnoRecente.valor(Formatos.inteiro(est.total())).contexto("Ano " + anos.get(0), KpiCard.Tendencia.NEUTRA)
                    .sparkline(mensais(est, anos.get(0)), "spark-recente");
        }

        Set<String> m1 = new HashSet<>(), m2 = new HashSet<>();
        for (FocoIncendio f : ctx.focosFiltradosProperty().get()) {
            if (anos.size() >= 2 && f.getAno() == anos.get(anos.size() - 2)) m1.add(f.getMunicipio());
            if (f.getAno() == anos.get(anos.size() - 1)) m2.add(f.getMunicipio());
        }
        m1.retainAll(m2);
        kMunicipios.valor(Formatos.inteiro(est.municipiosAfetados()))
                .contexto(anos.size() >= 2 ? Formatos.inteiro(m1.size()) + " com focos nos dois anos" : "com ao menos 1 foco",
                        KpiCard.Tendencia.NEUTRA);

        Map.Entry<YearMonth, Long> pico = est.mesPico();
        long noAno = est.porAno().getOrDefault(pico.getKey().getYear(), 1L);
        kPico.valor(rotuloMes(pico.getKey()))
                .contexto(Formatos.inteiro(pico.getValue()) + " focos · " + Formatos.decimal(100.0 * pico.getValue() / noAno, 1) + "% do ano",
                        KpiCard.Tendencia.NEUTRA)
                .icone(KpiCard.EstiloIcone.ALERTA);

        Contagem lider = est.topMunicipios(1).get(0);
        kLider.valorTexto(capitalizar(lider.chave()))
                .contexto(Formatos.inteiro(lider.total()) + " focos · " + Formatos.decimal(100.0 * lider.total() / est.total(), 1) + "% do total",
                        KpiCard.Tendencia.NEUTRA);

        Contagem bioma = est.porBioma().get(0);
        kBioma.valorTexto(bioma.chave())
                .contexto(Formatos.decimal(100.0 * bioma.total() / est.total(), 1) + "% dos focos", KpiCard.Tendencia.NEUTRA)
                .icone(KpiCard.EstiloIcone.NEUTRO);
    }

    private static double[] mensais(Estatisticas est, int ano) {
        long[] m = est.porMes(ano);
        double[] r = new double[12];
        for (int i = 0; i < 12; i++) r[i] = m[i];
        return r;
    }

    private static String sinal(double v) {
        return (v > 0 ? "+" : "") + Formatos.decimal(v, 1) + "%";
    }

    static String rotuloMes(YearMonth ym) {
        return Estatisticas.MESES[ym.getMonthValue() - 1] + "/" + ym.getYear();
    }

    static String capitalizar(String s) {
        StringBuilder sb = new StringBuilder();
        for (String p : s.toLowerCase(br.unip.aps.util.Textos.PT_BR).split(" ")) {
            if (!sb.isEmpty()) sb.append(' ');
            if (Set.of("de", "da", "do", "das", "dos", "e", "d'oeste").contains(p)) sb.append(p);
            else if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    private void montarMensal(Estatisticas est, List<Integer> anos, int anoMax) {
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
                s.getData().add(new XYChart.Data<>(Estatisticas.MESES[i], m[i]));
                tabela.get(i)[col] = Formatos.inteiro(m[i]);
            }
            col++;
            mensal.getData().add(s);
            String classe = ano == anoMax ? "serie-ano-recente" : "serie-ano-anterior";
            Graficos.classe(s, classe);
            final long tot = totalAno;
            Graficos.tooltips(s, d -> d.getXValue() + "/" + ano + ": " + Formatos.inteiro(d.getYValue().longValue()) + " focos ("
                    + Formatos.decimal(tot == 0 ? 0 : 100.0 * d.getYValue().longValue() / tot, 1) + "% do ano)");
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
            cMensal.setExtra(Chip.de("Pico: " + rotuloMes(pico.getKey()) + " — " + Formatos.inteiro(pico.getValue()),
                    Icones.MARCA, Chip.Variante.DESTAQUE));
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
                    i == 0 ? "" : "", i == 0,
                    (i + 1) + "º " + c.chave() + ": " + Formatos.inteiro(c.total()) + " focos (" + Formatos.decimal(pct, 1) + "% do total)"));
            linhas.add(new String[]{(i + 1) + "º " + c.chave(), Formatos.inteiro(c.total()), Formatos.decimal(pct, 2) + "%"});
        }
        top10.setItens(itens);
        if (!top.isEmpty()) cMunicipios.setExtra(Chip.de("Líder: " + capitalizar(top.get(0).chave()), Icones.LIDER, Chip.Variante.NEUTRO));
        cMunicipios.setDados(new String[]{"Município", "Focos", "% do total"}, () -> linhas);
        cMunicipios.estado(ChartCard.Estado.CONTEUDO);
    }

    private void montarHoras(Estatisticas est) {
        horas.getData().clear();
        long[] h = est.porHoraLocal();
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        List<String[]> linhas = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            s.getData().add(new XYChart.Data<>(String.valueOf(i), h[i]));
            linhas.add(new String[]{String.format("%02dh", i), Formatos.inteiro(h[i]),
                    Formatos.decimal(100.0 * h[i] / Math.max(1, est.total()), 1) + "%"});
        }
        horas.getData().add(s);
        Graficos.tooltips(s, d -> String.format("%sh–%sh: ", d.getXValue(), Integer.parseInt(d.getXValue()) + 1)
                + Formatos.inteiro(d.getYValue().longValue()) + " focos ("
                + Formatos.decimal(100.0 * d.getYValue().longValue() / Math.max(1, est.total()), 1) + "%)");
        int horaPico = 0;
        for (int i = 1; i < 24; i++) if (h[i] > h[horaPico]) horaPico = i;
        cHoras.setExtra(Chip.de(Formatos.decimal(100.0 * h[horaPico] / Math.max(1, est.total()), 1) + "% às " + horaPico + "h",
                Icones.RELOGIO, Chip.Variante.NEUTRO));
        cHoras.setDados(new String[]{"Hora local", "Focos", "%"}, () -> linhas);
        cHoras.estado(ChartCard.Estado.CONTEUDO);
    }

    private void montarCalendario(List<FocoIncendio> focos, List<Integer> anos) {
        Map<LocalDate, Long> porDia = new HashMap<>();
        for (FocoIncendio f : focos) porDia.merge(f.getData(), 1L, Long::sum);
        calendario.setDados(porDia, anos);
        long diasComFoco = porDia.size();
        cCalendario.setExtra(Chip.de(Formatos.inteiro(diasComFoco) + " dias com focos", Icones.CALENDARIO, Chip.Variante.NEUTRO));
        cCalendario.estado(ChartCard.Estado.CONTEUDO);
    }
}
