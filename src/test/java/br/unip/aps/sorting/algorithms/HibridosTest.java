package br.unip.aps.sorting.algorithms;

import br.unip.aps.sorting.OperationMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Algoritmos hibridos terminam com um Insertion Sort que "conserta" erros das fases anteriores; por isso o resultado
 * ordenado nao basta: estes testes conferem tambem o custo, que explode se uma fase estiver errada.
 */
@DisplayName("Intro Sort e Quick Sort com dois pivos: resultado e custo")
class HibridosTest {
    private static final Comparator<Integer> NATURAL = Comparator.naturalOrder();
    private static final int N = 10_000;
    private static final double LOG_N = Math.log(N) / Math.log(2);

    private static Integer[] aleatorio(long semente) {
        return new Random(semente).ints(N, 0, 1_000_000).boxed().toArray(Integer[]::new);
    }

    private static void confereOrdenado(Integer[] a, String contexto) {
        for (int i = 1; i < a.length; i++) assertTrue(a[i - 1] <= a[i], contexto + ": fora de ordem na posição " + i);
    }

    @Test
    @DisplayName("Intro Sort em dados aleatórios fica perto de n·log2(n) comparações")
    void introAleatorio() {
        Integer[] a = aleatorio(1);
        OperationMetrics m = new IntroSort().ordenar(a, NATURAL);
        confereOrdenado(a, "Intro");
        assertTrue(m.comparacoes() < 1.6 * N * LOG_N, "comparações demais: " + m.comparacoes());
        assertTrue(m.trocas() < 0.6 * N * LOG_N, "trocas demais: " + m.trocas());
    }

    @Test
    @DisplayName("com a profundidade esgotada o Intro Sort usa só o Heap Sort, ainda em O(n log n)")
    void introViraHeapSort() {
        for (long semente = 2; semente < 5; semente++) {
            Integer[] a = aleatorio(semente);
            OperationMetrics m = new IntroSort(0).ordenar(a, NATURAL);
            confereOrdenado(a, "Intro com profundidade 0");
            assertTrue(m.comparacoes() < 2.2 * N * LOG_N + N, "o Heap Sort interno deixou o vetor desordenado: " + m.comparacoes());
        }
    }

    @Test
    @DisplayName("Quick Sort com dois pivôs em dados aleatórios fica perto de n·log2(n) comparações")
    void doisPivosAleatorio() {
        Integer[] a = aleatorio(7);
        OperationMetrics m = new DualPivotQuickSort().ordenar(a, NATURAL);
        confereOrdenado(a, "Dois pivôs");
        assertTrue(m.comparacoes() < 1.6 * N * LOG_N, "comparações demais: " + m.comparacoes());
        assertTrue(m.trocas() < 0.8 * N * LOG_N, "trocas demais: " + m.trocas());
    }

    @Test
    @DisplayName("Quick Sort com dois pivôs com muitas repetições e com trechos pequenos")
    void doisPivosRepeticoes() {
        Integer[] a = new Random(9).ints(N, 0, 4).boxed().toArray(Integer[]::new);
        OperationMetrics m = new DualPivotQuickSort().ordenar(a, NATURAL);
        confereOrdenado(a, "Dois pivôs com 4 valores");
        assertTrue(m.comparacoes() < 3.0 * N * LOG_N, "comparações demais: " + m.comparacoes());
        for (int n = 2; n <= 40; n++) {
            Integer[] b = new Random(n).ints(n, 0, 50).boxed().toArray(Integer[]::new);
            new DualPivotQuickSort().ordenar(b, NATURAL);
            confereOrdenado(b, "Dois pivôs com n = " + n);
        }
    }
}
