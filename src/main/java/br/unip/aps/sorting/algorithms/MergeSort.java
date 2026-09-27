package br.unip.aps.sorting.algorithms;

import br.unip.aps.sorting.Complexidade;
import br.unip.aps.sorting.InstrumentedArray;
import br.unip.aps.sorting.SortAlgorithm;

/** Merge Sort (ordenacao por intercalacao, von Neumann 1945), versao top-down recursiva. */
public final class MergeSort implements SortAlgorithm {
    @Override
    public String nome() {
        return "Merge Sort";
    }

    @Override
    public Complexidade complexidade() {
        return new Complexidade("O(n)*", "O(n log n)", "O(n log n)", "O(n)", true, false, false, true);
    }

    @Override
    public String descricao() {
        return "Divide o vetor ao meio, ordena cada metade recursivamente e intercala as metades; "
                + "O(n log n) garantido e estavel, ao custo de O(n) de memoria auxiliar.";
    }

    @Override
    public <T> void ordenar(InstrumentedArray<T> a) {
        if (a.length() < 2) return;
        InstrumentedArray<T> aux = a.auxiliar(a.length());
        ordenar(a, aux, 0, a.length() - 1);
    }

    private <T> void ordenar(InstrumentedArray<T> a, InstrumentedArray<T> aux, int lo, int hi) {
        if (hi <= lo) return;
        int mid = lo + (hi - lo) / 2;
        ordenar(a, aux, lo, mid);
        ordenar(a, aux, mid + 1, hi);
        if (a.compare(mid, mid + 1) <= 0) {
            return;
        }
        Intercalacao.intercalar(a, aux, lo, mid, hi);
    }
}
