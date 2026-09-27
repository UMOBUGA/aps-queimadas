package br.unip.aps.sorting;

import java.util.Comparator;
import java.util.function.ToLongFunction;

/** Contrato comum de todos os algoritmos de ordenacao (padrao de projeto Strategy). */
public interface SortAlgorithm {
    String nome();

    Complexidade complexidade();

    String descricao();

    /** Ordena in-place o array instrumentado. */
    <T> void ordenar(InstrumentedArray<T> a);

    /** Indica se o algoritmo precisa de uma chave numerica em vez de um comparador (Radix Sort). */
    default boolean exigeChaveNumerica() {
        return false;
    }

    /** Ordena um vetor in-place e devolve as metricas da execucao. */
    default <T> OperationMetrics ordenar(T[] dados, Comparator<? super T> comparador) {
        return ordenar(dados, comparador, null);
    }

    /** Ordena um vetor in-place e devolve as metricas da execucao. */
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
