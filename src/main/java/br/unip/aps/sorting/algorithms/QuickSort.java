package br.unip.aps.sorting.algorithms;

import br.unip.aps.sorting.Complexidade;
import br.unip.aps.sorting.InstrumentedArray;
import br.unip.aps.sorting.SortAlgorithm;

/** Quick Sort (Hoare, 1962). */
public final class QuickSort implements SortAlgorithm {
    @Override
    public String nome() {
        return "Quick Sort";
    }

    @Override
    public Complexidade complexidade() {
        return new Complexidade("O(n log n)", "O(n log n)", "O(n²)", "O(log n)", false, true, false, true);
    }

    @Override
    public String descricao() {
        return "Particiona em torno de um pivo (mediana de tres) e ordena as partes recursivamente; "
                + "em media o mais rapido na pratica, mas O(n²) no pior caso e nao estavel.";
    }

    @Override
    public <T> void ordenar(InstrumentedArray<T> a) {
        ordenar(a, 0, a.length() - 1);
    }

    private <T> void ordenar(InstrumentedArray<T> a, int lo, int hi) {
        while (lo < hi) {
            int p = particionar(a, lo, hi);
            if (p - lo < hi - p) {
                ordenar(a, lo, p - 1);
                lo = p + 1;
            } else {
                ordenar(a, p + 1, hi);
                hi = p - 1;
            }
        }
    }

    private <T> int particionar(InstrumentedArray<T> a, int lo, int hi) {
        int mid = lo + (hi - lo) / 2;
        if (a.less(mid, lo)) a.swap(mid, lo);
        if (a.less(hi, lo)) a.swap(hi, lo);
        if (a.less(hi, mid)) a.swap(hi, mid);
        if (mid != lo) a.swap(lo, mid);

        T pivo = a.get(lo);
        int i = lo;
        int j = hi + 1;
        while (true) {
            while (a.lessValues(a.get(++i), pivo)) {
                if (i == hi) break;
            }
            while (a.lessValues(pivo, a.get(--j))) {
                if (j == lo) break;
            }
            if (i >= j) break;
            a.swap(i, j);
        }
        if (j != lo) a.swap(lo, j);
        return j;
    }
}
