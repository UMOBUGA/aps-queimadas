package br.unip.aps.sorting.algorithms;

import br.unip.aps.sorting.Complexidade;
import br.unip.aps.sorting.InstrumentedArray;
import br.unip.aps.sorting.SortAlgorithm;

/** Tim Sort simplificado (Tim Peters, 2002). */
public final class TimSortSimplificado implements SortAlgorithm {
    static final int RUN = 32;

    @Override
    public String nome() {
        return "Tim Sort (simplificado)";
    }

    @Override
    public Complexidade complexidade() {
        return new Complexidade("O(n)", "O(n log n)", "O(n log n)", "O(n)", true, false, false, true);
    }

    @Override
    public String descricao() {
        return "Hibrido: ordena blocos de 32 com Insertion Sort binario e intercala os blocos de baixo "
                + "para cima; estavel e muito eficiente em dados parcialmente ordenados.";
    }

    @Override
    public <T> void ordenar(InstrumentedArray<T> a) {
        int n = a.length();
        if (n < 2) return;
        for (int lo = 0; lo < n; lo += RUN) {
            insercaoBinaria(a, lo, Math.min(lo + RUN - 1, n - 1));
        }
        if (n <= RUN) return;
        InstrumentedArray<T> aux = a.auxiliar(n);
        for (int tamanho = RUN; tamanho < n; tamanho *= 2) {
            for (int lo = 0; lo < n - tamanho; lo += 2 * tamanho) {
                int mid = lo + tamanho - 1;
                int hi = Math.min(lo + 2 * tamanho - 1, n - 1);
                if (a.compare(mid, mid + 1) > 0) {
                    Intercalacao.intercalar(a, aux, lo, mid, hi);
                }
            }
        }
    }

    private <T> void insercaoBinaria(InstrumentedArray<T> a, int lo, int hi) {
        for (int i = lo + 1; i <= hi; i++) {
            T x = a.get(i);
            int esq = lo;
            int dir = i;
            while (esq < dir) {
                int m = (esq + dir) >>> 1;
                if (a.lessValues(x, a.get(m))) {
                    dir = m;
                } else {
                    esq = m + 1;
                }
            }
            for (int j = i; j > esq; j--) {
                a.set(j, a.get(j - 1));
            }
            if (esq != i) {
                a.set(esq, x);
            }
        }
    }
}
