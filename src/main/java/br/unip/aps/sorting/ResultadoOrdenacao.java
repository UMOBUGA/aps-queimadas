package br.unip.aps.sorting;

import java.util.List;

/**
 * Resultado de uma solicitacao de ordenacao: dados ordenados + numero de operacoes realizadas.
 *
 * @param dados         copia ordenada (imutavel)
 * @param algoritmo     nome do algoritmo usado
 * @param criterio      descricao do criterio (ex.: "Bioma ↑ → Data/hora ↓")
 * @param cenario       disposicao inicial da entrada
 * @param metricas      comparacoes, trocas, atribuicoes, acessos e tempo
 * @param verificado    {@code true} se a verificacao independente confirmou a ordenacao
 * @param aviso         aviso exibido ao usuario (ex.: custo O(n²)), ou {@code null}
 * @param <T>           tipo dos elementos
 */
public record ResultadoOrdenacao<T>(List<T> dados, String algoritmo, String criterio, CenarioEntrada cenario,
                                    OperationMetrics metricas, boolean verificado, String aviso) {

    /** @return quantidade de elementos ordenados */
    public int tamanho() {
        return dados.size();
    }

    /** @return resumo de uma linha para console/log */
    public String resumo() {
        return String.format("%s | %s | n=%d | %s | %s", algoritmo, criterio, dados.size(), metricas,
                verificado ? "resultado verificado" : "FALHA NA VERIFICACAO");
    }
}
