package br.unip.aps.sorting.algorithms;

import br.unip.aps.sorting.Complexidade;
import br.unip.aps.sorting.InstrumentedArray;
import br.unip.aps.sorting.SortAlgorithm;

/** Counting Sort (Seward, 1954): conta quantas vezes cada chave aparece e distribui os elementos em ordem. */
public final class CountingSort implements SortAlgorithm {
    /** Maior intervalo de chaves aceito: acima disso o vetor de contagem gastaria memoria demais. */
    public static final long FAIXA_MAXIMA = 1L << 22;

    @Override
    public String nome() {
        return "Counting Sort";
    }

    @Override
    public Complexidade complexidade() {
        return new Complexidade("O(n + k)", "O(n + k)", "O(n + k)", "O(n + k)", true, false, false, false);
    }

    @Override
    public String descricao() {
        return "Zero comparacoes: conta cada chave num vetor do tamanho do intervalo (k) e reposiciona de forma estavel. "
                + "Imbativel com poucas chaves distintas, como a hora do dia; inviavel com intervalos enormes, como a data em segundos.";
    }

    @Override
    public boolean exigeChaveNumerica() {
        return true;
    }

    @Override
    public boolean aceitaFaixa(long minimo, long maximo) {
        return maximo - minimo >= 0 && maximo - minimo < FAIXA_MAXIMA;
    }

    @Override
    public <T> void ordenar(InstrumentedArray<T> a) {
        int n = a.length();
        if (n < 2) return;
        long[] chaves = new long[n];
        long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            chaves[i] = a.key(i);
            min = Math.min(min, chaves[i]);
            max = Math.max(max, chaves[i]);
        }
        if (!aceitaFaixa(min, max)) {
            throw new IllegalArgumentException("O Counting Sort precisa de chaves num intervalo pequeno (até "
                    + FAIXA_MAXIMA + " valores); este critério vai de " + min + " a " + max + ". Use o Radix Sort.");
        }
        int[] posicao = new int[(int) (max - min) + 2];
        for (long c : chaves) posicao[(int) (c - min) + 1]++;
        for (int d = 1; d < posicao.length; d++) posicao[d] += posicao[d - 1];
        InstrumentedArray<T> aux = a.auxiliar(n);
        for (int i = 0; i < n; i++) aux.set(posicao[(int) (chaves[i] - min)]++, a.get(i));
        for (int i = 0; i < n; i++) a.set(i, aux.get(i));
    }
}
