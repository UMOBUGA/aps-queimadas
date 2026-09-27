package br.unip.aps.sorting.algorithms;

import br.unip.aps.sorting.InstrumentedArray;

/**
 * Intercalacao (merge) estavel de duas metades ordenadas, compartilhada por Merge Sort e Tim Sort.
 */
final class Intercalacao {

    private Intercalacao() { }

    /**
     * Intercala a[lo..mid] e a[mid+1..hi] (ambas ordenadas) usando o vetor auxiliar.
     *
     * <p>Estabilidade: em caso de empate o elemento da metade esquerda e copiado primeiro
     * (so pega da direita quando ela e <b>estritamente</b> menor), preservando a ordem original
     * de chaves iguais.</p>
     *
     * @param a   vetor
     * @param aux auxiliar de mesmo tamanho
     * @param lo  inicio
     * @param mid fim da metade esquerda
     * @param hi  fim da metade direita
     * @param <T> tipo
     */
    static <T> void intercalar(InstrumentedArray<T> a, InstrumentedArray<T> aux, int lo, int mid, int hi) {
        for (int k = lo; k <= hi; k++) {
            aux.set(k, a.get(k));
        }
        int i = lo;
        int j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if (i > mid) {
                a.set(k, aux.get(j++));
            } else if (j > hi) {
                a.set(k, aux.get(i++));
            } else {
                T direita = aux.get(j);
                T esquerda = aux.get(i);
                if (a.lessValues(direita, esquerda)) {
                    a.set(k, direita);
                    j++;
                } else {
                    a.set(k, esquerda);
                    i++;
                }
            }
        }
    }
}
