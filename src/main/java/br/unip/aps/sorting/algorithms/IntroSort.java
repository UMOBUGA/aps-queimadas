package br.unip.aps.sorting.algorithms;

import br.unip.aps.sorting.Complexidade;
import br.unip.aps.sorting.InstrumentedArray;
import br.unip.aps.sorting.SortAlgorithm;

/** Intro Sort (Musser, 1997): Quick Sort que troca para Heap Sort quando a recursao fica funda demais. */
public final class IntroSort implements SortAlgorithm {
    private static final int PEQUENO = 16;

    private final int fatorProfundidade;

    public IntroSort() {
        this(2);
    }

    /** Limite de profundidade = fator · log2(n); com fator 0 o trecho vai direto para o Heap Sort (usado nos testes). */
    IntroSort(int fatorProfundidade) {
        this.fatorProfundidade = fatorProfundidade;
    }

    @Override
    public String nome() {
        return "Intro Sort";
    }

    @Override
    public Complexidade complexidade() {
        return new Complexidade("O(n log n)", "O(n log n)", "O(n log n)", "O(log n)", false, true, false, true);
    }

    @Override
    public String descricao() {
        return "Quick Sort com limite de profundidade 2·log2(n): se passar dele, a parte vira Heap Sort; "
                + "partes pequenas vao para o Insertion Sort. Garante O(n log n) no pior caso (usado no std::sort do C++).";
    }

    @Override
    public <T> void ordenar(InstrumentedArray<T> a) {
        int n = a.length();
        if (n < 2) return;
        int limite = fatorProfundidade * (31 - Integer.numberOfLeadingZeros(n));
        introsort(a, 0, n - 1, limite);
        insercao(a, 0, n - 1);
    }

    private <T> void introsort(InstrumentedArray<T> a, int lo, int hi, int profundidade) {
        while (hi - lo + 1 > PEQUENO) {
            if (profundidade == 0) {
                heapSort(a, lo, hi);
                return;
            }
            profundidade--;
            int p = particionar(a, lo, hi);
            if (p - lo < hi - p) {
                introsort(a, lo, p - 1, profundidade);
                lo = p + 1;
            } else {
                introsort(a, p + 1, hi, profundidade);
                hi = p - 1;
            }
        }
    }

    private <T> int particionar(InstrumentedArray<T> a, int lo, int hi) {
        int mid = lo + (hi - lo) / 2;
        if (a.less(mid, lo)) a.swap(mid, lo);
        if (a.less(hi, lo)) a.swap(hi, lo);
        if (a.less(hi, mid)) a.swap(hi, mid);
        a.swap(lo, mid);
        int i = lo;
        int j = hi + 1;
        while (true) {
            while (a.less(++i, lo)) {
                if (i == hi) break;
            }
            while (a.less(lo, --j)) {
                if (j == lo) break;
            }
            if (i >= j) break;
            a.swap(i, j);
        }
        if (j != lo) a.swap(lo, j);
        return j;
    }

    private <T> void heapSort(InstrumentedArray<T> a, int lo, int hi) {
        int n = hi - lo + 1;
        for (int k = n / 2 - 1; k >= 0; k--) afundar(a, lo, k, n);
        while (n > 1) {
            a.swap(lo, lo + --n);
            afundar(a, lo, 0, n);
        }
    }

    private <T> void afundar(InstrumentedArray<T> a, int base, int k, int n) {
        while (2 * k + 1 < n) {
            int j = 2 * k + 1;
            if (j + 1 < n && a.less(base + j, base + j + 1)) j++;
            if (!a.less(base + k, base + j)) break;
            a.swap(base + k, base + j);
            k = j;
        }
    }

    private <T> void insercao(InstrumentedArray<T> a, int lo, int hi) {
        for (int i = lo + 1; i <= hi; i++) {
            for (int j = i; j > lo && a.less(j, j - 1); j--) a.swap(j, j - 1);
        }
    }
}
