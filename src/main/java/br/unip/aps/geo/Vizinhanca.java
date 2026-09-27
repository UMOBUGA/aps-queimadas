package br.unip.aps.geo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Municipios vizinhos (fronteira comum na malha do IBGE); sem fronteira detectada, usa os 5 centroides mais proximos. */
public final class Vizinhanca {
    private static final Pattern FEATURE = Pattern.compile("\"chave\":\"([^\"]+)\".*?\"coordinates\":(\\[.*?\\]\\]\\]?\\]?)\\}");
    private static final Pattern PONTO = Pattern.compile("\\[(-?\\d+(?:\\.\\d+)?),(-?\\d+(?:\\.\\d+)?)\\]");

    private final Map<String, Set<String>> vizinhos = new HashMap<>();
    private int porFronteira;
    private int porProximidade;

    private Vizinhanca() {
    }

    public static Vizinhanca da(MalhaMunicipal malha) {
        Vizinhanca v = new Vizinhanca();
        Map<String, Set<String>> porVertice = new HashMap<>();
        Matcher f = FEATURE.matcher(malha.geojson());
        while (f.find()) {
            String chave = f.group(1);
            v.vizinhos.putIfAbsent(chave, new HashSet<>());
            Matcher p = PONTO.matcher(f.group(2));
            while (p.find()) porVertice.computeIfAbsent(p.group(1) + "," + p.group(2), k -> new HashSet<>()).add(chave);
        }
        for (Set<String> compartilham : porVertice.values()) {
            if (compartilham.size() < 2) continue;
            for (String a : compartilham) for (String b : compartilham) if (!a.equals(b)) v.vizinhos.get(a).add(b);
        }
        for (MalhaMunicipal.Municipio m : malha.municipios()) {
            Set<String> s = v.vizinhos.computeIfAbsent(m.chave(), k -> new HashSet<>());
            if (!s.isEmpty()) {
                v.porFronteira++;
                continue;
            }
            v.porProximidade++;
            List<MalhaMunicipal.Municipio> outros = new ArrayList<>(malha.municipios());
            outros.remove(m);
            for (int k = 0; k < 5 && !outros.isEmpty(); k++) {
                MalhaMunicipal.Municipio melhor = null;
                double menor = Double.MAX_VALUE;
                for (MalhaMunicipal.Municipio o : outros) {
                    double d = distanciaKm(m, o);
                    if (d < menor) {
                        menor = d;
                        melhor = o;
                    }
                }
                s.add(melhor.chave());
                outros.remove(melhor);
            }
        }
        return v;
    }

    private static double distanciaKm(MalhaMunicipal.Municipio a, MalhaMunicipal.Municipio b) {
        double lat = Math.toRadians((a.latitude() + b.latitude()) / 2);
        double dx = (a.longitude() - b.longitude()) * 111.32 * Math.cos(lat);
        double dy = (a.latitude() - b.latitude()) * 110.57;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public Set<String> de(String chave) {
        return vizinhos.getOrDefault(chave, Set.of());
    }

    public int municipiosComFronteira() {
        return porFronteira;
    }

    public int municipiosPorProximidade() {
        return porProximidade;
    }

    public double mediaVizinhos() {
        long soma = 0;
        for (Set<String> s : vizinhos.values()) soma += s.size();
        return vizinhos.isEmpty() ? 0 : (double) soma / vizinhos.size();
    }
}
