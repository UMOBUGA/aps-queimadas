package br.unip.aps.io;

import br.unip.aps.ApsException;

/** Lancada quando um arquivo de dados nao pode ser usado. */
public class DataValidationException extends ApsException {
    public DataValidationException(String mensagem) {
        super(mensagem);
    }

    public DataValidationException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
