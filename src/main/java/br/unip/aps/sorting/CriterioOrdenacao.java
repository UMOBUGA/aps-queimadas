package br.unip.aps.sorting;

import br.unip.aps.model.FocoIncendio;

import java.text.CollationKey;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * Criterios de ordenacao disponiveis para os focos de incendio.
 *
 * <p>Os tres primeiros (DATA, BIOMA, MUNICIPIO) sao os exigidos pelo enunciado; os demais sao
 * extras. Os marcados como opcionais so ficam habilitados se o arquivo carregado tiver a coluna
 * (os CSVs {@code EstadosBr_sat_ref} do INPE trazem apenas id, coordenadas, data, local e bioma).</p>
 *
 * <p>Textos sao comparados por {@link CollationKey} pt-BR (acentos tratados corretamente:
 * "ÁGUAS DE LINDÓIA" vem antes de "BAURU"). Valores ausentes ({@code null}) vao sempre para o
 * final, em ordem crescente ou decrescente.</p>
 */
public enum CriterioOrdenacao {

    DATA("Data/hora", false, FocoIncendio::getDataHora, f -> f.getDataHora().toEpochSecond(ZoneOffset.UTC)),
    BIOMA("Bioma", false, FocoIncendio::getChaveBioma, null),
    MUNICIPIO("Município", false, FocoIncendio::getChaveMunicipio, null),
    LATITUDE("Latitude", false, FocoIncendio::getLatitude, f -> chaveDouble(f.getLatitude())),
    LONGITUDE("Longitude", false, FocoIncendio::getLongitude, f -> chaveDouble(f.getLongitude())),
    ID("ID (id_bdq)", false, FocoIncendio::getIdBdq, FocoIncendio::getIdBdq),
    SATELITE("Satélite", true, FocoIncendio::getSatelite, null),
    FRP("FRP (potência radiativa)", true, FocoIncendio::getFrp, null),
    RISCO_FOGO("Risco de fogo", true, FocoIncendio::getRiscoFogo, null),
    DIAS_SEM_CHUVA("Dias sem chuva", true, FocoIncendio::getNumeroDiasSemChuva, null),
    PRECIPITACAO("Precipitação", true, FocoIncendio::getPrecipitacao, null);

    private final String rotulo;
    private final boolean opcional;
    private final Function<FocoIncendio, ? extends Comparable<?>> extrator;
    private final ToLongFunction<FocoIncendio> chaveNumerica;

    CriterioOrdenacao(String rotulo, boolean opcional, Function<FocoIncendio, ? extends Comparable<?>> extrator,
                      ToLongFunction<FocoIncendio> chaveNumerica) {
        this.rotulo = rotulo;
        this.opcional = opcional;
        this.extrator = extrator;
        this.chaveNumerica = chaveNumerica;
    }

    /** @return nome para exibicao */
    public String rotulo() {
        return rotulo;
    }

    /** @return {@code true} se depender de coluna opcional do CSV */
    public boolean opcional() {
        return opcional;
    }

    /**
     * @param foco foco
     * @return valor do campo usado pelo criterio (pode ser {@code null} em campos opcionais)
     */
    public Object valor(FocoIncendio foco) {
        return extrator.apply(foco);
    }

    /**
     * Comparador do criterio na ordem pedida; {@code null} sempre no final.
     *
     * @param ordem crescente ou decrescente
     * @return comparador
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Comparator<FocoIncendio> comparador(Ordem ordem) {
        Comparator<Comparable> natural = Comparator.naturalOrder();
        Comparator<Comparable> direcao = ordem == Ordem.DECRESCENTE ? natural.reversed() : natural;
        Function<FocoIncendio, Comparable> ext = (Function<FocoIncendio, Comparable>) (Function) extrator;
        return Comparator.comparing(ext, Comparator.nullsLast(direcao));
    }

    /**
     * Chave inteira cuja ordem numerica equivale a ordem do comparador (para Radix Sort).
     *
     * @param ordem crescente ou decrescente
     * @return funcao de chave, ou {@code null} se o criterio nao for numerico
     */
    public ToLongFunction<FocoIncendio> chaveNumerica(Ordem ordem) {
        if (chaveNumerica == null) return null;
        return ordem == Ordem.DECRESCENTE ? f -> ~chaveNumerica.applyAsLong(f) : chaveNumerica;
    }

    /** @return {@code true} se houver chave numerica (compativel com Radix Sort) */
    public boolean temChaveNumerica() {
        return chaveNumerica != null;
    }

    /**
     * Converte um double em long preservando a ordem de {@link Double#compare} (inclusive negativos).
     *
     * @param d valor
     * @return chave ordenavel
     */
    static long chaveDouble(double d) {
        long bits = Double.doubleToLongBits(d);
        return bits ^ ((bits >> 63) & Long.MAX_VALUE);
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
