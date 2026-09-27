package br.unip.aps.sorting.algorithms;

import br.unip.aps.sorting.Complexidade;
import br.unip.aps.sorting.InstrumentedArray;
import br.unip.aps.sorting.SortAlgorithm;

/** Heap Sort (Williams, 1964). */
public final class HeapSort implements SortAlgorithm {
    @Override
    public String nome() {
        return "Heap Sort";
    }

    @Override
    public Complexidade complexidade() {
        return new Complexidade("O(n log n)", "O(n log n)", "O(n log n)", "O(1)", false, true, false, true);
    }

    @Override
    public String descricao() {
        return "Monta um max-heap e extrai repetidamente o maior elemento para o fim do vetor; "
                + "O(n log n) garantido sem memoria extra, porem nao estavel.";
    }

    @Override
    public <T> void ordenar(InstrumentedArray<T> a) {
        int n = a.length();
        for (int k = n / 2 - 1; k >= 0; k--) {
            afundar(a, k, n);
        }
        while (n > 1) {
            a.swap(0, --n);
            afundar(a, 0, n);
        }
    }

    private <T> void afundar(InstrumentedArray<T> a, int k, int n) {
        while (2 * k + 1 < n) {
            int j = 2 * k + 1;
            if (j + 1 < n && a.less(j, j + 1)) {
                j++;
            }
            if (!a.less(k, j)) {
                break;
            }
            a.swap(k, j);
            k = j;
        }
    }
}
