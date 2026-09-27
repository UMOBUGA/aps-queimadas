package br.unip.aps;

/**
 * Excecao base (verificada) do sistema. Todas as falhas de negocio previsiveis (arquivo ausente,
 * coluna faltando, parametro invalido...) sao subclasses dela, o que permite a camada de
 * apresentacao (console ou dashboard) exibir uma mensagem amigavel em um unico ponto.
 */
public class ApsException extends Exception {

    /**
     * @param mensagem mensagem amigavel, em portugues, pronta para exibir ao usuario
     */
    public ApsException(String mensagem) {
        super(mensagem);
    }

    /**
     * @param mensagem mensagem amigavel
     * @param causa    excecao original
     */
    public ApsException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
