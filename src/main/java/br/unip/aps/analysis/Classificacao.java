package br.unip.aps.analysis;

import br.unip.aps.sorting.Ordenacoes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Classificacao de valores em classes para mapas coropleticos: quantis e quebras naturais de Jenks (Fisher, 1958). */
public final class Classificacao {
    /** Metodo de classificacao. */
    public enum Metodo { QUANTIS, JENKS }

    private Classificacao() {
    }

    /** Limites superiores de cada classe (o ultimo e o maximo); valores ja ordenados com o Merge Sort do projeto. */
    public static double[] limites(double[] valores, int classes, Metodo metodo) {
        if (valores.length == 0) return new double[0];
        List<Double> lista = new ArrayList<>(valores.length);
        for (double v : valores) lista.add(v);
        Ordenacoes.ordenar(lista, Comparator.naturalOrder());
        double[] ord = new double[lista.size()];
        for (int i = 0; i < ord.length; i++) ord[i] = lista.get(i);
        int k = Math.max(1, Math.min(classes, distintos(ord)));
        return metodo == Metodo.JENKS ? jenks(ord, k) : quantis(ord, k);
    }

    /** Indice da classe do valor (0 .. limites.length - 1). */
    public static int classe(double v, double[] limites) {
        for (int i = 0; i < limites.length; i++) if (v <= limites[i]) return i;
        return limites.length - 1;
    }

    private static int distintos(double[] ord) {
        int n = ord.length == 0 ? 0 : 1;
        for (int i = 1; i < ord.length; i++) if (ord[i] != ord[i - 1]) n++;
        return n;
    }

    private static double[] quantis(double[] ord, int k) {
        double[] r = new double[k];
        for (int c = 1; c <= k; c++) {
            int i = (int) Math.ceil(c * ord.length / (double) k) - 1;
            r[c - 1] = ord[Math.max(0, Math.min(ord.length - 1, i))];
        }
        for (int c = 1; c < k; c++) if (r[c] < r[c - 1]) r[c] = r[c - 1];
        return r;
    }

    /** Quebras naturais: minimiza a soma das variancias dentro das classes por programacao dinamica, O(k n^2). */
    private static double[] jenks(double[] ord, int k) {
        int n = ord.length;
        int[][] inferior = new int[n + 1][k + 1];
        double[][] custo = new double[n + 1][k + 1];
        for (int i = 1; i <= k; i++) {
            inferior[1][i] = 1;
            for (int j = 2; j <= n; j++) custo[j][i] = Double.POSITIVE_INFINITY;
        }
        for (int l = 2; l <= n; l++) {
            double s1 = 0, s2 = 0, w = 0, variancia = 0;
            for (int m = 1; m <= l; m++) {
                int i3 = l - m + 1;
                double v = ord[i3 - 1];
                s2 += v * v;
                s1 += v;
                w++;
                variancia = s2 - s1 * s1 / w;
                int i4 = i3 - 1;
                if (i4 != 0) {
                    for (int j = 2; j <= k; j++) {
                        if (custo[l][j] >= variancia + custo[i4][j - 1]) {
                            inferior[l][j] = i3;
                            custo[l][j] = variancia + custo[i4][j - 1];
                        }
                    }
                }
            }
            inferior[l][1] = 1;
            custo[l][1] = variancia;
        }
        double[] r = new double[k];
        r[k - 1] = ord[n - 1];
        int pos = n;
        for (int j = k; j >= 2; j--) {
            int id = inferior[pos][j] - 2;
            r[j - 2] = ord[Math.max(0, id)];
            pos = inferior[pos][j] - 1;
        }
        return r;
    }
}
