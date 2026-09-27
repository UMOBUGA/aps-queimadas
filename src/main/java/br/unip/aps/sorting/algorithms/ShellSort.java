package br.unip.aps.sorting.algorithms;

import br.unip.aps.sorting.Complexidade;
import br.unip.aps.sorting.InstrumentedArray;
import br.unip.aps.sorting.SortAlgorithm;

/** Shell Sort (Donald Shell, 1959) com a sequencia de incrementos de Ciura (2001). */
public final class ShellSort implements SortAlgorithm {
    private static final int[] CIURA = {1, 4, 10, 23, 57, 132, 301, 701};

    @Override
    public String nome() {
        return "Shell Sort";
    }

    @Override
    public Complexidade complexidade() {
        return new Complexidade("O(n log n)", "~O(n^1,25)", "O(n^1,5)", "O(1)", false, true, false, true);
    }

    @Override
    public String descricao() {
        return "Insertion Sort com saltos decrescentes (sequencia de Ciura): move elementos distantes "
                + "cedo e termina com h=1 sobre um vetor quase ordenado.";
    }

    @Override
    public <T> void ordenar(InstrumentedArray<T> a) {
        int n = a.length();
        for (int h : incrementos(n)) {
            for (int i = h; i < n; i++) {
                T x = a.get(i);
                int j = i;
                while (j >= h) {
                    T y = a.get(j - h);
                    if (a.compareValues(y, x) <= 0) {
                        break;
                    }
                    a.set(j, y);
                    j -= h;
                }
                if (j != i) {
                    a.set(j, x);
                }
            }
        }
    }

    static int[] incrementos(int n) {
        int[] tmp = new int[64];
        int k = 0;
        for (int h : CIURA) {
            if (h >= n) break;
            tmp[k++] = h;
        }
        if (k == CIURA.length) {
            long h = tmp[k - 1];
            while (true) {
                h = (long) (h * 2.25);
                if (h >= n) break;
                tmp[k++] = (int) h;
            }
        }
        if (k == 0) {
            tmp[k++] = 1;
        }
        int[] r = new int[k];
        for (int i = 0; i < k; i++) r[i] = tmp[k - 1 - i];
        return r;
    }
}
