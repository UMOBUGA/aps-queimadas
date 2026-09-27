package br.unip.aps.sorting;

import br.unip.aps.util.Formatos;

/**
 * Resultado imutavel da contagem de operacoes de uma execucao de ordenacao.
 *
 * @param comparacoes chamadas ao comparador
 * @param trocas      permutas de dois elementos
 * @param atribuicoes escritas de elementos em arrays (principal ou auxiliar)
 * @param leituras    leituras de posicoes de array
 * @param nanos       tempo de execucao em nanossegundos
 */
public record OperationMetrics(long comparacoes, long trocas, long atribuicoes, long leituras, long nanos) {

    /** @return acessos ao array (leituras + escritas) */
    public long acessos() {
        return leituras + atribuicoes;
    }

    /** @return tempo em milissegundos */
    public double millis() {
        return nanos / 1_000_000.0;
    }

    @Override
    public String toString() {
        return "comparacoes=" + Formatos.inteiro(comparacoes)
                + ", trocas=" + Formatos.inteiro(trocas)
                + ", atribuicoes=" + Formatos.inteiro(atribuicoes)
                + ", acessos=" + Formatos.inteiro(acessos())
                + ", tempo=" + Formatos.duracao(nanos);
    }
}
