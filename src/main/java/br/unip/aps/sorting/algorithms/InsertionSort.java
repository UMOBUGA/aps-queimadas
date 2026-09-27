package br.unip.aps.sorting.algorithms;

import br.unip.aps.sorting.Complexidade;
import br.unip.aps.sorting.InstrumentedArray;
import br.unip.aps.sorting.SortAlgorithm;

/**
 * <b>Insertion Sort</b> (ordenacao por insercao).
 *
 * <p>Mantem o prefixo a[0..i-1] ordenado e insere a[i] na posicao correta, deslocando uma casa
 * para a direita os elementos maiores (como ordenar cartas na mao). Usa deslocamentos
 * (1 escrita cada) em vez de trocas (2 escritas), o que reduz os acessos ao array pela metade.</p>
 *
 * <ul>
 *   <li>Melhor caso: O(n) — vetor ordenado: n-1 comparacoes e nenhum deslocamento.</li>
 *   <li>Caso medio: O(n²) — ~n²/4 comparacoes. Pior caso (invertido): n(n-1)/2.</li>
 *   <li>O custo e proporcional ao numero de inversoes: excelente para dados "quase ordenados"
 *       (por isso e usado dentro do Tim Sort).</li>
 *   <li>Espaco: O(1). Estavel: sim (para no primeiro elemento menor <b>ou igual</b>).</li>
 * </ul>
 */
public final class InsertionSort implements SortAlgorithm {

    @Override
    public String nome() {
        return "Insertion Sort";
    }

    @Override
    public Complexidade complexidade() {
        return new Complexidade("O(n)", "O(n²)", "O(n²)", "O(1)", true, true, true, true);
    }

    @Override
    public String descricao() {
        return "Insere cada elemento na posicao correta do prefixo ja ordenado, deslocando os maiores; "
                + "custo proporcional ao numero de inversoes (otimo para dados quase ordenados).";
    }

    @Override
    public <T> void ordenar(InstrumentedArray<T> a) {
        int n = a.length();
        for (int i = 1; i < n; i++) {
            T x = a.get(i);
            int j = i - 1;
            while (j >= 0) {
                T y = a.get(j);
                if (a.compareValues(y, x) <= 0) {
                    break;
                }
                a.set(j + 1, y);
                j--;
            }
            if (j + 1 != i) {
                a.set(j + 1, x);
            }
        }
    }
}
