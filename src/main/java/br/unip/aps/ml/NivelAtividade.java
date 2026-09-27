package br.unip.aps.ml;

/**
 * Nivel de atividade de fogo de um municipio em um mes — alvo do classificador.
 *
 * <p>O enunciado sugere classificar o "risco de fogo" a partir de precipitacao, dias sem chuva e
 * FRP, mas os CSVs {@code EstadosBr_sat_ref} do INPE nao trazem essas colunas. Adaptacao adotada:
 * classificar o <b>nivel de atividade</b> esperado (baixo/medio/alto) a partir do historico
 * recente, da sazonalidade, da localizacao e do bioma — um indicador de risco operacional.</p>
 */
public enum NivelAtividade {
    /** Nenhum foco no mes. */
    BAIXO("Baixo (0 focos)"),
    /** De 1 a {@value #LIMITE_ALTO_MENOS_1} focos. */
    MEDIO("Médio (1–4 focos)"),
    /** {@value #LIMITE_ALTO} ou mais focos. */
    ALTO("Alto (5+ focos)");

    /** Limite inferior da classe ALTO. */
    public static final int LIMITE_ALTO = 5;
    /** Limite superior da classe MEDIO. */
    public static final int LIMITE_ALTO_MENOS_1 = LIMITE_ALTO - 1;

    private final String rotulo;

    NivelAtividade(String rotulo) {
        this.rotulo = rotulo;
    }

    /**
     * @param focos focos no mes
     * @return nivel correspondente
     */
    public static NivelAtividade de(int focos) {
        if (focos <= 0) return BAIXO;
        return focos < LIMITE_ALTO ? MEDIO : ALTO;
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
