package br.unip.aps.benchmark;

import br.unip.aps.sorting.CenarioEntrada;
import br.unip.aps.sorting.CriterioOrdenacao;

/** Medicao de um caso do benchmark (um algoritmo, um criterio, um cenario, um tamanho). */
public record BenchmarkResult(String algoritmo, CriterioOrdenacao criterio, CenarioEntrada cenario, int n,
                              int repeticoes, double mediaNs, double desvioNs, long minNs, long maxNs,
                              long comparacoes, long trocas, long atribuicoes, long acessos,
                              boolean verificado) {
    public double mediaMs() {
        return mediaNs / 1e6;
    }

    public double desvioMs() {
        return desvioNs / 1e6;
    }

    public double coeficienteVariacao() {
        return mediaNs == 0 ? 0 : desvioNs / mediaNs;
    }

    public double referenciaNLogN() {
        return n <= 1 ? 0 : n * (Math.log(n) / Math.log(2));
    }

    public double referenciaN2() {
        return (double) n * (n - 1) / 2.0;
    }
}
