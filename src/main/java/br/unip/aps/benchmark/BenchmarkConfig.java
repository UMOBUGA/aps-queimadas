package br.unip.aps.benchmark;

import br.unip.aps.sorting.AlgoritmoTipo;
import br.unip.aps.sorting.CenarioEntrada;
import br.unip.aps.sorting.CriterioOrdenacao;

import java.util.List;

/** Parametros de uma bateria de benchmark. */
public record BenchmarkConfig(List<AlgoritmoTipo> algoritmos, List<CriterioOrdenacao> criterios,
                              List<Integer> tamanhos, List<CenarioEntrada> cenarios,
                              int aquecimentos, int repeticoes, long semente, int limiteQuadratico, long aquecimentoMs) {
    /** Aquecimento minimo por tempo usado nas execucoes completas: garante o JIT compilado antes de medir. */
    public static final long AQUECIMENTO_MS_PADRAO = 500;

    public BenchmarkConfig {
        algoritmos = List.copyOf(algoritmos);
        criterios = List.copyOf(criterios);
        tamanhos = List.copyOf(tamanhos);
        cenarios = List.copyOf(cenarios);
        if (algoritmos.isEmpty() || criterios.isEmpty() || tamanhos.isEmpty() || cenarios.isEmpty()) {
            throw new IllegalArgumentException("Selecione ao menos um algoritmo, criterio, tamanho e cenario.");
        }
        if (repeticoes < 1) throw new IllegalArgumentException("Repeticoes deve ser >= 1.");
        if (aquecimentos < 0) throw new IllegalArgumentException("Aquecimentos deve ser >= 0.");
        if (aquecimentoMs < 0) throw new IllegalArgumentException("O aquecimento por tempo deve ser >= 0 ms.");
    }

    /** Configuracao so com aquecimento por contagem (sem tempo minimo). */
    public BenchmarkConfig(List<AlgoritmoTipo> algoritmos, List<CriterioOrdenacao> criterios, List<Integer> tamanhos,
                           List<CenarioEntrada> cenarios, int aquecimentos, int repeticoes, long semente, int limiteQuadratico) {
        this(algoritmos, criterios, tamanhos, cenarios, aquecimentos, repeticoes, semente, limiteQuadratico, 0);
    }

    /** Configuracao padrao do enunciado. */
    public static BenchmarkConfig padrao() {
        return new BenchmarkConfig(
                List.of(AlgoritmoTipo.values()),
                List.of(CriterioOrdenacao.DATA, CriterioOrdenacao.BIOMA, CriterioOrdenacao.MUNICIPIO),
                List.of(100, 1_000, 5_000, 10_000, 0),
                List.of(CenarioEntrada.ALEATORIO, CenarioEntrada.ORDENADO, CenarioEntrada.INVERSO),
                2, 5, 42L, Integer.MAX_VALUE, AQUECIMENTO_MS_PADRAO);
    }

    /** Configuracao reduzida para demonstracoes rapidas e testes. */
    public static BenchmarkConfig rapido() {
        return new BenchmarkConfig(
                List.of(AlgoritmoTipo.values()),
                List.of(CriterioOrdenacao.DATA, CriterioOrdenacao.BIOMA, CriterioOrdenacao.MUNICIPIO),
                List.of(100, 500, 1_000, 2_000),
                List.of(CenarioEntrada.ALEATORIO, CenarioEntrada.ORDENADO, CenarioEntrada.INVERSO),
                1, 3, 42L, Integer.MAX_VALUE);
    }

    public int totalCasos() {
        return algoritmos.size() * criterios.size() * tamanhos.size() * cenarios.size();
    }
}
