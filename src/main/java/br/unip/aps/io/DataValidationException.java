package br.unip.aps.io;

import br.unip.aps.ApsException;

/**
 * Lancada quando um arquivo de dados nao pode ser usado: arquivo inexistente ou ilegivel,
 * cabecalho sem as colunas obrigatorias, ou nenhum registro valido.
 *
 * <p>Erros em linhas individuais NAO lancam esta excecao: a linha e descartada e registrada no
 * {@link RelatorioCarga}, para que um unico registro corrompido nao impeca a analise.</p>
 */
public class DataValidationException extends ApsException {

    /** @param mensagem descricao amigavel do problema */
    public DataValidationException(String mensagem) {
        super(mensagem);
    }

    /**
     * @param mensagem descricao amigavel do problema
     * @param causa    excecao original (ex.: {@link java.io.IOException})
     */
    public DataValidationException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
