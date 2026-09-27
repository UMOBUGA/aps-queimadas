package br.unip.aps.sorting;

import br.unip.aps.model.FocoIncendio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.ToLongFunction;

/**
 * Criterio de ordenacao completo, possivelmente multicriterio (ex.: bioma ↑ → municipio ↑ → data ↓).
 *
 * <p>O comparador composto e montado com {@link Comparator#thenComparing}: o segundo criterio so
 * desempata o primeiro, o terceiro so desempata os dois anteriores, e assim por diante. Tambem
 * expoe comparadores prontos para os tres criterios exigidos pelo enunciado.</p>
 */
public final class Criterios {

    /** Data/hora crescente. */
    public static final Comparator<FocoIncendio> POR_DATA = CriterioOrdenacao.DATA.comparador(Ordem.CRESCENTE);
    /** Bioma crescente (colacao pt-BR). */
    public static final Comparator<FocoIncendio> POR_BIOMA = CriterioOrdenacao.BIOMA.comparador(Ordem.CRESCENTE);
    /** Municipio crescente (colacao pt-BR). */
    public static final Comparator<FocoIncendio> POR_MUNICIPIO = CriterioOrdenacao.MUNICIPIO.comparador(Ordem.CRESCENTE);

    /** Um nivel da ordenacao: criterio + direcao. */
    public record Nivel(CriterioOrdenacao criterio, Ordem ordem) {
        public Nivel {
            Objects.requireNonNull(criterio, "criterio");
            Objects.requireNonNull(ordem, "ordem");
        }

        @Override
        public String toString() {
            return criterio.rotulo() + " " + ordem.simbolo();
        }
    }

    private final List<Nivel> niveis;
    private final Comparator<FocoIncendio> comparador;

    private Criterios(List<Nivel> niveis) {
        if (niveis.isEmpty()) throw new IllegalArgumentException("Informe ao menos um criterio de ordenacao.");
        this.niveis = List.copyOf(niveis);
        Comparator<FocoIncendio> c = null;
        for (Nivel n : this.niveis) {
            Comparator<FocoIncendio> atual = n.criterio().comparador(n.ordem());
            c = c == null ? atual : c.thenComparing(atual);
        }
        this.comparador = c;
    }

    /**
     * @param criterio criterio unico
     * @param ordem    direcao
     * @return criterio simples
     */
    public static Criterios de(CriterioOrdenacao criterio, Ordem ordem) {
        return new Criterios(List.of(new Nivel(criterio, ordem)));
    }

    /**
     * @param niveis niveis na ordem de prioridade (niveis repetidos sao ignorados)
     * @return criterio composto
     */
    public static Criterios composto(List<Nivel> niveis) {
        List<Nivel> semRepeticao = new ArrayList<>();
        List<CriterioOrdenacao> usados = new ArrayList<>();
        for (Nivel n : niveis) {
            if (!usados.contains(n.criterio())) {
                usados.add(n.criterio());
                semRepeticao.add(n);
            }
        }
        return new Criterios(semRepeticao);
    }

    /** @return comparador (composto) equivalente */
    public Comparator<FocoIncendio> comparador() {
        return comparador;
    }

    /** @return chave numerica para Radix Sort, ou {@code null} se multicriterio ou nao numerico */
    public ToLongFunction<FocoIncendio> chaveNumerica() {
        if (niveis.size() != 1) return null;
        Nivel n = niveis.get(0);
        return n.criterio().chaveNumerica(n.ordem());
    }

    /** @return niveis da ordenacao */
    public List<Nivel> niveis() {
        return niveis;
    }

    /** @return criterio principal (primeiro nivel) */
    public CriterioOrdenacao principal() {
        return niveis.get(0).criterio();
    }

    /** @return descricao legivel, ex.: "Bioma ↑ → Município ↑ → Data/hora ↓" */
    public String descricao() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < niveis.size(); i++) {
            if (i > 0) sb.append(" → ");
            sb.append(niveis.get(i));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return descricao();
    }
}
