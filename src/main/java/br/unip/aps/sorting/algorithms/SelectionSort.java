package br.unip.aps.sorting.algorithms;

import br.unip.aps.sorting.Complexidade;
import br.unip.aps.sorting.InstrumentedArray;
import br.unip.aps.sorting.SortAlgorithm;

/**
 * <b>Selection Sort</b> (ordenacao por selecao).
 *
 * <p>Para cada posicao i, procura o menor elemento em a[i..n-1] e o troca com a[i].</p>
 *
 * <ul>
 *   <li>Melhor, medio e pior caso: O(n²) — sempre exatamente n(n-1)/2 comparacoes, pois a busca
 *       do minimo nao sabe que o vetor ja esta ordenado.</li>
 *   <li>Trocas: no maximo n-1 (O(n)) — vantagem quando escrever e caro.</li>
 *   <li>Espaco: O(1). Estavel: <b>nao</b> — a troca de longa distancia pode passar um elemento por
 *       cima de outro com a mesma chave (ex.: [B1, B2, A] vira [A, B2, B1]).</li>
 * </ul>
 */
public final class SelectionSort implements SortAlgorithm {

    @Override
    public String nome() {
        return "Selection Sort";
    }

    @Override
    public Complexidade complexidade() {
        return new Complexidade("O(n²)", "O(n²)", "O(n²)", "O(1)", false, true, true, true);
    }

    @Override
    public String descricao() {
        return "Seleciona o menor elemento da parte nao ordenada e o coloca na proxima posicao; "
                + "sempre n(n-1)/2 comparacoes, mas no maximo n-1 trocas.";
    }

    @Override
    public <T> void ordenar(InstrumentedArray<T> a) {
        int n = a.length();
        for (int i = 0; i < n - 1; i++) {
            int min = i;
            for (int j = i + 1; j < n; j++) {
                if (a.less(j, min)) {
                    min = j;
                }
            }
            if (min != i) {
                a.swap(i, min);
            }
        }
    }
}
