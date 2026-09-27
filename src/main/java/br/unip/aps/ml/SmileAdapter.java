package br.unip.aps.ml;

import smile.data.DataFrame;
import smile.data.vector.DoubleVector;
import smile.data.vector.IntVector;
import smile.data.vector.ValueVector;

import java.util.List;
import java.util.Random;

/**
 * Converte as linhas da {@link BaseMensal} para o formato {@link DataFrame} da biblioteca Smile
 * (padrao <b>Adapter</b>): o restante do sistema nao depende da API do Smile.
 */
final class SmileAdapter {

    /** Nome da coluna-alvo nos DataFrames. */
    static final String ALVO = "y";

    private SmileAdapter() { }

    /**
     * @param linhas observacoes
     * @param alvo   valores-alvo inteiros (focos ou classe), um por linha
     * @return DataFrame com as variaveis de {@link BaseMensal#VARIAVEIS} + coluna {@value #ALVO} (int)
     */
    static DataFrame paraDataFrameInteiro(List<BaseMensal.Linha> linhas, int[] alvo) {
        ValueVector[] colunas = colunasVariaveis(linhas, 1);
        colunas[colunas.length - 1] = new IntVector(ALVO, alvo);
        return new DataFrame(colunas);
    }

    /**
     * @param linhas observacoes
     * @return DataFrame com as variaveis + alvo continuo (quantidade de focos, double)
     */
    static DataFrame paraDataFrameReal(List<BaseMensal.Linha> linhas) {
        ValueVector[] colunas = colunasVariaveis(linhas, 1);
        double[] y = new double[linhas.size()];
        for (int i = 0; i < y.length; i++) y[i] = linhas.get(i).focos();
        colunas[colunas.length - 1] = new DoubleVector(ALVO, y);
        return new DataFrame(colunas);
    }

    private static ValueVector[] colunasVariaveis(List<BaseMensal.Linha> linhas, int extras) {
        int p = BaseMensal.VARIAVEIS.length;
        ValueVector[] colunas = new ValueVector[p + extras];
        for (int j = 0; j < p; j++) {
            double[] v = new double[linhas.size()];
            for (int i = 0; i < v.length; i++) v[i] = linhas.get(i).x()[j];
            colunas[j] = new DoubleVector(BaseMensal.VARIAVEIS[j], v);
        }
        return colunas;
    }

    /**
     * @param n       quantidade
     * @param semente semente base
     * @return sementes deterministicas (uma por arvore) para resultados reprodutiveis
     */
    static long[] sementes(int n, long semente) {
        Random r = new Random(semente);
        long[] s = new long[n];
        for (int i = 0; i < n; i++) s[i] = r.nextLong();
        return s;
    }
}
