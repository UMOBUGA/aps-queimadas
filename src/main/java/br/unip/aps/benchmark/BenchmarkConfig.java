package br.unip.aps.benchmark;

import br.unip.aps.sorting.AlgoritmoTipo;
import br.unip.aps.sorting.CenarioEntrada;
import br.unip.aps.sorting.CriterioOrdenacao;

import java.util.List;

/**
 * Parametros de uma bateria de benchmark.
 *
 * @param algoritmos         algoritmos a medir
 * @param criterios          criterios (ordem crescente)
 * @param tamanhos           tamanhos de entrada; valores &lt;= 0 ou maiores que a base = base inteira
 * @param cenarios           disposicoes iniciais (aleatoria, ordenada, inversa...)
 * @param aquecimentos       execucoes descartadas antes de medir (warm-up do JIT) por caso
 * @param repeticoes         execucoes medidas por caso (media e desvio-padrao)
 * @param semente            semente da amostragem/embaralhamento (reprodutibilidade)
 * @param limiteQuadratico   algoritmos O(n²) sao pulados quando n excede este valor
 */
public record BenchmarkConfig(List<AlgoritmoTipo> algoritmos, List<CriterioOrdenacao> criterios,
                              List<Integer> tamanhos, List<CenarioEntrada> cenarios,
                              int aquecimentos, int repeticoes, long semente, int limiteQuadratico) {

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
    }

    /**
     * Configuracao padrao do enunciado: todos os algoritmos, criterios data/bioma/municipio,
     * tamanhos 100, 1.000, 5.000, 10.000 e total, entradas aleatoria, ordenada e inversa.
     *
     * @return configuracao padrao
     */
    public static BenchmarkConfig padrao() {
        return new BenchmarkConfig(
                List.of(AlgoritmoTipo.values()),
                List.of(CriterioOrdenacao.DATA, CriterioOrdenacao.BIOMA, CriterioOrdenacao.MUNICIPIO),
                List.of(100, 1_000, 5_000, 10_000, 0),
                List.of(CenarioEntrada.ALEATORIO, CenarioEntrada.ORDENADO, CenarioEntrada.INVERSO),
                2, 5, 42L, Integer.MAX_VALUE);
    }

    /**
     * Configuracao reduzida para demonstracoes rapidas e testes.
     *
     * @return configuracao rapida
     */
    public static BenchmarkConfig rapido() {
        return new BenchmarkConfig(
                List.of(AlgoritmoTipo.values()),
                List.of(CriterioOrdenacao.DATA, CriterioOrdenacao.BIOMA, CriterioOrdenacao.MUNICIPIO),
                List.of(100, 500, 1_000, 2_000),
                List.of(CenarioEntrada.ALEATORIO, CenarioEntrada.ORDENADO, CenarioEntrada.INVERSO),
                1, 3, 42L, Integer.MAX_VALUE);
    }

    /** @return numero de casos (algoritmo x criterio x tamanho x cenario) */
    public int totalCasos() {
        return algoritmos.size() * criterios.size() * tamanhos.size() * cenarios.size();
    }
}
