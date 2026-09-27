package br.unip.aps.estruturas;

import br.unip.aps.io.CsvParser;
import br.unip.aps.sorting.OperationCounter;
import br.unip.aps.sorting.OperationMetrics;
import br.unip.aps.sorting.algorithms.MergeSort;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Ordenacao externa de CSVs de focos por data: runs ordenados em disco com memoria limitada + intercalacao k-way com heap. */
public final class ExternalMergeSort {
    private static final String[] COLUNAS_DATA = {"data_pas", "data_hora_gmt", "datahora", "data_hora"};

    /** Medicoes de uma execucao. */
    public record Resultado(long registros, int runs, int registrosPorRun, OperationMetrics fase1, OperationMetrics fase2,
                            long bytesEntrada, long nanosFase1, long nanosFase2, long memoriaPicoBytes, int arquivos,
                            Path saida, boolean ordenado) {
        public long comparacoes() {
            return fase1.comparacoes() + fase2.comparacoes();
        }

        public long nanosTotal() {
            return nanosFase1 + nanosFase2;
        }
    }

    private record Registro(String chave, String linha) { }

    private record Cabeca(Registro registro, int run) { }

    private static final Comparator<Registro> POR_CHAVE = (a, b) -> {
        int c = a.chave.compareTo(b.chave);
        return c != 0 ? c : a.linha.compareTo(b.linha);
    };

    /**
     * Ordena as linhas dos CSVs pela data/hora, mantendo no maximo {@code registrosPorRun} registros em memoria.
     * O arquivo de saida recebe o cabecalho do primeiro CSV.
     */
    public Resultado ordenar(List<Path> csvs, Path saida, int registrosPorRun, Consumer<String> progresso) throws IOException {
        if (csvs.isEmpty()) throw new IllegalArgumentException("Nenhum CSV informado para a ordenacao externa.");
        if (registrosPorRun < 2) throw new IllegalArgumentException("A memoria precisa comportar ao menos 2 registros.");
        Path temp = Files.createTempDirectory("aps-runs");
        List<Path> runs = new ArrayList<>();
        OperationCounter c1 = new OperationCounter();
        long registros = 0, bytes = 0;
        long[] pico = {0};
        String cabecalho = null;
        long t0 = System.nanoTime();
        Registro[] buffer = new Registro[registrosPorRun];
        int ocupados = 0;
        for (Path csv : csvs) {
            bytes += Files.size(csv);
            try (BufferedReader in = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
                String cab = in.readLine();
                if (cab == null) continue;
                if (!cab.isEmpty() && cab.charAt(0) == '﻿') cab = cab.substring(1);
                if (cabecalho == null) cabecalho = cab;
                char sep = CsvParser.detectarSeparador(cab);
                CsvParser parser = new CsvParser(sep);
                int col = colunaData(parser.dividir(cab), csv);
                String linha;
                while ((linha = in.readLine()) != null) {
                    if (linha.isBlank()) continue;
                    List<String> campos = parser.dividir(linha);
                    String chave = col < campos.size() ? campos.get(col) : "";
                    buffer[ocupados++] = new Registro(chave, linha);
                    registros++;
                    if (ocupados == registrosPorRun) {
                        runs.add(gravarRun(buffer, ocupados, temp, runs.size(), c1, pico));
                        ocupados = 0;
                        if (progresso != null) progresso.accept("Run " + runs.size() + " gravado (" + registros + " registros lidos)");
                    }
                }
            }
        }
        if (ocupados > 0) runs.add(gravarRun(buffer, ocupados, temp, runs.size(), c1, pico));
        buffer = null;
        long t1 = System.nanoTime();

        OperationCounter c2 = new OperationCounter();
        intercalar(runs, saida, cabecalho, c2, pico, progresso);
        long t2 = System.nanoTime();
        boolean ok = verificar(saida);
        for (Path r : runs) Files.deleteIfExists(r);
        Files.deleteIfExists(temp);
        return new Resultado(registros, runs.size(), registrosPorRun, c1.snapshot(), c2.snapshot(), bytes, t1 - t0, t2 - t1,
                pico[0], csvs.size(), saida, ok);
    }

    private static int colunaData(List<String> cabecalho, Path csv) {
        for (String nome : COLUNAS_DATA) {
            for (int i = 0; i < cabecalho.size(); i++) {
                if (cabecalho.get(i).strip().toLowerCase(Locale.ROOT).equals(nome)) return i;
            }
        }
        throw new IllegalArgumentException("O arquivo " + csv.getFileName() + " nao tem coluna de data/hora (" + String.join(", ", COLUNAS_DATA) + ")");
    }

    private Path gravarRun(Registro[] buffer, int n, Path temp, int indice, OperationCounter contador, long[] pico) throws IOException {
        Registro[] parte = new Registro[n];
        System.arraycopy(buffer, 0, parte, 0, n);
        contador.somar(new MergeSort().ordenar(parte, POR_CHAVE));
        amostrarMemoria(pico);
        Path run = temp.resolve("run-" + indice + ".txt");
        try (BufferedWriter out = Files.newBufferedWriter(run, StandardCharsets.UTF_8)) {
            for (Registro r : parte) {
                out.write(r.chave);
                out.write('\t');
                out.write(r.linha);
                out.newLine();
            }
        }
        return run;
    }

    private void intercalar(List<Path> runs, Path saida, String cabecalho, OperationCounter contador, long[] pico,
                            Consumer<String> progresso) throws IOException {
        BufferedReader[] leitores = new BufferedReader[runs.size()];
        HeapBinario<Cabeca> heap = new HeapBinario<>(runs.size() + 1,
                (a, b) -> POR_CHAVE.compare(a.registro, b.registro), contador);
        try (BufferedWriter out = Files.newBufferedWriter(saida, StandardCharsets.UTF_8)) {
            if (cabecalho != null) {
                out.write(cabecalho);
                out.newLine();
            }
            for (int i = 0; i < runs.size(); i++) {
                leitores[i] = Files.newBufferedReader(runs.get(i), StandardCharsets.UTF_8);
                Registro r = ler(leitores[i]);
                if (r != null) heap.inserir(new Cabeca(r, i));
            }
            long escritos = 0;
            while (!heap.vazio()) {
                Cabeca menor = heap.topo();
                out.write(menor.registro.linha);
                out.newLine();
                escritos++;
                Registro proximo = ler(leitores[menor.run]);
                if (proximo != null) heap.substituirTopo(new Cabeca(proximo, menor.run));
                else heap.removerTopo();
                if ((escritos & 0x1FFFF) == 0) {
                    amostrarMemoria(pico);
                    if (progresso != null) progresso.accept("Intercalando: " + escritos + " registros gravados");
                }
            }
        } finally {
            for (BufferedReader r : leitores) {
                if (r != null) r.close();
            }
        }
    }

    private static Registro ler(BufferedReader in) {
        try {
            String s = in.readLine();
            if (s == null) return null;
            int tab = s.indexOf('\t');
            return new Registro(s.substring(0, tab), s.substring(tab + 1));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean verificar(Path saida) throws IOException {
        try (BufferedReader in = Files.newBufferedReader(saida, StandardCharsets.UTF_8)) {
            String cab = in.readLine();
            if (cab == null) return true;
            CsvParser parser = new CsvParser(CsvParser.detectarSeparador(cab));
            int col = colunaData(parser.dividir(cab), saida);
            String anterior = null, linha;
            while ((linha = in.readLine()) != null) {
                List<String> campos = parser.dividir(linha);
                String chave = col < campos.size() ? campos.get(col) : "";
                if (anterior != null && anterior.compareTo(chave) > 0) return false;
                anterior = chave;
            }
        }
        return true;
    }

    private static void amostrarMemoria(long[] pico) {
        Runtime rt = Runtime.getRuntime();
        pico[0] = Math.max(pico[0], rt.totalMemory() - rt.freeMemory());
    }
}
