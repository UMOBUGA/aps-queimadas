package br.unip.aps.estruturas;

import br.unip.aps.sorting.OperationCounter;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;

/** Tabela hash com encadeamento separado; cada chave guarda uma lista de valores (multimapa). */
public final class TabelaHash<K, V> {
    private static final double CARGA_MAXIMA = 0.75;

    private static final class Entrada<K, V> {
        private final K chave;
        private final int hash;
        private final Vetor<V> valores = new Vetor<>(2);
        private Entrada<K, V> proxima;

        private Entrada(K chave, int hash, Entrada<K, V> proxima) {
            this.chave = chave;
            this.hash = hash;
            this.proxima = proxima;
        }
    }

    private final ToIntFunction<? super K> funcaoHash;
    private final Comparator<? super K> comparador;
    private final OperationCounter contador;
    private Entrada<K, V>[] baldes;
    private int chaves;
    private long valores;
    private long colisoes;
    private int redimensionamentos;

    public TabelaHash(ToIntFunction<? super K> funcaoHash, Comparator<? super K> comparador, OperationCounter contador) {
        this(16, funcaoHash, comparador, contador);
    }

    @SuppressWarnings("unchecked")
    public TabelaHash(int capacidade, ToIntFunction<? super K> funcaoHash, Comparator<? super K> comparador, OperationCounter contador) {
        int c = 1;
        while (c < capacidade) c <<= 1;
        this.baldes = (Entrada<K, V>[]) new Entrada[c];
        this.funcaoHash = funcaoHash;
        this.comparador = comparador;
        this.contador = contador;
    }

    /** Tabela para chaves de texto com a funcao FNV-1a de 32 bits. */
    public static <V> TabelaHash<String, V> paraTexto(OperationCounter contador) {
        return new TabelaHash<>(TabelaHash::fnv1a, Comparator.naturalOrder(), contador);
    }

    /** FNV-1a (Fowler, Noll e Vo): combina cada caractere com XOR e multiplica pelo primo 16777619. */
    public static int fnv1a(CharSequence s) {
        int h = 0x811C9DC5;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x01000193;
        }
        return h;
    }

    private int indice(int hash) {
        return (hash ^ (hash >>> 16)) & (baldes.length - 1);
    }

    private Entrada<K, V> localizar(K chave, int hash) {
        contador.leitura();
        for (Entrada<K, V> e = baldes[indice(hash)]; e != null; e = e.proxima) {
            contador.leitura();
            if (e.hash == hash && contador.comparar(comparador, e.chave, chave) == 0) return e;
        }
        return null;
    }

    public void colocar(K chave, V valor) {
        int h = funcaoHash.applyAsInt(chave);
        Entrada<K, V> e = localizar(chave, h);
        if (e == null) {
            int i = indice(h);
            if (baldes[i] != null) colisoes++;
            e = new Entrada<>(chave, h, baldes[i]);
            baldes[i] = e;
            contador.atribuicao();
            chaves++;
            if (chaves > CARGA_MAXIMA * baldes.length) redimensionar();
        }
        e.valores.adicionar(valor);
        valores++;
    }

    @SuppressWarnings("unchecked")
    private void redimensionar() {
        Entrada<K, V>[] antigos = baldes;
        baldes = (Entrada<K, V>[]) new Entrada[antigos.length * 2];
        redimensionamentos++;
        for (Entrada<K, V> cabeca : antigos) {
            Entrada<K, V> e = cabeca;
            while (e != null) {
                Entrada<K, V> proxima = e.proxima;
                int i = indice(e.hash);
                e.proxima = baldes[i];
                baldes[i] = e;
                contador.atribuicao();
                e = proxima;
            }
        }
    }

    /** Valores da chave (lista vazia se ausente). */
    public List<V> buscar(K chave) {
        Entrada<K, V> e = localizar(chave, funcaoHash.applyAsInt(chave));
        return e == null ? List.of() : e.valores.paraLista();
    }

    /** Quantidade de valores da chave, sem copiar a lista. */
    public int contar(K chave) {
        Entrada<K, V> e = localizar(chave, funcaoHash.applyAsInt(chave));
        return e == null ? 0 : e.valores.tamanho();
    }

    public boolean remover(K chave) {
        int h = funcaoHash.applyAsInt(chave);
        int i = indice(h);
        Entrada<K, V> anterior = null;
        contador.leitura();
        for (Entrada<K, V> e = baldes[i]; e != null; anterior = e, e = e.proxima) {
            contador.leitura();
            if (e.hash == h && contador.comparar(comparador, e.chave, chave) == 0) {
                if (anterior == null) baldes[i] = e.proxima;
                else anterior.proxima = e.proxima;
                contador.atribuicao();
                chaves--;
                valores -= e.valores.tamanho();
                return true;
            }
        }
        return false;
    }

    /** Visita cada chave com sua quantidade de valores (ordem dos baldes). */
    public void paraCada(BiConsumer<? super K, Integer> visita) {
        for (Entrada<K, V> cabeca : baldes) {
            for (Entrada<K, V> e = cabeca; e != null; e = e.proxima) visita.accept(e.chave, e.valores.tamanho());
        }
    }

    /** Histograma dos comprimentos de cadeia: posicao i = quantidade de baldes com i entradas. */
    public int[] distribuicaoCadeias() {
        int maior = maiorCadeia();
        int[] h = new int[maior + 1];
        for (Entrada<K, V> cabeca : baldes) {
            int n = 0;
            for (Entrada<K, V> e = cabeca; e != null; e = e.proxima) n++;
            h[n]++;
        }
        return h;
    }

    public int maiorCadeia() {
        int maior = 0;
        for (Entrada<K, V> cabeca : baldes) {
            int n = 0;
            for (Entrada<K, V> e = cabeca; e != null; e = e.proxima) n++;
            maior = Math.max(maior, n);
        }
        return maior;
    }

    public double fatorCarga() {
        return (double) chaves / baldes.length;
    }

    public int chaves() { return chaves; }
    public long valores() { return valores; }
    public int capacidade() { return baldes.length; }
    public long colisoes() { return colisoes; }
    public int redimensionamentos() { return redimensionamentos; }
    public OperationCounter contador() { return contador; }
}
