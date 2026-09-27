package br.unip.aps.model;

import br.unip.aps.util.Textos;

import java.text.CollationKey;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Registro imutavel de um foco de incendio detectado por satelite (base de queimadas do INPE).
 *
 * <p>E a estrutura de dados central do sistema: cada linha valida do CSV vira uma instancia.
 * Os campos obrigatorios existem em todos os arquivos {@code focos_br_XX_ref_AAAA.csv}
 * (id, coordenadas, data/hora, pais, estado, municipio e bioma). Os campos meteorologicos e de
 * radiacao ({@code satelite}, {@code numeroDiasSemChuva}, {@code precipitacao}, {@code riscoFogo},
 * {@code frp}) so aparecem em outros conjuntos do INPE e por isso sao opcionais ({@code null}
 * quando ausentes ou quando o INPE informa o valor-sentinela {@code -999}).</p>
 *
 * <p>Para acelerar as ordenacoes alfabeticas, as chaves de colacao (pt-BR) de municipio e bioma
 * sao calculadas uma unica vez na construcao do objeto: comparar duas {@link CollationKey}
 * e muito mais barato do que chamar {@code Collator.compare} a cada comparacao do algoritmo.</p>
 *
 * <p>Instancias sao criadas pelo {@link Builder}.</p>
 */
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

    /** @return novo construtor de focos */
    public static Builder builder() {
        return new Builder();
    }

    /** @return identificador numerico do banco de dados de queimadas (coluna {@code id_bdq}) */
    public long getIdBdq() { return idBdq; }

    /** @return identificador unico (UUID) do foco (coluna {@code foco_id}) */
    public String getFocoId() { return focoId; }

    /** @return latitude em graus decimais (WGS84) */
    public double getLatitude() { return latitude; }

    /** @return longitude em graus decimais (WGS84) */
    public double getLongitude() { return longitude; }

    /** @return data e hora da passagem do satelite, em GMT/UTC (coluna {@code data_pas}) */
    public LocalDateTime getDataHora() { return dataHora; }

    /** @return data (sem hora) da deteccao */
    public LocalDate getData() { return dataHora.toLocalDate(); }

    /** @return ano da deteccao */
    public int getAno() { return dataHora.getYear(); }

    /** @return mes da deteccao (1 a 12) */
    public int getMes() { return dataHora.getMonthValue(); }

    /** @return pais (normalmente "Brasil") */
    public String getPais() { return pais; }

    /** @return unidade federativa por extenso, em maiusculas */
    public String getEstado() { return estado; }

    /** @return nome do municipio, em maiusculas e sem espacos redundantes */
    public String getMunicipio() { return municipio; }

    /** @return bioma do IBGE (Cerrado, Mata Atlantica, Amazonia...) */
    public String getBioma() { return bioma; }

    /** @return satelite que detectou o foco, ou {@code null} se a coluna nao existir no arquivo */
    public String getSatelite() { return satelite; }

    /** @return numero de dias sem chuva, ou {@code null} se indisponivel */
    public Integer getNumeroDiasSemChuva() { return numeroDiasSemChuva; }

    /** @return precipitacao (mm), ou {@code null} se indisponivel */
    public Double getPrecipitacao() { return precipitacao; }

    /** @return risco de fogo (0 a 1), ou {@code null} se indisponivel */
    public Double getRiscoFogo() { return riscoFogo; }

    /** @return potencia radiativa do fogo (MW), ou {@code null} se indisponivel */
    public Double getFrp() { return frp; }

    /** @return chave de colacao pt-BR do municipio (ignora acentos/caixa na comparacao primaria) */
    public CollationKey getChaveMunicipio() { return chaveMunicipio; }

    /** @return chave de colacao pt-BR do bioma */
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

        /**
         * @return foco imutavel
         * @throws NullPointerException se data/hora, municipio ou bioma nao foram informados
         */
        public FocoIncendio build() {
            return new FocoIncendio(this);
        }
    }
}
