package br.unip.aps.sorting.algorithms;

import br.unip.aps.sorting.Complexidade;
import br.unip.aps.sorting.InstrumentedArray;
import br.unip.aps.sorting.SortAlgorithm;

/** Quick Sort 3-Way (particao em tres vias de Dijkstra, "bandeira holandesa"). */
public final class QuickSort3Way implements SortAlgorithm {
    @Override
    public String nome() {
        return "Quick Sort 3-Way";
    }

    @Override
    public Complexidade complexidade() {
        return new Complexidade("O(n)", "O(n log n)", "O(n²)", "O(log n)", false, true, false, true);
    }

    @Override
    public String descricao() {
        return "Quick Sort com particao em tres faixas (<, =, >) de Dijkstra; os iguais ao pivo nao "
                + "sao reprocessados, ideal para criterios com muitas repeticoes (bioma, municipio).";
    }

    @Override
    public <T> void ordenar(InstrumentedArray<T> a) {
        ordenar(a, 0, a.length() - 1);
    }

    private <T> void ordenar(InstrumentedArray<T> a, int lo, int hi) {
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (a.less(mid, lo)) a.swap(mid, lo);
            if (a.less(hi, lo)) a.swap(hi, lo);
            if (a.less(hi, mid)) a.swap(hi, mid);
            if (mid != lo) a.swap(lo, mid);

            T pivo = a.get(lo);
            int lt = lo;
            int gt = hi;
            int i = lo + 1;
            while (i <= gt) {
                int c = a.compareValues(a.get(i), pivo);
                if (c < 0) {
                    a.swap(lt++, i++);
                } else if (c > 0) {
                    a.swap(i, gt--);
                } else {
                    i++;
                }
            }
            if (lt - lo < hi - gt) {
                ordenar(a, lo, lt - 1);
                lo = gt + 1;
            } else {
                ordenar(a, gt + 1, hi);
                hi = lt - 1;
            }
        }
    }
}
