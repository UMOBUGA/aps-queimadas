package br.unip.aps.sorting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Cenario (disposicao inicial) da entrada entregue aos algoritmos, para estudar melhor, medio e pior caso. */
public enum CenarioEntrada {
    /** Ordem original dos arquivos do INPE (cronologica por passagem do satelite). */
    ORIGINAL("Original (arquivo)"),
    /** Embaralhada com semente fixa. */
    ALEATORIO("Aleatória"),
    /** Ja ordenada pelo criterio. */
    ORDENADO("Já ordenada"),
    /** Ordenada de tras para frente. */
    INVERSO("Inversamente ordenada"),
    /** Ordenada com 5% de pares trocados aleatoriamente (dados "quase ordenados"). */
    QUASE_ORDENADO("Quase ordenada (5%)");

    private final String rotulo;

    CenarioEntrada(String rotulo) {
        this.rotulo = rotulo;
    }

    /** Prepara uma nova lista com a disposicao do cenario (a lista base nao e alterada). */
    public <T> List<T> preparar(List<T> base, Comparator<? super T> comparador, long semente) {
        List<T> r = new ArrayList<>(base);
        Random rnd = new Random(semente);
        switch (this) {
            case ORIGINAL -> { }
            case ALEATORIO -> embaralhar(r, rnd);
            case ORDENADO -> Ordenacoes.ordenar(r, comparador);
            case INVERSO -> {
                Ordenacoes.ordenar(r, comparador);
                inverter(r);
            }
            case QUASE_ORDENADO -> {
                Ordenacoes.ordenar(r, comparador);
                int trocas = Math.max(1, r.size() / 20);
                for (int k = 0; k < trocas && r.size() > 1; k++) {
                    int i = rnd.nextInt(r.size());
                    int j = rnd.nextInt(r.size());
                    T t = r.get(i);
                    r.set(i, r.get(j));
                    r.set(j, t);
                }
            }
        }
        return r;
    }

    /** Embaralhamento de Fisher-Yates (Knuth, algoritmo P). */
    public static <T> void embaralhar(List<T> lista, Random rnd) {
        for (int i = lista.size() - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            T t = lista.get(i);
            lista.set(i, lista.get(j));
            lista.set(j, t);
        }
    }

    private static <T> void inverter(List<T> lista) {
        for (int i = 0, j = lista.size() - 1; i < j; i++, j--) {
            T t = lista.get(i);
            lista.set(i, lista.get(j));
            lista.set(j, t);
        }
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
