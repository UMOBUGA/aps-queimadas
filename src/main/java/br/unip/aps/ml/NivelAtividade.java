package br.unip.aps.ml;

/** Nivel de atividade de fogo de um municipio em um mes, alvo do classificador. */
public enum NivelAtividade {
    BAIXO("Baixo (0 focos)"),
    MEDIO("Médio (1–4 focos)"),
    /** {@value #LIMITE_ALTO} ou mais focos. */
    ALTO("Alto (5+ focos)");

    public static final int LIMITE_ALTO = 5;
    public static final int LIMITE_ALTO_MENOS_1 = LIMITE_ALTO - 1;

    private final String rotulo;

    NivelAtividade(String rotulo) {
        this.rotulo = rotulo;
    }

    public static NivelAtividade de(int focos) {
        if (focos <= 0) return BAIXO;
        return focos < LIMITE_ALTO ? MEDIO : ALTO;
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
