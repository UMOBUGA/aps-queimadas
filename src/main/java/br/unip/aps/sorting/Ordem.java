package br.unip.aps.sorting;

/** Direcao da ordenacao. */
public enum Ordem {
    CRESCENTE("Crescente", "↑"),
    DECRESCENTE("Decrescente", "↓");

    private final String rotulo;
    private final String simbolo;

    Ordem(String rotulo, String simbolo) {
        this.rotulo = rotulo;
        this.simbolo = simbolo;
    }

    /** @return simbolo de seta para descricoes compactas */
    public String simbolo() {
        return simbolo;
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
