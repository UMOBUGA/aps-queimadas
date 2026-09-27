package br.unip.aps.benchmark;

import br.unip.aps.analysis.EstatisticaDescritiva;
import br.unip.aps.sorting.CenarioEntrada;
import br.unip.aps.sorting.CriterioOrdenacao;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * Estima empiricamente a ordem de crescimento de cada algoritmo a partir das medicoes.
 *
 * <p>Se o custo segue T(n) ≈ c·n^k, entao log T = log c + k·log n: uma regressao linear no grafico
 * log-log fornece o expoente k (a inclinacao). Esperamos k ≈ 2 para O(n²), k ≈ 1,0–1,15 para
 * O(n log n) na faixa de tamanhos medida e k ≈ 1 para os lineares (Radix, melhores casos).
 * E a "prova experimental" da analise assintotica, discutida na dissertacao.</p>
 */
public final class AnaliseComplexidade {

    private AnaliseComplexidade() { }

    /**
     * Expoente empirico de um algoritmo em um criterio/cenario.
     *
     * @param algoritmo       nome
     * @param criterio        criterio
     * @param cenario         cenario
     * @param expoenteTempo   inclinacao log-log do tempo
     * @param expoenteComparacoes inclinacao log-log das comparacoes (NaN se nao houver comparacoes)
     * @param r2Tempo         qualidade do ajuste do tempo
     * @param classificacao   interpretacao ("~O(n²)", "~O(n log n)", "~O(n)")
     */
    public record Estimativa(String algoritmo, CriterioOrdenacao criterio, CenarioEntrada cenario,
                             double expoenteTempo, double expoenteComparacoes, double r2Tempo,
                             String classificacao) { }

    /**
     * @param resultados medicoes do benchmark
     * @return uma estimativa por (algoritmo, criterio, cenario) com ao menos 3 tamanhos distintos
     */
    public static List<Estimativa> estimar(List<BenchmarkResult> resultados) {
        Map<String, List<BenchmarkResult>> grupos = new LinkedHashMap<>();
        for (BenchmarkResult r : resultados) {
            grupos.computeIfAbsent(r.algoritmo() + '|' + r.criterio() + '|' + r.cenario(), k -> new ArrayList<>()).add(r);
        }
        List<Estimativa> est = new ArrayList<>();
        for (List<BenchmarkResult> g : grupos.values()) {
            List<BenchmarkResult> validos = new ArrayList<>();
            for (BenchmarkResult r : g) if (r.n() >= 50) validos.add(r);
            if (validos.size() < 3) continue;
            double kTempo = inclinacao(validos, r -> r.minNs());
            double r2 = ajuste(validos, r -> r.minNs());
            boolean temComparacoes = validos.get(0).comparacoes() > 0;
            double kComp = temComparacoes ? inclinacao(validos, r -> r.comparacoes()) : Double.NaN;
            BenchmarkResult ref = validos.get(0);
            est.add(new Estimativa(ref.algoritmo(), ref.criterio(), ref.cenario(), kTempo, kComp, r2,
                    classificar(temComparacoes ? kComp : kTempo)));
        }
        return est;
    }

    /**
     * @param k expoente estimado
     * @return classe de complexidade mais proxima
     */
    public static String classificar(double k) {
        if (Double.isNaN(k)) return "indeterminado";
        if (k < 0.5) return "~O(1)/sublinear";
        if (k < 1.05) return "~O(n)";
        if (k < 1.5) return "~O(n log n)";
        if (k < 1.8) return "~O(n^1,5)";
        return "~O(n²)";
    }

    private static double inclinacao(List<BenchmarkResult> g, ToDoubleFunction<BenchmarkResult> y) {
        return regressao(g, y).inclinacao();
    }

    private static double ajuste(List<BenchmarkResult> g, ToDoubleFunction<BenchmarkResult> y) {
        return regressao(g, y).r2();
    }

    private static EstatisticaDescritiva.Regressao regressao(List<BenchmarkResult> g, ToDoubleFunction<BenchmarkResult> f) {
        double[] x = new double[g.size()];
        double[] y = new double[g.size()];
        for (int i = 0; i < g.size(); i++) {
            x[i] = Math.log(g.get(i).n());
            y[i] = Math.log(Math.max(1.0, f.applyAsDouble(g.get(i))));
        }
        return EstatisticaDescritiva.regressaoLinear(x, y);
    }
}
