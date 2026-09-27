package br.unip.aps.estruturas;

import br.unip.aps.sorting.OperationCounter;
import br.unip.aps.sorting.OperationMetrics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/** Merge Sort paralelo com Fork/Join: as metades sao ordenadas em paralelo e intercaladas; cada tarefa conta suas operacoes. */
public final class MergeSortParalelo {
    public static final int LIMIAR_PADRAO = 8_192;

    /** Tempo e ganho com um numero de threads. */
    public record Medicao(int threads, double mediaMs, double speedup, double eficiencia, double fracaoSerial,
                          OperationMetrics metricas, boolean ordenado) { }

    private MergeSortParalelo() {
    }

    /** Ordena in-place usando o pool informado; abaixo do limiar, a tarefa ordena sequencialmente. */
    public static <T> OperationMetrics ordenar(T[] a, Comparator<? super T> c, ForkJoinPool pool, int limiar) {
        if (a.length < 2) return new OperationMetrics(0, 0, 0, 0, 0);
        @SuppressWarnings("unchecked")
        T[] aux = (T[]) new Object[a.length];
        long t0 = System.nanoTime();
        OperationMetrics m = pool.invoke(new Tarefa<>(a, aux, 0, a.length - 1, c, Math.max(2, limiar)));
        return new OperationMetrics(m.comparacoes(), m.trocas(), m.atribuicoes(), m.leituras(), System.nanoTime() - t0);
    }

    private static final class Tarefa<T> extends RecursiveTask<OperationMetrics> {
        private final T[] a, aux;
        private final int lo, hi, limiar;
        private final Comparator<? super T> c;

        Tarefa(T[] a, T[] aux, int lo, int hi, Comparator<? super T> c, int limiar) {
            this.a = a;
            this.aux = aux;
            this.lo = lo;
            this.hi = hi;
            this.c = c;
            this.limiar = limiar;
        }

        @Override
        protected OperationMetrics compute() {
            OperationCounter k = new OperationCounter();
            if (hi - lo + 1 <= limiar) {
                sequencial(a, aux, lo, hi, c, k);
                return k.snapshot();
            }
            int meio = lo + (hi - lo) / 2;
            Tarefa<T> esquerda = new Tarefa<>(a, aux, lo, meio, c, limiar);
            Tarefa<T> direita = new Tarefa<>(a, aux, meio + 1, hi, c, limiar);
            esquerda.fork();
            k.somar(direita.compute());
            k.somar(esquerda.join());
            k.leituras(2);
            if (k.comparar(c, a[meio], a[meio + 1]) > 0) intercalar(a, aux, lo, meio, hi, c, k);
            return k.snapshot();
        }
    }

    private static <T> void sequencial(T[] a, T[] aux, int lo, int hi, Comparator<? super T> c, OperationCounter k) {
        if (hi <= lo) return;
        int meio = lo + (hi - lo) / 2;
        sequencial(a, aux, lo, meio, c, k);
        sequencial(a, aux, meio + 1, hi, c, k);
        k.leituras(2);
        if (k.comparar(c, a[meio], a[meio + 1]) > 0) intercalar(a, aux, lo, meio, hi, c, k);
    }

    private static <T> void intercalar(T[] a, T[] aux, int lo, int meio, int hi, Comparator<? super T> c, OperationCounter k) {
        for (int i = lo; i <= hi; i++) aux[i] = a[i];
        k.leituras(hi - lo + 1);
        k.atribuicoes(hi - lo + 1);
        int i = lo, j = meio + 1;
        for (int p = lo; p <= hi; p++) {
            if (i > meio) a[p] = aux[j++];
            else if (j > hi) a[p] = aux[i++];
            else if (k.comparar(c, aux[j], aux[i]) < 0) a[p] = aux[j++];
            else a[p] = aux[i++];
            k.leitura();
            k.atribuicao();
        }
    }

    /**
     * Mede o speedup S(p) = T(1)/T(p) para cada numero de threads; a fracao serial vem da metrica de Karp-Flatt
     * e = (1/S - 1/p) / (1 - 1/p), que estima a parte nao paralelizavel da Lei de Amdahl.
     */
    public static <T> List<Medicao> speedup(T[] base, Comparator<? super T> c, int[] threads, int aquecimentos, int repeticoes) {
        List<Medicao> r = new ArrayList<>();
        double t1 = Double.NaN;
        for (int p : threads) {
            ForkJoinPool pool = new ForkJoinPool(p);
            try {
                OperationMetrics ultima = null;
                boolean ok = true;
                for (int i = 0; i < aquecimentos; i++) ordenar(base.clone(), c, pool, LIMIAR_PADRAO);
                double soma = 0;
                for (int i = 0; i < repeticoes; i++) {
                    T[] copia = base.clone();
                    ultima = ordenar(copia, c, pool, LIMIAR_PADRAO);
                    soma += ultima.nanos() / 1e6;
                    ok &= ordenado(copia, c);
                }
                double media = soma / repeticoes;
                if (Double.isNaN(t1)) t1 = media;
                double s = t1 / media;
                double e = p == 1 ? 0 : (1 / s - 1.0 / p) / (1 - 1.0 / p);
                r.add(new Medicao(p, media, s, s / p, e, ultima, ok));
            } finally {
                pool.shutdown();
            }
        }
        return r;
    }

    private static <T> boolean ordenado(T[] a, Comparator<? super T> c) {
        for (int i = 1; i < a.length; i++) if (c.compare(a[i - 1], a[i]) > 0) return false;
        return true;
    }
}
