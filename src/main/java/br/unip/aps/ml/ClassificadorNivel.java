package br.unip.aps.ml;

import smile.base.cart.SplitRule;
import smile.classification.RandomForest;
import smile.data.DataFrame;
import smile.data.formula.Formula;

import java.util.List;

/** Classifica o nivel de atividade (baixo/medio/alto) com Random Forest. */
public final class ClassificadorNivel {
    /** Resultado da classificacao. */
    public record Resultado(int anoTreino, int anoTeste, Metricas.Classificacao metricas,
                            double baselineAcuracia, double baselineF1Macro, int[] distribuicaoTeste,
                            double[] importancia) { }

    private final int arvores;
    private final long semente;

    public ClassificadorNivel(int arvores, long semente) {
        this.arvores = arvores;
        this.semente = semente;
    }

    public Resultado executar(BaseMensal base, int anoTreino, int anoTeste) {
        List<BaseMensal.Linha> treino = base.linhasDoAno(anoTreino);
        List<BaseMensal.Linha> teste = base.linhasDoAno(anoTeste);
        int k = NivelAtividade.values().length;
        int[] yTreino = classes(treino);
        int[] yTeste = classes(teste);

        int[] contTreino = new int[k];
        for (int c : yTreino) contTreino[c]++;

        DataFrame dfTreino = SmileAdapter.paraDataFrameInteiro(treino, yTreino);
        DataFrame dfTeste = SmileAdapter.paraDataFrameInteiro(teste, yTeste);
        RandomForest rf = RandomForest.fit(Formula.lhs(SmileAdapter.ALVO), dfTreino,
                new RandomForest.Options(arvores, 0, SplitRule.GINI, 20, 0, 1, 1.0, pesos(contTreino),
                        SmileAdapter.sementes(arvores, semente), null));

        int[] prev = new int[yTeste.length];
        for (int i = 0; i < prev.length; i++) prev[i] = rf.predict(dfTeste.get(i));

        int majoritaria = 0;
        for (int c = 1; c < k; c++) if (contTreino[c] > contTreino[majoritaria]) majoritaria = c;
        int[] base0 = new int[yTeste.length];
        java.util.Arrays.fill(base0, majoritaria);
        Metricas.Classificacao baseline = Metricas.classificacao(yTeste, base0, k);

        int[] dist = new int[k];
        for (int c : yTeste) dist[c]++;
        return new Resultado(anoTreino, anoTeste, Metricas.classificacao(yTeste, prev, k),
                baseline.acuracia(), baseline.f1Macro(), dist, rf.importance());
    }

    static int[] pesos(int[] contagem) {
        int min = Integer.MAX_VALUE;
        for (int c : contagem) if (c > 0) min = Math.min(min, c);
        int[] w = new int[contagem.length];
        for (int i = 0; i < w.length; i++) {
            w[i] = contagem[i] == 0 || min == Integer.MAX_VALUE ? 1 : (int) Math.max(1, Math.round((double) contagem[i] / min));
        }
        return w;
    }

    private static int[] classes(List<BaseMensal.Linha> linhas) {
        int[] y = new int[linhas.size()];
        for (int i = 0; i < y.length; i++) y[i] = NivelAtividade.de(linhas.get(i).focos()).ordinal();
        return y;
    }
}
