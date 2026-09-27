package br.unip.aps.sorting.algorithms;

import br.unip.aps.sorting.Complexidade;
import br.unip.aps.sorting.InstrumentedArray;
import br.unip.aps.sorting.SortAlgorithm;

/** Selection Sort (ordenacao por selecao). */
public final class SelectionSort implements SortAlgorithm {
    @Override
    public String nome() {
        return "Selection Sort";
    }

    @Override
    public Complexidade complexidade() {
        return new Complexidade("O(n²)", "O(n²)", "O(n²)", "O(1)", false, true, true, true);
    }

    @Override
    public String descricao() {
        return "Seleciona o menor elemento da parte nao ordenada e o coloca na proxima posicao; "
                + "sempre n(n-1)/2 comparacoes, mas no maximo n-1 trocas.";
    }

    @Override
    public <T> void ordenar(InstrumentedArray<T> a) {
        int n = a.length();
        for (int i = 0; i < n - 1; i++) {
            int min = i;
            for (int j = i + 1; j < n; j++) {
                if (a.less(j, min)) {
                    min = j;
                }
            }
            if (min != i) {
                a.swap(i, min);
            }
        }
    }
}
