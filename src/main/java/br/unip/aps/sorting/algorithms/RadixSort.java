package br.unip.aps.sorting.algorithms;

import br.unip.aps.sorting.Complexidade;
import br.unip.aps.sorting.InstrumentedArray;
import br.unip.aps.sorting.SortAlgorithm;

/**
 * <b>Radix Sort LSD</b> (digito menos significativo primeiro), base 256, sobre chaves {@code long}.
 *
 * <p>Nao compara elementos: distribui-os por "digitos" de 8 bits da chave numerica, do menos para
 * o mais significativo, usando Counting Sort estavel em cada passada (8 passadas no maximo para
 * 64 bits). Passadas em que todos os elementos tem o mesmo digito sao puladas — para datas
 * (segundos desde 1970) os bytes altos sao iguais e apenas ~4 passadas sao necessarias.</p>
 *
 * <p>Por nao comparar, o numero de <b>comparacoes e zero</b> e o limite inferior Ω(n log n) das
 * ordenacoes por comparacao nao se aplica. Em compensacao, so funciona para criterios que tenham
 * uma chave inteira (data, latitude, longitude, id); nao serve para bioma/municipio (texto com
 * colacao) nem para ordenacao multicriterio.</p>
 *
 * <p>Chaves negativas: o bit de sinal e invertido ({@code chave ^ Long.MIN_VALUE}) para que a
 * ordem sem sinal dos bytes coincida com a ordem numerica. A ordem decrescente e obtida pelo
 * criterio fornecendo {@code ~chave}.</p>
 *
 * <ul>
 *   <li>Tempo: O(d·(n + b)) com d = 8 digitos e b = 256 — linear em n.</li>
 *   <li>Espaco: O(n + b). Estavel: <b>sim</b>.</li>
 * </ul>
 */
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

    /** Todos os elementos com o mesmo digito: a passada nao mudaria nada. */
    private static boolean passadaTrivial(int[] contagem, int n) {
        for (int d = 1; d <= BASE; d++) {
            if (contagem[d] == n) return true;
        }
        return false;
    }
}
