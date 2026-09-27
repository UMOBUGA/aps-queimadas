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

/** Previsao de focos por municipio e mes com Random Forest de regressao. */
public final class PrevisaoFocos {
    /** Resultado da previsao. */
    public record Resultado(int anoTreino, int anoTeste, int linhasTreino, int linhasTeste,
                            Metricas.Regressao modelo, Metricas.Regressao persistencia,
                            Metricas.Regressao mediaHistorica,
                            Map<YearMonth, Double> realPorMes, Map<YearMonth, Double> previstoPorMes,
                            double[] importancia, List<Contagem> maioresPrevisoes,
                            List<ErroPrevisao> maioresErros, Metricas.Regressao janelaExpansivel,
                            Map<YearMonth, Double> previstoJanelaPorMes) { }

    /** Erro de previsao de um municipio em um mes. */
    public record ErroPrevisao(String municipio, YearMonth mes, int real, double previsto) {
        public double erroAbsoluto() {
            return Math.abs(real - previsto);
        }
    }

    private final int arvores;
    private final long semente;

    public PrevisaoFocos(int arvores, long semente) {
        this.arvores = arvores;
        this.semente = semente;
    }

    /** Treina em {@code anoTreino} e avalia em {@code anoTeste}. */
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
