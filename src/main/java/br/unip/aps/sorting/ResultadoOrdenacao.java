package br.unip.aps.sorting;

import java.util.List;

/** Resultado de uma solicitacao de ordenacao. */
public record ResultadoOrdenacao<T>(List<T> dados, String algoritmo, String criterio, CenarioEntrada cenario,
                                    OperationMetrics metricas, boolean verificado, String aviso) {
    public int tamanho() {
        return dados.size();
    }

    public String resumo() {
        return String.format("%s | %s | n=%d | %s | %s", algoritmo, criterio, dados.size(), metricas,
                verificado ? "resultado verificado" : "FALHA NA VERIFICACAO");
    }
}
