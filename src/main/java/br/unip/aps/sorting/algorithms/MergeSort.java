package br.unip.aps.sorting.algorithms;

import br.unip.aps.sorting.Complexidade;
import br.unip.aps.sorting.InstrumentedArray;
import br.unip.aps.sorting.SortAlgorithm;

/**
 * <b>Merge Sort</b> (ordenacao por intercalacao, von Neumann 1945) — versao top-down recursiva.
 *
 * <p>Divisao e conquista: divide o vetor ao meio, ordena recursivamente cada metade e intercala
 * as duas metades ordenadas usando um vetor auxiliar alocado uma unica vez. Otimizacao de
 * Sedgewick: se o ultimo elemento da metade esquerda ja e &lt;= o primeiro da direita, as metades
 * ja estao em ordem e a intercalacao e pulada (o vetor ordenado custa so n-1 comparacoes).</p>
 *
 * <ul>
 *   <li>Melhor caso: O(n) com a otimizacao acima (sem ela, O(n log n)).</li>
 *   <li>Caso medio e pior caso: O(n log n) garantido — ate ~n·log2(n) comparacoes. A arvore de
 *       recursao tem log2(n) niveis e cada nivel intercala n elementos.</li>
 *   <li>Espaco: O(n) (vetor auxiliar) + O(log n) de pilha.</li>
 *   <li>Estavel: <b>sim</b> — em empates a intercalacao sempre copia primeiro a metade esquerda.
 *       Por isso e o algoritmo recomendado para ordenacao multicriterio em passadas sucessivas.</li>
 * </ul>
 */
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
            return; // metades ja estao em ordem
        }
        Intercalacao.intercalar(a, aux, lo, mid, hi);
    }
}
