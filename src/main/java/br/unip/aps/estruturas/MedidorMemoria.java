package br.unip.aps.estruturas;

import br.unip.aps.sorting.AlgoritmoTipo;
import br.unip.aps.sorting.SortAlgorithm;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToLongFunction;

/** Mede a memoria extra alocada por cada algoritmo de ordenacao, descontando o custo fixo da instrumentacao (n = 1). */
public final class MedidorMemoria {
    /** Espaco extra teorico x alocacao medida. */
    public record Medicao(String algoritmo, String espacoTeorico, long bytesAlocados, int n, long bytesPorElemento) { }

    private MedidorMemoria() {
    }

    /** Disponivel na JVM HotSpot (com.sun.management); em outras JVMs a medicao retorna -1. */
    public static boolean suportado() {
        return ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean t
                && t.isThreadAllocatedMemorySupported();
    }

    public static long alocadosPelaThread() {
        if (ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean t && t.isThreadAllocatedMemorySupported()) {
            if (!t.isThreadAllocatedMemoryEnabled()) t.setThreadAllocatedMemoryEnabled(true);
            return t.getThreadAllocatedBytes(Thread.currentThread().threadId());
        }
        return -1;
    }

    /** Ordena uma copia da entrada com cada algoritmo e registra os bytes alocados durante a ordenacao. */
    public static <T> List<Medicao> medir(T[] entrada, Comparator<? super T> c, ToLongFunction<? super T> chaveNumerica,
                                          List<AlgoritmoTipo> algoritmos) {
        List<Medicao> r = new ArrayList<>();
        for (AlgoritmoTipo tipo : algoritmos) {
            SortAlgorithm alg = tipo.criar();
            if (alg.exigeChaveNumerica() && chaveNumerica == null) continue;
            T[] copia = entrada.clone();
            alg.ordenar(entrada.clone(), c, chaveNumerica);
            T[] unitario = java.util.Arrays.copyOf(entrada, Math.min(1, entrada.length));
            long b0 = alocadosPelaThread();
            alg.ordenar(unitario, c, chaveNumerica);
            long instrumentacao = alocadosPelaThread() - b0;
            long antes = alocadosPelaThread();
            alg.ordenar(copia, c, chaveNumerica);
            long depois = alocadosPelaThread();
            long bytes = antes < 0 ? -1 : Math.max(0, depois - antes - instrumentacao);
            r.add(new Medicao(alg.nome(), alg.complexidade().espaco(), bytes, entrada.length,
                    entrada.length == 0 || bytes < 0 ? 0 : bytes / entrada.length));
        }
        return r;
    }
}
