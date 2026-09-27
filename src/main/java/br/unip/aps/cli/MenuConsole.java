package br.unip.aps.cli;

import br.unip.aps.ApsException;
import br.unip.aps.analysis.Contagem;
import br.unip.aps.analysis.Estatisticas;
import br.unip.aps.app.Sessao;
import br.unip.aps.benchmark.AnaliseComplexidade;
import br.unip.aps.benchmark.BenchmarkConfig;
import br.unip.aps.benchmark.BenchmarkResult;
import br.unip.aps.benchmark.BenchmarkRunner;
import br.unip.aps.io.RelatorioCarga;
import br.unip.aps.ml.BaseMensal;
import br.unip.aps.ml.ClusterizacaoHotspots;
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
import br.unip.aps.util.Formatos;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Menu interativo em terminal. */
public final class MenuConsole {
    private static final Logger LOG = Logger.getLogger(MenuConsole.class.getName());

    private final Sessao sessao;
    private final ConsoleIO io;
    private final PrintStream out;

    public MenuConsole(Sessao sessao, PrintStream out) {
        this.sessao = sessao;
        this.out = out;
        this.io = new ConsoleIO(new Scanner(System.in, StandardCharsets.UTF_8), out);
    }

    /** Laco principal do menu. */
    public void executar() {
        cabecalho();
        tentarCarregar();
        while (true) {
            out.println();
            out.println("================ MENU PRINCIPAL ================");
            out.println(" 1) Resumo da base e qualidade dos dados");
            out.println(" 2) Ordenar e exibir focos (com contagem de operacoes)");
            out.println(" 3) Comparar todos os algoritmos (mesma entrada)");
            out.println(" 4) Benchmark completo (tempo x n, cenarios)");
            out.println(" 5) Estatisticas (2023 x 2024, biomas, municipios)");
            out.println(" 6) Machine Learning (previsao, classificacao, hotspots)");
            out.println(" 7) Exportar relatorios (CSV, Excel, PDF)");
            out.println(" 8) Gerar relatorio com as linhas de codigo");
            out.println(" 9) Recarregar dados / baixar do INPE");
            out.println(" 0) Sair");
            int op = io.inteiro("Opcao", 0, 9, 2);
            if (op == 0) {
                out.println("Ate logo!");
                return;
            }
            try {
                switch (op) {
                    case 1 -> resumo();
                    case 2 -> ordenar();
                    case 3 -> compararTodos();
                    case 4 -> benchmark();
                    case 5 -> estatisticas();
                    case 6 -> machineLearning();
                    case 7 -> exportar();
                    case 8 -> relatorioCodigo();
                    case 9 -> recarregar();
                    default -> { }
                }
            } catch (ApsException e) {
                out.println("\n[!] " + e.getMessage());
            } catch (IllegalArgumentException | UnsupportedOperationException e) {
                out.println("\n[!] Parametro invalido: " + e.getMessage());
            } catch (RuntimeException e) {
                LOG.log(Level.SEVERE, "Erro inesperado no menu", e);
                out.println("\n[!] Erro inesperado: " + e + " (detalhes em logs/aps-0.log)");
            }
        }
    }

    private void cabecalho() {
        out.println("==================================================================");
        out.println("  APS UNIP - Estrutura de Dados");
        out.println("  Analise de Performance de Algoritmos de Ordenacao");
        out.println("  Focos de queimadas detectados por satelite (INPE) - " + sessao.config().uf()
                + " " + sessao.config().anos());
        out.println("==================================================================");
    }

    private void tentarCarregar() {
        try {
            BaseDeFocos b = sessao.carregar();
            out.println("Base carregada: " + Formatos.inteiro(b.tamanho()) + " focos de " + b.getFontes().size() + " arquivo(s).");
        } catch (ApsException e) {
            out.println("[!] " + e.getMessage());
            if (io.simNao("Deseja baixar agora os dados do INPE (" + sessao.config().uf() + " " + sessao.config().anos() + ")?", true)) {
                try {
                    baixar();
                } catch (ApsException ex) {
                    out.println("[!] " + ex.getMessage());
                }
            }
        }
    }

    private void resumo() throws ApsException {
        BaseDeFocos b = sessao.exigirBase();
        Estatisticas est = new Estatisticas(b.getFocos());
        out.println("\n--- Resumo da base ---");
        out.println("Total de focos: " + Formatos.inteiro(b.tamanho()));
        est.porAno().forEach((a, n) -> out.println("  " + a + ": " + Formatos.inteiro(n)));
        out.println("Periodo: " + b.dataInicial().format(Formatos.DATA) + " a " + b.dataFinal().format(Formatos.DATA));
        out.println("Municipios: " + b.municipios().size() + " | Biomas: " + b.biomas());
        RelatorioCarga r = b.getRelatorio();
        out.println("\n--- Qualidade dos dados (limpeza) ---");
        out.print(r.resumoTexto());
        if (!r.getRejeicoes().isEmpty()) {
            out.println("Primeiras rejeicoes:");
            for (RelatorioCarga.Rejeicao x : r.getRejeicoes().subList(0, Math.min(5, r.getRejeicoes().size()))) {
                out.println("  " + x.arquivo().getFileName() + ":" + x.linha() + " -> " + x.motivo());
            }
        }
    }

    private Criterios lerCriterios() {
        List<CriterioOrdenacao> disponiveis = criteriosDisponiveis();
        List<Criterios.Nivel> niveis = new ArrayList<>();
        int qtd = io.inteiro("Quantos niveis de criterio (multicriterio: ex. bioma -> municipio -> data)", 1, 3, 1);
        for (int i = 1; i <= qtd; i++) {
            CriterioOrdenacao c = io.escolher("Criterio " + i + ":", disponiveis, i == 1 ? 0 : Math.min(i, disponiveis.size() - 1));
            Ordem o = io.escolher("Ordem:", List.of(Ordem.values()), 0);
            niveis.add(new Criterios.Nivel(c, o));
        }
        return Criterios.composto(niveis);
    }

    private List<CriterioOrdenacao> criteriosDisponiveis() {
        BaseDeFocos b = sessao.base();
        List<CriterioOrdenacao> r = new ArrayList<>();
        for (CriterioOrdenacao c : CriterioOrdenacao.values()) {
            if (!c.opcional() || (b != null && b.possuiCampo(c::valor))) r.add(c);
        }
        return r;
    }

    private void ordenar() throws ApsException {
        BaseDeFocos b = sessao.exigirBase();
        Criterios crit = lerCriterios();
        AlgoritmoTipo alg = io.escolher("Algoritmo:", List.of(AlgoritmoTipo.values()), AlgoritmoTipo.MERGE.ordinal());
        int n = io.inteiro("Tamanho da amostra (0 = base inteira)", 0, b.tamanho(), 0);
        CenarioEntrada cen = io.escolher("Disposicao inicial da entrada:", List.of(CenarioEntrada.values()), 0);
        String aviso = sessao.servicoOrdenacao().avisoDesempenho(alg, n == 0 ? b.tamanho() : n);
        if (aviso != null) {
            out.println("[aviso] " + aviso);
            if (!io.simNao("Continuar mesmo assim?", false)) return;
        }
        ResultadoOrdenacao<FocoIncendio> r = sessao.servicoOrdenacao().ordenar(b.getFocos(),
                new ServicoOrdenacao.Solicitacao(alg, crit, n, n > 0, cen, 42L));
        sessao.setUltimaOrdenacao(r);
        int linhas = io.inteiro("Quantas linhas exibir", 1, r.tamanho(), Math.min(20, r.tamanho()));
        out.println();
        TabelaConsole.focos(out, r.dados(), linhas);
        imprimirMetricas(r);
    }

    private void imprimirMetricas(ResultadoOrdenacao<FocoIncendio> r) {
        out.println("\n--- Operacoes realizadas: " + r.algoritmo() + " | " + r.criterio() + " | n = " + Formatos.inteiro(r.tamanho()) + " ---");
        out.println("  Comparacoes ........ " + Formatos.inteiro(r.metricas().comparacoes()));
        out.println("  Trocas ............. " + Formatos.inteiro(r.metricas().trocas()));
        out.println("  Atribuicoes ........ " + Formatos.inteiro(r.metricas().atribuicoes()));
        out.println("  Acessos ao array ... " + Formatos.inteiro(r.metricas().acessos()));
        out.println("  Tempo .............. " + Formatos.duracao(r.metricas().nanos()));
        out.println("  Verificacao ........ " + (r.verificado() ? "OK - resultado ordenado" : "FALHOU"));
    }

    private void compararTodos() throws ApsException {
        BaseDeFocos b = sessao.exigirBase();
        Criterios crit = lerCriterios();
        int n = io.inteiro("Tamanho da amostra (0 = base inteira)", 0, b.tamanho(), 0);
        CenarioEntrada cen = io.escolher("Disposicao inicial:", List.of(CenarioEntrada.values()), 1);
        out.println("Executando...");
        List<ResultadoOrdenacao<FocoIncendio>> rs = sessao.servicoOrdenacao().compararTodos(b.getFocos(), crit, n, cen, true);
        sessao.setUltimoComparativo(rs);
        List<String[]> linhas = new ArrayList<>();
        for (ResultadoOrdenacao<FocoIncendio> r : rs) {
            linhas.add(new String[]{r.algoritmo(), Formatos.inteiro(r.metricas().comparacoes()), Formatos.inteiro(r.metricas().trocas()),
                    Formatos.inteiro(r.metricas().atribuicoes()), Formatos.inteiro(r.metricas().acessos()),
                    Formatos.duracao(r.metricas().nanos()), r.verificado() ? "OK" : "FALHA"});
        }
        out.println("\nCriterio: " + crit + " | cenario: " + cen + " | n = " + Formatos.inteiro(rs.isEmpty() ? 0 : rs.get(0).tamanho()));
        TabelaConsole.imprimir(out, new String[]{"Algoritmo", "Comparacoes", "Trocas", "Atribuicoes", "Acessos", "Tempo", "Verif."}, linhas);
        out.println("(Tempos de uma execucao unica, sem aquecimento do JIT: use o Benchmark para medicoes rigorosas.)");
    }

    private void benchmark() throws ApsException {
        BaseDeFocos b = sessao.exigirBase();
        boolean rapido = io.simNao("Modo rapido (tamanhos ate 2.000, 3 repeticoes)?", true);
        BenchmarkConfig cfg = rapido ? BenchmarkConfig.rapido() : sessao.benchmarkConfig();
        out.println("Executando " + cfg.totalCasos() + " casos...");
        long t0 = System.nanoTime();
        List<BenchmarkResult> rs = new BenchmarkRunner().executar(b.getFocos(), cfg, p -> {
            if (p.concluidos() % 10 == 0 || p.concluidos() == p.total()) {
                out.printf("\r  %3.0f%%  %-70s", p.fracao() * 100, p.mensagem());
                out.flush();
            }
        });
        out.println("\nConcluido em " + Formatos.duracao(System.nanoTime() - t0));
        sessao.setUltimoBenchmark(rs);
        List<String[]> linhas = new ArrayList<>();
        for (AnaliseComplexidade.Estimativa e : AnaliseComplexidade.estimar(rs)) {
            linhas.add(new String[]{e.algoritmo(), e.criterio().rotulo(), e.cenario().toString(),
                    Formatos.decimal(e.expoenteTempo(), 2),
                    Double.isNaN(e.expoenteComparacoes()) ? "-" : Formatos.decimal(e.expoenteComparacoes(), 2), e.classificacao()});
        }
        out.println("\nExpoente empirico (custo ~ n^k):");
        TabelaConsole.imprimir(out, new String[]{"Algoritmo", "Criterio", "Cenario", "k tempo", "k comp.", "Classe"}, linhas);
        Path csv = sessao.exporter().exportarBenchmarkCsv(rs, sessao.arquivoRelatorio("benchmark", ".csv"));
        out.println("Resultados completos: " + csv.toAbsolutePath());
    }

    private void estatisticas() throws ApsException {
        BaseDeFocos b = sessao.exigirBase();
        Estatisticas est = new Estatisticas(b.getFocos());
        List<Integer> anos = new ArrayList<>(est.porAno().keySet());
        if (anos.size() >= 2) {
            int a1 = anos.get(anos.size() - 2), a2 = anos.get(anos.size() - 1);
            List<String[]> linhas = new ArrayList<>();
            for (Estatisticas.LinhaComparativo l : est.comparativo(a1, a2)) {
                linhas.add(new String[]{l.mes() == 0 ? "TOTAL" : Estatisticas.MESES[l.mes() - 1], Formatos.inteiro(l.anoA()),
                        Formatos.inteiro(l.anoB()), Double.isNaN(l.variacao()) ? "-" : Formatos.decimal(l.variacao(), 1) + "%"});
            }
            out.println("\nComparativo mensal " + a1 + " x " + a2 + ":");
            TabelaConsole.imprimir(out, new String[]{"Mes", String.valueOf(a1), String.valueOf(a2), "Variacao"}, linhas);
        }
        out.println("\nFocos por bioma:");
        for (Contagem c : est.porBioma()) {
            out.printf("  %-18s %8s  (%s%%)%n", c.chave(), Formatos.inteiro(c.total()), Formatos.decimal(100.0 * c.total() / est.total(), 1));
        }
        out.println("\nTop 15 municipios:");
        int i = 1;
        for (Contagem c : est.topMunicipios(15)) out.printf("  %2d. %-34s %6s%n", i++, c.chave(), Formatos.inteiro(c.total()));
        Map.Entry<java.time.YearMonth, Long> pico = est.mesPico();
        if (pico != null) out.println("\nMes com mais focos: " + pico.getKey() + " (" + Formatos.inteiro(pico.getValue()) + ")");
    }

    private void machineLearning() throws ApsException {
        BaseDeFocos b = sessao.exigirBase();
        List<Integer> anos = b.anos();
        if (anos.size() < 2) throw new ApsException("O ML precisa de dois anos (treino e teste); a base tem apenas " + anos + ".");
        int treino = anos.get(anos.size() - 2), teste = anos.get(anos.size() - 1);
        out.println("Treinando modelos (treino " + treino + ", teste " + teste + ")...");
        Preditor.ResultadoML ml = new Preditor().executar(b.getFocos(), sessao.parametrosMl(treino, teste));
        sessao.setUltimoMl(ml);
        var reg = ml.regressao();
        out.println("\nPre-processamento: base ordenada por municipio -> data com Merge Sort (" + ml.ordenacaoPreparo() + ")");
        out.println("\n[Regressao] Focos por municipio/mes - Random Forest (" + ml.parametros().arvores() + " arvores)");
        List<String[]> linhas = new ArrayList<>();
        linhas.add(linhaReg("Random Forest (treino fixo)", reg.modelo()));
        linhas.add(linhaReg("Random Forest (janela expansivel)", reg.janelaExpansivel()));
        linhas.add(linhaReg("Persistencia (lag1)", reg.persistencia()));
        linhas.add(linhaReg("Media historica", reg.mediaHistorica()));
        TabelaConsole.imprimir(out, new String[]{"Modelo", "MAE", "RMSE", "R2"}, linhas);
        out.println("\nTotal do estado por mes (" + teste + "): real | RF treino fixo | RF janela expansivel");
        reg.realPorMes().forEach((m, v) -> out.printf("  %s  %6.0f  %8.1f  %8.1f%n", m, v, reg.previstoPorMes().get(m),
                reg.previstoJanelaPorMes().getOrDefault(m, 0.0)));
        out.println("\nImportancia das variaveis (RF regressao):");
        for (int i = 0; i < BaseMensal.VARIAVEIS.length; i++) {
            out.printf("  %-12s %10.2f%n", BaseMensal.VARIAVEIS[i], reg.importancia()[i]);
        }
        var cls = ml.classificacao();
        out.printf("%n[Classificacao] Nivel de atividade: acuracia %.3f (baseline %.3f) | F1 macro %.3f (baseline %.3f)%n",
                cls.metricas().acuracia(), cls.baselineAcuracia(), cls.metricas().f1Macro(), cls.baselineF1Macro());
        out.println("\n[Clusterizacao] " + ml.dbscan().metodo() + ": " + ml.dbscan().hotspots().size()
                + " hotspots, " + ml.dbscan().ruido() + " focos isolados");
        for (ClusterizacaoHotspots.Hotspot h : ml.dbscan().hotspots().subList(0, Math.min(10, ml.dbscan().hotspots().size()))) {
            out.printf("  #%-2d %5d focos  (%.4f, %.4f)  raio %.1f km  %s / %s  %s%n", h.id(), h.focos(), h.latitude(),
                    h.longitude(), h.raioKm(), h.municipioPrincipal(), h.biomaPredominante(), h.focosPorAno());
        }
        out.println("\nConcluido em " + ml.duracaoMs() + " ms.");
    }

    private static String[] linhaReg(String nome, br.unip.aps.ml.Metricas.Regressao m) {
        return new String[]{nome, Formatos.decimal(m.mae(), 3), Formatos.decimal(m.rmse(), 3), Formatos.decimal(m.r2(), 3)};
    }

    private void exportar() throws ApsException {
        ContextoRelatorio ctx = sessao.contextoRelatorio();
        Path xlsx = sessao.exporter().exportarExcel(ctx, sessao.arquivoRelatorio("relatorio", ".xlsx"));
        Path pdf = sessao.exporter().exportarPdf(ctx, sessao.arquivoRelatorio("relatorio", ".pdf"));
        out.println("Excel: " + xlsx.toAbsolutePath());
        out.println("PDF:   " + pdf.toAbsolutePath());
        if (sessao.ultimaOrdenacao() != null) {
            Path csv = sessao.exporter().exportarFocosCsv(sessao.ultimaOrdenacao().dados(), sessao.arquivoRelatorio("dados-ordenados", ".csv"));
            out.println("CSV:   " + csv.toAbsolutePath());
        }
        out.println("(Execute ordenacao, comparativo, benchmark e ML antes para incluir essas secoes.)");
    }

    private void relatorioCodigo() throws ApsException {
        Path raiz = Path.of("").toAbsolutePath();
        long linhas = new CodigoFonteReport().gerar(raiz, sessao.config().diretorioRelatorios().resolve("codigo-fonte.pdf"),
                sessao.config().diretorioRelatorios().resolve("codigo-fonte.txt"));
        out.println("Relatorio gerado (" + Formatos.inteiro(linhas) + " linhas): "
                + sessao.config().diretorioRelatorios().toAbsolutePath().resolve("codigo-fonte.pdf"));
    }

    private void recarregar() throws ApsException {
        if (io.simNao("Baixar novamente os arquivos do INPE?", false)) baixar();
        BaseDeFocos b = sessao.carregar();
        out.println("Base recarregada: " + Formatos.inteiro(b.tamanho()) + " focos.");
    }

    private void baixar() throws ApsException {
        String uf = io.texto("UF", sessao.config().uf());
        List<Path> arquivos = sessao.baixarDoInpe(uf, sessao.config().anos());
        arquivos.forEach(a -> out.println("  baixado: " + a.toAbsolutePath()));
        BaseDeFocos b = sessao.carregar();
        out.println("Base carregada: " + Formatos.inteiro(b.tamanho()) + " focos.");
    }
}
