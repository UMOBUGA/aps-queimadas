package br.unip.aps.benchmark;

import br.unip.aps.sorting.CenarioEntrada;
import br.unip.aps.sorting.CriterioOrdenacao;

/**
 * Medicao de um caso do benchmark (um algoritmo, um criterio, um cenario, um tamanho).
 *
 * @param algoritmo    nome do algoritmo
 * @param criterio     criterio de ordenacao
 * @param cenario      disposicao inicial
 * @param n            tamanho da entrada
 * @param repeticoes   execucoes medidas
 * @param mediaNs      tempo medio (ns)
 * @param desvioNs     desvio-padrao amostral do tempo (ns)
 * @param minNs        menor tempo observado (ns)
 * @param maxNs        maior tempo observado (ns)
 * @param comparacoes  comparacoes (deterministicas para a mesma entrada)
 * @param trocas       trocas
 * @param atribuicoes  escritas em array
 * @param acessos      leituras + escritas
 * @param verificado   resultado conferido como ordenado em todas as repeticoes
 */
public record BenchmarkResult(String algoritmo, CriterioOrdenacao criterio, CenarioEntrada cenario, int n,
                              int repeticoes, double mediaNs, double desvioNs, long minNs, long maxNs,
                              long comparacoes, long trocas, long atribuicoes, long acessos,
                              boolean verificado) {

    /** @return tempo medio em ms */
    public double mediaMs() {
        return mediaNs / 1e6;
    }

    /** @return desvio-padrao em ms */
    public double desvioMs() {
        return desvioNs / 1e6;
    }

    /** @return coeficiente de variacao (desvio/media), indicador de estabilidade da medicao */
    public double coeficienteVariacao() {
        return mediaNs == 0 ? 0 : desvioNs / mediaNs;
    }

    /** @return referencia teorica n·log2(n) */
    public double referenciaNLogN() {
        return n <= 1 ? 0 : n * (Math.log(n) / Math.log(2));
    }

    /** @return referencia teorica n²/2 (comparacoes de um O(n²) simples) */
    public double referenciaN2() {
        return (double) n * (n - 1) / 2.0;
    }
}
