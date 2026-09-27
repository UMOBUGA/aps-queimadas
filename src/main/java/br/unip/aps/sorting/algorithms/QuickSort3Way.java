package br.unip.aps.sorting.algorithms;

import br.unip.aps.sorting.Complexidade;
import br.unip.aps.sorting.InstrumentedArray;
import br.unip.aps.sorting.SortAlgorithm;

/**
 * <b>Quick Sort 3-Way</b> (particao em tres vias de Dijkstra — "bandeira holandesa").
 *
 * <p>Divide o vetor em tres faixas: menores que o pivo | iguais ao pivo | maiores. A faixa dos
 * iguais ja fica na posicao final e nao entra na recursao. Com muitas chaves repetidas o custo
 * cai drasticamente: ordenar esta base por <b>bioma</b> (apenas 2 valores distintos) exige apenas
 * ~2 passadas lineares, contra ~n·log n do Quick Sort classico.</p>
 *
 * <ul>
 *   <li>Melhor caso: O(n) (todas as chaves iguais, ou poucas chaves distintas: O(n·k)).</li>
 *   <li>Caso medio: O(n log n); proporcional a entropia das chaves quando ha repeticoes.</li>
 *   <li>Pior caso: O(n²) (improvavel com mediana de tres).</li>
 *   <li>Espaco: O(log n) de pilha. Estavel: <b>nao</b>.</li>
 * </ul>
 */
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
            // invariante: a[lo..lt-1] < pivo, a[lt..i-1] == pivo, a[gt+1..hi] > pivo
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
