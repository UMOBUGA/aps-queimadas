package br.unip.aps.sorting.algorithms;

import br.unip.aps.sorting.Complexidade;
import br.unip.aps.sorting.InstrumentedArray;
import br.unip.aps.sorting.SortAlgorithm;

/** Radix Sort LSD (digito menos significativo primeiro), base 256, sobre chaves {@code long}. */
public final class RadixSort implements SortAlgorithm {
    private static final int BITS = 8;
    private static final int BASE = 1 << BITS;

    @Override
    public String nome() {
        return "Radix Sort (LSD)";
    }

    @Override
    public Complexidade complexidade() {
        return new Complexidade("O(d·n)", "O(d·n)", "O(d·n)", "O(n + b)", true, false, false, false);
    }

    @Override
    public String descricao() {
        return "Distribui os elementos pelos bytes da chave numerica (Counting Sort estavel por digito); "
                + "zero comparacoes, tempo linear, mas so para criterios numericos (data, lat/lon).";
    }

    @Override
    public boolean exigeChaveNumerica() {
        return true;
    }

    @Override
    public <T> void ordenar(InstrumentedArray<T> a) {
        int n = a.length();
        if (n < 2) return;
        long[] chaves = new long[n];
        for (int i = 0; i < n; i++) {
            chaves[i] = a.key(i) ^ Long.MIN_VALUE;
        }
        InstrumentedArray<T> aux = a.auxiliar(n);
        long[] chavesAux = new long[n];

        for (int deslocamento = 0; deslocamento < Long.SIZE; deslocamento += BITS) {
            int[] contagem = new int[BASE + 1];
            for (int i = 0; i < n; i++) {
                contagem[digito(chaves[i], deslocamento) + 1]++;
            }
            if (passadaTrivial(contagem, n)) {
                continue;
            }
            for (int d = 0; d < BASE; d++) {
                contagem[d + 1] += contagem[d];
            }
            for (int i = 0; i < n; i++) {
                int d = digito(chaves[i], deslocamento);
                int destino = contagem[d]++;
                aux.set(destino, a.get(i));
                chavesAux[destino] = chaves[i];
            }
            for (int i = 0; i < n; i++) {
                a.set(i, aux.get(i));
            }
            long[] t = chaves;
            chaves = chavesAux;
            chavesAux = t;
        }
    }

    private static int digito(long chave, int deslocamento) {
        return (int) ((chave >>> deslocamento) & (BASE - 1));
    }

    private static boolean passadaTrivial(int[] contagem, int n) {
        for (int d = 1; d <= BASE; d++) {
            if (contagem[d] == n) return true;
        }
        return false;
    }
}
