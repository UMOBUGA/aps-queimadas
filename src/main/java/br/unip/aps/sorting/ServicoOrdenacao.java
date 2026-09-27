package br.unip.aps.sorting;

import br.unip.aps.ApsException;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.util.Formatos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Servico de aplicacao que atende uma "solicitacao de exibicao ordenada".
 *
 * <p>Fluxo: (1) valida a solicitacao; (2) extrai a amostra do tamanho pedido; (3) prepara o
 * cenario de entrada; (4) copia para um array — os dados originais nunca sao alterados; (5)
 * executa o algoritmo com contagem de operacoes e cronometro; (6) verifica de forma independente
 * se o resultado esta ordenado; (7) devolve dados + metricas + avisos.</p>
 */
public final class ServicoOrdenacao {

    private static final Logger LOG = Logger.getLogger(ServicoOrdenacao.class.getName());

    /** Acima deste n, algoritmos O(n²) geram aviso (configuravel em application.properties). */
    private final int limiteQuadratico;

    /** @param limiteQuadratico tamanho a partir do qual algoritmos O(n²) geram aviso */
    public ServicoOrdenacao(int limiteQuadratico) {
        this.limiteQuadratico = limiteQuadratico;
    }

    /**
     * Parametros de uma solicitacao de ordenacao.
     *
     * @param algoritmo algoritmo escolhido
     * @param criterios criterio simples ou composto
     * @param tamanho   tamanho da amostra (&lt;= 0 ou maior que a base = base inteira)
     * @param amostraAleatoria {@code true} para sortear a amostra; {@code false} para os primeiros n
     * @param cenario   disposicao inicial da entrada
     * @param semente   semente para amostragem/embaralhamento
     */
    public record Solicitacao(AlgoritmoTipo algoritmo, Criterios criterios, int tamanho,
                              boolean amostraAleatoria, CenarioEntrada cenario, long semente) {

        /**
         * Solicitacao simples: base inteira, ordem original.
         *
         * @param algoritmo algoritmo
         * @param criterios criterio
         * @return solicitacao
         */
        public static Solicitacao de(AlgoritmoTipo algoritmo, Criterios criterios) {
            return new Solicitacao(algoritmo, criterios, 0, false, CenarioEntrada.ORIGINAL, 42L);
        }
    }

    /**
     * Executa a ordenacao solicitada.
     *
     * @param base dados de entrada (nao sao modificados)
     * @param s    solicitacao
     * @return dados ordenados e metricas
     * @throws ApsException se a solicitacao for invalida (ex.: Radix com criterio de texto, base vazia)
     */
    public ResultadoOrdenacao<FocoIncendio> ordenar(List<FocoIncendio> base, Solicitacao s) throws ApsException {
        validar(base, s);
        List<FocoIncendio> amostra = amostrar(base, s.tamanho(), s.amostraAleatoria(), s.semente());
        List<FocoIncendio> entrada = s.cenario().preparar(amostra, s.criterios().comparador(), s.semente());
        String aviso = avisoDesempenho(s.algoritmo(), entrada.size());

        FocoIncendio[] arr = entrada.toArray(new FocoIncendio[0]);
        SortAlgorithm alg = SortAlgorithmFactory.criar(s.algoritmo());
        OperationMetrics m = alg.ordenar(arr, s.criterios().comparador(), s.criterios().chaveNumerica());

        List<FocoIncendio> ordenados = Collections.unmodifiableList(java.util.Arrays.asList(arr));
        boolean ok = Ordenacoes.estaOrdenada(ordenados, s.criterios().comparador());
        if (!ok) {
            LOG.severe(() -> "Verificacao falhou para " + alg.nome() + " / " + s.criterios());
        }
        ResultadoOrdenacao<FocoIncendio> r = new ResultadoOrdenacao<>(ordenados, alg.nome(),
                s.criterios().descricao(), s.cenario(), m, ok, aviso);
        LOG.info(r::resumo);
        return r;
    }

    /**
     * Executa todos os algoritmos compativeis sobre a MESMA entrada (comparativo rapido).
     *
     * @param base      dados
     * @param criterios criterio
     * @param tamanho   tamanho da amostra
     * @param cenario   disposicao inicial
     * @param incluirQuadraticos se {@code false}, pula algoritmos O(n²) quando n &gt; limite
     * @return um resultado por algoritmo
     * @throws ApsException se a base estiver vazia
     */
    public List<ResultadoOrdenacao<FocoIncendio>> compararTodos(List<FocoIncendio> base, Criterios criterios,
                                                                int tamanho, CenarioEntrada cenario,
                                                                boolean incluirQuadraticos) throws ApsException {
        List<ResultadoOrdenacao<FocoIncendio>> r = new ArrayList<>();
        for (AlgoritmoTipo t : AlgoritmoTipo.values()) {
            if (t.exigeChaveNumerica() && criterios.chaveNumerica() == null) continue;
            int n = tamanho <= 0 ? base.size() : Math.min(tamanho, base.size());
            if (!incluirQuadraticos && t.quadratico() && n > limiteQuadratico) continue;
            r.add(ordenar(base, new Solicitacao(t, criterios, tamanho, false, cenario, 42L)));
        }
        return r;
    }

    private void validar(List<FocoIncendio> base, Solicitacao s) throws ApsException {
        if (s == null || s.algoritmo() == null || s.criterios() == null || s.cenario() == null) {
            throw new ApsException("Solicitacao de ordenacao incompleta: escolha algoritmo, criterio e cenario.");
        }
        if (base == null || base.isEmpty()) {
            throw new ApsException("Nao ha dados para ordenar. Carregue os arquivos ou revise os filtros aplicados.");
        }
        if (s.algoritmo().exigeChaveNumerica() && s.criterios().chaveNumerica() == null) {
            throw new ApsException(s.algoritmo().nome() + " nao compara elementos: so ordena por um unico criterio "
                    + "numerico (Data/hora, Latitude, Longitude ou ID). Escolha outro algoritmo ou criterio.");
        }
    }

    /**
     * @param alg algoritmo
     * @param n   tamanho da entrada
     * @return aviso de custo para algoritmos O(n²) acima do limite, ou {@code null}
     */
    public String avisoDesempenho(AlgoritmoTipo alg, int n) {
        if (!alg.quadratico() || n <= limiteQuadratico) return null;
        long estimativa = (long) n * (n - 1) / 2;
        return alg.nome() + " e O(n²): com n = " + Formatos.inteiro(n) + " pode realizar ate ~"
                + Formatos.inteiro(estimativa) + " comparacoes. Considere reduzir a amostra ou usar um "
                + "algoritmo O(n log n).";
    }

    /** @return limite de n para avisos de algoritmos O(n²) */
    public int getLimiteQuadratico() {
        return limiteQuadratico;
    }

    /**
     * Extrai uma amostra da base.
     *
     * @param base      base
     * @param tamanho   tamanho desejado (&lt;= 0 = base inteira)
     * @param aleatoria sorteia (Fisher-Yates parcial) ou pega os primeiros
     * @param semente   semente
     * @param <T>       tipo
     * @return nova lista com a amostra
     */
    public static <T> List<T> amostrar(List<T> base, int tamanho, boolean aleatoria, long semente) {
        int n = tamanho <= 0 ? base.size() : Math.min(tamanho, base.size());
        if (!aleatoria || n == base.size()) {
            return new ArrayList<>(base.subList(0, n));
        }
        List<T> copia = new ArrayList<>(base);
        Random rnd = new Random(semente);
        for (int i = 0; i < n; i++) {
            int j = i + rnd.nextInt(copia.size() - i);
            T t = copia.get(i);
            copia.set(i, copia.get(j));
            copia.set(j, t);
        }
        return new ArrayList<>(copia.subList(0, n));
    }
}
