package br.unip.aps.sorting.algorithms;

import br.unip.aps.sorting.Complexidade;
import br.unip.aps.sorting.InstrumentedArray;
import br.unip.aps.sorting.SortAlgorithm;

/** Quick Sort com dois pivos (Yaroslavskiy, 2009), a base do Arrays.sort do Java para tipos primitivos. */
public final class DualPivotQuickSort implements SortAlgorithm {
    private static final int PEQUENO = 16;

    @Override
    public String nome() {
        return "Quick Sort 2 pivôs";
    }

    @Override
    public Complexidade complexidade() {
        return new Complexidade("O(n log n)", "O(n log n)", "O(n²)", "O(log n)", false, true, false, true);
    }

    @Override
    public String descricao() {
        return "Divide em tres partes com dois pivos (menores que p, entre p e q, maiores que q); "
                + "faz menos acessos a memoria que o Quick Sort classico. Pivos nos tercos do trecho.";
    }

    @Override
    public <T> void ordenar(InstrumentedArray<T> a) {
        ordenar(a, 0, a.length() - 1);
    }

    private <T> void ordenar(InstrumentedArray<T> a, int lo, int hi) {
        if (hi - lo + 1 <= PEQUENO) {
            for (int i = lo + 1; i <= hi; i++) {
                for (int j = i; j > lo && a.less(j, j - 1); j--) a.swap(j, j - 1);
            }
            return;
        }
        int terco = (hi - lo + 1) / 3;
        a.swap(lo, lo + terco);
        a.swap(hi, hi - terco);
        if (a.less(hi, lo)) a.swap(lo, hi);

        int lt = lo + 1;
        int gt = hi - 1;
        int k = lt;
        while (k <= gt) {
            if (a.less(k, lo)) {
                a.swap(k, lt++);
            } else if (a.less(hi, k)) {
                while (k < gt && a.less(hi, gt)) gt--;
                a.swap(k, gt--);
                if (a.less(k, lo)) a.swap(k, lt++);
            }
            k++;
        }
        lt--;
        gt++;
        a.swap(lo, lt);
        a.swap(hi, gt);

        ordenar(a, lo, lt - 1);
        if (a.less(lt, gt)) ordenar(a, lt + 1, gt - 1);
        ordenar(a, gt + 1, hi);
    }
}
