package br.unip.aps.sorting;

import java.util.Comparator;
import java.util.concurrent.CancellationException;

/**
 * Contador de operacoes elementares de um algoritmo de ordenacao.
 *
 * <p>Modelo de custo adotado (baseado em Sedgewick &amp; Wayne, <i>Algorithms</i>, 4. ed., sec. 2.1):</p>
 * <ul>
 *   <li><b>comparacoes</b>: cada chamada ao {@link Comparator} (chave x chave);</li>
 *   <li><b>trocas</b>: cada permuta de dois elementos ({@code swap});</li>
 *   <li><b>atribuicoes</b> (substituicoes/movimentacoes): cada escrita de um elemento em um array,
 *       principal ou auxiliar. Uma troca realiza 2 escritas; um deslocamento do Insertion Sort ou
 *       uma copia do Merge Sort realizam 1;</li>
 *   <li><b>leituras</b>: cada leitura de uma posicao de array;</li>
 *   <li><b>acessos ao array</b> = leituras + atribuicoes;</li>
 *   <li><b>tempo</b>: medido com {@link System#nanoTime()} (relogio monotonico).</li>
 * </ul>
 * <p>Todos os contadores sao {@code long}: o Bubble Sort com 100 mil elementos ja faz ~5 bilhoes
 * de comparacoes, o que estouraria um {@code int} (limite ~2,1 bilhoes).</p>
 *
 * <p>O contador tambem implementa o cancelamento cooperativo: a cada 65.536 comparacoes verifica
 * se a thread foi interrompida (botao "Cancelar" do dashboard) e, nesse caso, lanca
 * {@link CancellationException}. Nao e thread-safe: cada execucao usa sua propria instancia.</p>
 */
public final class OperationCounter {

    private static final long MASCARA_CANCELAMENTO = 0xFFFF;

    private long comparacoes;
    private long trocas;
    private long atribuicoes;
    private long leituras;
    private long inicio;
    private long nanos;
    private boolean medindo;

    /**
     * Compara dois valores contabilizando a comparacao.
     *
     * @param c comparador (criterio de ordenacao)
     * @param a primeiro valor
     * @param b segundo valor
     * @param <T> tipo dos elementos
     * @return resultado de {@code c.compare(a, b)}
     * @throws CancellationException se a thread atual foi interrompida
     */
    public <T> int comparar(Comparator<? super T> c, T a, T b) {
        if ((++comparacoes & MASCARA_CANCELAMENTO) == 0 && Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Ordenacao cancelada pelo usuario");
        }
        return c.compare(a, b);
    }

    /** Registra uma troca (2 leituras + 2 escritas). */
    public void troca() {
        trocas++;
        leituras += 2;
        atribuicoes += 2;
    }

    /** Registra uma escrita de elemento em array. */
    public void atribuicao() {
        atribuicoes++;
    }

    /**
     * Registra varias escritas de uma vez.
     *
     * @param n quantidade
     */
    public void atribuicoes(long n) {
        atribuicoes += n;
    }

    /** Registra uma leitura de posicao de array. */
    public void leitura() {
        leituras++;
    }

    /**
     * Registra varias leituras de uma vez.
     *
     * @param n quantidade
     */
    public void leituras(long n) {
        leituras += n;
    }

    /** Inicia a medicao de tempo. */
    public void iniciar() {
        medindo = true;
        inicio = System.nanoTime();
    }

    /** Encerra a medicao de tempo (acumula, caso iniciada mais de uma vez). */
    public void parar() {
        if (medindo) {
            nanos += System.nanoTime() - inicio;
            medindo = false;
        }
    }

    /** Zera todos os contadores. */
    public void zerar() {
        comparacoes = trocas = atribuicoes = leituras = nanos = 0;
        medindo = false;
    }

    /** @return comparacoes realizadas */
    public long getComparacoes() { return comparacoes; }

    /** @return trocas realizadas */
    public long getTrocas() { return trocas; }

    /** @return atribuicoes (escritas) realizadas */
    public long getAtribuicoes() { return atribuicoes; }

    /** @return leituras realizadas */
    public long getLeituras() { return leituras; }

    /** @return acessos ao array (leituras + escritas) */
    public long getAcessos() { return leituras + atribuicoes; }

    /** @return tempo medido em nanossegundos */
    public long getNanos() { return nanos; }

    /** @return fotografia imutavel dos contadores */
    public OperationMetrics snapshot() {
        return new OperationMetrics(comparacoes, trocas, atribuicoes, leituras, nanos);
    }

    @Override
    public String toString() {
        return snapshot().toString();
    }
}
