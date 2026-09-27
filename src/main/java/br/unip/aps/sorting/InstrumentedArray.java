package br.unip.aps.sorting;

import java.util.Comparator;
import java.util.function.ToLongFunction;

/**
 * Array "instrumentado": envolve o vetor a ser ordenado e contabiliza, no
 * {@link OperationCounter}, toda leitura, escrita, troca e comparacao feita pelos algoritmos.
 *
 * <p>E uma aplicacao do padrao <b>Decorator</b> sobre um array comum: os algoritmos escrevem
 * {@code a.get(i)}, {@code a.set(i, v)}, {@code a.swap(i, j)} e {@code a.less(i, j)} em vez de
 * acessar o vetor diretamente. Assim a contagem e uniforme e impossivel de "esquecer" em algum
 * algoritmo, e o codigo de cada algoritmo continua proximo do pseudocodigo classico. O JIT da JVM
 * faz inline desses metodos, entao o custo extra e pequeno.</p>
 *
 * @param <T> tipo dos elementos
 */
public final class InstrumentedArray<T> {

    private final T[] dados;
    private final Comparator<? super T> comparador;
    private final ToLongFunction<? super T> chaveNumerica;
    private final OperationCounter contador;
    private final Ouvinte ouvinte;

    /**
     * Observador opcional das operacoes (padrao <b>Observer</b>). Usado apenas pela visualizacao
     * animada do dashboard; a ordenacao e a contagem sao identicas com ou sem ouvinte.
     */
    public interface Ouvinte {
        /**
         * @param i posicao comparada
         * @param j posicao comparada
         */
        default void comparacao(int i, int j) { }

        /**
         * @param i posicao trocada
         * @param j posicao trocada
         */
        default void troca(int i, int j) { }

        /**
         * @param i     posicao escrita
         * @param valor novo valor
         */
        default void atribuicao(int i, Object valor) { }
    }

    /**
     * @param dados         vetor a ordenar (sera modificado in-place)
     * @param comparador    criterio de ordenacao
     * @param chaveNumerica chave inteira equivalente ao comparador (para Radix Sort), ou {@code null}
     * @param contador      contador de operacoes
     */
    public InstrumentedArray(T[] dados, Comparator<? super T> comparador,
                             ToLongFunction<? super T> chaveNumerica, OperationCounter contador) {
        this.dados = dados;
        this.comparador = comparador;
        this.chaveNumerica = chaveNumerica;
        this.contador = contador;
        this.ouvinte = null;
    }

    /**
     * Cria um array instrumentado que tambem notifica um {@link Ouvinte} a cada comparacao por
     * posicao, troca e atribuicao (arrays auxiliares nao notificam).
     *
     * @param dados         vetor a ordenar
     * @param comparador    criterio de ordenacao
     * @param chaveNumerica chave inteira equivalente (Radix), ou {@code null}
     * @param contador      contador de operacoes
     * @param ouvinte       observador das operacoes
     */
    public InstrumentedArray(T[] dados, Comparator<? super T> comparador,
                             ToLongFunction<? super T> chaveNumerica, OperationCounter contador, Ouvinte ouvinte) {
        this.dados = dados;
        this.comparador = comparador;
        this.chaveNumerica = chaveNumerica;
        this.contador = contador;
        this.ouvinte = ouvinte;
    }

    /** @return numero de elementos */
    public int length() {
        return dados.length;
    }

    /**
     * @param i posicao
     * @return elemento na posicao (1 leitura)
     */
    public T get(int i) {
        contador.leitura();
        return dados[i];
    }

    /**
     * @param i posicao
     * @param v novo valor (1 escrita)
     */
    public void set(int i, T v) {
        contador.atribuicao();
        dados[i] = v;
        if (ouvinte != null) ouvinte.atribuicao(i, v);
    }

    /**
     * Troca os elementos das posicoes i e j (1 troca, 2 leituras, 2 escritas).
     *
     * @param i posicao
     * @param j posicao
     */
    public void swap(int i, int j) {
        contador.troca();
        T t = dados[i];
        dados[i] = dados[j];
        dados[j] = t;
        if (ouvinte != null) ouvinte.troca(i, j);
    }

    /**
     * Compara as posicoes i e j (2 leituras, 1 comparacao).
     *
     * @param i posicao
     * @param j posicao
     * @return negativo, zero ou positivo, como {@link Comparator#compare}
     */
    public int compare(int i, int j) {
        contador.leituras(2);
        if (ouvinte != null) ouvinte.comparacao(i, j);
        return contador.comparar(comparador, dados[i], dados[j]);
    }

    /**
     * @param i posicao
     * @param j posicao
     * @return {@code true} se a[i] &lt; a[j]
     */
    public boolean less(int i, int j) {
        return compare(i, j) < 0;
    }

    /**
     * Compara dois valores ja lidos (1 comparacao, nenhuma leitura).
     *
     * @param x valor
     * @param y valor
     * @return resultado do comparador
     */
    public int compareValues(T x, T y) {
        return contador.comparar(comparador, x, y);
    }

    /**
     * @param x valor
     * @param y valor
     * @return {@code true} se x &lt; y
     */
    public boolean lessValues(T x, T y) {
        return compareValues(x, y) < 0;
    }

    /**
     * Cria um array auxiliar do mesmo tipo, contabilizado no mesmo contador (Merge Sort, Radix).
     *
     * @param n tamanho
     * @return novo array instrumentado vazio
     */
    @SuppressWarnings("unchecked")
    public InstrumentedArray<T> auxiliar(int n) {
        T[] aux = (T[]) java.lang.reflect.Array.newInstance(dados.getClass().getComponentType(), n);
        return new InstrumentedArray<>(aux, comparador, chaveNumerica, contador);
    }

    /**
     * Chave inteira do elemento na posicao i, para algoritmos que nao comparam (Radix Sort).
     *
     * @param i posicao (1 leitura)
     * @return chave numerica
     * @throws UnsupportedOperationException se o criterio nao possuir chave numerica
     */
    public long key(int i) {
        if (chaveNumerica == null) {
            throw new UnsupportedOperationException(
                    "Este algoritmo exige um criterio com chave numerica (ex.: data, latitude, longitude, id).");
        }
        contador.leitura();
        return chaveNumerica.applyAsLong(dados[i]);
    }

    /** @return {@code true} se ha chave numerica disponivel para Radix Sort */
    public boolean temChaveNumerica() {
        return chaveNumerica != null;
    }

    /** @return contador de operacoes associado */
    public OperationCounter contador() {
        return contador;
    }

    /** @return vetor subjacente (sem contabilizacao; uso interno e verificacoes) */
    T[] bruto() {
        return dados;
    }
}
