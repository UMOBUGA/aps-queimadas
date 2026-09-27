package br.unip.aps.geo;

import br.unip.aps.util.Textos;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Malha municipal de Sao Paulo (IBGE): geometria simplificada, area territorial e centroide de cada municipio. */
public final class MalhaMunicipal {
    /** Municipio do IBGE com area em km2 e centroide oficial. */
    public record Municipio(String codigo, String nome, String chave, double areaKm2, double latitude, double longitude) { }

    private static MalhaMunicipal instancia;

    private final List<Municipio> municipios;
    private final Map<String, Municipio> porChave = new HashMap<>();

    private MalhaMunicipal(List<Municipio> municipios) {
        this.municipios = Collections.unmodifiableList(municipios);
        for (Municipio m : municipios) porChave.put(m.chave(), m);
    }

    public static synchronized MalhaMunicipal sp() {
        if (instancia == null) instancia = new MalhaMunicipal(lerTabela());
        return instancia;
    }

    public List<Municipio> municipios() {
        return municipios;
    }

    /** Localiza o municipio pelo nome como vem no CSV do INPE (caixa e acentos sao ignorados). */
    public Municipio buscar(String nomeInpe) {
        return nomeInpe == null ? null : porChave.get(chave(nomeInpe));
    }

    /** GeoJSON dos limites municipais, para o mapa (camada offline e coropletico). */
    public String geojson() {
        return ler("/geo/sp-municipios.geojson");
    }

    public static String chave(String nome) {
        return Textos.semAcentos(nome).toUpperCase(Locale.ROOT).replace("'", "").replace('-', ' ').strip();
    }

    private static List<Municipio> lerTabela() {
        List<Municipio> r = new ArrayList<>();
        String[] linhas = ler("/geo/sp-municipios.csv").split("\\R");
        for (int i = 1; i < linhas.length; i++) {
            if (linhas[i].isBlank()) continue;
            String[] c = linhas[i].split(";");
            r.add(new Municipio(c[0], c[1], c[2], Double.parseDouble(c[3]), Double.parseDouble(c[4]), Double.parseDouble(c[5])));
        }
        return r;
    }

    private static String ler(String recurso) {
        try (InputStream in = MalhaMunicipal.class.getResourceAsStream(recurso)) {
            if (in == null) throw new IllegalStateException("Recurso ausente no JAR: " + recurso);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
