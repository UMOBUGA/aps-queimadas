package br.unip.aps.app;

import br.unip.aps.ApsException;
import br.unip.aps.benchmark.BenchmarkConfig;
import br.unip.aps.benchmark.BenchmarkResult;
import br.unip.aps.config.AppConfig;
import br.unip.aps.io.DataValidationException;
import br.unip.aps.io.DownloaderInpe;
import br.unip.aps.io.RepositorioDados;
import br.unip.aps.ml.Preditor;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.report.ContextoRelatorio;
import br.unip.aps.report.ReportExporter;
import br.unip.aps.sorting.AlgoritmoTipo;
import br.unip.aps.sorting.CenarioEntrada;
import br.unip.aps.sorting.CriterioOrdenacao;
import br.unip.aps.sorting.ResultadoOrdenacao;
import br.unip.aps.sorting.ServicoOrdenacao;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Estado da aplicacao compartilhado entre o menu de console e o dashboard (camada de aplicacao):
 * configuracao, base carregada, servicos e os ultimos resultados produzidos, que alimentam os
 * relatorios. Nao e thread-safe; o dashboard so a altera na thread da interface.
 */
public final class Sessao {

    private static final DateTimeFormatter CARIMBO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final AppConfig config;
    private final RepositorioDados repositorio;
    private final ServicoOrdenacao servicoOrdenacao;
    private final ReportExporter exporter = new ReportExporter();

    private BaseDeFocos base;
    private ResultadoOrdenacao<FocoIncendio> ultimaOrdenacao;
    private List<ResultadoOrdenacao<FocoIncendio>> ultimoComparativo;
    private List<BenchmarkResult> ultimoBenchmark;
    private Preditor.ResultadoML ultimoMl;

    /** @param config configuracao carregada */
    public Sessao(AppConfig config) {
        this.config = config;
        this.repositorio = new RepositorioDados(config.diretorioDados());
        this.servicoOrdenacao = new ServicoOrdenacao(config.limiteQuadratico());
    }

    /**
     * Carrega todos os CSVs da pasta de dados.
     *
     * @return base carregada
     * @throws DataValidationException se nao houver arquivos validos
     */
    public BaseDeFocos carregar() throws DataValidationException {
        base = repositorio.carregarTodos();
        limparResultados();
        return base;
    }

    /**
     * Carrega CSVs escolhidos pelo usuario.
     *
     * @param arquivos arquivos
     * @return base carregada
     * @throws DataValidationException se a carga falhar
     */
    public BaseDeFocos carregar(List<Path> arquivos) throws DataValidationException {
        base = repositorio.carregar(arquivos);
        limparResultados();
        return base;
    }

    /**
     * Baixa os arquivos da UF/anos configurados para a pasta de dados.
     *
     * @param uf   sigla
     * @param anos anos
     * @return CSVs baixados
     * @throws ApsException se algum download falhar
     */
    public List<Path> baixarDoInpe(String uf, List<Integer> anos) throws ApsException {
        DownloaderInpe d = new DownloaderInpe(config.urlInpe());
        List<Path> r = new ArrayList<>();
        for (int ano : anos) r.add(d.baixar(uf, ano, config.diretorioDados()));
        return r;
    }

    private void limparResultados() {
        ultimaOrdenacao = null;
        ultimoComparativo = null;
        ultimoBenchmark = null;
        ultimoMl = null;
    }

    /**
     * @return base carregada
     * @throws ApsException se nenhuma base foi carregada ainda
     */
    public BaseDeFocos exigirBase() throws ApsException {
        if (base == null) throw new ApsException("Nenhum dado carregado. Carregue os CSVs do INPE primeiro.");
        return base;
    }

    /** @return configuracao de benchmark a partir de application.properties */
    public BenchmarkConfig benchmarkConfig() {
        return new BenchmarkConfig(List.of(AlgoritmoTipo.values()),
                List.of(CriterioOrdenacao.DATA, CriterioOrdenacao.BIOMA, CriterioOrdenacao.MUNICIPIO),
                config.inteiros("aps.benchmark.tamanhos", List.of(100, 1_000, 5_000, 10_000, 0)),
                List.of(CenarioEntrada.ALEATORIO, CenarioEntrada.ORDENADO, CenarioEntrada.INVERSO),
                config.inteiro("aps.benchmark.aquecimentos", 2), config.inteiro("aps.benchmark.repeticoes", 5),
                config.inteiro("aps.benchmark.semente", 42), Integer.MAX_VALUE);
    }

    /**
     * @param anoTreino ano de treino
     * @param anoTeste  ano de teste
     * @return parametros de ML a partir de application.properties
     */
    public Preditor.Parametros parametrosMl(int anoTreino, int anoTeste) {
        return new Preditor.Parametros(anoTreino, anoTeste, config.inteiro("aps.ml.arvores", 200),
                config.inteiro("aps.ml.k", 8), config.decimal("aps.ml.epsKm", 10.0), config.inteiro("aps.ml.minPts", 30), 42L);
    }

    /**
     * Monta o contexto de relatorio com tudo o que foi produzido na sessao.
     *
     * @return contexto
     * @throws ApsException se nenhuma base foi carregada
     */
    public ContextoRelatorio contextoRelatorio() throws ApsException {
        return new ContextoRelatorio(exigirBase()).ordenacao(ultimaOrdenacao).comparativo(ultimoComparativo)
                .benchmark(ultimoBenchmark).ml(ultimoMl);
    }

    /**
     * @param prefixo   prefixo do nome
     * @param extensao  extensao com ponto
     * @return caminho com carimbo de data/hora na pasta de relatorios
     */
    public Path arquivoRelatorio(String prefixo, String extensao) {
        return config.diretorioRelatorios().resolve(prefixo + "-" + LocalDateTime.now().format(CARIMBO) + extensao);
    }

    public AppConfig config() { return config; }
    public RepositorioDados repositorio() { return repositorio; }
    public ServicoOrdenacao servicoOrdenacao() { return servicoOrdenacao; }
    public ReportExporter exporter() { return exporter; }
    public BaseDeFocos base() { return base; }

    public ResultadoOrdenacao<FocoIncendio> ultimaOrdenacao() { return ultimaOrdenacao; }
    public void setUltimaOrdenacao(ResultadoOrdenacao<FocoIncendio> r) { this.ultimaOrdenacao = r; }
    public List<ResultadoOrdenacao<FocoIncendio>> ultimoComparativo() { return ultimoComparativo; }
    public void setUltimoComparativo(List<ResultadoOrdenacao<FocoIncendio>> r) { this.ultimoComparativo = r; }
    public List<BenchmarkResult> ultimoBenchmark() { return ultimoBenchmark; }
    public void setUltimoBenchmark(List<BenchmarkResult> r) { this.ultimoBenchmark = r; }
    public Preditor.ResultadoML ultimoMl() { return ultimoMl; }
    public void setUltimoMl(Preditor.ResultadoML r) { this.ultimoMl = r; }
}
