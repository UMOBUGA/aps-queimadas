package br.unip.aps.sorting.algorithms;

import br.unip.aps.sorting.Complexidade;
import br.unip.aps.sorting.InstrumentedArray;
import br.unip.aps.sorting.SortAlgorithm;

/** Insertion Sort (ordenacao por insercao). */
public final class InsertionSort implements SortAlgorithm {
    @Override
    public String nome() {
        return "Insertion Sort";
    }

    @Override
    public Complexidade complexidade() {
        return new Complexidade("O(n)", "O(n²)", "O(n²)", "O(1)", true, true, true, true);
    }

    @Override
    public String descricao() {
        return "Insere cada elemento na posicao correta do prefixo ja ordenado, deslocando os maiores; "
                + "custo proporcional ao numero de inversoes (otimo para dados quase ordenados).";
    }

    @Override
    public <T> void ordenar(InstrumentedArray<T> a) {
        int n = a.length();
        for (int i = 1; i < n; i++) {
            T x = a.get(i);
            int j = i - 1;
            while (j >= 0) {
                T y = a.get(j);
                if (a.compareValues(y, x) <= 0) {
                    break;
                }
                a.set(j + 1, y);
                j--;
            }
            if (j + 1 != i) {
                a.set(j + 1, x);
            }
        }
    }
}
