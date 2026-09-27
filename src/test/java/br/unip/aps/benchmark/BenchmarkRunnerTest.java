package br.unip.aps.benchmark;

import br.unip.aps.Focos;
import br.unip.aps.sorting.AlgoritmoTipo;
import br.unip.aps.sorting.CenarioEntrada;
import br.unip.aps.sorting.CriterioOrdenacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Benchmark")
class BenchmarkRunnerTest {
    @Test
    @DisplayName("executa todos os casos, verifica os resultados e estima o expoente")
    void bateriaPequena() {
        BenchmarkConfig cfg = new BenchmarkConfig(
                List.of(AlgoritmoTipo.INSERTION, AlgoritmoTipo.MERGE, AlgoritmoTipo.RADIX),
                List.of(CriterioOrdenacao.DATA, CriterioOrdenacao.BIOMA),
                List.of(200, 400, 800, 1600), List.of(CenarioEntrada.ALEATORIO, CenarioEntrada.INVERSO), 1, 2, 1L,
                Integer.MAX_VALUE);
        List<BenchmarkRunner.Progresso> progresso = new ArrayList<>();
        List<BenchmarkResult> r = new BenchmarkRunner().executar(Focos.aleatorios(2_000, 3), cfg, progresso::add);
        assertEquals(24 + 16, r.size());
        assertTrue(r.stream().allMatch(BenchmarkResult::verificado));
        assertEquals(1.0, progresso.get(progresso.size() - 1).fracao());

        List<AnaliseComplexidade.Estimativa> est = AnaliseComplexidade.estimar(r);
        AnaliseComplexidade.Estimativa insInv = est.stream()
                .filter(e -> e.algoritmo().equals("Insertion Sort") && e.criterio() == CriterioOrdenacao.DATA
                        && e.cenario() == CenarioEntrada.INVERSO).findFirst().orElseThrow();
        assertEquals(2.0, insInv.expoenteComparacoes(), 0.05, "Insertion no pior caso e O(n²)");
        assertEquals("~O(n²)", insInv.classificacao());
        AnaliseComplexidade.Estimativa merge = est.stream()
                .filter(e -> e.algoritmo().equals("Merge Sort") && e.cenario() == CenarioEntrada.ALEATORIO
                        && e.criterio() == CriterioOrdenacao.DATA).findFirst().orElseThrow();
        assertTrue(merge.expoenteComparacoes() > 1.0 && merge.expoenteComparacoes() < 1.2, "n log n: " + merge.expoenteComparacoes());
        assertTrue(Double.isNaN(est.stream().filter(e -> e.algoritmo().startsWith("Radix")).findFirst().orElseThrow()
                .expoenteComparacoes()), "Radix nao compara");
    }

    @Test
    @DisplayName("aquecimento por tempo roda o algoritmo ate atingir o minimo antes de medir")
    void aquecimentoPorTempo() {
        BenchmarkConfig cfg = new BenchmarkConfig(List.of(AlgoritmoTipo.MERGE), List.of(CriterioOrdenacao.DATA), List.of(200),
                List.of(CenarioEntrada.ALEATORIO), 0, 1, 1L, Integer.MAX_VALUE, 150);
        long t0 = System.nanoTime();
        List<BenchmarkResult> r = new BenchmarkRunner().executar(Focos.aleatorios(500, 2), cfg, null);
        assertTrue((System.nanoTime() - t0) / 1e6 >= 150, "o aquecimento deve durar pelo menos 150 ms");
        assertEquals(1, r.size());
        assertTrue(r.get(0).verificado());
        assertEquals(BenchmarkConfig.AQUECIMENTO_MS_PADRAO, BenchmarkConfig.padrao().aquecimentoMs());
        assertThrows(IllegalArgumentException.class, () -> new BenchmarkConfig(List.of(AlgoritmoTipo.MERGE),
                List.of(CriterioOrdenacao.DATA), List.of(1), List.of(CenarioEntrada.ALEATORIO), 0, 1, 1L, 1, -1));
    }

    @Test
    @DisplayName("O(n²) acima do limite e pulado; configuracao invalida e rejeitada")
    void limitesEValidacao() {
        BenchmarkConfig cfg = new BenchmarkConfig(List.of(AlgoritmoTipo.BUBBLE, AlgoritmoTipo.HEAP),
                List.of(CriterioOrdenacao.MUNICIPIO), List.of(100, 1000), List.of(CenarioEntrada.ALEATORIO), 0, 1, 1L, 500);
        List<BenchmarkResult> r = new BenchmarkRunner().executar(Focos.aleatorios(1_000, 1), cfg, null);
        assertFalse(r.stream().anyMatch(x -> x.algoritmo().startsWith("Bubble") && x.n() == 1000));
        assertEquals(3, r.size());
        assertThrows(IllegalArgumentException.class, () -> new BenchmarkConfig(List.of(), List.of(CriterioOrdenacao.DATA),
                List.of(1), List.of(CenarioEntrada.ALEATORIO), 0, 1, 1L, 1));
        assertThrows(IllegalArgumentException.class, () -> new BenchmarkRunner().executar(List.of(), cfg, null));
    }

    @Test
    void classificacaoDeExpoentes() {
        assertEquals("~O(n)", AnaliseComplexidade.classificar(1.0));
        assertEquals("~O(n log n)", AnaliseComplexidade.classificar(1.12));
        assertEquals("~O(n²)", AnaliseComplexidade.classificar(1.97));
    }
}
