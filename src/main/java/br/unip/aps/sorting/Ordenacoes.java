package br.unip.aps.sorting;

import br.unip.aps.sorting.algorithms.MergeSort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Atalhos de ordenacao para uso interno do sistema (listas de filtros, rankings, preparo de cenarios do benchmark). */
public final class Ordenacoes {
    private static final SortAlgorithm PADRAO = new MergeSort();

    private Ordenacoes() { }

    /** Ordena (in-place) e devolve a propria lista. */
    public static <T> List<T> ordenar(List<T> lista, Comparator<? super T> comparador) {
        @SuppressWarnings("unchecked")
        T[] arr = (T[]) lista.toArray();
        PADRAO.ordenar(arr, comparador);
        for (int i = 0; i < arr.length; i++) lista.set(i, arr[i]);
        return lista;
    }

    /** Devolve uma copia ordenada, sem alterar a original. */
    public static <T> List<T> copiaOrdenada(List<? extends T> lista, Comparator<? super T> comparador) {
        return ordenar(new ArrayList<>(lista), comparador);
    }

    /** Verifica se a lista esta ordenada (usado para validar o resultado dos algoritmos). */
    public static <T> boolean estaOrdenada(List<? extends T> lista, Comparator<? super T> comparador) {
        for (int i = 1; i < lista.size(); i++) {
            if (comparador.compare(lista.get(i - 1), lista.get(i)) > 0) return false;
        }
        return true;
    }
}
