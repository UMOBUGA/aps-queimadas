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

/**
 * Clusterizacao geografica dos focos para identificar <b>hotspots</b> (aprendizado nao supervisionado).
 *
 * <ul>
 *   <li><b>K-Means</b> (Lloyd, 1982; inicializacao K-Means++): particiona os focos em k grupos
 *       minimizando a soma dos quadrados das distancias ao centroide. Exige escolher k — o
 *       "metodo do cotovelo" ({@link #cotovelo}) ajuda nessa escolha.</li>
 *   <li><b>DBSCAN</b> (Ester et al., 1996): agrupa pontos com pelo menos {@code minPts} vizinhos num
 *       raio {@code eps}; descobre sozinho o numero de grupos, aceita formatos irregulares e marca
 *       focos isolados como ruido — mais adequado a hotspots de queimada.</li>
 * </ul>
 * <p>Distancias em graus nao sao isotropicas (1° de longitude encolhe com a latitude), entao as
 * coordenadas sao projetadas para quilometros (projecao equirretangular local) antes de agrupar.</p>
 */
public final class ClusterizacaoHotspots {

    private static final double KM_POR_GRAU_LAT = 110.574;
    private static final double KM_POR_GRAU_LON_EQUADOR = 111.320;

    /**
     * Um hotspot (grupo de focos).
     *
     * @param id                  identificador (1 = maior grupo)
     * @param latitude            centroide
     * @param longitude           centroide
     * @param focos               quantidade de focos
     * @param raioKm              raio medio quadratico (km) dos focos ao centroide
     * @param municipioPrincipal  municipio com mais focos no grupo
     * @param biomaPredominante   bioma com mais focos no grupo
     * @param focosPorAno         focos do grupo por ano (ordem crescente)
     */
    public record Hotspot(int id, double latitude, double longitude, int focos, double raioKm,
                          String municipioPrincipal, String biomaPredominante, Map<Integer, Integer> focosPorAno) { }

    /**
     * Resultado de uma clusterizacao.
     *
     * @param metodo    descricao do metodo e parametros
     * @param hotspots  grupos, do maior para o menor
     * @param rotulos   grupo de cada foco (indice de {@code focos}); -1 = ruido
     * @param ruido     focos isolados (DBSCAN)
     * @param focos     focos agrupados (mesma ordem de {@code rotulos})
     */
    public record Resultado(String metodo, List<Hotspot> hotspots, int[] rotulos, int ruido, List<FocoIncendio> focos) { }

    private final long semente;

    /** @param semente semente do gerador do Smile (K-Means++ e reprodutivel) */
    public ClusterizacaoHotspots(long semente) {
        this.semente = semente;
    }

    /**
     * @param focos focos
     * @param k     numero de grupos
     * @return hotspots
     */
    public Resultado kMeans(List<FocoIncendio> focos, int k) {
        validar(focos, k);
        double[][] pts = projetar(focos);
        MathEx.setSeed(semente);
        CentroidClustering<double[], double[]> km = KMeans.fit(pts, k, 100);
        int[] g = new int[focos.size()];
        for (int i = 0; i < g.length; i++) g[i] = km.group(i);
        return montar("K-Means (k=" + k + ")", focos, g, k);
    }

    /**
     * @param focos  focos
     * @param epsKm  raio de vizinhanca em km
     * @param minPts minimo de vizinhos para um ponto central
     * @return hotspots e ruido
     */
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

    /**
     * Metodo do cotovelo: distorcao (WCSS medio por foco, km²) para k = 1..kMax.
     *
     * @param focos focos
     * @param kMax  maior k testado
     * @return vetor com WCSS na posicao k-1
     */
    public double[] cotovelo(List<FocoIncendio> focos, int kMax) {
        validar(focos, kMax);
        double[][] pts = projetar(focos);
        double[] wcss = new double[kMax];
        // k = 1: um unico grupo cujo centroide e a media de todos os pontos (o Smile exige k >= 2)
        double mx = 0, my = 0;
        for (double[] p : pts) {
            mx += p[0];
            my += p[1];
        }
        mx /= pts.length;
        my /= pts.length;
        for (double[] p : pts) wcss[0] += (p[0] - mx) * (p[0] - mx) + (p[1] - my) * (p[1] - my);
        wcss[0] /= pts.length; // mesma escala do Smile: distorcao media por ponto
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

    /** Projecao equirretangular local em km, centrada na latitude media. */
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
        // renumera: 1 = maior hotspot; rotulos acompanham a nova numeracao
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

    /** Copia o mapa ano -> focos com as chaves em ordem crescente (ordenadas pelo Merge Sort do projeto). */
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

    /**
     * Distancia de grande circulo (formula de haversine).
     *
     * @param lat1 latitude 1 (graus)
     * @param lon1 longitude 1
     * @param lat2 latitude 2
     * @param lon2 longitude 2
     * @return distancia em km
     */
    public static double distanciaKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0088;
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.min(1, Math.sqrt(a)));
    }
}
