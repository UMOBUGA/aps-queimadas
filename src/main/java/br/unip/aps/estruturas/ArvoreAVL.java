package br.unip.aps.estruturas;

import br.unip.aps.sorting.OperationCounter;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Arvore AVL (Adelson-Velsky e Landis, 1962): arvore binaria de busca balanceada por rotacoes. */
public final class ArvoreAVL<K, V> {
    /** No da arvore; chaves repetidas acumulam valores no mesmo no. */
    public static final class No<K, V> {
        private K chave;
        private Vetor<V> valores = new Vetor<>(2);
        private No<K, V> esquerda, direita;
        private int altura = 1;

        private No(K chave, V valor) {
            this.chave = chave;
            valores.adicionar(valor);
        }

        public K chave() { return chave; }
        public No<K, V> esquerda() { return esquerda; }
        public No<K, V> direita() { return direita; }
        public int altura() { return altura; }
        public int quantidade() { return valores.tamanho(); }
    }

    /** Recebe cada rotacao realizada (para a animacao da arvore). */
    public interface Ouvinte<K> {
        void rotacao(String tipo, K pivo);
    }

    private final Comparator<? super K> comparador;
    private final OperationCounter contador;
    private No<K, V> raiz;
    private int nos;
    private long valores;
    private long rotacoesSimples;
    private long rotacoesDuplas;
    private Ouvinte<K> ouvinte;
    private int removidos;

    public ArvoreAVL(Comparator<? super K> comparador, OperationCounter contador) {
        this.comparador = comparador;
        this.contador = contador;
    }

    public void setOuvinte(Ouvinte<K> ouvinte) {
        this.ouvinte = ouvinte;
    }

    public void inserir(K chave, V valor) {
        raiz = inserir(raiz, chave, valor);
        valores++;
    }

    private No<K, V> inserir(No<K, V> n, K chave, V valor) {
        if (n == null) {
            nos++;
            contador.atribuicao();
            return new No<>(chave, valor);
        }
        contador.leitura();
        int r = contador.comparar(comparador, chave, n.chave);
        if (r < 0) {
            n.esquerda = inserir(n.esquerda, chave, valor);
        } else if (r > 0) {
            n.direita = inserir(n.direita, chave, valor);
        } else {
            n.valores.adicionar(valor);
            return n;
        }
        return balancear(n);
    }

    /** Remove a chave e todos os seus valores. */
    public boolean remover(K chave) {
        removidos = 0;
        raiz = remover(raiz, chave);
        valores -= removidos;
        return removidos > 0;
    }

    private No<K, V> remover(No<K, V> n, K chave) {
        if (n == null) return null;
        contador.leitura();
        int r = contador.comparar(comparador, chave, n.chave);
        if (r < 0) {
            n.esquerda = remover(n.esquerda, chave);
        } else if (r > 0) {
            n.direita = remover(n.direita, chave);
        } else {
            removidos = n.valores.tamanho();
            if (n.esquerda == null || n.direita == null) {
                nos--;
                return n.esquerda != null ? n.esquerda : n.direita;
            }
            No<K, V> sucessor = n.direita;
            while (sucessor.esquerda != null) {
                contador.leitura();
                sucessor = sucessor.esquerda;
            }
            n.chave = sucessor.chave;
            n.valores = sucessor.valores;
            n.direita = removerMinimo(n.direita);
            nos--;
            contador.atribuicoes(2);
        }
        return balancear(n);
    }

    private No<K, V> removerMinimo(No<K, V> n) {
        if (n.esquerda == null) return n.direita;
        n.esquerda = removerMinimo(n.esquerda);
        return balancear(n);
    }

    /** Valores associados a chave (lista vazia se ausente). */
    public List<V> buscar(K chave) {
        No<K, V> n = raiz;
        while (n != null) {
            contador.leitura();
            int r = contador.comparar(comparador, chave, n.chave);
            if (r == 0) return n.valores.paraLista();
            n = r < 0 ? n.esquerda : n.direita;
        }
        return List.of();
    }

    /** Visita em ordem crescente os valores com chave em [de, ate], podando subarvores fora do intervalo. */
    public void intervalo(K de, K ate, Consumer<? super V> destino) {
        intervalo(raiz, de, ate, destino);
    }

    private void intervalo(No<K, V> n, K de, K ate, Consumer<? super V> destino) {
        if (n == null) return;
        contador.leitura();
        int cDe = contador.comparar(comparador, n.chave, de);
        int cAte = contador.comparar(comparador, n.chave, ate);
        if (cDe > 0) intervalo(n.esquerda, de, ate, destino);
        if (cDe >= 0 && cAte <= 0) n.valores.paraCada(destino);
        if (cAte < 0) intervalo(n.direita, de, ate, destino);
    }

    /** Percurso em ordem (chaves crescentes). */
    public void emOrdem(BiConsumer<? super K, List<V>> visita) {
        emOrdem(raiz, visita);
    }

    private void emOrdem(No<K, V> n, BiConsumer<? super K, List<V>> visita) {
        if (n == null) return;
        emOrdem(n.esquerda, visita);
        visita.accept(n.chave, n.valores.paraLista());
        emOrdem(n.direita, visita);
    }

    private static int altura(No<?, ?> n) {
        return n == null ? 0 : n.altura;
    }

    private static void atualizar(No<?, ?> n) {
        n.altura = 1 + Math.max(altura(n.esquerda), altura(n.direita));
    }

    private static int fator(No<?, ?> n) {
        return altura(n.esquerda) - altura(n.direita);
    }

    private No<K, V> balancear(No<K, V> n) {
        atualizar(n);
        int fb = fator(n);
        if (fb > 1) {
            if (fator(n.esquerda) < 0) {
                n.esquerda = rotacaoEsquerda(n.esquerda);
                rotacoesDuplas++;
                avisar("dupla esquerda-direita", n.chave);
            } else {
                rotacoesSimples++;
                avisar("simples à direita", n.chave);
            }
            return rotacaoDireita(n);
        }
        if (fb < -1) {
            if (fator(n.direita) > 0) {
                n.direita = rotacaoDireita(n.direita);
                rotacoesDuplas++;
                avisar("dupla direita-esquerda", n.chave);
            } else {
                rotacoesSimples++;
                avisar("simples à esquerda", n.chave);
            }
            return rotacaoEsquerda(n);
        }
        return n;
    }

    private No<K, V> rotacaoDireita(No<K, V> y) {
        No<K, V> x = y.esquerda;
        y.esquerda = x.direita;
        x.direita = y;
        contador.atribuicoes(2);
        atualizar(y);
        atualizar(x);
        return x;
    }

    private No<K, V> rotacaoEsquerda(No<K, V> x) {
        No<K, V> y = x.direita;
        x.direita = y.esquerda;
        y.esquerda = x;
        contador.atribuicoes(2);
        atualizar(x);
        atualizar(y);
        return y;
    }

    private void avisar(String tipo, K pivo) {
        if (ouvinte != null) ouvinte.rotacao(tipo, pivo);
    }

    /** Verifica as invariantes: ordem de busca e |fator de balanceamento| &lt;= 1 em todos os nos. */
    public boolean valida() {
        return valida(raiz, null, null);
    }

    private boolean valida(No<K, V> n, K min, K max) {
        if (n == null) return true;
        if (min != null && comparador.compare(n.chave, min) <= 0) return false;
        if (max != null && comparador.compare(n.chave, max) >= 0) return false;
        if (Math.abs(fator(n)) > 1) return false;
        if (n.altura != 1 + Math.max(altura(n.esquerda), altura(n.direita))) return false;
        return valida(n.esquerda, min, n.chave) && valida(n.direita, n.chave, max);
    }

    public No<K, V> raiz() { return raiz; }
    public int altura() { return altura(raiz); }
    public int nos() { return nos; }
    public long valores() { return valores; }
    public long rotacoesSimples() { return rotacoesSimples; }
    public long rotacoesDuplas() { return rotacoesDuplas; }
    public OperationCounter contador() { return contador; }
}
