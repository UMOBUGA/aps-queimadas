package br.unip.aps.ml;

import br.unip.aps.analysis.Contagem;
import br.unip.aps.sorting.Ordenacoes;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.regression.RandomForest;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Previsao da quantidade de focos por municipio e mes com <b>Random Forest de regressao</b>
 * (Breiman, 2001) — conjunto de arvores de decisao treinadas em amostras bootstrap com sorteio
 * de variaveis em cada divisao; a previsao e a media das arvores.
 *
 * <h2>Protocolo de validacao</h2>
 * <p>Validacao temporal (nunca embaralhar series temporais): treina com um ano (ex.: 2023) e
 * testa no ano seguinte (2024), prevendo cada mes com a informacao disponivel ate o mes anterior
 * (previsao de um passo a frente). Comparamos com dois modelos de referencia (baselines):</p>
 * <ul>
 *   <li><b>Persistencia</b>: "o mes que vem sera igual a este" (ŷ = lag1);</li>
 *   <li><b>Media historica</b>: ŷ = media mensal do municipio ate o mes anterior.</li>
 * </ul>
 * <p>Um modelo de ML so agrega valor se superar esses baselines.</p>
 */
public final class PrevisaoFocos {

    /**
     * Resultado da previsao.
     *
     * @param anoTreino          ano de treino
     * @param anoTeste           ano de teste
     * @param linhasTreino       observacoes de treino
     * @param linhasTeste        observacoes de teste
     * @param modelo             metricas do Random Forest no teste
     * @param persistencia       metricas do baseline de persistencia
     * @param mediaHistorica     metricas do baseline de media historica
     * @param realPorMes         focos reais do estado por mes do ano de teste
     * @param previstoPorMes     focos previstos (soma dos municipios) por mes
     * @param importancia        importancia de cada variavel (reducao de impureza), mesma ordem de {@link BaseMensal#VARIAVEIS}
     * @param maioresPrevisoes   municipios com maior previsao acumulada no ano de teste
     * @param maioresErros       municipio/mes com maiores erros absolutos
     * @param janelaExpansivel   metricas do RF re-treinado a cada mes com todo o historico anterior
     * @param previstoJanelaPorMes focos previstos por mes na validacao em janela expansivel
     */
    public record Resultado(int anoTreino, int anoTeste, int linhasTreino, int linhasTeste,
                            Metricas.Regressao modelo, Metricas.Regressao persistencia,
                            Metricas.Regressao mediaHistorica,
                            Map<YearMonth, Double> realPorMes, Map<YearMonth, Double> previstoPorMes,
                            double[] importancia, List<Contagem> maioresPrevisoes,
                            List<ErroPrevisao> maioresErros, Metricas.Regressao janelaExpansivel,
                            Map<YearMonth, Double> previstoJanelaPorMes) { }

    /**
     * Erro de previsao de um municipio em um mes.
     *
     * @param municipio municipio
     * @param mes       mes
     * @param real      focos observados
     * @param previsto  focos previstos
     */
    public record ErroPrevisao(String municipio, YearMonth mes, int real, double previsto) {
        /** @return erro absoluto */
        public double erroAbsoluto() {
            return Math.abs(real - previsto);
        }
    }

    private final int arvores;
    private final long semente;

    /**
     * @param arvores numero de arvores da floresta
     * @param semente semente (reprodutibilidade)
     */
    public PrevisaoFocos(int arvores, long semente) {
        this.arvores = arvores;
        this.semente = semente;
    }

    /**
     * Treina em {@code anoTreino} e avalia em {@code anoTeste}.
     *
     * @param base      base mensal
     * @param anoTreino ano de treino
     * @param anoTeste  ano de teste
     * @return metricas, series e importancias
     */
    public Resultado executar(BaseMensal base, int anoTreino, int anoTeste) {
        List<BaseMensal.Linha> treino = base.linhasDoAno(anoTreino);
        List<BaseMensal.Linha> teste = base.linhasDoAno(anoTeste);
        if (treino.isEmpty() || teste.isEmpty()) {
            throw new IllegalArgumentException("Nao ha dados para o ano de treino " + anoTreino + " ou de teste " + anoTeste + ".");
        }
        DataFrame dfTreino = SmileAdapter.paraDataFrameReal(treino);
        DataFrame dfTeste = SmileAdapter.paraDataFrameReal(teste);
        RandomForest rf = RandomForest.fit(Formula.lhs(SmileAdapter.ALVO), dfTreino,
                new RandomForest.Options(arvores, 0, 20, 0, 5, 1.0, SmileAdapter.sementes(arvores, semente), null));

        int n = teste.size();
        double[] real = new double[n], prev = new double[n], pers = new double[n], media = new double[n];
        int iLag1 = indice("lag1"), iMedia = indice("media_hist");
        Map<YearMonth, Double> realMes = new LinkedHashMap<>(), prevMes = new LinkedHashMap<>();
        Map<String, Double> acumulado = new LinkedHashMap<>();
        List<ErroPrevisao> erros = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            BaseMensal.Linha l = teste.get(i);
            real[i] = l.focos();
            prev[i] = Math.max(0, rf.predict(dfTeste.get(i)));
            pers[i] = l.x()[iLag1];
            media[i] = l.x()[iMedia];
            realMes.merge(l.mes(), real[i], Double::sum);
            prevMes.merge(l.mes(), prev[i], Double::sum);
            acumulado.merge(l.municipio(), prev[i], Double::sum);
            erros.add(new ErroPrevisao(l.municipio(), l.mes(), l.focos(), prev[i]));
        }

        List<Contagem> top = new ArrayList<>();
        acumulado.forEach((m, v) -> top.add(new Contagem(m, Math.round(v))));
        Ordenacoes.ordenar(top, (a, b) -> Long.compare(b.total(), a.total()));
        Ordenacoes.ordenar(erros, (a, b) -> Double.compare(b.erroAbsoluto(), a.erroAbsoluto()));

        Map<YearMonth, Double> prevJanela = new LinkedHashMap<>();
        double[] prevJ = janelaExpansivel(base, anoTeste, prevJanela);

        return new Resultado(anoTreino, anoTeste, treino.size(), n,
                Metricas.regressao(real, prev), Metricas.regressao(real, pers), Metricas.regressao(real, media),
                realMes, prevMes, rf.importance(),
                new ArrayList<>(top.subList(0, Math.min(15, top.size()))),
                new ArrayList<>(erros.subList(0, Math.min(15, erros.size()))),
                Metricas.regressao(real, prevJ), prevJanela);
    }

    /**
     * Validacao em janela expansivel (walk-forward): para cada mes m do ano de teste, treina uma
     * floresta com TODOS os meses anteriores a m (inclusive os meses ja observados do proprio ano
     * de teste) e preve apenas m. Simula o uso real do modelo, re-treinado todo mes.
     */
    private double[] janelaExpansivel(BaseMensal base, int anoTeste, Map<YearMonth, Double> porMes) {
        int arvoresJanela = Math.max(50, arvores / 2);
        List<BaseMensal.Linha> teste = base.linhasDoAno(anoTeste);
        double[] prev = new double[teste.size()];
        int pos = 0;
        for (int mes = 1; mes <= 12; mes++) {
            YearMonth alvo = YearMonth.of(anoTeste, mes);
            List<BaseMensal.Linha> treino = base.linhasEntre(base.inicio(), alvo.minusMonths(1));
            List<BaseMensal.Linha> doMes = base.linhasEntre(alvo, alvo);
            if (treino.isEmpty() || doMes.isEmpty()) {
                pos += doMes.size();
                continue;
            }
            RandomForest rf = RandomForest.fit(Formula.lhs(SmileAdapter.ALVO), SmileAdapter.paraDataFrameReal(treino),
                    new RandomForest.Options(arvoresJanela, 0, 20, 0, 5, 1.0, SmileAdapter.sementes(arvoresJanela, semente + mes), null));
            DataFrame df = SmileAdapter.paraDataFrameReal(doMes);
            double soma = 0;
            for (int i = 0; i < doMes.size(); i++) {
                prev[pos + i] = Math.max(0, rf.predict(df.get(i)));
                soma += prev[pos + i];
            }
            porMes.put(alvo, soma);
            pos += doMes.size();
        }
        return prev;
    }

    static int indice(String variavel) {
        for (int i = 0; i < BaseMensal.VARIAVEIS.length; i++) {
            if (BaseMensal.VARIAVEIS[i].equals(variavel)) return i;
        }
        throw new IllegalArgumentException(variavel);
    }
}
