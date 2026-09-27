package br.unip.aps.sorting;

import java.util.Comparator;
import java.util.function.ToLongFunction;

/**
 * Contrato comum de todos os algoritmos de ordenacao (padrao de projeto <b>Strategy</b>).
 *
 * <p>O codigo cliente ({@link ServicoOrdenacao}, benchmark, dashboard) depende apenas desta
 * interface; trocar de algoritmo e trocar de estrategia, sem alterar o restante do sistema.
 * O criterio (data, bioma, municipio...) e injetado como {@link Comparator}, de modo que qualquer
 * algoritmo ordena por qualquer criterio. As implementacoes nao usam {@code Collections.sort},
 * {@code Arrays.sort}, {@code List.sort} nem {@code Stream.sorted}.</p>
 *
 * <p>Implementacoes devem ser <i>stateless</i> (sem campos mutaveis) para poderem ser usadas
 * em varias threads ao mesmo tempo.</p>
 */
public interface SortAlgorithm {

    /** @return nome do algoritmo para exibicao (ex.: "Merge Sort") */
    String nome();

    /** @return ficha de complexidade e propriedades */
    Complexidade complexidade();

    /** @return explicacao curta do funcionamento (dashboard / relatorio) */
    String descricao();

    /**
     * Ordena in-place o array instrumentado.
     *
     * @param a   array a ordenar (todas as operacoes sao contabilizadas)
     * @param <T> tipo dos elementos
     */
    <T> void ordenar(InstrumentedArray<T> a);

    /**
     * Indica se o algoritmo precisa de uma chave numerica em vez de um comparador (Radix Sort).
     *
     * @return {@code true} se exigir chave numerica
     */
    default boolean exigeChaveNumerica() {
        return false;
    }

    /**
     * Ordena um vetor in-place e devolve as metricas da execucao.
     *
     * @param dados      vetor (sera modificado)
     * @param comparador criterio
     * @param <T>        tipo
     * @return metricas (comparacoes, trocas, atribuicoes, acessos, tempo)
     */
    default <T> OperationMetrics ordenar(T[] dados, Comparator<? super T> comparador) {
        return ordenar(dados, comparador, null);
    }

    /**
     * Ordena um vetor in-place e devolve as metricas da execucao.
     *
     * @param dados         vetor (sera modificado)
     * @param comparador    criterio
     * @param chaveNumerica chave inteira equivalente (necessaria apenas para Radix), ou {@code null}
     * @param <T>           tipo
     * @return metricas da execucao
     */
    default <T> OperationMetrics ordenar(T[] dados, Comparator<? super T> comparador,
                                         ToLongFunction<? super T> chaveNumerica) {
        OperationCounter contador = new OperationCounter();
        InstrumentedArray<T> a = new InstrumentedArray<>(dados, comparador, chaveNumerica, contador);
        contador.iniciar();
        try {
            ordenar(a);
        } finally {
            contador.parar();
        }
        return contador.snapshot();
    }
}
