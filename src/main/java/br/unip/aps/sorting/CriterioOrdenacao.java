package br.unip.aps.sorting;

import br.unip.aps.model.FocoIncendio;

import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/** Criterios de ordenacao disponiveis para os focos de incendio. */
public enum CriterioOrdenacao {
    DATA("Data/hora", false, FocoIncendio::getDataHora, f -> f.getDataHora().toEpochSecond(ZoneOffset.UTC)),
    HORA_LOCAL("Hora local", false, CriterioOrdenacao::horaLocal, f -> horaLocal(f)),
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

    public String rotulo() {
        return rotulo;
    }

    public boolean opcional() {
        return opcional;
    }

    public Object valor(FocoIncendio foco) {
        return extrator.apply(foco);
    }

    /** Comparador do criterio na ordem pedida; {@code null} sempre no final. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Comparator<FocoIncendio> comparador(Ordem ordem) {
        Comparator<Comparable> natural = Comparator.naturalOrder();
        Comparator<Comparable> direcao = ordem == Ordem.DECRESCENTE ? natural.reversed() : natural;
        Function<FocoIncendio, Comparable> ext = (Function<FocoIncendio, Comparable>) (Function) extrator;
        return Comparator.comparing(ext, Comparator.nullsLast(direcao));
    }

    /** Chave inteira cuja ordem numerica equivale a ordem do comparador (para Radix Sort). */
    public ToLongFunction<FocoIncendio> chaveNumerica(Ordem ordem) {
        if (chaveNumerica == null) return null;
        return ordem == Ordem.DECRESCENTE ? f -> ~chaveNumerica.applyAsLong(f) : chaveNumerica;
    }

    public boolean temChaveNumerica() {
        return chaveNumerica != null;
    }

    /** Hora do dia em Sao Paulo (UTC-3): data_pas vem em GMT. */
    public static int horaLocal(FocoIncendio f) {
        return (f.getDataHora().getHour() + 21) % 24;
    }

    static long chaveDouble(double d) {
        long bits = Double.doubleToLongBits(d);
        return bits ^ ((bits >> 63) & Long.MAX_VALUE);
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
