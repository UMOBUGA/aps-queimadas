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

/**
 * Tabela municipio x mes (contagem de focos) com as variaveis explicativas usadas pelos modelos.
 *
 * <h2>Como a ordenacao prepara os dados para o ML</h2>
 * <p>Os focos sao ordenados por <b>municipio → data</b> com o Merge Sort do projeto (estavel,
 * O(n log n)). Com a base ordenada, a agregacao vira uma unica varredura linear: todos os focos
 * de um municipio estao contiguos e em ordem cronologica, entao basta detectar a troca de
 * municipio e de mes ("control break"). Esta e a etapa de pre-processamento que o enunciado
 * descreve — a ordenacao prepara os dados para as tecnicas de aprendizado de maquina.</p>
 *
 * <h2>Variaveis (features) de cada linha municipio/mes t</h2>
 * <ul>
 *   <li>{@code mes_sin}, {@code mes_cos}: sazonalidade codificada no circulo (dez fica perto de jan);</li>
 *   <li>{@code lat}, {@code lon}: centroide dos focos do municipio;</li>
 *   <li>{@code frac_cerrado}: fracao dos focos do municipio no bioma Cerrado;</li>
 *   <li>{@code lag1..lag3}: focos do municipio em t-1, t-2 e t-3;</li>
 *   <li>{@code media_hist}: media mensal do municipio em todos os meses anteriores a t;</li>
 *   <li>{@code estado_lag1}: total de focos do estado em t-1 (intensidade regional da estacao).</li>
 * </ul>
 * <p>Nenhuma variavel usa informacao do proprio mes t ou do futuro (sem vazamento de dados). Meses
 * anteriores ao inicio da serie contam como zero.</p>
 */
public final class BaseMensal {

    /** Nomes das variaveis explicativas, na ordem do vetor {@link Linha#x()}. */
    public static final String[] VARIAVEIS = {"mes_sin", "mes_cos", "lat", "lon", "frac_cerrado",
            "lag1", "lag2", "lag3", "media_hist", "estado_lag1"};

    /**
     * Uma observacao municipio/mes.
     *
     * @param municipio nome
     * @param mes       ano-mes
     * @param x         variaveis explicativas (ordem de {@link #VARIAVEIS})
     * @param focos     focos observados no mes (alvo)
     */
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

    /**
     * Constroi a tabela a partir dos focos.
     *
     * @param focos focos (qualquer ordem; serao ordenados por municipio e data)
     * @throws IllegalArgumentException se a lista estiver vazia
     */
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

        // contagem de municipios distintos: numa base ordenada, e o numero de "quebras"
        int nMun = 0;
        for (int i = 0; i < ord.length; i++) {
            if (i == 0 || !ord[i].getMunicipio().equals(ord[i - 1].getMunicipio())) nMun++;
        }
        contagem = new int[nMun][nMeses];
        lat = new double[nMun];
        lon = new double[nMun];
        fracCerrado = new double[nMun];
        totalEstado = new long[nMeses];

        // varredura unica com "control break" por municipio
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

    /**
     * Gera as observacoes de todos os municipios nos meses do ano informado.
     *
     * @param ano ano
     * @return linhas (municipios x 12 meses)
     */
    public List<Linha> linhasDoAno(int ano) {
        return linhasEntre(YearMonth.of(ano, 1), YearMonth.of(ano, 12));
    }

    /**
     * Gera as observacoes de todos os municipios no intervalo [de, ate].
     *
     * @param de  mes inicial
     * @param ate mes final
     * @return linhas
     */
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

    /** @return municipios, em ordem alfabetica pt-BR */
    public List<String> municipios() {
        return List.copyOf(municipios);
    }

    /** @return primeiro mes da serie */
    public YearMonth inicio() {
        return inicio;
    }

    /** @return numero de meses da serie */
    public int meses() {
        return nMeses;
    }

    /**
     * @param ym ano-mes
     * @return total de focos no estado naquele mes
     */
    public long totalEstado(YearMonth ym) {
        int t = indice(ym);
        return t < 0 || t >= nMeses ? 0 : totalEstado[t];
    }

    /** @return metricas da ordenacao (municipio → data) usada no pre-processamento */
    public OperationMetrics metricasOrdenacao() {
        return metricasOrdenacao;
    }
}
