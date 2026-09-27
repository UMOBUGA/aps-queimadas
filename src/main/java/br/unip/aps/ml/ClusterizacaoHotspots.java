package br.unip.aps.ml;

import br.unip.aps.model.FocoIncendio;
import br.unip.aps.sorting.Ordenacoes;
import smile.clustering.CentroidClustering;
import smile.clustering.Clustering;
import smile.clustering.DBSCAN;
import smile.clustering.KMeans;
import smile.math.MathEx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/** Clusterizacao geografica dos focos para identificar hotspots (aprendizado nao supervisionado). */
public final class ClusterizacaoHotspots {
    private static final double KM_POR_GRAU_LAT = 110.574;
    private static final double KM_POR_GRAU_LON_EQUADOR = 111.320;

    /** Um hotspot (grupo de focos). */
    public record Hotspot(int id, double latitude, double longitude, int focos, double raioKm,
                          String municipioPrincipal, String biomaPredominante, Map<Integer, Integer> focosPorAno) { }

    /** Resultado de uma clusterizacao. */
    public record Resultado(String metodo, List<Hotspot> hotspots, int[] rotulos, int ruido, List<FocoIncendio> focos) { }

    private final long semente;

    public ClusterizacaoHotspots(long semente) {
        this.semente = semente;
    }

    public Resultado kMeans(List<FocoIncendio> focos, int k) {
        validar(focos, k);
        double[][] pts = projetar(focos);
        MathEx.setSeed(semente);
        CentroidClustering<double[], double[]> km = KMeans.fit(pts, k, 100);
        int[] g = new int[focos.size()];
        for (int i = 0; i < g.length; i++) g[i] = km.group(i);
        return montar("K-Means (k=" + k + ")", focos, g, k);
    }

    public Resultado dbscan(List<FocoIncendio> focos, double epsKm, int minPts) {
        validar(focos, 1);
        if (epsKm <= 0 || minPts < 1) throw new IllegalArgumentException("eps deve ser > 0 e minPts >= 1.");
        double[][] pts = projetar(focos);
        DBSCAN<double[]> db = DBSCAN.fit(pts, minPts, epsKm);
        int[] g = new int[focos.size()];
        for (int i = 0; i < g.length; i++) {
            int grupo = db.group(i);
            g[i] = grupo == Clustering.OUTLIER || grupo < 0 || grupo >= db.k() ? -1 : grupo;
        }
        return montar(String.format(java.util.Locale.ROOT, "DBSCAN (eps=%.1f km, minPts=%d)", epsKm, minPts),
                focos, g, db.k());
    }

    /** Metodo do cotovelo: distorcao (WCSS medio por foco, km²) para k = 1..kMax. */
    public double[] cotovelo(List<FocoIncendio> focos, int kMax) {
        validar(focos, kMax);
        double[][] pts = projetar(focos);
        double[] wcss = new double[kMax];
        double mx = 0, my = 0;
        for (double[] p : pts) {
            mx += p[0];
            my += p[1];
        }
        mx /= pts.length;
        my /= pts.length;
        for (double[] p : pts) wcss[0] += (p[0] - mx) * (p[0] - mx) + (p[1] - my) * (p[1] - my);
        wcss[0] /= pts.length;
        for (int k = 2; k <= kMax; k++) {
            MathEx.setSeed(semente);
            wcss[k - 1] = KMeans.fit(pts, k, 100).distortion();
        }
        return wcss;
    }

    private static void validar(List<FocoIncendio> focos, int k) {
        if (focos == null || focos.isEmpty()) throw new IllegalArgumentException("Nenhum foco para agrupar (revise os filtros).");
        if (k < 1 || k > focos.size()) throw new IllegalArgumentException("k deve estar entre 1 e o numero de focos (" + focos.size() + ").");
    }

    static double[][] projetar(List<FocoIncendio> focos) {
        double latMedia = 0;
        for (FocoIncendio f : focos) latMedia += f.getLatitude();
        latMedia /= focos.size();
        double kmLon = KM_POR_GRAU_LON_EQUADOR * Math.cos(Math.toRadians(latMedia));
        double[][] p = new double[focos.size()][2];
        for (int i = 0; i < p.length; i++) {
            p[i][0] = focos.get(i).getLongitude() * kmLon;
            p[i][1] = focos.get(i).getLatitude() * KM_POR_GRAU_LAT;
        }
        return p;
    }

    private static Resultado montar(String metodo, List<FocoIncendio> focos, int[] g, int k) {
        double[] somaLat = new double[k], somaLon = new double[k];
        int[] n = new int[k];
        List<Map<String, Integer>> mun = new ArrayList<>(), bio = new ArrayList<>();
        List<Map<Integer, Integer>> anos = new ArrayList<>();
        for (int c = 0; c < k; c++) {
            mun.add(new HashMap<>());
            bio.add(new HashMap<>());
            anos.add(new HashMap<>());
        }
        int ruido = 0;
        for (int i = 0; i < g.length; i++) {
            int c = g[i];
            if (c < 0) {
                ruido++;
                continue;
            }
            FocoIncendio f = focos.get(i);
            somaLat[c] += f.getLatitude();
            somaLon[c] += f.getLongitude();
            n[c]++;
            mun.get(c).merge(f.getMunicipio(), 1, Integer::sum);
            bio.get(c).merge(f.getBioma(), 1, Integer::sum);
            anos.get(c).merge(f.getAno(), 1, Integer::sum);
        }
        double[] somaQuad = new double[k];
        for (int i = 0; i < g.length; i++) {
            int c = g[i];
            if (c < 0 || n[c] == 0) continue;
            FocoIncendio f = focos.get(i);
            somaQuad[c] += Math.pow(distanciaKm(f.getLatitude(), f.getLongitude(), somaLat[c] / n[c], somaLon[c] / n[c]), 2);
        }
        List<Hotspot> hs = new ArrayList<>();
        for (int c = 0; c < k; c++) {
            if (n[c] == 0) continue;
            hs.add(new Hotspot(c, somaLat[c] / n[c], somaLon[c] / n[c], n[c], Math.sqrt(somaQuad[c] / n[c]),
                    maisFrequente(mun.get(c)), maisFrequente(bio.get(c)), porAnoOrdenado(anos.get(c))));
        }
        Ordenacoes.ordenar(hs, (a, b) -> Integer.compare(b.focos(), a.focos()));
        int[] novoId = new int[k];
        List<Hotspot> renumerados = new ArrayList<>();
        for (int i = 0; i < hs.size(); i++) {
            Hotspot h = hs.get(i);
            novoId[h.id()] = i + 1;
            renumerados.add(new Hotspot(i + 1, h.latitude(), h.longitude(), h.focos(), h.raioKm(),
                    h.municipioPrincipal(), h.biomaPredominante(), h.focosPorAno()));
        }
        int[] rotulos = new int[g.length];
        for (int i = 0; i < g.length; i++) rotulos[i] = g[i] < 0 ? -1 : novoId[g[i]];
        return new Resultado(metodo, renumerados, rotulos, ruido, List.copyOf(focos));
    }

    private static Map<Integer, Integer> porAnoOrdenado(Map<Integer, Integer> m) {
        Map<Integer, Integer> r = new LinkedHashMap<>();
        for (Integer ano : Ordenacoes.ordenar(new ArrayList<>(m.keySet()), Integer::compare)) r.put(ano, m.get(ano));
        return r;
    }

    private static String maisFrequente(Map<String, Integer> m) {
        String melhor = null;
        int max = -1;
        for (Map.Entry<String, Integer> e : m.entrySet()) {
            if (e.getValue() > max || (e.getValue() == max && e.getKey().compareTo(melhor) < 0)) {
                melhor = e.getKey();
                max = e.getValue();
            }
        }
        return melhor;
    }

    /** Distancia de grande circulo (formula de haversine). */
    public static double distanciaKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0088;
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.min(1, Math.sqrt(a)));
    }
}
