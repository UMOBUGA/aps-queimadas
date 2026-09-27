package br.unip.aps.report;

import br.unip.aps.Focos;
import br.unip.aps.benchmark.BenchmarkConfig;
import br.unip.aps.benchmark.BenchmarkRunner;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.sorting.AlgoritmoTipo;
import br.unip.aps.sorting.CenarioEntrada;
import br.unip.aps.sorting.CriterioOrdenacao;
import br.unip.aps.sorting.Criterios;
import br.unip.aps.sorting.Ordem;
import br.unip.aps.sorting.ServicoOrdenacao;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Relatorios (CSV, Excel, PDF, codigo-fonte)")
class ReportExporterTest {

    @TempDir
    Path dir;

    private ContextoRelatorio contexto() throws Exception {
        List<FocoIncendio> focos = Focos.aleatorios(300, 2);
        BaseDeFocos base = new BaseDeFocos(focos, new br.unip.aps.io.RelatorioCarga(), List.of(Path.of("teste.csv")));
        var ord = new ServicoOrdenacao(20_000).ordenar(focos, ServicoOrdenacao.Solicitacao.de(AlgoritmoTipo.MERGE,
                Criterios.de(CriterioOrdenacao.MUNICIPIO, Ordem.CRESCENTE)));
        var bench = new BenchmarkRunner().executar(focos, new BenchmarkConfig(List.of(AlgoritmoTipo.QUICK),
                List.of(CriterioOrdenacao.DATA), List.of(50, 100, 200), List.of(CenarioEntrada.ALEATORIO), 0, 1, 1L,
                Integer.MAX_VALUE), null);
        return new ContextoRelatorio(base).ordenacao(ord).benchmark(bench)
                .grafico("teste", new java.awt.image.BufferedImage(200, 100, java.awt.image.BufferedImage.TYPE_INT_RGB));
    }

    @Test
    void csv() throws Exception {
        ContextoRelatorio ctx = contexto();
        Path p = new ReportExporter().exportarFocosCsv(ctx.ordenacao().dados(), dir.resolve("sub/dados.csv"));
        List<String> linhas = Files.readAllLines(p, StandardCharsets.UTF_8);
        assertEquals(301, linhas.size());
        assertTrue(linhas.get(0).startsWith("﻿posicao;id_bdq"));
        Path b = new ReportExporter().exportarBenchmarkCsv(ctx.benchmark(), dir.resolve("bench.csv"));
        assertEquals(4, Files.readAllLines(b).size());
    }

    @Test
    void excelEPdf() throws Exception {
        ContextoRelatorio ctx = contexto();
        Path x = new ReportExporter().exportarExcel(ctx, dir.resolve("r.xlsx"));
        try (Workbook wb = WorkbookFactory.create(x.toFile())) {
            assertNotNull(wb.getSheet("Resumo"));
            assertNotNull(wb.getSheet("Ordenacao"));
            assertNotNull(wb.getSheet("Benchmark"));
            assertNotNull(wb.getSheet("Complexidade empirica"));
            assertTrue(wb.getSheet("Ordenacao").getLastRowNum() > 300);
        }
        Path pdf = new ReportExporter().exportarPdf(ctx, dir.resolve("r.pdf"));
        byte[] bytes = Files.readAllBytes(pdf);
        assertTrue(bytes.length > 2000);
        assertEquals("%PDF", new String(bytes, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    @DisplayName("relatorio com as linhas de codigo inclui este proprio teste")
    void codigoFonte() throws Exception {
        Path raiz = Path.of("").toAbsolutePath();
        long linhas = new CodigoFonteReport().gerar(raiz, dir.resolve("c.pdf"), dir.resolve("c.txt"));
        assertTrue(linhas > 1000);
        String txt = Files.readString(dir.resolve("c.txt"));
        assertTrue(txt.contains("src/test/java/br/unip/aps/report/ReportExporterTest.java"));
        assertTrue(txt.contains("src/main/java/br/unip/aps/sorting/algorithms/QuickSort.java"));
    }
}
