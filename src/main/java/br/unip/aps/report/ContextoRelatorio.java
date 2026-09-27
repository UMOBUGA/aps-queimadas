package br.unip.aps.report;

import br.unip.aps.benchmark.BenchmarkResult;
import br.unip.aps.ml.Preditor;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.sorting.ResultadoOrdenacao;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Tudo o que pode entrar em um relatorio. Apenas a base e obrigatoria; as demais secoes aparecem
 * quando preenchidas (padrao <b>Builder</b>).
 */
public final class ContextoRelatorio {

    /**
     * Grafico ja renderizado (snapshot do JavaFX) para inclusao no PDF.
     *
     * @param titulo legenda
     * @param imagem imagem
     */
    public record Grafico(String titulo, BufferedImage imagem) { }

    private final BaseDeFocos base;
    private String titulo = "Análise de Performance de Algoritmos de Ordenação — Focos de Queimadas (INPE)";
    private String filtro = "sem filtros";
    private List<FocoIncendio> focosFiltrados;
    private ResultadoOrdenacao<FocoIncendio> ordenacao;
    private List<ResultadoOrdenacao<FocoIncendio>> comparativo;
    private List<BenchmarkResult> benchmark;
    private Preditor.ResultadoML ml;
    private final List<Grafico> graficos = new ArrayList<>();

    /** @param base base carregada (obrigatoria) */
    public ContextoRelatorio(BaseDeFocos base) {
        this.base = base;
        this.focosFiltrados = base.getFocos();
    }

    public ContextoRelatorio titulo(String v) { this.titulo = v; return this; }
    public ContextoRelatorio filtro(String descricao, List<FocoIncendio> focos) {
        this.filtro = descricao;
        this.focosFiltrados = focos;
        return this;
    }
    public ContextoRelatorio ordenacao(ResultadoOrdenacao<FocoIncendio> v) { this.ordenacao = v; return this; }
    public ContextoRelatorio comparativo(List<ResultadoOrdenacao<FocoIncendio>> v) { this.comparativo = v; return this; }
    public ContextoRelatorio benchmark(List<BenchmarkResult> v) { this.benchmark = v; return this; }
    public ContextoRelatorio ml(Preditor.ResultadoML v) { this.ml = v; return this; }
    public ContextoRelatorio grafico(String titulo, BufferedImage img) {
        if (img != null) graficos.add(new Grafico(titulo, img));
        return this;
    }

    public BaseDeFocos base() { return base; }
    public String titulo() { return titulo; }
    public String filtro() { return filtro; }
    public List<FocoIncendio> focosFiltrados() { return focosFiltrados; }
    public ResultadoOrdenacao<FocoIncendio> ordenacao() { return ordenacao; }
    public List<ResultadoOrdenacao<FocoIncendio>> comparativo() { return comparativo; }
    public List<BenchmarkResult> benchmark() { return benchmark; }
    public Preditor.ResultadoML ml() { return ml; }
    public List<Grafico> graficos() { return graficos; }
}
