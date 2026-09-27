package br.unip.aps.sorting.algorithms;

import br.unip.aps.sorting.Complexidade;
import br.unip.aps.sorting.InstrumentedArray;
import br.unip.aps.sorting.SortAlgorithm;

/**
 * <b>Quick Sort</b> (C. A. R. Hoare, 1961) com pivo mediana-de-tres e particao de Hoare/Sedgewick.
 *
 * <p>Escolhe um pivo, particiona o vetor em "menores ou iguais" | pivo | "maiores ou iguais" e
 * ordena recursivamente as duas partes. Decisoes de projeto que evitam o pior caso:</p>
 * <ol>
 *   <li><b>Mediana de tres</b> (primeiro, meio, ultimo): com pivo fixo no primeiro elemento, um
 *       vetor ja ordenado gera particoes 0 | n-1, custo O(n²) e recursao com profundidade n —
 *       {@code StackOverflowError} com dezenas de milhares de registros. A mediana-de-tres torna
 *       esse caso O(n log n).</li>
 *   <li><b>Particao de Hoare</b> (os ponteiros param em chaves iguais ao pivo): com muitas chaves
 *       repetidas — o criterio "bioma" desta base tem so 2 valores! — a particao continua
 *       balanceada. A particao de Lomuto degeneraria para O(n²) nesse cenario.</li>
 *   <li><b>Recursao na menor parte e laco na maior</b>: garante pilha O(log n) no pior caso.</li>
 * </ol>
 *
 * <ul>
 *   <li>Melhor caso e caso medio: O(n log n) (~1,39·n·log2 n comparacoes em media).</li>
 *   <li>Pior caso: O(n²) — raro com mediana-de-tres, mas ainda possivel com entradas adversarias.</li>
 *   <li>Espaco: O(log n) de pilha. Estavel: <b>nao</b>.</li>
 * </ul>
 */
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
        // mediana de tres: deixa a[lo] <= a[mid] <= a[hi]
        if (a.less(mid, lo)) a.swap(mid, lo);
        if (a.less(hi, lo)) a.swap(hi, lo);
        if (a.less(hi, mid)) a.swap(hi, mid);
        if (mid != lo) a.swap(lo, mid); // pivo (mediana) vai para a[lo]; a[hi] >= pivo serve de sentinela

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
