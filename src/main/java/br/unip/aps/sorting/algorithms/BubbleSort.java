package br.unip.aps.sorting.algorithms;

import br.unip.aps.sorting.Complexidade;
import br.unip.aps.sorting.InstrumentedArray;
import br.unip.aps.sorting.SortAlgorithm;

/** Bubble Sort (ordenacao por flutuacao / "bolha"). */
public final class BubbleSort implements SortAlgorithm {
    @Override
    public String nome() {
        return "Bubble Sort";
    }

    @Override
    public Complexidade complexidade() {
        return new Complexidade("O(n)", "O(n²)", "O(n²)", "O(1)", true, true, true, true);
    }

    @Override
    public String descricao() {
        return "Compara pares adjacentes e troca os que estao fora de ordem; o maior elemento "
                + "'flutua' para o fim a cada passada. Para cedo se uma passada nao fizer trocas.";
    }

    @Override
    public <T> void ordenar(InstrumentedArray<T> a) {
        int limite = a.length() - 1;
        while (limite > 0) {
            int ultimaTroca = 0;
            for (int j = 0; j < limite; j++) {
                if (a.compare(j, j + 1) > 0) {
                    a.swap(j, j + 1);
                    ultimaTroca = j;
                }
            }
            limite = ultimaTroca;
        }
    }
}
