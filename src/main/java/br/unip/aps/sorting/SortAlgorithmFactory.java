package br.unip.aps.sorting;

import br.unip.aps.util.Textos;

import java.util.ArrayList;
import java.util.List;

/**
 * Fabrica de algoritmos de ordenacao (padrao <b>Factory</b>).
 *
 * <p>Centraliza a criacao das estrategias: o menu de console, o dashboard e o benchmark pedem
 * um algoritmo pelo tipo ou pelo nome, sem conhecer as classes concretas. Adicionar um novo
 * algoritmo exige apenas implementar {@link SortAlgorithm} e registra-lo em {@link AlgoritmoTipo}.</p>
 */
public final class SortAlgorithmFactory {

    private SortAlgorithmFactory() { }

    /**
     * @param tipo tipo do algoritmo
     * @return nova instancia
     */
    public static SortAlgorithm criar(AlgoritmoTipo tipo) {
        return tipo.criar();
    }

    /**
     * Cria um algoritmo pelo nome, ignorando caixa, acentos, espacos e o sufixo "sort"
     * ("merge", "Merge Sort", "MERGE" funcionam).
     *
     * @param nome nome ou constante do algoritmo
     * @return nova instancia
     * @throws IllegalArgumentException se nenhum algoritmo corresponder ao nome
     */
    public static SortAlgorithm criar(String nome) {
        return tipo(nome).criar();
    }

    /**
     * @param nome nome ou constante do algoritmo
     * @return tipo correspondente
     * @throws IllegalArgumentException se nenhum algoritmo corresponder
     */
    public static AlgoritmoTipo tipo(String nome) {
        String alvo = simplificar(nome);
        for (AlgoritmoTipo t : AlgoritmoTipo.values()) {
            if (simplificar(t.name()).equals(alvo) || simplificar(t.nome()).equals(alvo)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Algoritmo desconhecido: '" + nome + "'. Opcoes: " + nomes());
    }

    /** @return uma instancia de cada algoritmo disponivel */
    public static List<SortAlgorithm> todos() {
        List<SortAlgorithm> r = new ArrayList<>();
        for (AlgoritmoTipo t : AlgoritmoTipo.values()) r.add(t.criar());
        return r;
    }

    /** @return nomes de exibicao de todos os algoritmos */
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
