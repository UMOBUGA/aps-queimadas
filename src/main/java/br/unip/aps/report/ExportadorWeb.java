package br.unip.aps.report;

import br.unip.aps.analysis.Contagem;
import br.unip.aps.config.Versao;
import br.unip.aps.geo.MalhaMunicipal;
import br.unip.aps.geo.PerfilMunicipio;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.sorting.AlgoritmoTipo;
import br.unip.aps.sorting.Ordenacoes;
import br.unip.aps.sorting.ResultadoOrdenacao;
import br.unip.aps.util.Formatos;
import br.unip.aps.util.Json;
import br.unip.aps.util.Textos;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Gera os dados da versao web (celular): focos, fichas dos municipios, totais e custo real dos algoritmos, tudo calculado em Java. */
public final class ExportadorWeb {
    private static final int MAX_VIZINHOS = 8;
    private static final String[] COPIAS = {
        "/br/unip/aps/ui/web/leaflet.js", "vendor/leaflet.js",
        "/br/unip/aps/ui/web/leaflet.css", "vendor/leaflet.css",
        "/br/unip/aps/ui/web/mundo.js", "vendor/mundo.js",
        "/geo/sp-municipios.geojson", "dados/malha.geojson"
    };

    /** Arquivos gravados e totais, para o resumo no terminal. */
    public record Resultado(Path pasta, int focos, int municipios, int algoritmos, long bytes) { }

    private ExportadorWeb() { }

    /** Grava em {@code destino} as pastas dados/ e vendor/ que o app web le. */
    public static Resultado gerar(BaseDeFocos base, List<List<ResultadoOrdenacao<FocoIncendio>>> comparativos, Path destino) throws IOException {
        Path dados = Files.createDirectories(destino.resolve("dados"));
        Files.createDirectories(destino.resolve("vendor"));
        List<FocoIncendio> focos = base.getFocos();
        List<PerfilMunicipio> perfis = PerfilMunicipio.todos(focos);
        long bytes = 0;
        bytes += gravar(dados.resolve("resumo.json"), resumo(base, perfis));
        bytes += gravar(dados.resolve("focos.json"), focos(base, perfis));
        bytes += gravar(dados.resolve("municipios.json"), municipios(perfis));
        bytes += gravar(dados.resolve("algoritmos.json"), algoritmos(comparativos));
        for (int i = 0; i < COPIAS.length; i += 2) {
            Path alvo = destino.resolve(COPIAS[i + 1]);
            try (InputStream in = ExportadorWeb.class.getResourceAsStream(COPIAS[i])) {
                if (in == null) throw new IOException("Recurso ausente no JAR: " + COPIAS[i]);
                byte[] b = in.readAllBytes();
                Files.write(alvo, b);
                bytes += b.length;
            }
        }
        List<String> nomes = new ArrayList<>();
        for (List<ResultadoOrdenacao<FocoIncendio>> c : comparativos) {
            for (ResultadoOrdenacao<FocoIncendio> r : c) if (!nomes.contains(r.algoritmo())) nomes.add(r.algoritmo());
        }
        return new Resultado(destino, focos.size(), perfis.size(), nomes.size(), bytes);
    }

    private static long gravar(Path arquivo, String json) throws IOException {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        Files.write(arquivo, b);
        return b.length;
    }

    static String resumo(BaseDeFocos base, List<PerfilMunicipio> perfis) {
        List<FocoIncendio> focos = base.getFocos();
        Map<Integer, Long> anos = new LinkedHashMap<>();
        for (int a : base.anos()) anos.put(a, 0L);
        Map<String, Long> biomas = new LinkedHashMap<>();
        for (String b : base.biomas()) biomas.put(b, 0L);
        long[] meses = new long[base.anos().isEmpty() ? 0 : base.anos().size() * 12];
        int anoMin = base.anos().isEmpty() ? 0 : base.anos().get(0);
        for (FocoIncendio f : focos) {
            anos.merge(f.getAno(), 1L, Long::sum);
            biomas.merge(f.getBioma(), 1L, Long::sum);
            int i = (f.getAno() - anoMin) * 12 + f.getMes() - 1;
            if (i >= 0 && i < meses.length) meses[i]++;
        }
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"versao\":").append(Json.texto(Versao.atual()));
        sb.append(",\"geradoEm\":").append(Json.texto(LocalDate.now().toString()));
        sb.append(",\"estado\":").append(Json.texto("São Paulo"));
        sb.append(",\"total\":").append(focos.size());
        sb.append(",\"municipiosComFocos\":").append(perfis.size());
        sb.append(",\"municipiosNaMalha\":").append(MalhaMunicipal.sp().municipios().size());
        sb.append(",\"anoInicial\":").append(anoMin);
        sb.append(",\"anos\":[");
        boolean primeiro = true;
        for (Map.Entry<Integer, Long> e : anos.entrySet()) {
            sb.append(primeiro ? "" : ",").append("{\"ano\":").append(e.getKey()).append(",\"total\":").append(e.getValue()).append('}');
            primeiro = false;
        }
        sb.append("],\"meses\":").append(lista(meses));
        sb.append(",\"biomas\":[");
        primeiro = true;
        for (Map.Entry<String, Long> e : biomas.entrySet()) {
            sb.append(primeiro ? "" : ",").append("{\"nome\":").append(Json.texto(e.getKey())).append(",\"total\":").append(e.getValue()).append('}');
            primeiro = false;
        }
        sb.append("],\"fontes\":").append(Json.texto("Focos: INPE, BDQueimadas (satélite de referência). Malha e áreas: IBGE. Países: Natural Earth."));
        return sb.append('}').toString();
    }

    static String focos(BaseDeFocos base, List<PerfilMunicipio> perfis) {
        List<String> biomas = base.biomas();
        Map<String, Integer> indice = new HashMap<>();
        for (int i = 0; i < perfis.size(); i++) indice.put(MalhaMunicipal.chave(perfis.get(i).nomeInpe()), i);
        StringBuilder sb = new StringBuilder(base.tamanho() * 60 + 200);
        sb.append("{\"biomas\":[");
        for (int i = 0; i < biomas.size(); i++) sb.append(i > 0 ? "," : "").append(Json.texto(biomas.get(i)));
        sb.append("],\"campos\":[\"lat\",\"lon\",\"bioma\",\"municipio\",\"anoMes\",\"dataHoraGmt\"],\"focos\":[");
        boolean primeiro = true;
        for (FocoIncendio f : base.getFocos()) {
            sb.append(primeiro ? "" : ",").append('[')
                    .append(Json.numero(f.getLatitude(), 4)).append(',').append(Json.numero(f.getLongitude(), 4)).append(',')
                    .append(Math.max(0, biomas.indexOf(f.getBioma()))).append(',')
                    .append(indice.getOrDefault(MalhaMunicipal.chave(f.getMunicipio()), -1)).append(',')
                    .append(f.getAno() * 100 + f.getMes()).append(',')
                    .append(Json.texto(f.getDataHora().format(Formatos.DATA_HORA))).append(']');
            primeiro = false;
        }
        return sb.append("]}").toString();
    }

    static String municipios(List<PerfilMunicipio> perfis) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < perfis.size(); i++) {
            PerfilMunicipio p = perfis.get(i);
            MalhaMunicipal.Municipio m = MalhaMunicipal.sp().buscar(p.nomeInpe());
            sb.append(i > 0 ? ",\n" : "").append('{');
            sb.append("\"nome\":").append(Json.texto(p.nome()));
            sb.append(",\"chave\":").append(Json.texto(m == null ? MalhaMunicipal.chave(p.nomeInpe()) : m.chave()));
            sb.append(",\"busca\":").append(Json.texto(Textos.semAcentos(p.nome()).toLowerCase(java.util.Locale.ROOT)));
            sb.append(",\"total\":").append(p.total());
            sb.append(",\"posicao\":").append(p.posicao());
            sb.append(",\"anos\":[");
            for (int k = 0; k < p.porAno().size(); k++) sb.append(k > 0 ? "," : "").append(p.porAno().get(k).total());
            sb.append("],\"meses\":[");
            for (int k = 0; k < p.porMes().size(); k++) sb.append(k > 0 ? "," : "").append(p.porMes().get(k).focos());
            sb.append("],\"bioma\":").append(Json.texto(p.biomaPrincipal()));
            sb.append(",\"areaKm2\":").append(Json.numero(p.areaKm2(), 1));
            sb.append(",\"densidade\":").append(Json.numero(p.densidade(), 2));
            sb.append(",\"lat\":").append(Json.numero(p.latitude(), 4));
            sb.append(",\"lon\":").append(Json.numero(p.longitude(), 4));
            sb.append(",\"nVizinhos\":").append(p.vizinhos());
            sb.append(",\"vizinhos\":[");
            for (int k = 0; k < Math.min(MAX_VIZINHOS, p.vizinhosComFocos().size()); k++) {
                Contagem c = p.vizinhosComFocos().get(k);
                sb.append(k > 0 ? "," : "").append('[').append(Json.texto(c.chave())).append(',').append(c.total()).append(']');
            }
            sb.append("],\"vizinhosComFocos\":").append(p.vizinhosComFocos().size()).append('}');
        }
        return sb.append(']').toString();
    }

    static String algoritmos(List<List<ResultadoOrdenacao<FocoIncendio>>> comparativos) {
        StringBuilder sb = new StringBuilder("{\"tabelas\":[");
        for (int i = 0; i < comparativos.size(); i++) sb.append(i > 0 ? ",\n" : "").append(tabela(comparativos.get(i)));
        sb.append("],\"curvas\":").append(curvas("Data/hora", "Aleatória"));
        return sb.append('}').toString();
    }

    private static String tabela(List<ResultadoOrdenacao<FocoIncendio>> comparativo) {
        Map<String, AlgoritmoTipo> tipos = new HashMap<>();
        for (AlgoritmoTipo t : AlgoritmoTipo.values()) tipos.put(t.nome(), t);
        StringBuilder sb = new StringBuilder("{");
        ResultadoOrdenacao<FocoIncendio> r0 = comparativo.isEmpty() ? null : comparativo.get(0);
        sb.append("\"criterio\":").append(Json.texto(r0 == null ? "" : r0.criterio()));
        sb.append(",\"cenario\":").append(Json.texto(r0 == null ? "" : r0.cenario().toString()));
        sb.append(",\"n\":").append(r0 == null ? 0 : r0.tamanho());
        sb.append(",\"resultados\":[");
        for (int i = 0; i < comparativo.size(); i++) {
            ResultadoOrdenacao<FocoIncendio> r = comparativo.get(i);
            AlgoritmoTipo t = tipos.get(r.algoritmo());
            sb.append(i > 0 ? ",\n" : "").append('{')
                    .append("\"algoritmo\":").append(Json.texto(r.algoritmo()))
                    .append(",\"complexidade\":").append(Json.texto(t == null ? "" : t.criar().complexidade().casoMedio()))
                    .append(",\"comparacoes\":").append(r.metricas().comparacoes())
                    .append(",\"trocas\":").append(r.metricas().trocas())
                    .append(",\"atribuicoes\":").append(r.metricas().atribuicoes())
                    .append(",\"acessos\":").append(r.metricas().acessos())
                    .append(",\"ms\":").append(Json.numero(r.metricas().nanos() / 1e6, 3))
                    .append(",\"n\":").append(r.tamanho())
                    .append(",\"verificado\":").append(r.verificado())
                    .append(",\"aviso\":").append(Json.texto(r.aviso() == null ? "" : r.aviso()))
                    .append('}');
        }
        sb.append("],\"ordens\":{");
        String[] chaves = {"ms", "comparacoes", "trocas"};
        for (int k = 0; k < chaves.length; k++) {
            final int campo = k;
            List<Integer> ordem = new ArrayList<>();
            for (int i = 0; i < comparativo.size(); i++) ordem.add(i);
            Ordenacoes.ordenar(ordem, java.util.Comparator.comparingLong(i -> valor(comparativo.get(i), campo)));
            sb.append(k > 0 ? "," : "").append(Json.texto(chaves[k])).append(':').append(ordem);
        }
        return sb.append("}}").toString();
    }

    private static long valor(ResultadoOrdenacao<FocoIncendio> r, int campo) {
        return campo == 0 ? r.metricas().nanos() : campo == 1 ? r.metricas().comparacoes() : r.metricas().trocas();
    }

    /** Comparacoes e tempo medio por tamanho de entrada, lidos do benchmark gravado junto com o programa. */
    static String curvas(String criterio, String cenario) {
        String csv;
        try (InputStream in = ExportadorWeb.class.getResourceAsStream("/resultados/benchmark.csv")) {
            if (in == null) return "null";
            csv = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("﻿", "");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Map<String, List<String>> series = new LinkedHashMap<>();
        String[] linhas = csv.split("\\R");
        for (int i = 1; i < linhas.length; i++) {
            String[] c = linhas[i].split(";");
            if (c.length < 13 || !c[1].equals(criterio) || !c[2].equals(cenario)) continue;
            series.computeIfAbsent(c[0], k -> new ArrayList<>())
                    .add("[" + c[3] + "," + c[9] + "," + c[5].replace(',', '.') + "]");
        }
        StringBuilder sb = new StringBuilder("{\"criterio\":").append(Json.texto(criterio))
                .append(",\"cenario\":").append(Json.texto(cenario))
                .append(",\"campos\":[\"n\",\"comparacoes\",\"mediaMs\"],\"series\":[");
        boolean primeiro = true;
        for (Map.Entry<String, List<String>> e : series.entrySet()) {
            sb.append(primeiro ? "" : ",").append("{\"algoritmo\":").append(Json.texto(e.getKey()))
                    .append(",\"pontos\":[").append(String.join(",", e.getValue())).append("]}");
            primeiro = false;
        }
        return sb.append("]}").toString();
    }

    private static String lista(long[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) sb.append(i > 0 ? "," : "").append(v[i]);
        return sb.append(']').toString();
    }
}
