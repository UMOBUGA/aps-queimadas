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

/** Ponto de entrada do sistema. */
public final class Main {
    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    private Main() { }

    @SuppressWarnings("PMD.CloseResource")
    public static void main(String[] args) {
        LogConfig.inicializar();
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        String modo = args.length == 0 ? "dashboard" : args[0].toLowerCase(Textos.PT_BR).replaceFirst("^--", "");
        Map<String, String> op = opcoes(args);
        Sessao sessao = new Sessao(AppConfig.carregar());
        try {
            switch (modo) {
                case "dashboard", "gui" -> br.unip.aps.ui.DashboardApp.iniciar(args);
                case "capturas" -> {
                    System.setProperty("aps.capturas", op.getOrDefault("saida", "docs/prints"));
                    System.setProperty("aps.capturas.sair", "true");
                    br.unip.aps.ui.DashboardApp.iniciar(new String[0]);
                }
                case "cli", "menu" -> new MenuConsole(sessao, out).executar();
                case "ordenar" -> ordenar(sessao, op, out);
                case "comparar" -> comparar(sessao, op, out);
                case "benchmark" -> benchmark(sessao, op.containsKey("rapido"), out, true);
                case "ml" -> ml(sessao, out, true);
                case "resultados" -> resultados(sessao, op.containsKey("rapido"), out);
                case "relatorio-codigo" -> relatorioCodigo(sessao, out);
                case "baixar" -> baixar(sessao, op, out);
                case "estruturas" -> estruturas(sessao, op, out);
                case "historico" -> historico(sessao, op, out);
                case "ml-estudo" -> mlEstudo(op, out);
                case "manual" -> out.println("Manual: " + br.unip.aps.report.ManualPdf.gerar(Path.of("docs", "MANUAL.md"),
                        Path.of("docs", "MANUAL.pdf")).toAbsolutePath());
                case "agregados" -> out.println(br.unip.aps.app.Agregados.gerar(Path.of("data", "historico"), Path.of("data", "brasil"),
                        Path.of("src", "main", "resources", "dados")));
                case "web" -> web(sessao, op, out);
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
        out.println("Benchmark: " + cfg.totalCasos() + " casos, aquecimento de " + cfg.aquecimentos() + " execucoes"
                + (cfg.aquecimentoMs() > 0 ? " e no minimo " + cfg.aquecimentoMs() + " ms" : "") + " + "
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
        out.println("Preparo:        " + ml.municipios() + " municipios; ordenacao municipio -> data (Merge Sort): " + ml.ordenacaoPreparo());
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

    private static void web(Sessao s, Map<String, String> op, PrintStream out) throws ApsException {
        s.carregar();
        List<List<ResultadoOrdenacao<FocoIncendio>>> rs = new ArrayList<>();
        for (String c : List.of("bioma,municipio,data", "data", "hora")) {
            Map<String, String> o = new HashMap<>();
            o.put("criterios", c);
            o.put("cenario", "original");
            rs.add(comparar(s, o, out));
        }
        Path destino = Path.of(op.getOrDefault("saida", "site/app"));
        try {
            br.unip.aps.report.ExportadorWeb.Resultado r = br.unip.aps.report.ExportadorWeb.gerar(s.base(), rs, destino);
            out.println("Versao web: " + Formatos.inteiro(r.focos()) + " focos, " + Formatos.inteiro(r.municipios()) + " municipios, "
                    + r.algoritmos() + " algoritmos, " + Formatos.inteiro(r.bytes() / 1024) + " KB em " + r.pasta().toAbsolutePath());
        } catch (java.io.IOException e) {
            throw new ApsException("Não foi possível gravar a versão web em " + destino + ": " + e.getMessage(), e);
        }
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

    private static void estruturas(Sessao sessao, Map<String, String> op, PrintStream out) throws ApsException {
        try {
            executarEstruturas(sessao, op, out);
        } catch (java.io.IOException e) {
            throw new ApsException("Falha nos experimentos de estruturas: " + e.getMessage(), e);
        }
    }

    private static void executarEstruturas(Sessao sessao, Map<String, String> op, PrintStream out) throws ApsException, java.io.IOException {
        var base = sessao.carregar();
        java.util.List<java.nio.file.Path> brasil = new java.util.ArrayList<>();
        if (op.containsKey("brasil")) {
            String[] faixa = op.get("brasil").split("-");
            int de = Integer.parseInt(faixa[0].strip()), ate = Integer.parseInt(faixa[faixa.length - 1].strip());
            java.nio.file.Path dir = java.nio.file.Path.of("data", "brasil");
            br.unip.aps.io.DownloaderInpe d = new br.unip.aps.io.DownloaderInpe(sessao.config().urlInpe());
            for (int ano = de; ano <= ate; ano++) {
                java.nio.file.Path csv = dir.resolve("focos_br_ref_" + ano + ".csv");
                if (!java.nio.file.Files.isRegularFile(csv)) {
                    out.println("Baixando focos do Brasil " + ano + "...");
                    csv = d.baixarBrasil(ano, dir);
                }
                brasil.add(csv);
            }
        }
        int memoria = Integer.parseInt(op.getOrDefault("memoria", "200000"));
        java.nio.file.Path temp = java.nio.file.Files.createTempDirectory("aps-estruturas");
        if (op.containsKey("so-externo")) {
            String md = new br.unip.aps.app.ExperimentosEstruturas(s -> out.println("  " + s)).somenteExterno(brasil, memoria, temp);
            java.nio.file.Path destino = java.nio.file.Path.of("docs", "resultados", "estruturas-externo.md");
            java.nio.file.Files.writeString(destino, md, java.nio.charset.StandardCharsets.UTF_8);
            out.println(md);
            return;
        }
        String md = new br.unip.aps.app.ExperimentosEstruturas(s -> out.println("  " + s))
                .executar(base.getFocos(), brasil, memoria, temp);
        java.nio.file.Path destino = java.nio.file.Path.of("docs", "resultados", "estruturas.md");
        java.nio.file.Files.createDirectories(destino.getParent());
        java.nio.file.Files.writeString(destino, md, java.nio.charset.StandardCharsets.UTF_8);
        out.println(md);
        out.println("Gravado em " + destino.toAbsolutePath());
    }

    private static void historico(Sessao sessao, Map<String, String> op, PrintStream out) throws ApsException {
        String[] faixa = op.getOrDefault("anos", "2019-2024").split("-");
        int de = Integer.parseInt(faixa[0].strip()), ate = Integer.parseInt(faixa[faixa.length - 1].strip());
        String uf = op.getOrDefault("uf", sessao.config().uf());
        java.nio.file.Path dir = java.nio.file.Path.of("data", "historico");
        br.unip.aps.io.DownloaderInpe d = new br.unip.aps.io.DownloaderInpe(sessao.config().urlInpe());
        for (int ano = de; ano <= ate; ano++) {
            java.nio.file.Path ref = dir.resolve("focos_br_" + uf.toLowerCase(Textos.PT_BR) + "_ref_" + ano + ".csv");
            if (!java.nio.file.Files.isRegularFile(ref)) {
                out.println("Satélite de referência " + uf + " " + ano + "...");
                d.baixar(uf, ano, dir);
            }
            if (op.containsKey("meteorologia")) {
                String estado = op.getOrDefault("estado", "SÃO PAULO");
                java.nio.file.Path todos = dir.resolve("focos_" + uf.toLowerCase(Textos.PT_BR) + "_todos-sats_" + ano + ".csv");
                if (!java.nio.file.Files.isRegularFile(todos)) {
                    out.println("Todos os satélites (meteorologia) " + ano + ", filtrando " + estado + "...");
                    d.baixarTodosSatelites(ano, estado, uf, dir);
                }
            }
        }
        out.println("Histórico em " + dir.toAbsolutePath());
    }

    private static void mlEstudo(Map<String, String> op, PrintStream out) throws ApsException {
        try {
            java.nio.file.Path dir = java.nio.file.Path.of(op.getOrDefault("dir", "data/historico"));
            java.util.List<java.nio.file.Path> ref = new java.util.ArrayList<>();
            java.util.List<java.nio.file.Path> todos = new java.util.ArrayList<>();
            try (var s = java.nio.file.Files.list(dir)) {
                s.forEach(p -> {
                    String n = p.getFileName().toString();
                    if (n.contains("_ref_") && n.endsWith(".csv")) ref.add(p);
                    if (n.contains("todos-sats") && n.endsWith(".csv")) todos.add(p);
                });
            }
            if (ref.isEmpty()) throw new ApsException("Sem histórico em " + dir + ". Rode: historico --anos 2019-2024 --meteorologia");
            br.unip.aps.sorting.Ordenacoes.ordenar(ref, java.util.Comparator.comparing(p -> p.getFileName().toString()));
            var base = new br.unip.aps.io.CsvLoader().carregar(ref);
            out.println("Focos de referência: " + base.tamanho() + " (" + ref.size() + " arquivos)");
            var meteo = br.unip.aps.ml.MeteoMensal.ler(todos);
            out.println("Detecções de todos os satélites: " + meteo.linhas());
            int anoTeste = base.anos().get(base.anos().size() - 1);
            var r = new br.unip.aps.ml.EstudoPrevisao(Integer.parseInt(op.getOrDefault("arvores", "200")), 42L, s -> out.println("  " + s))
                    .executar(base.getFocos(), meteo, anoTeste);
            String md = br.unip.aps.app.RelatorioEstudoMl.markdown(r);
            java.nio.file.Path destino = java.nio.file.Path.of("docs", "resultados", "ml-estudo.md");
            java.nio.file.Files.writeString(destino, md, StandardCharsets.UTF_8);
            java.nio.file.Path bin = java.nio.file.Path.of("src", "main", "resources", "resultados", "ml-estudo.bin");
            java.nio.file.Files.createDirectories(bin.getParent());
            try (var o = new java.io.ObjectOutputStream(java.nio.file.Files.newOutputStream(bin))) {
                o.writeObject(r);
            }
            out.println(md);
            out.println("Gravado em " + destino.toAbsolutePath() + " e " + bin);
        } catch (java.io.IOException e) {
            throw new ApsException("Falha no estudo de ML: " + e.getMessage(), e);
        }
    }

    private static void ajuda(PrintStream out) {
        out.println("""
                APS Queimadas - uso: java -jar aps-queimadas-2.2.0-all.jar [modo] [opcoes]

                  (sem modo)            abre o dashboard (JavaFX)
                  cli                   menu interativo no terminal
                  ordenar               --criterios bioma,municipio,data:desc --algoritmo quick --n 0
                                        --cenario original|aleatorio|ordenado|inverso|quase --linhas 20 [--csv]
                  comparar              todos os algoritmos na mesma entrada (--criterios, --n, --cenario)
                  benchmark [--rapido]  bateria completa; grava CSV, Excel e PDF em relatorios/
                  ml                    previsao (Random Forest), classificacao e hotspots (DBSCAN/K-Means)
                  resultados [--rapido] comparativo + benchmark + ML + relatorios (numeros da dissertacao)
                  relatorio-codigo      gera relatorios/codigo-fonte.pdf (Relatorio com as linhas de codigo)
                  capturas [--saida DIR] abre o dashboard e salva prints de todas as telas (claro/escuro) em docs/prints
                  baixar                --uf SP --anos 2023,2024  (baixa do INPE para data/raw)
                  estruturas            buscas, AVL, hash, heap, memoria, Merge Sort paralelo e External Merge Sort;
                                        [--brasil 2019-2024] baixa o Brasil para data/brasil; [--memoria 200000]
                                        grava docs/resultados/estruturas.md
                  historico             --anos 2019-2024 [--meteorologia]  baixa o historico de SP para data/historico
                  ml-estudo             validacao em janelas, ablacao, intervalo conforme e permutacao com o historico;
                                        grava docs/resultados/ml-estudo.md
                  manual                gera docs/MANUAL.pdf a partir de docs/MANUAL.md
                  agregados             resume data/historico (SP 2019-2024) e data/brasil (estados) nos CSVs embarcados
                  web [--saida DIR]     gera os dados da versao web para celular (padrao: site/app)

                Algoritmos: """ + SortAlgorithmFactory.nomes());
    }
}
