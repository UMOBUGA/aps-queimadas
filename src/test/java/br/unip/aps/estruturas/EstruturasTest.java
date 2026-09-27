package br.unip.aps.estruturas;

import br.unip.aps.busca.Buscas;
import br.unip.aps.sorting.OperationCounter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Estruturas de dados implementadas a mao: invariantes e contagem de operacoes. */
@DisplayName("Estruturas de dados")
class EstruturasTest {

    @Test
    void buscaBinariaConcordaComASequencialEUsaLogN() {
        Random rnd = new Random(7);
        Integer[] a = new Integer[10_000];
        for (int i = 0; i < a.length; i++) a[i] = rnd.nextInt(2_000);
        List<Integer> ref = new ArrayList<>(List.of(a));
        Collections.sort(ref);
        a = ref.toArray(new Integer[0]);
        for (int alvo = -5; alvo < 2_010; alvo += 13) {
            OperationCounter bin = new OperationCounter(), lin = new OperationCounter();
            Buscas.Intervalo i = Buscas.intervaloBinario(a, x -> x, Comparator.<Integer>naturalOrder(), alvo, alvo + 40, bin);
            int n = Buscas.contarLinear(a, x -> x, Comparator.<Integer>naturalOrder(), alvo, alvo + 40, lin);
            assertEquals(n, i.tamanho());
            assertTrue(bin.getComparacoes() <= 2 * 15, "duas buscas de no maximo ceil(log2(n+1)) = 14 comparacoes");
            assertTrue(lin.getComparacoes() >= a.length);
        }
    }

    @Test
    void avlPermaneceBalanceadaComInsercoesERemocoes() {
        OperationCounter k = new OperationCounter();
        ArvoreAVL<Integer, Integer> t = new ArvoreAVL<>(Comparator.naturalOrder(), k);
        Random rnd = new Random(11);
        List<Integer> chaves = new ArrayList<>();
        for (int i = 0; i < 5_000; i++) {
            int c = rnd.nextInt(100_000);
            t.inserir(c, i);
            chaves.add(c);
        }
        assertTrue(t.valida());
        double limite = 1.4405 * (Math.log(t.nos() + 2) / Math.log(2)) - 0.3277;
        assertTrue(t.altura() <= limite, "altura " + t.altura() + " acima do limite AVL " + limite);
        for (int i = 0; i < 2_000; i++) t.remover(chaves.get(i));
        assertTrue(t.valida());
        List<Integer> emOrdem = new ArrayList<>();
        t.emOrdem((c, v) -> emOrdem.add(c));
        for (int i = 1; i < emOrdem.size(); i++) assertTrue(emOrdem.get(i - 1) < emOrdem.get(i));
        assertEquals(t.nos(), emOrdem.size());
    }

    @Test
    void avlContaRotacoesSimplesEDuplas() {
        ArvoreAVL<Integer, String> simples = new ArvoreAVL<>(Comparator.naturalOrder(), new OperationCounter());
        for (int c : new int[]{1, 2, 3}) simples.inserir(c, "x");
        assertEquals(1, simples.rotacoesSimples());
        assertEquals(0, simples.rotacoesDuplas());
        assertEquals(2, simples.raiz().chave());

        ArvoreAVL<Integer, String> dupla = new ArvoreAVL<>(Comparator.naturalOrder(), new OperationCounter());
        for (int c : new int[]{3, 1, 2}) dupla.inserir(c, "x");
        assertEquals(0, dupla.rotacoesSimples());
        assertEquals(1, dupla.rotacoesDuplas());
        assertEquals(2, dupla.raiz().chave());
    }

    @Test
    void avlConsultaIntervaloEChavesRepetidas() {
        ArvoreAVL<Integer, Integer> t = new ArvoreAVL<>(Comparator.naturalOrder(), new OperationCounter());
        for (int i = 0; i < 1_000; i++) t.inserir(i % 100, i);
        assertEquals(100, t.nos());
        assertEquals(1_000, t.valores());
        assertEquals(10, t.buscar(42).size());
        List<Integer> r = new ArrayList<>();
        t.intervalo(10, 19, r::add);
        assertEquals(100, r.size());
        assertTrue(t.remover(42));
        assertEquals(990, t.valores());
        assertTrue(t.buscar(42).isEmpty());
        assertFalse(t.remover(4242));
    }

    @Test
    void tabelaHashRedimensionaEContaColisoes() {
        OperationCounter k = new OperationCounter();
        TabelaHash<String, Integer> h = TabelaHash.paraTexto(k);
        for (int i = 0; i < 5_000; i++) h.colocar("MUNICIPIO " + (i % 700), i);
        assertEquals(700, h.chaves());
        assertEquals(5_000, h.valores());
        assertTrue(h.fatorCarga() <= 0.75);
        assertTrue(h.redimensionamentos() >= 5, "16 -> 1024 baldes");
        assertTrue(h.colisoes() > 0);
        assertEquals(8, h.contar("MUNICIPIO 5"));
        int[] dist = h.distribuicaoCadeias();
        int soma = 0;
        for (int i = 0; i < dist.length; i++) soma += i * dist[i];
        assertEquals(700, soma);
        assertTrue(h.remover("MUNICIPIO 5"));
        assertEquals(0, h.contar("MUNICIPIO 5"));
        assertEquals(699, h.chaves());
    }

    @Test
    void fnv1aConhecido() {
        assertEquals(0x811C9DC5, TabelaHash.fnv1a(""));
        assertEquals(0xE40C292C, TabelaHash.fnv1a("a"));
    }

    @Test
    void heapOrdenaETopKCoincideComOrdenacao() {
        Random rnd = new Random(3);
        List<Integer> dados = new ArrayList<>();
        for (int i = 0; i < 20_000; i++) dados.add(rnd.nextInt(1_000_000));
        OperationCounter k = new OperationCounter();
        HeapBinario<Integer> h = new HeapBinario<>(Comparator.naturalOrder(), k);
        dados.forEach(h::inserir);
        int anterior = Integer.MIN_VALUE;
        while (!h.vazio()) {
            int x = h.removerTopo();
            assertTrue(x >= anterior);
            anterior = x;
        }
        OperationCounter kTop = new OperationCounter();
        List<Integer> top = HeapBinario.topK(dados, 10, Comparator.naturalOrder(), kTop);
        List<Integer> ref = new ArrayList<>(dados);
        ref.sort(Comparator.reverseOrder());
        assertEquals(ref.subList(0, 10), top);
        double nLogN = dados.size() * (Math.log(dados.size()) / Math.log(2));
        assertTrue(kTop.getComparacoes() < nLogN / 3, "top-k com heap de k itens: ~n log k");
    }

    @Test
    void mergeSortParaleloIgualAoSequencialComQualquerNumeroDeThreads() {
        Random rnd = new Random(5);
        Integer[] base = new Integer[60_000];
        for (int i = 0; i < base.length; i++) base[i] = rnd.nextInt(10_000);
        Integer[] ref = base.clone();
        List<Integer> l = new ArrayList<>(List.of(ref));
        Collections.sort(l);
        ref = l.toArray(new Integer[0]);
        long comparacoes = -1;
        for (int p : new int[]{1, 2, 4}) {
            Integer[] a = base.clone();
            ForkJoinPool pool = new ForkJoinPool(p);
            try {
                var m = MergeSortParalelo.ordenar(a, Comparator.naturalOrder(), pool, 1_024);
                assertArrayEquals(ref, a);
                if (comparacoes < 0) comparacoes = m.comparacoes();
                assertEquals(comparacoes, m.comparacoes(), "a contagem nao depende do numero de threads");
            } finally {
                pool.shutdown();
            }
        }
    }

    @Test
    void mergeSortAlocaMaisMemoriaQueHeapSort() {
        org.junit.jupiter.api.Assumptions.assumeTrue(MedidorMemoria.suportado());
        Integer[] a = new Integer[20_000];
        Random rnd = new Random(1);
        for (int i = 0; i < a.length; i++) a[i] = rnd.nextInt();
        var r = MedidorMemoria.medir(a, Comparator.naturalOrder(), null,
                List.of(br.unip.aps.sorting.AlgoritmoTipo.MERGE, br.unip.aps.sorting.AlgoritmoTipo.HEAP));
        long merge = r.get(0).bytesAlocados(), heap = r.get(1).bytesAlocados();
        assertTrue(merge >= 4L * a.length, "Merge Sort aloca o vetor auxiliar O(n): " + merge);
        assertTrue(heap < merge / 10, "Heap Sort e in-place: " + heap);
    }
}
