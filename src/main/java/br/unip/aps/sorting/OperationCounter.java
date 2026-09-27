package br.unip.aps.sorting;

import java.util.Comparator;
import java.util.concurrent.CancellationException;

/** Contador de operacoes elementares de um algoritmo de ordenacao. */
public final class OperationCounter {
    private static final long MASCARA_CANCELAMENTO = 0xFFFF;

    private long comparacoes;
    private long trocas;
    private long atribuicoes;
    private long leituras;
    private long inicio;
    private long nanos;
    private boolean medindo;

    /** Compara dois valores contabilizando a comparacao. */
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

    /** Registra varias escritas de uma vez. */
    public void atribuicoes(long n) {
        atribuicoes += n;
    }

    /** Registra uma leitura de posicao de array. */
    public void leitura() {
        leituras++;
    }

    /** Registra varias leituras de uma vez. */
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

    public long getComparacoes() { return comparacoes; }

    public long getTrocas() { return trocas; }

    public long getAtribuicoes() { return atribuicoes; }

    public long getLeituras() { return leituras; }

    public long getAcessos() { return leituras + atribuicoes; }

    public long getNanos() { return nanos; }

    public OperationMetrics snapshot() {
        return new OperationMetrics(comparacoes, trocas, atribuicoes, leituras, nanos);
    }

    @Override
    public String toString() {
        return snapshot().toString();
    }
}
