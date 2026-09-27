package br.unip.aps.benchmark;

import br.unip.aps.analysis.EstatisticaDescritiva;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.sorting.AlgoritmoTipo;
import br.unip.aps.sorting.CenarioEntrada;
import br.unip.aps.sorting.CriterioOrdenacao;
import br.unip.aps.sorting.Criterios;
import br.unip.aps.sorting.OperationMetrics;
import br.unip.aps.sorting.Ordem;
import br.unip.aps.sorting.Ordenacoes;
import br.unip.aps.sorting.ServicoOrdenacao;
import br.unip.aps.sorting.SortAlgorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import java.util.function.ToLongFunction;
import java.util.logging.Logger;

/**
 * Executor do benchmark de algoritmos de ordenacao.
 *
 * <h2>Cuidados metodologicos (JVM)</h2>
 * <ul>
 *   <li><b>Aquecimento do JIT</b>: a JVM comeca interpretando o bytecode e so depois compila os
 *       metodos "quentes" para codigo nativo (C1/C2). Medir as primeiras execucoes mede o
 *       interpretador. Por isso ha um aquecimento global (todos os algoritmos em uma entrada
 *       pequena) e {@code aquecimentos} execucoes descartadas antes de cada caso.</li>
 *   <li><b>Repeticoes</b>: cada caso roda {@code repeticoes} vezes; reportamos media, desvio-padrao
 *       amostral, minimo e maximo. O minimo e o estimador menos sensivel a ruido (GC, SO).</li>
 *   <li><b>Mesma entrada</b>: para cada (criterio, cenario, n) a entrada e preparada uma unica vez
 *       e cada execucao recebe uma copia identica — todos os algoritmos competem em igualdade.</li>
 *   <li><b>Reprodutibilidade</b>: amostragem e embaralhamento usam semente fixa.</li>
 *   <li><b>Verificacao</b>: toda execucao e conferida como ordenada; uma falha e reportada.</li>
 * </ul>
 */
public final class BenchmarkRunner {

    private static final Logger LOG = Logger.getLogger(BenchmarkRunner.class.getName());

    /**
     * Progresso para barras de progresso (dashboard) ou console.
     *
     * @param concluidos casos concluidos
     * @param total      total de casos
     * @param mensagem   caso atual
     */
    public record Progresso(int concluidos, int total, String mensagem) {
        /** @return fracao concluida (0 a 1) */
        public double fracao() {
            return total == 0 ? 1 : (double) concluidos / total;
        }
    }

    /**
     * Executa a bateria configurada.
     *
     * @param base     focos (a base inteira; as amostras sao extraidas dela)
     * @param config   configuracao
     * @param progresso ouvinte de progresso (pode ser {@code null})
     * @return uma medicao por caso executado
     * @throws CancellationException se a thread for interrompida (botao Cancelar)
     */
    public List<BenchmarkResult> executar(List<FocoIncendio> base, BenchmarkConfig config,
                                          Consumer<Progresso> progresso) {
        if (base == null || base.isEmpty()) {
            throw new IllegalArgumentException("A base de focos esta vazia.");
        }
        Consumer<Progresso> ouvinte = progresso != null ? progresso : p -> { };
        List<Integer> tamanhos = normalizarTamanhos(config.tamanhos(), base.size());
        int total = config.algoritmos().size() * config.criterios().size() * tamanhos.size() * config.cenarios().size();
        int concluidos = 0;

        aquecimentoGlobal(base, config);

        List<BenchmarkResult> resultados = new ArrayList<>();
        for (CriterioOrdenacao criterio : config.criterios()) {
            Criterios crit = Criterios.de(criterio, Ordem.CRESCENTE);
            Comparator<FocoIncendio> cmp = crit.comparador();
            ToLongFunction<FocoIncendio> chave = crit.chaveNumerica();
            for (int n : tamanhos) {
                List<FocoIncendio> amostra = ServicoOrdenacao.amostrar(base, n, true, config.semente());
                for (CenarioEntrada cenario : config.cenarios()) {
                    FocoIncendio[] entrada = cenario.preparar(amostra, cmp, config.semente()).toArray(new FocoIncendio[0]);
                    for (AlgoritmoTipo tipo : config.algoritmos()) {
                        verificarCancelamento();
                        String caso = tipo.nome() + " | " + criterio.rotulo() + " | " + cenario + " | n=" + n;
                        ouvinte.accept(new Progresso(concluidos, total, caso));
                        concluidos++;
                        if (tipo.exigeChaveNumerica() && chave == null) continue;
                        if (tipo.quadratico() && n > config.limiteQuadratico()) {
                            LOG.info(() -> "Pulado (O(n²) acima do limite): " + caso);
                            continue;
                        }
                        resultados.add(medir(tipo.criar(), criterio, cenario, entrada, cmp, chave, config));
                    }
                }
            }
        }
        ouvinte.accept(new Progresso(total, total, "Benchmark concluido"));
        return resultados;
    }

    private BenchmarkResult medir(SortAlgorithm alg, CriterioOrdenacao criterio, CenarioEntrada cenario,
                                  FocoIncendio[] entrada, Comparator<FocoIncendio> cmp,
                                  ToLongFunction<FocoIncendio> chave, BenchmarkConfig config) {
        for (int i = 0; i < config.aquecimentos(); i++) {
            verificarCancelamento();
            alg.ordenar(Arrays.copyOf(entrada, entrada.length), cmp, chave);
        }
        long[] tempos = new long[config.repeticoes()];
        OperationMetrics ultima = null;
        boolean ok = true;
        for (int r = 0; r < config.repeticoes(); r++) {
            verificarCancelamento();
            FocoIncendio[] copia = Arrays.copyOf(entrada, entrada.length);
            ultima = alg.ordenar(copia, cmp, chave);
            tempos[r] = ultima.nanos();
            ok &= Ordenacoes.estaOrdenada(Arrays.asList(copia), cmp);
        }
        double media = EstatisticaDescritiva.media(tempos);
        double desvio = EstatisticaDescritiva.desvioPadrao(tempos);
        long min = Long.MAX_VALUE, max = Long.MIN_VALUE;
        for (long t : tempos) {
            min = Math.min(min, t);
            max = Math.max(max, t);
        }
        BenchmarkResult r = new BenchmarkResult(alg.nome(), criterio, cenario, entrada.length, config.repeticoes(),
                media, desvio, min, max, ultima.comparacoes(), ultima.trocas(), ultima.atribuicoes(),
                ultima.acessos(), ok);
        LOG.fine(() -> String.format("%s | %s | %s | n=%d -> %.3f ms", r.algoritmo(), criterio, cenario, r.n(), r.mediaMs()));
        return r;
    }

    /** Executa cada algoritmo algumas vezes em uma entrada pequena para disparar a compilacao JIT. */
    private void aquecimentoGlobal(List<FocoIncendio> base, BenchmarkConfig config) {
        if (config.aquecimentos() == 0) return;
        List<FocoIncendio> amostra = ServicoOrdenacao.amostrar(base, 1_000, true, config.semente() + 1);
        for (CriterioOrdenacao c : config.criterios()) {
            Criterios crit = Criterios.de(c, Ordem.CRESCENTE);
            FocoIncendio[] entrada = amostra.toArray(new FocoIncendio[0]);
            for (AlgoritmoTipo t : config.algoritmos()) {
                if (t.exigeChaveNumerica() && crit.chaveNumerica() == null) continue;
                for (int i = 0; i < 3; i++) {
                    verificarCancelamento();
                    t.criar().ordenar(Arrays.copyOf(entrada, entrada.length), crit.comparador(), crit.chaveNumerica());
                }
            }
        }
    }

    private static List<Integer> normalizarTamanhos(List<Integer> tamanhos, int total) {
        Set<Integer> s = new LinkedHashSet<>();
        for (int t : tamanhos) s.add(t <= 0 || t > total ? total : t);
        return Ordenacoes.ordenar(new ArrayList<>(s), Comparator.naturalOrder());
    }

    private static void verificarCancelamento() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("Benchmark cancelado pelo usuario");
        }
    }
}
