package br.unip.aps.estruturas;

import br.unip.aps.io.RepositorioDados;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ordenacao externa com memoria limitada sobre os CSVs reais de SP. */
@DisplayName("External Merge Sort")
class ExternalMergeSortTest {

    @Test
    void ordenaComMemoriaMenorQueABase(@TempDir Path dir) throws Exception {
        List<Path> csvs = RepositorioDados.extrairEmbarcados();
        Path saida = dir.resolve("ordenado.csv");
        ExternalMergeSort.Resultado r = new ExternalMergeSort().ordenar(csvs, saida, 1_000, null);
        assertEquals(10_378, r.registros());
        assertEquals(11, r.runs(), "ceil(10378 / 1000) runs");
        assertTrue(r.ordenado());
        assertEquals(10_379, Files.readAllLines(saida, StandardCharsets.UTF_8).size(), "cabecalho + registros");
        assertTrue(r.fase2().comparacoes() > 0);
        assertTrue(r.fase1().comparacoes() > 0);
    }

    @Test
    void umUnicoRunQuandoTudoCabeNaMemoria(@TempDir Path dir) throws Exception {
        ExternalMergeSort.Resultado r = new ExternalMergeSort().ordenar(RepositorioDados.extrairEmbarcados(),
                dir.resolve("s.csv"), 50_000, null);
        assertEquals(1, r.runs());
        assertTrue(r.ordenado());
    }
}
