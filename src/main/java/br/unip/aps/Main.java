package br.unip.aps;

import br.unip.aps.app.Sessao;
import br.unip.aps.benchmark.BenchmarkConfig;
import br.unip.aps.benchmark.BenchmarkResult;
import br.unip.aps.benchmark.BenchmarkRunner;
import br.unip.aps.cli.MenuConsole;
import br.unip.aps.cli.TabelaConsole;
import br.unip.aps.config.AppConfig;
import br.unip.aps.config.LogConfig;
import br.unip.aps.ml.Preditor;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.report.CodigoFonteReport;
import br.unip.aps.report.ContextoRelatorio;
import br.unip.aps.sorting.AlgoritmoTipo;
import br.unip.aps.sorting.CenarioEntrada;
import br.unip.aps.sorting.CriterioOrdenacao;
import br.unip.aps.sorting.Criterios;
import br.unip.aps.sorting.Ordem;
import br.unip.aps.sorting.ResultadoOrdenacao;
import br.unip.aps.sorting.ServicoOrdenacao;
import br.unip.aps.sorting.SortAlgorithmFactory;
import br.unip.aps.util.Formatos;
import br.unip.aps.util.Textos;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ponto de entrada do sistema.
 *
 * <pre>
 *   (sem argumentos)      abre o dashboard JavaFX
 *   cli                   menu interativo no terminal
 *   ordenar [opcoes]      ordena e exibe (ex.: ordenar --criterios bioma,municipio,data:desc --algoritmo quick --linhas 30)
 *   comparar [opcoes]     roda todos os algoritmos sobre a mesma entrada
 *   benchmark [--rapido]  bateria completa + CSV/Excel/PDF em relatorios/
 *   ml                    previsao, classificacao e hotspots + Excel
 *   resultados            benchmark + ML + comparativos + relatorios (tudo o que a dissertacao usa)
 *   relatorio-codigo      gera relatorios/codigo-fonte.pdf e .txt
 *   baixar [--uf SP] [--anos 2023,2024]   baixa os CSVs do INPE para data/raw
 * </pre>
 */
public final class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    private Main() { }

    /**
     * @param args modo e opcoes (ver documentacao da classe)
     */
    public static void main(String[] args) {
        LogConfig.inicializar();
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        String modo = args.length == 0 ? "dashboard" : args[0].toLowerCase(Textos.PT_BR).replaceFirst("^--", "");
        Map<String, String> op = opcoes(args);
        Sessao sessao = new Sessao(AppConfig.carregar());
        try {
            switch (modo) {
                case "dashboard", "gui" -> br.unip.aps.ui.DashboardApp.iniciar(args);
                case "cli", "menu" -> new MenuConsole(sessao, out).executar();
                case "ordenar" -> ordenar(sessao, op, out);
                case "comparar" -> comparar(sessao, op, out);
                case "benchmark" -> benchmark(sessao, op.containsKey("rapido"), out, true);
                case "ml" -> ml(sessao, out, true);
                case "resultados" -> resultados(sessao, op.containsKey("rapido"), out);
                case "relatorio-codigo" -> relatorioCodigo(sessao, out);
                case "baixar" -> baixar(sessao, op, out);
                case "help", "ajuda", "h" -> ajuda(out);
                default -> {
                    out.println("Modo desconhecido: " + args[0]);
                    ajuda(out);
                    System.exit(2);
                }
            }
        } catch (ApsException e) {
            System.err.println("[erro] " + e.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("[parametro invalido] " + e.getMessage());
            System.exit(2);
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Falha inesperada", e);
            System.err.println("[erro inesperado] " + e + " (veja logs/aps-0.log)");
            System.exit(3);
        }
    }

    private static void ordenar(Sessao s, Map<String, String> op, PrintStream out) throws ApsException {
        BaseDeFocos b = s.carregar();
        Criterios crit = criterios(op.getOrDefault("criterios", "data"));
        AlgoritmoTipo alg = SortAlgorithmFactory.tipo(op.getOrDefault("algoritmo", "merge"));
        int n = Integer.parseInt(op.getOrDefault("n", "0"));
        CenarioEntrada cen = cenario(op.getOrDefault("cenario", "original"));
        ResultadoOrdenacao<FocoIncendio> r = s.servicoOrdenacao().ordenar(b.getFocos(),
                new ServicoOrdenacao.Solicitacao(alg, crit, n, n > 0, cen, 42L));
        if (r.aviso() != null) out.println("[aviso] " + r.aviso());
        TabelaConsole.focos(out, r.dados(), Integer.parseInt(op.getOrDefault("linhas", "20")));
        out.println();
        out.println(r.resumo());
        if (op.containsKey("csv")) {
            out.println("CSV: " + s.exporter().exportarFocosCsv(r.dados(), s.arquivoRelatorio("dados-ordenados", ".csv")).toAbsolutePath());
        }
    }

    private static List<ResultadoOrdenacao<FocoIncendio>> comparar(Sessao s, Map<String, String> op, PrintStream out) throws ApsException {
        BaseDeFocos b = s.base() != null ? s.base() : s.carregar();
        Criterios crit = criterios(op.getOrDefault("criterios", "data"));
        CenarioEntrada cen = cenario(op.getOrDefault("cenario", "aleatorio"));
        List<ResultadoOrdenacao<FocoIncendio>> rs = s.servicoOrdenacao().compararTodos(b.getFocos(), crit,
                Integer.parseInt(op.getOrDefault("n", "0")), cen, true);
        List<String[]> linhas = new ArrayList<>();
        for (ResultadoOrdenacao<FocoIncendio> r : rs) {
            linhas.add(new String[]{r.algoritmo(), Formatos.inteiro(r.metricas().comparacoes()), Formatos.inteiro(r.metricas().trocas()),
                    Formatos.inteiro(r.metricas().atribuicoes()), Formatos.inteiro(r.metricas().acessos()),
                    Formatos.duracao(r.metricas().nanos()), r.verificado() ? "OK" : "FALHA"});
        }
        out.println("\nCriterio: " + crit + " | cenario: " + cen + " | n = " + (rs.isEmpty() ? 0 : rs.get(0).tamanho()));
        TabelaConsole.imprimir(out, new String[]{"Algoritmo", "Comparacoes", "Trocas", "Atribuicoes", "Acessos", "Tempo", "Verif."}, linhas);
        s.setUltimoComparativo(rs);
        return rs;
    }

    private static List<BenchmarkResult> benchmark(Sessao s, boolean rapido, PrintStream out, boolean exportar) throws ApsException {
        BaseDeFocos b = s.base() != null ? s.base() : s.carregar();
        BenchmarkConfig cfg = rapido ? BenchmarkConfig.rapido() : s.benchmarkConfig();
        out.println("Benchmark: " + cfg.totalCasos() + " casos, " + cfg.aquecimentos() + " aquecimentos + "
                + cfg.repeticoes() + " repeticoes por caso...");
        long t0 = System.nanoTime();
        List<BenchmarkResult> rs = new BenchmarkRunner().executar(b.getFocos(), cfg, p -> {
            out.printf("\r  %3.0f%%  %-80s", p.fracao() * 100, p.mensagem());
            out.flush();
        });
        out.println("\nConcluido em " + Formatos.duracao(System.nanoTime() - t0));
        s.setUltimoBenchmark(rs);
        if (exportar) {
            out.println("CSV:   " + s.exporter().exportarBenchmarkCsv(rs, s.arquivoRelatorio("benchmark", ".csv")).toAbsolutePath());
            ContextoRelatorio ctx = s.contextoRelatorio();
            out.println("Excel: " + s.exporter().exportarExcel(ctx, s.arquivoRelatorio("benchmark", ".xlsx")).toAbsolutePath());
            out.println("PDF:   " + s.exporter().exportarPdf(ctx, s.arquivoRelatorio("benchmark", ".pdf")).toAbsolutePath());
        }
        return rs;
    }

    private static void ml(Sessao s, PrintStream out, boolean exportar) throws ApsException {
        BaseDeFocos b = s.base() != null ? s.base() : s.carregar();
        List<Integer> anos = b.anos();
        if (anos.size() < 2) throw new ApsException("O ML precisa de dois anos na base (treino e teste).");
        Preditor.ResultadoML ml = new Preditor().executar(b.getFocos(),
                s.parametrosMl(anos.get(anos.size() - 2), anos.get(anos.size() - 1)));
        s.setUltimoMl(ml);
        var reg = ml.regressao();
        out.printf("Regressao RF:   MAE=%.3f RMSE=%.3f R2=%.3f%n", reg.modelo().mae(), reg.modelo().rmse(), reg.modelo().r2());
        out.printf("RF janela exp.: MAE=%.3f RMSE=%.3f R2=%.3f%n", reg.janelaExpansivel().mae(), reg.janelaExpansivel().rmse(), reg.janelaExpansivel().r2());
        out.printf("Persistencia:   MAE=%.3f RMSE=%.3f R2=%.3f%n", reg.persistencia().mae(), reg.persistencia().rmse(), reg.persistencia().r2());
        out.printf("Media hist.:    MAE=%.3f RMSE=%.3f R2=%.3f%n", reg.mediaHistorica().mae(), reg.mediaHistorica().rmse(), reg.mediaHistorica().r2());
        var cls = ml.classificacao();
        out.printf("Classificacao:  acuracia=%.3f (baseline %.3f)  F1 macro=%.3f (baseline %.3f)%n",
                cls.metricas().acuracia(), cls.baselineAcuracia(), cls.metricas().f1Macro(), cls.baselineF1Macro());
        out.println("Hotspots:       " + ml.dbscan().metodo() + " -> " + ml.dbscan().hotspots().size() + " grupos, "
                + ml.dbscan().ruido() + " focos isolados; " + ml.kMeans().metodo());
        if (exportar) {
            out.println("Excel: " + s.exporter().exportarExcel(s.contextoRelatorio(), s.arquivoRelatorio("ml", ".xlsx")).toAbsolutePath());
        }
    }

    private static void resultados(Sessao s, boolean rapido, PrintStream out) throws ApsException {
        s.carregar();
        Map<String, String> op = new HashMap<>();
        op.put("criterios", "bioma,municipio,data");
        op.put("cenario", "original");
        comparar(s, op, out);
        benchmark(s, rapido, out, false);
        ml(s, out, false);
        ContextoRelatorio ctx = s.contextoRelatorio();
        out.println("Benchmark CSV: " + s.exporter().exportarBenchmarkCsv(s.ultimoBenchmark(), s.arquivoRelatorio("benchmark", ".csv")).toAbsolutePath());
        out.println("Excel:         " + s.exporter().exportarExcel(ctx, s.arquivoRelatorio("resultados", ".xlsx")).toAbsolutePath());
        out.println("PDF:           " + s.exporter().exportarPdf(ctx, s.arquivoRelatorio("resultados", ".pdf")).toAbsolutePath());
    }

    private static void relatorioCodigo(Sessao s, PrintStream out) throws ApsException {
        Path dir = s.config().diretorioRelatorios();
        long n = new CodigoFonteReport().gerar(Path.of("").toAbsolutePath(), dir.resolve("codigo-fonte.pdf"), dir.resolve("codigo-fonte.txt"));
        out.println("Relatorio com " + Formatos.inteiro(n) + " linhas: " + dir.resolve("codigo-fonte.pdf").toAbsolutePath());
    }

    private static void baixar(Sessao s, Map<String, String> op, PrintStream out) throws ApsException {
        String uf = op.getOrDefault("uf", s.config().uf());
        List<Integer> anos = new ArrayList<>();
        for (String a : op.getOrDefault("anos", "").split(",")) if (!a.isBlank()) anos.add(Integer.parseInt(a.strip()));
        if (anos.isEmpty()) anos = s.config().anos();
        for (Path p : s.baixarDoInpe(uf, anos)) out.println("Baixado: " + p.toAbsolutePath());
    }

    /**
     * Converte "bioma,municipio:asc,data:desc" em criterio composto.
     *
     * @param texto lista de criterios
     * @return criterio composto
     */
    static Criterios criterios(String texto) {
        List<Criterios.Nivel> niveis = new ArrayList<>();
        for (String parte : texto.split(",")) {
            if (parte.isBlank()) continue;
            String[] kv = parte.strip().split(":");
            CriterioOrdenacao c = criterio(kv[0]);
            Ordem o = kv.length > 1 && Textos.semAcentos(kv[1]).startsWith("d") ? Ordem.DECRESCENTE : Ordem.CRESCENTE;
            niveis.add(new Criterios.Nivel(c, o));
        }
        return Criterios.composto(niveis);
    }

    private static CriterioOrdenacao criterio(String nome) {
        String alvo = Textos.semAcentos(nome).replaceAll("[^a-z0-9]", "");
        for (CriterioOrdenacao c : CriterioOrdenacao.values()) {
            String n1 = Textos.semAcentos(c.name()).replace("_", "");
            String n2 = Textos.semAcentos(c.rotulo()).replaceAll("[^a-z0-9]", "");
            if (n1.equals(alvo) || n2.startsWith(alvo)) return c;
        }
        throw new IllegalArgumentException("Criterio desconhecido: " + nome + ". Use: data, bioma, municipio, latitude, longitude, id...");
    }

    private static CenarioEntrada cenario(String nome) {
        String alvo = Textos.semAcentos(nome).replaceAll("[^a-z]", "");
        for (CenarioEntrada c : CenarioEntrada.values()) {
            if (Textos.semAcentos(c.name()).replace("_", "").startsWith(alvo)) return c;
        }
        throw new IllegalArgumentException("Cenario desconhecido: " + nome + ". Use: original, aleatorio, ordenado, inverso, quase");
    }

    private static Map<String, String> opcoes(String[] args) {
        Map<String, String> m = new HashMap<>();
        for (int i = 1; i < args.length; i++) {
            if (!args[i].startsWith("--")) continue;
            String k = args[i].substring(2).toLowerCase(Textos.PT_BR);
            String v = i + 1 < args.length && !args[i + 1].startsWith("--") ? args[++i] : "true";
            m.put(k, v);
        }
        return m;
    }

    private static void ajuda(PrintStream out) {
        out.println("""
                APS Queimadas - uso: java -jar aps-queimadas-1.0.0-all.jar [modo] [opcoes]

                  (sem modo)            abre o dashboard (JavaFX)
                  cli                   menu interativo no terminal
                  ordenar               --criterios bioma,municipio,data:desc --algoritmo quick --n 0
                                        --cenario original|aleatorio|ordenado|inverso|quase --linhas 20 [--csv]
                  comparar              todos os algoritmos na mesma entrada (--criterios, --n, --cenario)
                  benchmark [--rapido]  bateria completa; grava CSV, Excel e PDF em relatorios/
                  ml                    previsao (Random Forest), classificacao e hotspots (DBSCAN/K-Means)
                  resultados [--rapido] comparativo + benchmark + ML + relatorios (numeros da dissertacao)
                  relatorio-codigo      gera relatorios/codigo-fonte.pdf (Relatorio com as linhas de codigo)
                  baixar                --uf SP --anos 2023,2024  (baixa do INPE para data/raw)

                Algoritmos: """ + SortAlgorithmFactory.nomes());
    }
}
