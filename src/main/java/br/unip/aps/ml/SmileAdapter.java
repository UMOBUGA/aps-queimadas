package br.unip.aps.ml;

import smile.data.DataFrame;
import smile.data.vector.DoubleVector;
import smile.data.vector.IntVector;
import smile.data.vector.ValueVector;

import java.util.List;
import java.util.Random;

/** Converte as linhas da {@link BaseMensal} para o formato {@link DataFrame} da biblioteca Smile (padrao Adapter). */
final class SmileAdapter {
    static final String ALVO = "y";

    private SmileAdapter() { }

    static DataFrame paraDataFrameInteiro(List<BaseMensal.Linha> linhas, int[] alvo) {
        ValueVector[] colunas = colunasVariaveis(linhas, 1);
        colunas[colunas.length - 1] = new IntVector(ALVO, alvo);
        return new DataFrame(colunas);
    }

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

    static long[] sementes(int n, long semente) {
        Random r = new Random(semente);
        long[] s = new long[n];
        for (int i = 0; i < n; i++) s[i] = r.nextLong();
        return s;
    }
}
