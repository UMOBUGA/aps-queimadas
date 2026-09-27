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

/**
 * Estado da barra de filtros global, compartilhado por Visao geral, Ordenacao e Mapa. Delega ao
 * {@link FiltroFocos} do dominio e acrescenta a selecao EXATA de municipio (escolhido na lista de
 * sugestoes), que evita que "ITU" tambem traga "ITUVERAVA".
 *
 * @param anos      anos selecionados (vazio = todos)
 * @param biomas    biomas selecionados (vazio = todos)
 * @param municipio texto do municipio ({@code null} = todos)
 * @param exato     {@code true} se o municipio foi escolhido na lista (comparacao exata)
 * @param de        mes inicial ({@code null} = sem limite)
 * @param ate       mes final ({@code null} = sem limite)
 */
public record FiltroGlobal(Set<Integer> anos, Set<String> biomas, String municipio, boolean exato,
                           YearMonth de, YearMonth ate) implements Predicate<FocoIncendio> {

    /** Sem filtros. */
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

    /** @return filtro equivalente do dominio (usado nos relatorios) */
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

    /** @return {@code true} se ha algum filtro ativo */
    public boolean ativo() {
        return !anos.isEmpty() || !biomas.isEmpty() || municipio != null || de != null || ate != null;
    }

    /**
     * @return descricoes curtas dos filtros ativos (para chips e relatorios)
     */
    public List<String> descricoes() {
        List<String> r = new ArrayList<>();
        if (!biomas.isEmpty()) r.add("Bioma: " + String.join(", ", biomas));
        if (!anos.isEmpty()) r.add("Ano: " + String.join(", ", anos.stream().map(String::valueOf).toList()));
        if (municipio != null) r.add("Município: " + municipio);
        if (de != null || ate != null) r.add("Período: " + fmt(de, "início") + " – " + fmt(ate, "fim"));
        return r;
    }

    /** @return descricao em uma linha */
    public String descricao() {
        List<String> d = descricoes();
        return d.isEmpty() ? "sem filtros" : String.join(" · ", d);
    }

    private static String fmt(YearMonth ym, String padrao) {
        return ym == null ? padrao : br.unip.aps.analysis.Estatisticas.MESES[ym.getMonthValue() - 1] + "/" + ym.getYear();
    }

    /** @return copia sem biomas */
    public FiltroGlobal semBiomas() { return new FiltroGlobal(anos, Set.of(), municipio, exato, de, ate); }
    /** @return copia sem anos */
    public FiltroGlobal semAnos() { return new FiltroGlobal(Set.of(), biomas, municipio, exato, de, ate); }
    /** @return copia sem municipio */
    public FiltroGlobal semMunicipio() { return new FiltroGlobal(anos, biomas, null, false, de, ate); }
    /** @return copia sem periodo */
    public FiltroGlobal semPeriodo() { return new FiltroGlobal(anos, biomas, municipio, exato, null, null); }
}
