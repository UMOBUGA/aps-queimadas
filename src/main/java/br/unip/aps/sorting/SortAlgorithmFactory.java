package br.unip.aps.sorting;

import br.unip.aps.util.Textos;

import java.util.ArrayList;
import java.util.List;

/** Fabrica de algoritmos de ordenacao (padrao Factory). */
public final class SortAlgorithmFactory {
    private SortAlgorithmFactory() { }

    public static SortAlgorithm criar(AlgoritmoTipo tipo) {
        return tipo.criar();
    }

    /** Cria um algoritmo pelo nome, ignorando caixa, acentos e o sufixo "sort". */
    public static SortAlgorithm criar(String nome) {
        return tipo(nome).criar();
    }

    public static AlgoritmoTipo tipo(String nome) {
        String alvo = simplificar(nome);
        for (AlgoritmoTipo t : AlgoritmoTipo.values()) {
            if (simplificar(t.name()).equals(alvo) || simplificar(t.nome()).equals(alvo)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Algoritmo desconhecido: '" + nome + "'. Opcoes: " + nomes());
    }

    public static List<SortAlgorithm> todos() {
        List<SortAlgorithm> r = new ArrayList<>();
        for (AlgoritmoTipo t : AlgoritmoTipo.values()) r.add(t.criar());
        return r;
    }

    public static List<String> nomes() {
        List<String> r = new ArrayList<>();
        for (AlgoritmoTipo t : AlgoritmoTipo.values()) r.add(t.nome());
        return r;
    }

    private static String simplificar(String s) {
        return Textos.semAcentos(s).replaceAll("[^a-z0-9]", "").replace("sort", "")
                .replace("simplificado", "").replace("lsd", "");
    }
}
