package br.unip.aps.ml;

import br.unip.aps.geo.MalhaMunicipal;
import br.unip.aps.geo.Vizinhanca;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.sorting.OperationMetrics;
import br.unip.aps.sorting.Ordenacoes;
import br.unip.aps.sorting.algorithms.MergeSort;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.vector.DoubleVector;
import smile.data.vector.ValueVector;
import smile.regression.RandomForest;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

/** Estudo de previsao com historico longo: validacao em janelas, ablacao, intervalos conformes e estudo de caso. */
public final class EstudoPrevisao {
    /** Grupos de variaveis (a ablacao os acumula na ordem). */
    public enum Grupo {
        BASE("Base (versão anterior)", new String[]{"mes_sin", "mes_cos", "lat", "lon", "frac_cerrado", "lag1", "lag2", "lag3", "media_hist", "estado_lag1"}),
        SAZONAL("+ ano anterior", new String[]{"lag12", "media_12m"}),
        VIZINHOS("+ municípios vizinhos", new String[]{"viz_lag1", "viz_lag12"}),
        METEO("+ meteorologia (mês anterior)", new String[]{"risco_lag1", "dias_sem_chuva_lag1", "precipitacao_lag1", "frp_lag1", "todos_sats_lag1", "estado_risco_lag1"}),
        EXPLICATIVO("Explicativo: clima do mês", new String[]{"risco_mes", "dias_sem_chuva_mes", "precipitacao_mes", "estado_dias_sem_chuva_mes"});

        private final String rotulo;
        private final String[] variaveis;

        Grupo(String rotulo, String[] variaveis) {
            this.rotulo = rotulo;
            this.variaveis = variaveis;
        }

        public String rotulo() { return rotulo; }
        public String[] variaveis() { return variaveis.clone(); }
    }

    /** Metricas de um modelo ou baseline num ano de teste. */
    public record Linha(String nome, double mae, double rmse, double r2) implements Serializable { }

    /** Uma janela da validacao temporal (rolling origin). */
    public record Janela(int anoTeste, String treino, int linhasTreino, int linhasTeste, Linha modelo, Linha persistencia,
                         Linha mediaHistorica, Linha sazonal) implements Serializable { }

    /** Importancia por permutacao: aumento do MAE ao embaralhar a variavel. */
    public record Importancia(String variavel, double aumentoMae) implements Serializable { }

    /** Intervalo de previsao conforme (split conformal). */
    public record Intervalo(double quantil, double meiaLargura, double coberturaEsperada, double coberturaCalibracao,
                            double coberturaTeste, int anoCalibracao, int anoTeste) implements Serializable { }

    /** Estudo de caso de um mes. */
    public record Caso(YearMonth mes, long real, double previsto, long maiorMesTreino, YearMonth maiorMesTreinoData,
                       int maiorMunicipioMesTreino, int maiorMunicipioMesCaso, String municipioCaso,
                       List<long[]> diario, double[] mediaDiariaAnteriores, double diasSemChuva, double diasSemChuvaAnteriores,
                       double risco, double riscoAnteriores, List<String[]> municipios) implements Serializable { }

    /** Resultado completo. */
    public record Resultado(int anoInicio, int anoFim, int municipios, long focos, long deteccoesTodosSatelites,
                            int vizinhosPorFronteira, int vizinhosPorProximidade, double mediaVizinhos,
                            OperationMetrics ordenacaoPreparo, List<Janela> janelas, List<Linha> ablacao,
                            List<Importancia> importancia, Intervalo intervalo, Caso caso, Map<YearMonth, double[]> serie,
                            String[] variaveis, int arvores, long duracaoMs) implements Serializable { }

    private final int arvores;
    private final long semente;
    private final Consumer<String> log;

    private String[] nomes;
    private List<String> municipios;
    private YearMonth inicio;
    private int nMeses;
    private int[][] contagem;
    private double[] lat, lon, cerrado;
    private long[] estadoMes;
    private int[][] vizinhosIdx;
    private MeteoMensal meteo;
    private OperationMetrics ordenacao;

    public EstudoPrevisao(int arvores, long semente, Consumer<String> log) {
        this.arvores = arvores;
        this.semente = semente;
        this.log = log == null ? s -> { } : log;
    }

    /** Executa o estudo com os focos do satelite de referencia (varios anos) e, se houver, a meteorologia. */
    public Resultado executar(List<FocoIncendio> focos, MeteoMensal meteo, int anoTeste) {
        long t0 = System.nanoTime();
        this.meteo = meteo == null ? new MeteoMensal() : meteo;
        montarBase(focos);
        Vizinhanca viz = Vizinhanca.da(MalhaMunicipal.sp());
        ligarVizinhos(viz);
        List<Grupo> grupos = new ArrayList<>(List.of(Grupo.BASE, Grupo.SAZONAL, Grupo.VIZINHOS, Grupo.METEO));
        if (this.meteo.vazio()) grupos.remove(Grupo.METEO);
        nomes = nomesDe(grupos);
        int primeiroAno = inicio.getYear();

        List<Janela> janelas = new ArrayList<>();
        for (int teste = primeiroAno + 2; teste <= anoTeste; teste++) {
            log.accept("Validação em janelas: teste " + teste);
            janelas.add(janela(primeiroAno + 1, teste, nomes));
        }

        List<Linha> ablacao = new ArrayList<>();
        List<Grupo> acumulado = new ArrayList<>();
        for (Grupo g : grupos) {
            acumulado.add(g);
            log.accept("Ablação: " + g.rotulo());
            String[] vars = nomesDe(acumulado);
            Modelo m = treinar(primeiroAno + 1, anoTeste - 1, vars);
            ablacao.add(avaliar(m, anoTeste, vars, g.rotulo()));
        }
        if (!this.meteo.vazio()) {
            acumulado.add(Grupo.EXPLICATIVO);
            log.accept("Ablação: " + Grupo.EXPLICATIVO.rotulo());
            String[] vars = nomesDe(acumulado);
            ablacao.add(avaliar(treinar(primeiroAno + 1, anoTeste - 1, vars), anoTeste, vars, Grupo.EXPLICATIVO.rotulo()));
        }

        log.accept("Intervalos conformes");
        Intervalo intervalo = intervalo(primeiroAno + 1, anoTeste, nomes);
        log.accept("Importância por permutação");
        Modelo finalM = treinar(primeiroAno + 1, anoTeste - 1, nomes);
        List<Importancia> imp = permutacao(finalM, anoTeste, nomes);
        log.accept("Estudo de caso");
        Caso caso = caso(focos, finalM, YearMonth.of(anoTeste, 8), nomes);

        Map<YearMonth, double[]> serie = new LinkedHashMap<>();
        double[] prev = prever(finalM, linhas(YearMonth.of(anoTeste, 1), YearMonth.of(anoTeste, 12), nomes).x);
        int pos = 0;
        for (int mes = 1; mes <= 12; mes++) {
            YearMonth ym = YearMonth.of(anoTeste, mes);
            double soma = 0;
            for (int i = 0; i < municipios.size(); i++) soma += prev[pos++];
            serie.put(ym, new double[]{estadoMes[indice(ym)], soma});
        }
        return new Resultado(primeiroAno, anoTeste, municipios.size(), focos.size(), this.meteo.linhas(),
                viz.municipiosComFronteira(), viz.municipiosPorProximidade(), viz.mediaVizinhos(), ordenacao, janelas, ablacao, imp,
                intervalo, caso, serie, nomes, arvores, (System.nanoTime() - t0) / 1_000_000);
    }

    private void montarBase(List<FocoIncendio> focos) {
        FocoIncendio[] ord = focos.toArray(new FocoIncendio[0]);
        Comparator<FocoIncendio> porMunicipioData = Comparator.comparing((FocoIncendio f) -> MalhaMunicipal.chave(f.getMunicipio()))
                .thenComparing(FocoIncendio::getDataHora);
        ordenacao = new MergeSort().ordenar(ord, porMunicipioData);
        YearMonth min = null, max = null;
        for (FocoIncendio f : ord) {
            YearMonth ym = YearMonth.from(f.getDataHora());
            if (min == null || ym.isBefore(min)) min = ym;
            if (max == null || ym.isAfter(max)) max = ym;
        }
        inicio = YearMonth.of(min.getYear(), 1);
        nMeses = (int) inicio.until(YearMonth.of(max.getYear(), 12), ChronoUnit.MONTHS) + 1;
        municipios = new ArrayList<>();
        List<int[]> cont = new ArrayList<>();
        List<double[]> geo = new ArrayList<>();
        estadoMes = new long[nMeses];
        String atual = null;
        int[] linha = null;
        double[] g = null;
        for (FocoIncendio f : ord) {
            String chave = MalhaMunicipal.chave(f.getMunicipio());
            if (!chave.equals(atual)) {
                atual = chave;
                municipios.add(chave);
                linha = new int[nMeses];
                g = new double[4];
                cont.add(linha);
                geo.add(g);
            }
            int t = indice(YearMonth.from(f.getDataHora()));
            linha[t]++;
            estadoMes[t]++;
            g[0] += f.getLatitude();
            g[1] += f.getLongitude();
            g[2]++;
            if (f.getBioma().toLowerCase(java.util.Locale.ROOT).startsWith("cerrado")) g[3]++;
        }
        contagem = cont.toArray(new int[0][]);
        lat = new double[municipios.size()];
        lon = new double[municipios.size()];
        cerrado = new double[municipios.size()];
        for (int i = 0; i < municipios.size(); i++) {
            double[] v = geo.get(i);
            lat[i] = v[0] / v[2];
            lon[i] = v[1] / v[2];
            cerrado[i] = v[3] / v[2];
        }
    }

    private void ligarVizinhos(Vizinhanca viz) {
        Map<String, Integer> pos = new HashMap<>();
        for (int i = 0; i < municipios.size(); i++) pos.put(municipios.get(i), i);
        vizinhosIdx = new int[municipios.size()][];
        for (int i = 0; i < municipios.size(); i++) {
            List<Integer> r = new ArrayList<>();
            for (String v : viz.de(municipios.get(i))) {
                Integer j = pos.get(v);
                if (j != null) r.add(j);
            }
            vizinhosIdx[i] = r.stream().mapToInt(Integer::intValue).toArray();
        }
    }

    private int indice(YearMonth ym) {
        return (int) inicio.until(ym, ChronoUnit.MONTHS);
    }

    private static String[] nomesDe(List<Grupo> grupos) {
        List<String> r = new ArrayList<>();
        for (Grupo g : grupos) r.addAll(List.of(g.variaveis));
        return r.toArray(new String[0]);
    }

    private double variavel(String nome, int m, int t) {
        YearMonth ym = inicio.plusMonths(t);
        YearMonth ant = ym.minusMonths(1);
        String chave = municipios.get(m);
        return switch (nome) {
            case "mes_sin" -> Math.sin(2 * Math.PI * (ym.getMonthValue() - 1) / 12.0);
            case "mes_cos" -> Math.cos(2 * Math.PI * (ym.getMonthValue() - 1) / 12.0);
            case "lat" -> lat[m];
            case "lon" -> lon[m];
            case "frac_cerrado" -> cerrado[m];
            case "lag1" -> lag(m, t, 1);
            case "lag2" -> lag(m, t, 2);
            case "lag3" -> lag(m, t, 3);
            case "lag12" -> lag(m, t, 12);
            case "media_hist" -> media(m, 0, t);
            case "media_12m" -> media(m, Math.max(0, t - 12), t);
            case "estado_lag1" -> t >= 1 ? estadoMes[t - 1] : 0;
            case "viz_lag1" -> somaVizinhos(m, t - 1);
            case "viz_lag12" -> somaVizinhos(m, t - 12);
            case "risco_lag1" -> meteo.media(chave, ant, MeteoMensal.RISCO);
            case "dias_sem_chuva_lag1" -> meteo.media(chave, ant, MeteoMensal.DIAS_SEM_CHUVA);
            case "precipitacao_lag1" -> meteo.media(chave, ant, MeteoMensal.PRECIPITACAO);
            case "frp_lag1" -> meteo.media(chave, ant, MeteoMensal.FRP);
            case "todos_sats_lag1" -> meteo.media(chave, ant, MeteoMensal.FOCOS);
            case "estado_risco_lag1" -> meteo.mediaEstado(ant, MeteoMensal.RISCO);
            case "risco_mes" -> meteo.media(chave, ym, MeteoMensal.RISCO);
            case "dias_sem_chuva_mes" -> meteo.media(chave, ym, MeteoMensal.DIAS_SEM_CHUVA);
            case "precipitacao_mes" -> meteo.media(chave, ym, MeteoMensal.PRECIPITACAO);
            case "estado_dias_sem_chuva_mes" -> meteo.mediaEstado(ym, MeteoMensal.DIAS_SEM_CHUVA);
            default -> throw new IllegalArgumentException(nome);
        };
    }

    private int lag(int m, int t, int k) {
        return t - k >= 0 ? contagem[m][t - k] : 0;
    }

    private double media(int m, int de, int ate) {
        if (ate <= de) return 0;
        double s = 0;
        for (int k = de; k < ate; k++) s += contagem[m][k];
        return s / (ate - de);
    }

    private double somaVizinhos(int m, int t) {
        if (t < 0) return 0;
        double s = 0;
        for (int j : vizinhosIdx[m]) s += contagem[j][t];
        return s;
    }

    private record Tabela(double[][] x, double[] y, int[] municipio, int[] tempo) { }

    private Tabela linhas(YearMonth de, YearMonth ate, String[] vars) {
        int t0 = Math.max(0, indice(de)), t1 = Math.min(nMeses - 1, indice(ate));
        int n = Math.max(0, t1 - t0 + 1) * municipios.size();
        double[][] x = new double[n][vars.length];
        double[] y = new double[n];
        int[] mun = new int[n], tempo = new int[n];
        int i = 0;
        for (int t = t0; t <= t1; t++) {
            for (int m = 0; m < municipios.size(); m++) {
                for (int j = 0; j < vars.length; j++) x[i][j] = variavel(vars[j], m, t);
                y[i] = contagem[m][t];
                mun[i] = m;
                tempo[i] = t;
                i++;
            }
        }
        return new Tabela(x, y, mun, tempo);
    }

    private static DataFrame dataFrame(double[][] x, double[] y, String[] vars) {
        ValueVector[] col = new ValueVector[vars.length + 1];
        for (int j = 0; j < vars.length; j++) {
            double[] v = new double[x.length];
            for (int i = 0; i < x.length; i++) v[i] = x[i][j];
            col[j] = new DoubleVector(vars[j], v);
        }
        col[vars.length] = new DoubleVector(SmileAdapter.ALVO, y);
        return new DataFrame(col);
    }

    private record Modelo(RandomForest rf, String[] vars, int linhas) { }

    private Modelo treinar(int anoDe, int anoAte, String[] vars) {
        Tabela t = linhas(YearMonth.of(anoDe, 1), YearMonth.of(anoAte, 12), vars);
        RandomForest rf = RandomForest.fit(Formula.lhs(SmileAdapter.ALVO), dataFrame(t.x, t.y, vars),
                new RandomForest.Options(arvores, 0, 20, 0, 5, 1.0, SmileAdapter.sementes(arvores, semente), null));
        return new Modelo(rf, vars, t.y.length);
    }

    private static double[] prever(Modelo m, double[][] x) {
        DataFrame df = dataFrame(x, new double[x.length], m.vars);
        double[] r = new double[x.length];
        for (int i = 0; i < x.length; i++) r[i] = Math.max(0, m.rf.predict(df.get(i)));
        return r;
    }

    private Linha avaliar(Modelo m, int ano, String[] vars, String nome) {
        Tabela t = linhas(YearMonth.of(ano, 1), YearMonth.of(ano, 12), vars);
        Metricas.Regressao r = Metricas.regressao(t.y, prever(m, t.x));
        return new Linha(nome, r.mae(), r.rmse(), r.r2());
    }

    private Janela janela(int anoInicioTreino, int anoTeste, String[] vars) {
        Modelo m = treinar(anoInicioTreino, anoTeste - 1, vars);
        Tabela t = linhas(YearMonth.of(anoTeste, 1), YearMonth.of(anoTeste, 12), vars);
        double[] prev = prever(m, t.x);
        double[] pers = new double[t.y.length], hist = new double[t.y.length], saz = new double[t.y.length];
        for (int i = 0; i < t.y.length; i++) {
            pers[i] = lag(t.municipio[i], t.tempo[i], 1);
            hist[i] = media(t.municipio[i], 0, t.tempo[i]);
            saz[i] = lag(t.municipio[i], t.tempo[i], 12);
        }
        return new Janela(anoTeste, anoInicioTreino + "–" + (anoTeste - 1), m.linhas, t.y.length,
                linha("Random Forest", t.y, prev), linha("Persistência", t.y, pers),
                linha("Média histórica", t.y, hist), linha("Sazonal ingênuo", t.y, saz));
    }

    private static Linha linha(String nome, double[] real, double[] prev) {
        Metricas.Regressao r = Metricas.regressao(real, prev);
        return new Linha(nome, r.mae(), r.rmse(), r.r2());
    }

    private Intervalo intervalo(int anoInicioTreino, int anoTeste, String[] vars) {
        int anoCal = anoTeste - 1;
        Modelo calib = treinar(anoInicioTreino, anoCal - 1, vars);
        Tabela tc = linhas(YearMonth.of(anoCal, 1), YearMonth.of(anoCal, 12), vars);
        double[] pc = prever(calib, tc.x);
        List<Double> residuos = new ArrayList<>(pc.length);
        for (int i = 0; i < pc.length; i++) residuos.add(Math.abs(tc.y[i] - pc[i]));
        Ordenacoes.ordenar(residuos, Comparator.naturalOrder());
        double alvo = 0.9;
        int k = (int) Math.ceil((residuos.size() + 1) * alvo) - 1;
        double q = residuos.get(Math.min(residuos.size() - 1, Math.max(0, k)));
        int dentroCal = 0;
        for (double r : residuos) if (r <= q) dentroCal++;
        Modelo finalM = treinar(anoInicioTreino, anoTeste - 1, vars);
        Tabela tt = linhas(YearMonth.of(anoTeste, 1), YearMonth.of(anoTeste, 12), vars);
        double[] pt = prever(finalM, tt.x);
        int dentro = 0;
        for (int i = 0; i < pt.length; i++) if (Math.abs(tt.y[i] - pt[i]) <= q) dentro++;
        return new Intervalo(alvo, q, alvo, (double) dentroCal / residuos.size(), (double) dentro / pt.length, anoCal, anoTeste);
    }

    private List<Importancia> permutacao(Modelo m, int ano, String[] vars) {
        Tabela t = linhas(YearMonth.of(ano, 1), YearMonth.of(ano, 12), vars);
        double base = Metricas.regressao(t.y, prever(m, t.x)).mae();
        List<Importancia> r = new ArrayList<>();
        for (int j = 0; j < vars.length; j++) {
            double soma = 0;
            int repeticoes = 3;
            for (int rep = 0; rep < repeticoes; rep++) {
                double[][] x = new double[t.x.length][];
                for (int i = 0; i < x.length; i++) x[i] = t.x[i].clone();
                Random rnd = new Random(semente + 31L * j + rep);
                for (int i = x.length - 1; i > 0; i--) {
                    int k = rnd.nextInt(i + 1);
                    double tmp = x[i][j];
                    x[i][j] = x[k][j];
                    x[k][j] = tmp;
                }
                soma += Metricas.regressao(t.y, prever(m, x)).mae() - base;
            }
            r.add(new Importancia(vars[j], soma / repeticoes));
        }
        Ordenacoes.ordenar(r, (a, b) -> Double.compare(b.aumentoMae(), a.aumentoMae()));
        return r;
    }

    private Caso caso(List<FocoIncendio> focos, Modelo m, YearMonth mes, String[] vars) {
        int t = indice(mes);
        Tabela tt = linhas(mes, mes, vars);
        double[] p = prever(m, tt.x);
        double previsto = 0;
        for (double v : p) previsto += v;
        long maiorMes = 0;
        YearMonth maiorMesData = null;
        int maiorMun = 0;
        for (int k = 12; k < t - (mes.getMonthValue() - 1); k++) {
            if (estadoMes[k] > maiorMes) {
                maiorMes = estadoMes[k];
                maiorMesData = inicio.plusMonths(k);
            }
            for (int[] c : contagem) maiorMun = Math.max(maiorMun, c[k]);
        }
        int maiorCaso = 0;
        String munCaso = "";
        for (int i = 0; i < contagem.length; i++) {
            if (contagem[i][t] > maiorCaso) {
                maiorCaso = contagem[i][t];
                munCaso = municipios.get(i);
            }
        }
        int dias = mes.lengthOfMonth();
        long[] doMes = new long[dias];
        double[] anteriores = new double[dias];
        int anosAnteriores = mes.getYear() - inicio.getYear();
        for (FocoIncendio f : focos) {
            LocalDate d = f.getData();
            if (d.getMonthValue() != mes.getMonthValue() || d.getDayOfMonth() > dias) continue;
            if (d.getYear() == mes.getYear()) doMes[d.getDayOfMonth() - 1]++;
            else if (d.getYear() < mes.getYear()) anteriores[d.getDayOfMonth() - 1] += 1.0 / anosAnteriores;
        }
        List<long[]> diario = new ArrayList<>();
        for (int d = 0; d < dias; d++) diario.add(new long[]{d + 1, doMes[d]});
        double dsc = meteo.mediaEstado(mes, MeteoMensal.DIAS_SEM_CHUVA), risco = meteo.mediaEstado(mes, MeteoMensal.RISCO);
        double dscAnt = 0, riscoAnt = 0;
        int n = 0;
        for (int ano = inicio.getYear(); ano < mes.getYear(); ano++) {
            YearMonth ym = YearMonth.of(ano, mes.getMonthValue());
            dscAnt += meteo.mediaEstado(ym, MeteoMensal.DIAS_SEM_CHUVA);
            riscoAnt += meteo.mediaEstado(ym, MeteoMensal.RISCO);
            n++;
        }
        List<String[]> mun = new ArrayList<>();
        List<Integer> ordem = new ArrayList<>();
        for (int i = 0; i < contagem.length; i++) ordem.add(i);
        Ordenacoes.ordenar(ordem, (a, b) -> Integer.compare(contagem[b][t], contagem[a][t]));
        for (int k = 0; k < Math.min(8, ordem.size()); k++) {
            int i = ordem.get(k);
            mun.add(new String[]{municipios.get(i), String.valueOf(contagem[i][t]), String.format(java.util.Locale.ROOT, "%.1f", p[i]),
                    String.valueOf(lag(i, t, 12))});
        }
        return new Caso(mes, estadoMes[t], previsto, maiorMes, maiorMesData, maiorMun, maiorCaso, munCaso, diario, anteriores,
                dsc, n == 0 ? 0 : dscAnt / n, risco, n == 0 ? 0 : riscoAnt / n, mun);
    }
}
