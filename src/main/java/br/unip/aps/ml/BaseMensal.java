package br.unip.aps.ml;

import br.unip.aps.model.FocoIncendio;
import br.unip.aps.sorting.CriterioOrdenacao;
import br.unip.aps.sorting.Criterios;
import br.unip.aps.sorting.OperationMetrics;
import br.unip.aps.sorting.Ordem;
import br.unip.aps.sorting.algorithms.MergeSort;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** Tabela municipio x mes com as variaveis usadas pelos modelos. */
public final class BaseMensal {
    public static final String[] VARIAVEIS = {"mes_sin", "mes_cos", "lat", "lon", "frac_cerrado",
            "lag1", "lag2", "lag3", "media_hist", "estado_lag1"};

    /** Uma observacao municipio/mes. */
    public record Linha(String municipio, YearMonth mes, double[] x, int focos) { }

    private final List<String> municipios = new ArrayList<>();
    private final YearMonth inicio;
    private final int nMeses;
    private final int[][] contagem;
    private final double[] lat;
    private final double[] lon;
    private final double[] fracCerrado;
    private final long[] totalEstado;
    private final OperationMetrics metricasOrdenacao;

    /** Constroi a tabela a partir dos focos. */
    public BaseMensal(List<FocoIncendio> focos) {
        if (focos.isEmpty()) throw new IllegalArgumentException("Sem focos para montar a base mensal.");
        Criterios crit = Criterios.composto(List.of(
                new Criterios.Nivel(CriterioOrdenacao.MUNICIPIO, Ordem.CRESCENTE),
                new Criterios.Nivel(CriterioOrdenacao.DATA, Ordem.CRESCENTE)));
        FocoIncendio[] ord = focos.toArray(new FocoIncendio[0]);
        metricasOrdenacao = new MergeSort().ordenar(ord, crit.comparador());

        YearMonth min = null, max = null;
        for (FocoIncendio f : ord) {
            YearMonth ym = YearMonth.from(f.getDataHora());
            if (min == null || ym.isBefore(min)) min = ym;
            if (max == null || ym.isAfter(max)) max = ym;
        }
        inicio = YearMonth.of(min.getYear(), 1);
        nMeses = (int) (inicio.until(YearMonth.of(max.getYear(), 12), java.time.temporal.ChronoUnit.MONTHS) + 1);

        int nMun = 0;
        for (int i = 0; i < ord.length; i++) {
            if (i == 0 || !ord[i].getMunicipio().equals(ord[i - 1].getMunicipio())) nMun++;
        }
        contagem = new int[nMun][nMeses];
        lat = new double[nMun];
        lon = new double[nMun];
        fracCerrado = new double[nMun];
        totalEstado = new long[nMeses];

        int m = -1;
        int focosMun = 0, cerrado = 0;
        double somaLat = 0, somaLon = 0;
        for (int i = 0; i < ord.length; i++) {
            FocoIncendio f = ord[i];
            if (i == 0 || !f.getMunicipio().equals(ord[i - 1].getMunicipio())) {
                if (m >= 0) fecharMunicipio(m, focosMun, cerrado, somaLat, somaLon);
                m++;
                municipios.add(f.getMunicipio());
                focosMun = cerrado = 0;
                somaLat = somaLon = 0;
            }
            int t = indice(YearMonth.from(f.getDataHora()));
            contagem[m][t]++;
            totalEstado[t]++;
            focosMun++;
            somaLat += f.getLatitude();
            somaLon += f.getLongitude();
            if (f.getBioma().toLowerCase(java.util.Locale.ROOT).startsWith("cerrado")) cerrado++;
        }
        fecharMunicipio(m, focosMun, cerrado, somaLat, somaLon);
    }

    private void fecharMunicipio(int m, int n, int cerrado, double somaLat, double somaLon) {
        lat[m] = somaLat / n;
        lon[m] = somaLon / n;
        fracCerrado[m] = (double) cerrado / n;
    }

    private int indice(YearMonth ym) {
        return (int) inicio.until(ym, java.time.temporal.ChronoUnit.MONTHS);
    }

    /** Gera as observacoes de todos os municipios nos meses do ano informado. */
    public List<Linha> linhasDoAno(int ano) {
        return linhasEntre(YearMonth.of(ano, 1), YearMonth.of(ano, 12));
    }

    /** Gera as observacoes de todos os municipios no intervalo [de, ate]. */
    public List<Linha> linhasEntre(YearMonth de, YearMonth ate) {
        List<Linha> r = new ArrayList<>();
        for (YearMonth ym = de; !ym.isAfter(ate); ym = ym.plusMonths(1)) {
            int t = indice(ym);
            if (t < 0 || t >= nMeses) continue;
            for (int m = 0; m < municipios.size(); m++) {
                r.add(new Linha(municipios.get(m), ym, variaveis(m, t, ym), contagem[m][t]));
            }
        }
        return r;
    }

    private double[] variaveis(int m, int t, YearMonth ym) {
        double ang = 2 * Math.PI * (ym.getMonthValue() - 1) / 12.0;
        double soma = 0;
        for (int k = 0; k < t; k++) soma += contagem[m][k];
        return new double[]{
                Math.sin(ang), Math.cos(ang), lat[m], lon[m], fracCerrado[m],
                lag(m, t, 1), lag(m, t, 2), lag(m, t, 3),
                t == 0 ? 0 : soma / t,
                t >= 1 ? totalEstado[t - 1] : 0};
    }

    private int lag(int m, int t, int k) {
        return t - k >= 0 ? contagem[m][t - k] : 0;
    }

    public List<String> municipios() {
        return List.copyOf(municipios);
    }

    public YearMonth inicio() {
        return inicio;
    }

    public int meses() {
        return nMeses;
    }

    public long totalEstado(YearMonth ym) {
        int t = indice(ym);
        return t < 0 || t >= nMeses ? 0 : totalEstado[t];
    }

    public OperationMetrics metricasOrdenacao() {
        return metricasOrdenacao;
    }
}
