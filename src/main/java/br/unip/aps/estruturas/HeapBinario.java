package br.unip.aps.estruturas;

import br.unip.aps.sorting.OperationCounter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Heap binario minimo em vetor (fila de prioridade): o topo e o menor elemento segundo o comparador. */
public final class HeapBinario<T> {
    private final Comparator<? super T> comparador;
    private final OperationCounter contador;
    private Object[] a;
    private int n;

    public HeapBinario(Comparator<? super T> comparador, OperationCounter contador) {
        this(16, comparador, contador);
    }

    public HeapBinario(int capacidade, Comparator<? super T> comparador, OperationCounter contador) {
        this.a = new Object[Math.max(2, capacidade)];
        this.comparador = comparador;
        this.contador = contador;
    }

    public void inserir(T x) {
        if (n == a.length) {
            Object[] maior = new Object[a.length * 2];
            System.arraycopy(a, 0, maior, 0, n);
            a = maior;
        }
        a[n] = x;
        contador.atribuicao();
        subir(n++);
    }

    @SuppressWarnings("unchecked")
    public T topo() {
        if (n == 0) throw new IllegalStateException("heap vazio");
        contador.leitura();
        return (T) a[0];
    }

    @SuppressWarnings("unchecked")
    public T removerTopo() {
        if (n == 0) throw new IllegalStateException("heap vazio");
        T topo = (T) a[0];
        a[0] = a[--n];
        a[n] = null;
        contador.leituras(2);
        contador.atribuicao();
        if (n > 0) descer(0);
        return topo;
    }

    /** Troca o topo por x e reorganiza: uma unica descida, mais barato que remover e inserir. */
    public void substituirTopo(T x) {
        a[0] = x;
        contador.atribuicao();
        descer(0);
    }

    @SuppressWarnings("unchecked")
    private int comparar(int i, int j) {
        contador.leituras(2);
        return contador.comparar(comparador, (T) a[i], (T) a[j]);
    }

    private void trocar(int i, int j) {
        Object t = a[i];
        a[i] = a[j];
        a[j] = t;
        contador.troca();
    }

    private void subir(int i) {
        while (i > 0) {
            int pai = (i - 1) / 2;
            if (comparar(i, pai) >= 0) break;
            trocar(i, pai);
            i = pai;
        }
    }

    private void descer(int i) {
        while (2 * i + 1 < n) {
            int filho = 2 * i + 1;
            if (filho + 1 < n && comparar(filho + 1, filho) < 0) filho++;
            if (comparar(i, filho) <= 0) break;
            trocar(i, filho);
            i = filho;
        }
    }

    public int tamanho() {
        return n;
    }

    public boolean vazio() {
        return n == 0;
    }

    /** Os k maiores itens em ordem decrescente, mantendo um heap minimo de tamanho k: O(n log k). */
    public static <T> List<T> topK(Iterable<T> itens, int k, Comparator<? super T> comparador, OperationCounter contador) {
        HeapBinario<T> h = new HeapBinario<>(k + 1, comparador, contador);
        if (k <= 0) return List.of();
        for (T x : itens) {
            if (h.tamanho() < k) {
                h.inserir(x);
            } else if (contador.comparar(comparador, x, h.topo()) > 0) {
                h.substituirTopo(x);
            }
        }
        List<T> crescente = new ArrayList<>(h.tamanho());
        while (!h.vazio()) crescente.add(h.removerTopo());
        List<T> r = new ArrayList<>(crescente.size());
        for (int i = crescente.size() - 1; i >= 0; i--) r.add(crescente.get(i));
        return r;
    }
}
