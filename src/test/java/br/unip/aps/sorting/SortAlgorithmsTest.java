package br.unip.aps.sorting;

import br.unip.aps.Focos;
import br.unip.aps.model.FocoIncendio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes parametrizados: TODOS os algoritmos sao executados sobre os mesmos casos e comparados
 * com uma ordenacao de referencia ({@link Collections#sort}, permitido apenas nos testes).
 */
@DisplayName("Algoritmos de ordenacao")
class SortAlgorithmsTest {

    private static final Comparator<Integer> NATURAL = Comparator.naturalOrder();

    static Stream<Arguments> casosInteiros() {
        Random r = new Random(7);
        List<Arguments> casos = new ArrayList<>();
        casos.add(Arguments.of("vazio", new Integer[0]));
        casos.add(Arguments.of("um elemento", new Integer[]{42}));
        casos.add(Arguments.of("dois invertidos", new Integer[]{2, 1}));
        casos.add(Arguments.of("todos iguais", new Integer[]{5, 5, 5, 5, 5, 5, 5}));
        casos.add(Arguments.of("ja ordenado", sequencia(200, false)));
        casos.add(Arguments.of("inverso", sequencia(200, true)));
        casos.add(Arguments.of("negativos e positivos", new Integer[]{0, -3, 7, -100, 42, -3, 0, Integer.MAX_VALUE, Integer.MIN_VALUE}));
        casos.add(Arguments.of("muitas repeticoes", r.ints(500, 0, 3).boxed().toArray(Integer[]::new)));
        casos.add(Arguments.of("aleatorio 1000", r.ints(1000, -10_000, 10_000).boxed().toArray(Integer[]::new)));
        casos.add(Arguments.of("aleatorio 3001 (impar, > RUN)", r.ints(3001).boxed().toArray(Integer[]::new)));
        return casos.stream();
    }

    static Stream<Arguments> algoritmosECasos() {
        List<Arguments> r = new ArrayList<>();
        for (AlgoritmoTipo t : AlgoritmoTipo.values()) {
            casosInteiros().forEach(c -> r.add(Arguments.of(t, c.get()[0], c.get()[1])));
        }
        return r.stream();
    }

    private static Integer[] sequencia(int n, boolean inversa) {
        Integer[] a = new Integer[n];
        for (int i = 0; i < n; i++) a[i] = inversa ? n - i : i;
        return a;
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("algoritmosECasos")
    @DisplayName("ordena igual a referencia")
    void ordenaIgualAReferencia(AlgoritmoTipo tipo, String caso, Integer[] entrada) {
        Integer[] esperado = entrada.clone();
        List<Integer> ref = new ArrayList<>(Arrays.asList(esperado));
        Collections.sort(ref);
        Integer[] obtido = entrada.clone();
        OperationMetrics m = tipo.criar().ordenar(obtido, NATURAL, Integer::longValue);
        assertArrayEquals(ref.toArray(new Integer[0]), obtido, tipo.nome() + " falhou em: " + caso);
        assertTrue(m.nanos() >= 0);
    }

    @ParameterizedTest
    @EnumSource(AlgoritmoTipo.class)
    @DisplayName("ordem decrescente via reversed() / chave invertida")
    void ordemDecrescente(AlgoritmoTipo tipo) {
        Integer[] a = new Random(3).ints(300, -50, 50).boxed().toArray(Integer[]::new);
        tipo.criar().ordenar(a, NATURAL.reversed(), x -> ~x.longValue());
        for (int i = 1; i < a.length; i++) assertTrue(a[i - 1] >= a[i], tipo.nome());
    }

    @ParameterizedTest
    @EnumSource(AlgoritmoTipo.class)
    @DisplayName("estabilidade conforme a ficha de complexidade")
    void estabilidade(AlgoritmoTipo tipo) {
        // registros (chave, posicao original): muitos empates de chave
        int n = 400;
        Random r = new Random(11);
        int[][] dados = new int[n][];
        for (int i = 0; i < n; i++) dados[i] = new int[]{r.nextInt(8), i};
        int[][] a = dados.clone();
        tipo.criar().ordenar(a, Comparator.comparingInt((int[] x) -> x[0]), x -> x[0]);
        boolean estavel = true;
        for (int i = 1; i < n; i++) {
            if (a[i - 1][0] == a[i][0] && a[i - 1][1] > a[i][1]) estavel = false;
        }
        if (tipo.complexidade().estavel()) {
            assertTrue(estavel, tipo.nome() + " declara ser estavel mas nao preservou a ordem dos empates");
        } else {
            // algoritmos instaveis: com esta entrada, todos inverteram algum empate
            assertTrue(!estavel, tipo.nome() + " declarado instavel preservou os empates (ajuste o teste ou a ficha)");
        }
    }

    @ParameterizedTest
    @EnumSource(value = AlgoritmoTipo.class, mode = EnumSource.Mode.EXCLUDE, names = "RADIX")
    @DisplayName("valores nulos com Comparator.nullsFirst")
    void valoresNulos(AlgoritmoTipo tipo) {
        Integer[] a = {3, null, 1, null, 2};
        tipo.criar().ordenar(a, Comparator.nullsFirst(NATURAL));
        assertArrayEquals(new Integer[]{null, null, 1, 2, 3}, a);
    }

    @ParameterizedTest
    @EnumSource(value = AlgoritmoTipo.class, mode = EnumSource.Mode.EXCLUDE, names = "RADIX")
    @DisplayName("multicriterio em focos: bioma -> municipio -> data desc")
    void multicriterioEmFocos(AlgoritmoTipo tipo) {
        List<FocoIncendio> base = Focos.aleatorios(1_500, 99);
        Criterios c = Criterios.composto(List.of(
                new Criterios.Nivel(CriterioOrdenacao.BIOMA, Ordem.CRESCENTE),
                new Criterios.Nivel(CriterioOrdenacao.MUNICIPIO, Ordem.CRESCENTE),
                new Criterios.Nivel(CriterioOrdenacao.DATA, Ordem.DECRESCENTE)));
        FocoIncendio[] a = base.toArray(new FocoIncendio[0]);
        tipo.criar().ordenar(a, c.comparador());
        List<FocoIncendio> ref = new ArrayList<>(base);
        ref.sort(c.comparador());
        for (int i = 0; i < a.length; i++) {
            assertEquals(0, c.comparador().compare(ref.get(i), a[i]), tipo.nome() + " divergiu na posicao " + i);
        }
    }

    @Test
    @DisplayName("Radix sem chave numerica gera erro amigavel")
    void radixSemChave() {
        Integer[] a = {3, 1, 2};
        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class,
                () -> AlgoritmoTipo.RADIX.criar().ordenar(a, NATURAL));
        assertTrue(e.getMessage().contains("chave numerica"));
    }

    @Test
    @DisplayName("Radix nao faz comparacoes")
    void radixSemComparacoes() {
        Integer[] a = new Random(1).ints(1000).boxed().toArray(Integer[]::new);
        OperationMetrics m = AlgoritmoTipo.RADIX.criar().ordenar(a, NATURAL, Integer::longValue);
        assertEquals(0, m.comparacoes());
    }

    @Test
    @DisplayName("Quick Sort nao estoura a pilha em 200 mil elementos ja ordenados")
    void quickSortEntradaOrdenadaGrande() {
        Integer[] a = sequencia(200_000, false);
        OperationMetrics m = AlgoritmoTipo.QUICK.criar().ordenar(a, NATURAL);
        assertArrayEquals(sequencia(200_000, false), a);
        // mediana de tres => O(n log n): muito abaixo de n²/2 = 2*10^10
        assertTrue(m.comparacoes() < 10_000_000L, "comparacoes = " + m.comparacoes());
    }

    @Test
    @DisplayName("ordenacao e cancelada quando a thread e interrompida")
    void cancelamento() throws InterruptedException {
        Integer[] a = new Random(5).ints(60_000).boxed().toArray(Integer[]::new);
        Throwable[] erro = new Throwable[1];
        Thread t = new Thread(() -> {
            try {
                AlgoritmoTipo.BUBBLE.criar().ordenar(a, NATURAL);
            } catch (Throwable e) {
                erro[0] = e;
            }
        });
        t.start();
        Thread.sleep(150);
        t.interrupt();
        t.join(20_000);
        assertTrue(erro[0] instanceof CancellationException, "esperava CancellationException, obteve " + erro[0]);
    }

    @Test
    @DisplayName("fabrica encontra algoritmos pelo nome em varios formatos")
    void fabricaPorNome() {
        assertEquals(AlgoritmoTipo.MERGE, SortAlgorithmFactory.tipo("merge"));
        assertEquals(AlgoritmoTipo.MERGE, SortAlgorithmFactory.tipo("Merge Sort"));
        assertEquals(AlgoritmoTipo.QUICK_3WAY, SortAlgorithmFactory.tipo("quick 3-way"));
        assertEquals(AlgoritmoTipo.TIM, SortAlgorithmFactory.tipo("Tim Sort (simplificado)"));
        assertEquals(10, SortAlgorithmFactory.todos().size());
        assertThrows(IllegalArgumentException.class, () -> SortAlgorithmFactory.tipo("gnome"));
    }
}
