package br.unip.aps.estruturas;

import br.unip.aps.sorting.OperationCounter;

import java.util.List;

/** Grafo nao direcionado com listas de adjacencia; vertices identificados por nome (indice via tabela hash propria). */
public final class Grafo {
    private final Vetor<String> nomes = new Vetor<>();
    private final Vetor<Vetor<Integer>> adjacencia = new Vetor<>();
    private final TabelaHash<String, Integer> indice;
    private final OperationCounter contador;
    private int arestas;

    public Grafo(OperationCounter contador) {
        this.contador = contador;
        this.indice = TabelaHash.paraTexto(new OperationCounter());
    }

    /** Indice do vertice, criando-o se ainda nao existir. */
    public int vertice(String nome) {
        List<Integer> v = indice.buscar(nome);
        if (!v.isEmpty()) return v.get(0);
        int i = nomes.tamanho();
        nomes.adicionar(nome);
        adjacencia.adicionar(new Vetor<>());
        indice.colocar(nome, i);
        return i;
    }

    /** Indice do vertice, ou -1 se nao existir. */
    public int indiceDe(String nome) {
        List<Integer> v = indice.buscar(nome);
        return v.isEmpty() ? -1 : v.get(0);
    }

    /** Liga dois vertices (ignora laco e aresta repetida). */
    public void ligar(String a, String b) {
        int i = vertice(a), j = vertice(b);
        if (i == j || vizinhos(i, j)) return;
        adjacencia.obter(i).adicionar(j);
        adjacencia.obter(j).adicionar(i);
        arestas++;
    }

    private boolean vizinhos(int i, int j) {
        Vetor<Integer> adj = adjacencia.obter(i);
        for (int k = 0; k < adj.tamanho(); k++) if (adj.obter(k) == j) return true;
        return false;
    }

    /** Busca em largura: distancia (em arestas) de cada vertice ate a origem; -1 se inalcancavel. */
    public int[] distancias(int origem) {
        int n = nomes.tamanho();
        int[] dist = new int[n];
        for (int i = 0; i < n; i++) dist[i] = -1;
        if (origem < 0 || origem >= n) return dist;
        int[] fila = new int[n];
        int inicio = 0, fim = 0;
        fila[fim++] = origem;
        dist[origem] = 0;
        while (inicio < fim) {
            int v = fila[inicio++];
            contador.leitura();
            Vetor<Integer> adj = adjacencia.obter(v);
            for (int k = 0; k < adj.tamanho(); k++) {
                int w = adj.obter(k);
                contador.leitura();
                if (dist[w] < 0) {
                    dist[w] = dist[v] + 1;
                    fila[fim++] = w;
                }
            }
        }
        return dist;
    }

    /** Vizinhos diretos de um vertice. */
    public List<String> vizinhosDe(String nome) {
        int i = indiceDe(nome);
        Vetor<String> r = new Vetor<>();
        if (i >= 0) adjacencia.obter(i).paraCada(w -> r.adicionar(nomes.obter(w)));
        return r.paraLista();
    }

    public String nome(int i) {
        return nomes.obter(i);
    }

    public int vertices() {
        return nomes.tamanho();
    }

    public int arestas() {
        return arestas;
    }

    /** Grau (numero de vizinhos) do vertice i. */
    public int grau(int i) {
        return adjacencia.obter(i).tamanho();
    }
}
