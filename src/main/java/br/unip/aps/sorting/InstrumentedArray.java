package br.unip.aps.sorting;

import java.util.Comparator;
import java.util.function.ToLongFunction;

/** Array que conta leituras, escritas, trocas e comparacoes feitas pelos algoritmos. */
public final class InstrumentedArray<T> {
    private final T[] dados;
    private final Comparator<? super T> comparador;
    private final ToLongFunction<? super T> chaveNumerica;
    private final OperationCounter contador;
    private final Ouvinte ouvinte;

    /** Observador opcional das operacoes (padrao Observer). */
    public interface Ouvinte {
        default void comparacao(int i, int j) { }

        default void troca(int i, int j) { }

        default void atribuicao(int i, Object valor) { }
    }

    public InstrumentedArray(T[] dados, Comparator<? super T> comparador,
                             ToLongFunction<? super T> chaveNumerica, OperationCounter contador) {
        this.dados = dados;
        this.comparador = comparador;
        this.chaveNumerica = chaveNumerica;
        this.contador = contador;
        this.ouvinte = null;
    }

    /** Cria o array com um ouvinte de operacoes. */
    public InstrumentedArray(T[] dados, Comparator<? super T> comparador,
                             ToLongFunction<? super T> chaveNumerica, OperationCounter contador, Ouvinte ouvinte) {
        this.dados = dados;
        this.comparador = comparador;
        this.chaveNumerica = chaveNumerica;
        this.contador = contador;
        this.ouvinte = ouvinte;
    }

    public int length() {
        return dados.length;
    }

    public T get(int i) {
        contador.leitura();
        return dados[i];
    }

    public void set(int i, T v) {
        contador.atribuicao();
        dados[i] = v;
        if (ouvinte != null) ouvinte.atribuicao(i, v);
    }

    /** Troca os elementos das posicoes i e j (1 troca, 2 leituras, 2 escritas). */
    public void swap(int i, int j) {
        contador.troca();
        T t = dados[i];
        dados[i] = dados[j];
        dados[j] = t;
        if (ouvinte != null) ouvinte.troca(i, j);
    }

    /** Compara as posicoes i e j (2 leituras, 1 comparacao). */
    public int compare(int i, int j) {
        contador.leituras(2);
        if (ouvinte != null) ouvinte.comparacao(i, j);
        return contador.comparar(comparador, dados[i], dados[j]);
    }

    public boolean less(int i, int j) {
        return compare(i, j) < 0;
    }

    /** Compara dois valores ja lidos (1 comparacao, nenhuma leitura). */
    public int compareValues(T x, T y) {
        return contador.comparar(comparador, x, y);
    }

    public boolean lessValues(T x, T y) {
        return compareValues(x, y) < 0;
    }

    /** Cria um array auxiliar do mesmo tipo, contabilizado no mesmo contador (Merge Sort, Radix). */
    @SuppressWarnings("unchecked")
    public InstrumentedArray<T> auxiliar(int n) {
        T[] aux = (T[]) java.lang.reflect.Array.newInstance(dados.getClass().getComponentType(), n);
        return new InstrumentedArray<>(aux, comparador, chaveNumerica, contador);
    }

    /** Chave inteira do elemento na posicao i, para algoritmos que nao comparam (Radix Sort). */
    public long key(int i) {
        if (chaveNumerica == null) {
            throw new UnsupportedOperationException(
                    "Este algoritmo exige um criterio com chave numerica (ex.: data, latitude, longitude, id).");
        }
        contador.leitura();
        return chaveNumerica.applyAsLong(dados[i]);
    }

    public boolean temChaveNumerica() {
        return chaveNumerica != null;
    }

    public OperationCounter contador() {
        return contador;
    }

    T[] bruto() {
        return dados;
    }
}
