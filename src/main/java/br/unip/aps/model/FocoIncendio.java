package br.unip.aps.model;

import br.unip.aps.util.Textos;

import java.text.CollationKey;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/** Registro imutavel de um foco de incendio detectado por satelite (base de queimadas do INPE). */
public final class FocoIncendio {
    private final long idBdq;
    private final String focoId;
    private final double latitude;
    private final double longitude;
    private final LocalDateTime dataHora;
    private final String pais;
    private final String estado;
    private final String municipio;
    private final String bioma;

    private final String satelite;
    private final Integer numeroDiasSemChuva;
    private final Double precipitacao;
    private final Double riscoFogo;
    private final Double frp;

    private final transient CollationKey chaveMunicipio;
    private final transient CollationKey chaveBioma;

    private FocoIncendio(Builder b) {
        this.idBdq = b.idBdq;
        this.focoId = b.focoId;
        this.latitude = b.latitude;
        this.longitude = b.longitude;
        this.dataHora = Objects.requireNonNull(b.dataHora, "dataHora");
        this.pais = b.pais;
        this.estado = b.estado;
        this.municipio = Objects.requireNonNull(b.municipio, "municipio");
        this.bioma = Objects.requireNonNull(b.bioma, "bioma");
        this.satelite = b.satelite;
        this.numeroDiasSemChuva = b.numeroDiasSemChuva;
        this.precipitacao = b.precipitacao;
        this.riscoFogo = b.riscoFogo;
        this.frp = b.frp;
        this.chaveMunicipio = Textos.chaveColacao(municipio);
        this.chaveBioma = Textos.chaveColacao(bioma);
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getIdBdq() { return idBdq; }

    public String getFocoId() { return focoId; }

    public double getLatitude() { return latitude; }

    public double getLongitude() { return longitude; }

    public LocalDateTime getDataHora() { return dataHora; }

    public LocalDate getData() { return dataHora.toLocalDate(); }

    public int getAno() { return dataHora.getYear(); }

    public int getMes() { return dataHora.getMonthValue(); }

    public String getPais() { return pais; }

    public String getEstado() { return estado; }

    public String getMunicipio() { return municipio; }

    public String getBioma() { return bioma; }

    public String getSatelite() { return satelite; }

    public Integer getNumeroDiasSemChuva() { return numeroDiasSemChuva; }

    public Double getPrecipitacao() { return precipitacao; }

    public Double getRiscoFogo() { return riscoFogo; }

    public Double getFrp() { return frp; }

    public CollationKey getChaveMunicipio() { return chaveMunicipio; }

    public CollationKey getChaveBioma() { return chaveBioma; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FocoIncendio other)) return false;
        return idBdq == other.idBdq && Objects.equals(focoId, other.focoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idBdq, focoId);
    }

    @Override
    public String toString() {
        return "FocoIncendio{" + dataHora + ", " + municipio + ", " + bioma
                + ", lat=" + latitude + ", lon=" + longitude + ", id=" + idBdq + '}';
    }

    /** Construtor fluente de {@link FocoIncendio}. */
    public static final class Builder {
        private long idBdq;
        private String focoId;
        private double latitude;
        private double longitude;
        private LocalDateTime dataHora;
        private String pais = "Brasil";
        private String estado;
        private String municipio;
        private String bioma;
        private String satelite;
        private Integer numeroDiasSemChuva;
        private Double precipitacao;
        private Double riscoFogo;
        private Double frp;

        private Builder() { }

        public Builder idBdq(long v) { this.idBdq = v; return this; }
        public Builder focoId(String v) { this.focoId = v; return this; }
        public Builder latitude(double v) { this.latitude = v; return this; }
        public Builder longitude(double v) { this.longitude = v; return this; }
        public Builder dataHora(LocalDateTime v) { this.dataHora = v; return this; }
        public Builder pais(String v) { this.pais = v; return this; }
        public Builder estado(String v) { this.estado = v; return this; }
        public Builder municipio(String v) { this.municipio = v; return this; }
        public Builder bioma(String v) { this.bioma = v; return this; }
        public Builder satelite(String v) { this.satelite = v; return this; }
        public Builder numeroDiasSemChuva(Integer v) { this.numeroDiasSemChuva = v; return this; }
        public Builder precipitacao(Double v) { this.precipitacao = v; return this; }
        public Builder riscoFogo(Double v) { this.riscoFogo = v; return this; }
        public Builder frp(Double v) { this.frp = v; return this; }

        public FocoIncendio build() {
            return new FocoIncendio(this);
        }
    }
}
