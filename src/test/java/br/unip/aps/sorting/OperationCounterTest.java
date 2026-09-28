package br.unip.aps.sorting;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Confere a contagem de operacoes contra os valores teoricos. */
@DisplayName("Contagem de operacoes")
class OperationCounterTest {
    private static final Comparator<Integer> NATURAL = Comparator.naturalOrder();
    private static final int N = 100;

    private static Integer[] seq(boolean inversa) {
        Integer[] a = new Integer[N];
        for (int i = 0; i < N; i++) a[i] = inversa ? N - i : i;
        return a;
    }

    @Test
    @DisplayName("Counting: zero comparacoes e exatamente 2n escritas, em qualquer ordem")
    void counting() {
        for (boolean inversa : new boolean[]{false, true}) {
            OperationMetrics m = AlgoritmoTipo.COUNTING.criar().ordenar(seq(inversa), NATURAL, Integer::longValue);
            assertEquals(0, m.comparacoes());
            assertEquals(0, m.trocas());
            assertEquals(2L * N, m.atribuicoes());
        }
    }

    @Test
    @DisplayName("Intro e Quick 2 pivos: n log n comparacoes mesmo com a entrada ja ordenada ou invertida")
    void introEDoisPivos() {
        int n = 4096;
        double limite = 3.0 * n * (Math.log(n) / Math.log(2));
        for (AlgoritmoTipo t : new AlgoritmoTipo[]{AlgoritmoTipo.INTRO, AlgoritmoTipo.QUICK_2PIVOS}) {
            for (boolean inversa : new boolean[]{false, true}) {
                Integer[] a = new Integer[n];
                for (int i = 0; i < n; i++) a[i] = inversa ? n - i : i;
                OperationMetrics m = t.criar().ordenar(a, NATURAL);
                assertTrue(m.comparacoes() < limite, t.nome() + " fez " + m.comparacoes() + " comparacoes");
            }
        }
    }

    @Test
    @DisplayName("Counting recusa intervalo de chaves grande demais com mensagem que indica o Radix")
    void countingFaixa() {
        Integer[] a = {0, 1 << 30};
        IllegalArgumentException e = org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> AlgoritmoTipo.COUNTING.criar().ordenar(a, NATURAL, Integer::longValue));
        assertTrue(e.getMessage().contains("Radix"));
        assertTrue(AlgoritmoTipo.COUNTING.aceita(java.util.List.of(3, 7, 23), Integer::longValue));
    }

    @Test
    @DisplayName("Bubble: melhor caso n-1 comparacoes e 0 trocas; pior caso n(n-1)/2 de cada")
    void bubble() {
        OperationMetrics melhor = AlgoritmoTipo.BUBBLE.criar().ordenar(seq(false), NATURAL);
        assertEquals(N - 1, melhor.comparacoes());
        assertEquals(0, melhor.trocas());
        OperationMetrics pior = AlgoritmoTipo.BUBBLE.criar().ordenar(seq(true), NATURAL);
        assertEquals((long) N * (N - 1) / 2, pior.comparacoes());
        assertEquals((long) N * (N - 1) / 2, pior.trocas());
        assertEquals(2 * pior.trocas(), pior.atribuicoes());
    }

    @Test
    @DisplayName("Selection: sempre n(n-1)/2 comparacoes; 0 trocas se ja ordenado")
    void selection() {
        OperationMetrics ord = AlgoritmoTipo.SELECTION.criar().ordenar(seq(false), NATURAL);
        OperationMetrics inv = AlgoritmoTipo.SELECTION.criar().ordenar(seq(true), NATURAL);
        assertEquals((long) N * (N - 1) / 2, ord.comparacoes());
        assertEquals((long) N * (N - 1) / 2, inv.comparacoes());
        assertEquals(0, ord.trocas());
        assertTrue(inv.trocas() <= N - 1);
    }

    @Test
    @DisplayName("Insertion: melhor caso n-1 comparacoes e 0 escritas; pior caso n(n-1)/2 comparacoes")
    void insertion() {
        OperationMetrics melhor = AlgoritmoTipo.INSERTION.criar().ordenar(seq(false), NATURAL);
        assertEquals(N - 1, melhor.comparacoes());
        assertEquals(0, melhor.atribuicoes());
        OperationMetrics pior = AlgoritmoTipo.INSERTION.criar().ordenar(seq(true), NATURAL);
        assertEquals((long) N * (N - 1) / 2, pior.comparacoes());
        assertEquals(0, pior.trocas(), "Insertion desloca, nao troca");
    }

    @Test
    @DisplayName("Merge: vetor ordenado custa n-1 comparacoes (merge pulado); aleatorio <= n*log2(n)")
    void merge() {
        OperationMetrics ord = AlgoritmoTipo.MERGE.criar().ordenar(seq(false), NATURAL);
        assertEquals(N - 1, ord.comparacoes());
        Integer[] a = new java.util.Random(1).ints(1024).boxed().toArray(Integer[]::new);
        OperationMetrics m = AlgoritmoTipo.MERGE.criar().ordenar(a, NATURAL);
        assertTrue(m.comparacoes() <= 1024 * 10, "comparacoes = " + m.comparacoes());
    }

    @Test
    @DisplayName("acessos = leituras + atribuicoes e troca = 2 leituras + 2 escritas")
    void acessos() {
        OperationCounter c = new OperationCounter();
        c.troca();
        c.leitura();
        c.atribuicao();
        assertEquals(1, c.getTrocas());
        assertEquals(3, c.getLeituras());
        assertEquals(3, c.getAtribuicoes());
        assertEquals(6, c.getAcessos());
        c.zerar();
        assertEquals(0, c.getAcessos());
    }

    @Test
    @DisplayName("contadores long nao estouram acima de Integer.MAX_VALUE")
    void semEstouro() {
        OperationCounter c = new OperationCounter();
        c.leituras(Integer.MAX_VALUE);
        c.leituras(Integer.MAX_VALUE);
        assertEquals(2L * Integer.MAX_VALUE, c.getLeituras());
    }
}
