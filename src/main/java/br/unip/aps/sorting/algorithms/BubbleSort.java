package br.unip.aps.sorting.algorithms;

import br.unip.aps.sorting.Complexidade;
import br.unip.aps.sorting.InstrumentedArray;
import br.unip.aps.sorting.SortAlgorithm;

/**
 * <b>Bubble Sort</b> (ordenacao por flutuacao / "bolha").
 *
 * <p>Percorre o vetor comparando pares adjacentes e trocando-os quando estao fora de ordem; a
 * cada passada o maior elemento restante "flutua" ate sua posicao final. Esta versao guarda a
 * posicao da ultima troca: tudo depois dela ja esta ordenado, entao a proxima passada termina
 * ali. Se uma passada nao faz nenhuma troca, o vetor esta ordenado e o algoritmo para.</p>
 *
 * <ul>
 *   <li>Melhor caso: O(n) — vetor ja ordenado: uma passada com n-1 comparacoes e 0 trocas.</li>
 *   <li>Caso medio e pior caso: O(n²) — vetor invertido: n(n-1)/2 comparacoes e n(n-1)/2 trocas.</li>
 *   <li>Espaco: O(1). Estavel: sim (so troca quando a[j] &gt; a[j+1], nunca em empate).</li>
 * </ul>
 */
public final class BubbleSort implements SortAlgorithm {

    @Override
    public String nome() {
        return "Bubble Sort";
    }

    @Override
    public Complexidade complexidade() {
        return new Complexidade("O(n)", "O(n²)", "O(n²)", "O(1)", true, true, true, true);
    }

    @Override
    public String descricao() {
        return "Compara pares adjacentes e troca os que estao fora de ordem; o maior elemento "
                + "'flutua' para o fim a cada passada. Para cedo se uma passada nao fizer trocas.";
    }

    @Override
    public <T> void ordenar(InstrumentedArray<T> a) {
        int limite = a.length() - 1;
        while (limite > 0) {
            int ultimaTroca = 0;
            for (int j = 0; j < limite; j++) {
                if (a.compare(j, j + 1) > 0) {
                    a.swap(j, j + 1);
                    ultimaTroca = j;
                }
            }
            limite = ultimaTroca;
        }
    }
}
