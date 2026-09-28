package br.unip.aps.sorting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Grava cada comparacao, troca e escrita que um algoritmo faz num vetor de inteiros, para reproduzir a ordenacao passo a passo. */
public final class GravadorPassos {
    public static final int COMPARA = 0;
    public static final int TROCA = 1;
    public static final int ATRIBUI = 2;

    /** Um passo: tipo, posicoes envolvidas (-1 quando nao ha) e o valor escrito, nas atribuicoes. */
    public record Passo(int tipo, int i, int j, int valor) { }

    private GravadorPassos() { }

    /** Ordena uma copia de {@code valores} com o algoritmo e devolve todos os passos, na ordem em que aconteceram. */
    public static List<Passo> gravar(AlgoritmoTipo tipo, int[] valores) {
        Integer[] arr = new Integer[valores.length];
        for (int i = 0; i < arr.length; i++) arr[i] = valores[i];
        List<Passo> passos = new ArrayList<>();
        InstrumentedArray.Ouvinte ouvinte = new InstrumentedArray.Ouvinte() {
            @Override
            public void comparacao(int i, int j) {
                passos.add(new Passo(COMPARA, i, j, 0));
            }

            @Override
            public void comparacaoDeValores() {
                passos.add(new Passo(COMPARA, -1, -1, 0));
            }

            @Override
            public void troca(int i, int j) {
                passos.add(new Passo(TROCA, i, j, 0));
            }

            @Override
            public void atribuicao(int i, Object valor) {
                passos.add(new Passo(ATRIBUI, i, -1, (Integer) valor));
            }
        };
        InstrumentedArray<Integer> a = new InstrumentedArray<>(arr, Comparator.naturalOrder(), Integer::longValue,
                new OperationCounter(), ouvinte);
        tipo.criar().ordenar(a);
        return passos;
    }
}
