package br.unip.aps.sorting;

import br.unip.aps.util.Formatos;

/** Resultado imutavel da contagem de operacoes de uma execucao de ordenacao. */
public record OperationMetrics(long comparacoes, long trocas, long atribuicoes, long leituras, long nanos) {
    public long acessos() {
        return leituras + atribuicoes;
    }

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
