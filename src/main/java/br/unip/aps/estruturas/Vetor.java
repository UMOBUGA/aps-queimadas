package br.unip.aps.estruturas;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Vetor dinamico (array redimensionavel) usado dentro das estruturas implementadas a mao. */
public final class Vetor<T> {
    private Object[] itens;
    private int tamanho;

    public Vetor() {
        this(4);
    }

    public Vetor(int capacidade) {
        itens = new Object[Math.max(1, capacidade)];
    }

    public void adicionar(T item) {
        if (tamanho == itens.length) {
            Object[] maior = new Object[itens.length * 2];
            System.arraycopy(itens, 0, maior, 0, tamanho);
            itens = maior;
        }
        itens[tamanho++] = item;
    }

    @SuppressWarnings("unchecked")
    public T obter(int i) {
        if (i < 0 || i >= tamanho) throw new IndexOutOfBoundsException(i);
        return (T) itens[i];
    }

    public int tamanho() {
        return tamanho;
    }

    public boolean vazio() {
        return tamanho == 0;
    }

    @SuppressWarnings("unchecked")
    public void paraCada(Consumer<? super T> acao) {
        for (int i = 0; i < tamanho; i++) acao.accept((T) itens[i]);
    }

    /** Copia para uma lista comum, na fronteira com o restante do sistema. */
    public List<T> paraLista() {
        List<T> r = new ArrayList<>(tamanho);
        paraCada(r::add);
        return r;
    }
}
