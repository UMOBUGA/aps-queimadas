package br.unip.aps;

/** Excecao base (verificada) do sistema. */
public class ApsException extends Exception {
    public ApsException(String mensagem) {
        super(mensagem);
    }

    public ApsException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
