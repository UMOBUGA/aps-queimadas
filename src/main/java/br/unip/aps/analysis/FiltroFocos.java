package br.unip.aps.analysis;

import br.unip.aps.model.FocoIncendio;
import br.unip.aps.util.Formatos;
import br.unip.aps.util.Textos;

import java.time.LocalDate;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Filtro combinavel de focos (ano, bioma, municipio e periodo), usado no mapa, nas tabelas e nos
 * graficos do dashboard. Campos {@code null}/vazios nao filtram.
 *
 * @param anos        anos aceitos (vazio = todos)
 * @param biomas      biomas aceitos (vazio = todos)
 * @param municipio   trecho do nome do municipio, sem diferenciar acentos/caixa ({@code null} = todos)
 * @param dataInicio  data minima inclusiva ({@code null} = sem limite)
 * @param dataFim     data maxima inclusiva ({@code null} = sem limite)
 */
public record FiltroFocos(Set<Integer> anos, Set<String> biomas, String municipio,
                          LocalDate dataInicio, LocalDate dataFim) implements Predicate<FocoIncendio> {

    /** Filtro que aceita tudo. */
    public static final FiltroFocos TODOS = new FiltroFocos(Set.of(), Set.of(), null, null, null);

    public FiltroFocos {
        anos = anos == null ? Set.of() : Set.copyOf(anos);
        biomas = biomas == null ? Set.of() : Set.copyOf(biomas);
        municipio = municipio == null || municipio.isBlank() ? null : Textos.semAcentos(municipio.strip());
        if (dataInicio != null && dataFim != null && dataFim.isBefore(dataInicio)) {
            throw new IllegalArgumentException("A data final (" + dataFim.format(Formatos.DATA)
                    + ") e anterior a data inicial (" + dataInicio.format(Formatos.DATA) + ").");
        }
    }

    @Override
    public boolean test(FocoIncendio f) {
        if (!anos.isEmpty() && !anos.contains(f.getAno())) return false;
        if (!biomas.isEmpty() && !biomas.contains(f.getBioma())) return false;
        if (municipio != null && !Textos.semAcentos(f.getMunicipio()).contains(municipio)) return false;
        LocalDate d = f.getData();
        if (dataInicio != null && d.isBefore(dataInicio)) return false;
        return dataFim == null || !d.isAfter(dataFim);
    }

    /** @return descricao do filtro para relatorios */
    public String descricao() {
        StringBuilder sb = new StringBuilder();
        if (!anos.isEmpty()) sb.append("anos=").append(anos).append("; ");
        if (!biomas.isEmpty()) sb.append("biomas=").append(biomas).append("; ");
        if (municipio != null) sb.append("municipio contem '").append(municipio).append("'; ");
        if (dataInicio != null) sb.append("de ").append(dataInicio.format(Formatos.DATA)).append("; ");
        if (dataFim != null) sb.append("ate ").append(dataFim.format(Formatos.DATA)).append("; ");
        return sb.isEmpty() ? "sem filtros" : sb.substring(0, sb.length() - 2);
    }
}
