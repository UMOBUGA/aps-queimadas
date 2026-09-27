package br.unip.aps.ui;

import br.unip.aps.analysis.FiltroFocos;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.util.Textos;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** Estado da barra de filtros global, compartilhado por Visao geral, Ordenacao e Mapa. */
public record FiltroGlobal(Set<Integer> anos, Set<String> biomas, String municipio, boolean exato,
                           YearMonth de, YearMonth ate) implements Predicate<FocoIncendio> {
    public static final FiltroGlobal VAZIO = new FiltroGlobal(Set.of(), Set.of(), null, false, null, null);

    public FiltroGlobal {
        anos = anos == null ? Set.of() : Set.copyOf(anos);
        biomas = biomas == null ? Set.of() : Set.copyOf(biomas);
        municipio = municipio == null || municipio.isBlank() ? null : municipio.strip();
        if (de != null && ate != null && ate.isBefore(de)) {
            YearMonth t = de;
            de = ate;
            ate = t;
        }
    }

    public FiltroFocos paraDominio() {
        LocalDate ini = de == null ? null : de.atDay(1);
        LocalDate fim = ate == null ? null : ate.atEndOfMonth();
        return new FiltroFocos(anos, biomas, municipio, ini, fim);
    }

    @Override
    public boolean test(FocoIncendio f) {
        if (!paraDominio().test(f)) return false;
        return !exato || municipio == null || Textos.semAcentos(f.getMunicipio()).equals(Textos.semAcentos(municipio));
    }

    public boolean ativo() {
        return !anos.isEmpty() || !biomas.isEmpty() || municipio != null || de != null || ate != null;
    }

    public List<String> descricoes() {
        List<String> r = new ArrayList<>();
        if (!biomas.isEmpty()) r.add("Bioma: " + String.join(", ", biomas));
        if (!anos.isEmpty()) r.add("Ano: " + String.join(", ", anos.stream().map(String::valueOf).toList()));
        if (municipio != null) r.add("Município: " + municipio);
        if (de != null || ate != null) r.add("Período: " + fmt(de, "início") + " – " + fmt(ate, "fim"));
        return r;
    }

    public String descricao() {
        List<String> d = descricoes();
        return d.isEmpty() ? "sem filtros" : String.join(" · ", d);
    }

    private static String fmt(YearMonth ym, String padrao) {
        return ym == null ? padrao : br.unip.aps.analysis.Estatisticas.MESES[ym.getMonthValue() - 1] + "/" + ym.getYear();
    }

    public FiltroGlobal semBiomas() { return new FiltroGlobal(anos, Set.of(), municipio, exato, de, ate); }
    public FiltroGlobal semAnos() { return new FiltroGlobal(Set.of(), biomas, municipio, exato, de, ate); }
    public FiltroGlobal semMunicipio() { return new FiltroGlobal(anos, biomas, null, false, de, ate); }
    public FiltroGlobal semPeriodo() { return new FiltroGlobal(anos, biomas, municipio, exato, null, null); }
}
