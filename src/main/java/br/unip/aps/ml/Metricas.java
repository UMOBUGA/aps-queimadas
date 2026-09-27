package br.unip.aps.ml;

/** Metricas de avaliacao de modelos, implementadas explicitamente para ficarem transparentes na dissertacao. */
public final class Metricas {
    private Metricas() { }

    /** Metricas de regressao. */
    public record Regressao(double mae, double rmse, double r2) { }

    /** Metricas de classificacao multiclasse. */
    public record Classificacao(double acuracia, double f1Macro, double[] precisao, double[] revocacao,
                                double[] f1, int[][] matriz) { }

    public static Regressao regressao(double[] real, double[] previsto) {
        int n = real.length;
        if (n == 0 || n != previsto.length) throw new IllegalArgumentException("Vetores vazios ou de tamanhos diferentes.");
        double media = 0;
        for (double v : real) media += v;
        media /= n;
        double abs = 0, sq = 0, tot = 0;
        for (int i = 0; i < n; i++) {
            double e = real[i] - previsto[i];
            abs += Math.abs(e);
            sq += e * e;
            tot += (real[i] - media) * (real[i] - media);
        }
        return new Regressao(abs / n, Math.sqrt(sq / n), tot == 0 ? 0 : 1 - sq / tot);
    }

    public static Classificacao classificacao(int[] real, int[] previsto, int k) {
        int[][] m = new int[k][k];
        int acertos = 0;
        for (int i = 0; i < real.length; i++) {
            m[real[i]][previsto[i]]++;
            if (real[i] == previsto[i]) acertos++;
        }
        double[] p = new double[k], r = new double[k], f1 = new double[k];
        double somaF1 = 0;
        for (int c = 0; c < k; c++) {
            int vp = m[c][c], fp = 0, fn = 0;
            for (int o = 0; o < k; o++) {
                if (o != c) {
                    fp += m[o][c];
                    fn += m[c][o];
                }
            }
            p[c] = vp + fp == 0 ? 0 : (double) vp / (vp + fp);
            r[c] = vp + fn == 0 ? 0 : (double) vp / (vp + fn);
            f1[c] = p[c] + r[c] == 0 ? 0 : 2 * p[c] * r[c] / (p[c] + r[c]);
            somaF1 += f1[c];
        }
        return new Classificacao(real.length == 0 ? 0 : (double) acertos / real.length, somaF1 / k, p, r, f1, m);
    }
}
