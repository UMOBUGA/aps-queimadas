package br.unip.aps.analysis;

/** Funcoes de estatistica descritiva e regressao linear simples usadas no benchmark e no ML. */
public final class EstatisticaDescritiva {
    private EstatisticaDescritiva() { }

    public static double media(long[] v) {
        if (v.length == 0) return 0;
        double s = 0;
        for (long x : v) s += x;
        return s / v.length;
    }

    public static double media(double[] v) {
        if (v.length == 0) return 0;
        double s = 0;
        for (double x : v) s += x;
        return s / v.length;
    }

    /** Desvio-padrao amostral (divisor n-1). */
    public static double desvioPadrao(long[] v) {
        if (v.length < 2) return 0;
        double m = media(v);
        double s = 0;
        for (long x : v) s += (x - m) * (x - m);
        return Math.sqrt(s / (v.length - 1));
    }

    /** Desvio-padrao amostral (divisor n-1). */
    public static double desvioPadrao(double[] v) {
        if (v.length < 2) return 0;
        double m = media(v);
        double s = 0;
        for (double x : v) s += (x - m) * (x - m);
        return Math.sqrt(s / (v.length - 1));
    }

    /** Resultado de regressao linear y = a + b·x. */
    public record Regressao(double intercepto, double inclinacao, double r2) { }

    /** Minimos quadrados ordinarios para y = a + b·x. */
    public static Regressao regressaoLinear(double[] x, double[] y) {
        if (x.length != y.length || x.length < 2) {
            throw new IllegalArgumentException("Sao necessarios ao menos 2 pontos pareados.");
        }
        double mx = media(x), my = media(y);
        double sxy = 0, sxx = 0, syy = 0;
        for (int i = 0; i < x.length; i++) {
            sxy += (x[i] - mx) * (y[i] - my);
            sxx += (x[i] - mx) * (x[i] - mx);
            syy += (y[i] - my) * (y[i] - my);
        }
        double b = sxx == 0 ? 0 : sxy / sxx;
        double a = my - b * mx;
        double r2 = (sxx == 0 || syy == 0) ? 1 : (sxy * sxy) / (sxx * syy);
        return new Regressao(a, b, r2);
    }
}
