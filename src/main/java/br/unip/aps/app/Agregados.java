package br.unip.aps.app;

import br.unip.aps.ApsException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Resume o historico de SP (2019-2024) e o Brasil por estado em CSVs pequenos que vao dentro do programa. */
public final class Agregados {
    /** Serie mensal de focos de SP (satelite de referencia). */
    public record Mes(int ano, int mes, long focos) { }

    /** Focos de um estado num ano (satelite de referencia). */
    public record Estado(String estado, int ano, long focos) { }

    public static final String RECURSO_HISTORICO = "/dados/historico-sp.csv";
    public static final String RECURSO_BRASIL = "/dados/brasil-estados.csv";

    private Agregados() { }

    /** Le os CSVs de data/historico e data/brasil e grava os resumos em src/main/resources/dados. */
    public static String gerar(Path historico, Path brasil, Path destino) throws ApsException {
        try {
            Map<String, Long> meses = new LinkedHashMap<>();
            for (int ano = 2019; ano <= 2024; ano++) {
                Path p = historico.resolve("focos_br_sp_ref_" + ano + ".csv");
                if (!Files.exists(p)) throw new ApsException("Falta " + p + ". Rode antes: historico --anos 2019-2024");
                contar(p, (estado, data) -> meses.merge(data.substring(0, 7), 1L, Long::sum));
            }
            Map<String, Long> estados = new LinkedHashMap<>();
            for (int ano = 2019; ano <= 2024; ano++) {
                Path p = brasil.resolve("focos_br_ref_" + ano + ".csv");
                if (!Files.exists(p)) throw new ApsException("Falta " + p + ". Rode antes: estruturas --brasil 2019-2024");
                int a = ano;
                contar(p, (estado, data) -> estados.merge(estado + ";" + a, 1L, Long::sum));
            }
            Files.createDirectories(destino);
            try (Writer w = Files.newBufferedWriter(destino.resolve("historico-sp.csv"), StandardCharsets.UTF_8)) {
                w.write("ano;mes;focos\n");
                for (Map.Entry<String, Long> e : meses.entrySet()) {
                    w.write(e.getKey().substring(0, 4) + ";" + Integer.parseInt(e.getKey().substring(5)) + ";" + e.getValue() + "\n");
                }
            }
            try (Writer w = Files.newBufferedWriter(destino.resolve("brasil-estados.csv"), StandardCharsets.UTF_8)) {
                w.write("estado;ano;focos\n");
                for (Map.Entry<String, Long> e : estados.entrySet()) w.write(e.getKey() + ";" + e.getValue() + "\n");
            }
            long sp2023 = estados.getOrDefault("SÃO PAULO;2023", 0L), sp2024 = estados.getOrDefault("SÃO PAULO;2024", 0L);
            return meses.size() + " meses de SP e " + estados.size() + " pares estado-ano gravados em " + destino
                    + ". Conferência: SP no arquivo do Brasil = " + sp2023 + " (2023) e " + sp2024 + " (2024).";
        } catch (IOException e) {
            throw new ApsException("Não foi possível gerar os agregados: " + e.getMessage(), e);
        }
    }

    private interface Contador {
        void contar(String estado, String dataPas);
    }

    private static void contar(Path csv, Contador c) throws IOException {
        Set<String> vistos = new HashSet<>();
        try (BufferedReader r = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            String cabecalho = r.readLine();
            if (cabecalho == null) throw new IOException("Arquivo vazio: " + csv);
            String[] cab = cabecalho.replace("﻿", "").split(",");
            int iId = indice(cab, "foco_id"), iData = indice(cab, "data_pas"), iEstado = indice(cab, "estado");
            String linha;
            while ((linha = r.readLine()) != null) {
                String[] v = linha.split(",", -1);
                if (v.length <= Math.max(iId, Math.max(iData, iEstado))) continue;
                if (!vistos.add(v[iId].strip())) continue;
                String data = v[iData].strip();
                if (data.length() < 7) continue;
                c.contar(v[iEstado].strip(), data);
            }
        }
    }

    private static int indice(String[] cab, String nome) throws IOException {
        for (int i = 0; i < cab.length; i++) if (cab[i].strip().equalsIgnoreCase(nome)) return i;
        throw new IOException("Coluna " + nome + " ausente");
    }

    /** Serie mensal de SP 2019-2024 embarcada no programa. */
    public static List<Mes> historicoSp() {
        List<Mes> r = new ArrayList<>();
        for (String[] c : ler(RECURSO_HISTORICO)) r.add(new Mes(Integer.parseInt(c[0]), Integer.parseInt(c[1]), Long.parseLong(c[2])));
        return r;
    }

    /** Focos por estado e ano 2019-2024 embarcados no programa. */
    public static List<Estado> brasil() {
        List<Estado> r = new ArrayList<>();
        for (String[] c : ler(RECURSO_BRASIL)) r.add(new Estado(c[0], Integer.parseInt(c[1]), Long.parseLong(c[2])));
        return r;
    }

    private static List<String[]> ler(String recurso) {
        List<String[]> r = new ArrayList<>();
        try (InputStream in = Agregados.class.getResourceAsStream(recurso)) {
            if (in == null) return r;
            try (BufferedReader b = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                b.readLine();
                String l;
                while ((l = b.readLine()) != null) if (!l.isBlank()) r.add(l.split(";"));
            }
        } catch (IOException e) {
            return r;
        }
        return r;
    }
}
