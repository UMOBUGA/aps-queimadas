package br.unip.aps.sorting;

import br.unip.aps.model.FocoIncendio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.ToLongFunction;

/** Criterio de ordenacao, simples ou multicriterio. */
public final class Criterios {
    public static final Comparator<FocoIncendio> POR_DATA = CriterioOrdenacao.DATA.comparador(Ordem.CRESCENTE);
    public static final Comparator<FocoIncendio> POR_BIOMA = CriterioOrdenacao.BIOMA.comparador(Ordem.CRESCENTE);
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

    public static Criterios de(CriterioOrdenacao criterio, Ordem ordem) {
        return new Criterios(List.of(new Nivel(criterio, ordem)));
    }

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

    public Comparator<FocoIncendio> comparador() {
        return comparador;
    }

    public ToLongFunction<FocoIncendio> chaveNumerica() {
        if (niveis.size() != 1) return null;
        Nivel n = niveis.get(0);
        return n.criterio().chaveNumerica(n.ordem());
    }

    public List<Nivel> niveis() {
        return niveis;
    }

    public CriterioOrdenacao principal() {
        return niveis.get(0).criterio();
    }

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
