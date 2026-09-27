package br.unip.aps.busca;

import br.unip.aps.sorting.OperationCounter;

import java.util.Comparator;
import java.util.function.Function;

/** Busca sequencial e busca binaria (lowerBound/upperBound) sobre vetores, com contagem de operacoes. */
public final class Buscas {
    /** Faixa de posicoes [inicio, fim) de um vetor ordenado. */
    public record Intervalo(int inicio, int fim) {
        public int tamanho() {
            return Math.max(0, fim - inicio);
        }
    }

    private Buscas() {
    }

    /** Primeira posicao cuja chave e maior ou igual ao alvo (vetor ordenado pela chave). */
    public static <T, K> int lowerBound(T[] a, Function<? super T, ? extends K> chave, Comparator<? super K> c, K alvo,
                                        OperationCounter k) {
        int lo = 0, hi = a.length;
        while (lo < hi) {
            int meio = (lo + hi) >>> 1;
            k.leitura();
            if (k.comparar(c, chave.apply(a[meio]), alvo) < 0) lo = meio + 1;
            else hi = meio;
        }
        return lo;
    }

    /** Primeira posicao cuja chave e estritamente maior que o alvo (vetor ordenado pela chave). */
    public static <T, K> int upperBound(T[] a, Function<? super T, ? extends K> chave, Comparator<? super K> c, K alvo,
                                        OperationCounter k) {
        int lo = 0, hi = a.length;
        while (lo < hi) {
            int meio = (lo + hi) >>> 1;
            k.leitura();
            if (k.comparar(c, chave.apply(a[meio]), alvo) <= 0) lo = meio + 1;
            else hi = meio;
        }
        return lo;
    }

    /** Busca binaria do intervalo de chaves [de, ate]: duas buscas de O(log n). */
    public static <T, K> Intervalo intervaloBinario(T[] a, Function<? super T, ? extends K> chave, Comparator<? super K> c,
                                                    K de, K ate, OperationCounter k) {
        return new Intervalo(lowerBound(a, chave, c, de, k), upperBound(a, chave, c, ate, k));
    }

    /** Busca sequencial do intervalo [de, ate]: examina todos os n elementos, ordenados ou nao. */
    public static <T, K> int contarLinear(T[] a, Function<? super T, ? extends K> chave, Comparator<? super K> c,
                                          K de, K ate, OperationCounter k) {
        int achados = 0;
        for (T item : a) {
            k.leitura();
            K v = chave.apply(item);
            if (k.comparar(c, v, de) >= 0 && k.comparar(c, v, ate) <= 0) achados++;
        }
        return achados;
    }

    /** Busca sequencial por igualdade. */
    public static <T, K> int contarLinearIgual(T[] a, Function<? super T, ? extends K> chave, Comparator<? super K> c,
                                               K alvo, OperationCounter k) {
        int achados = 0;
        for (T item : a) {
            k.leitura();
            if (k.comparar(c, chave.apply(item), alvo) == 0) achados++;
        }
        return achados;
    }
}
