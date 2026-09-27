package br.unip.aps.report;

import br.unip.aps.ApsException;
import br.unip.aps.analysis.Contagem;
import br.unip.aps.analysis.Estatisticas;
import br.unip.aps.benchmark.AnaliseComplexidade;
import br.unip.aps.benchmark.BenchmarkResult;
import br.unip.aps.io.CsvParser;
import br.unip.aps.io.RelatorioCarga;
import br.unip.aps.ml.BaseMensal;
import br.unip.aps.ml.ClusterizacaoHotspots;
import br.unip.aps.ml.Metricas;
import br.unip.aps.ml.NivelAtividade;
import br.unip.aps.ml.Preditor;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.sorting.ResultadoOrdenacao;
import br.unip.aps.util.Formatos;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Exportacao de relatorios em CSV, Excel (.xlsx, Apache POI) e PDF (OpenPDF).
 *
 * <p>Os CSVs usam ponto e virgula e BOM UTF-8, para abrirem corretamente no Excel em portugues.</p>
 */
public final class ReportExporter {

    private static final Logger LOG = Logger.getLogger(ReportExporter.class.getName());
    private static final char SEP = ';';
    private static final int MAX_LINHAS_PDF = 300;

    // ------------------------------------------------------------------ CSV

    /**
     * Exporta focos (por exemplo, o resultado de uma ordenacao) em CSV.
     *
     * @param focos   focos, na ordem em que devem aparecer
     * @param destino arquivo de saida
     * @return caminho gravado
     * @throws ApsException se nao for possivel gravar
     */
    public Path exportarFocosCsv(List<FocoIncendio> focos, Path destino) throws ApsException {
        try (BufferedWriter w = abrirCsv(destino)) {
            w.write(String.join(String.valueOf(SEP), "posicao", "id_bdq", "foco_id", "data_hora_gmt", "municipio",
                    "bioma", "estado", "latitude", "longitude", "satelite", "dias_sem_chuva", "precipitacao",
                    "risco_fogo", "frp"));
            w.newLine();
            int i = 1;
            for (FocoIncendio f : focos) {
                w.write(linhaCsv(String.valueOf(i++), String.valueOf(f.getIdBdq()), f.getFocoId(),
                        f.getDataHora().toString().replace('T', ' '), f.getMunicipio(), f.getBioma(), f.getEstado(),
                        num(f.getLatitude()), num(f.getLongitude()), f.getSatelite(), str(f.getNumeroDiasSemChuva()),
                        str(f.getPrecipitacao()), str(f.getRiscoFogo()), str(f.getFrp())));
                w.newLine();
            }
        } catch (IOException e) {
            throw falha(destino, e);
        }
        LOG.info(() -> "CSV de focos gravado: " + destino.toAbsolutePath());
        return destino;
    }

    /**
     * Exporta as medicoes do benchmark em CSV.
     *
     * @param resultados medicoes
     * @param destino    arquivo
     * @return caminho gravado
     * @throws ApsException se nao for possivel gravar
     */
    public Path exportarBenchmarkCsv(List<BenchmarkResult> resultados, Path destino) throws ApsException {
        try (BufferedWriter w = abrirCsv(destino)) {
            w.write(String.join(String.valueOf(SEP), "algoritmo", "criterio", "cenario", "n", "repeticoes",
                    "media_ms", "desvio_ms", "min_ms", "max_ms", "comparacoes", "trocas", "atribuicoes", "acessos",
                    "n_log2_n", "n2_sobre_2", "verificado"));
            w.newLine();
            for (BenchmarkResult r : resultados) {
                w.write(linhaCsv(r.algoritmo(), r.criterio().rotulo(), r.cenario().toString(), String.valueOf(r.n()),
                        String.valueOf(r.repeticoes()), num(r.mediaMs()), num(r.desvioMs()), num(r.minNs() / 1e6),
                        num(r.maxNs() / 1e6), String.valueOf(r.comparacoes()), String.valueOf(r.trocas()),
                        String.valueOf(r.atribuicoes()), String.valueOf(r.acessos()), num(r.referenciaNLogN()),
                        num(r.referenciaN2()), r.verificado() ? "sim" : "NAO"));
                w.newLine();
            }
        } catch (IOException e) {
            throw falha(destino, e);
        }
        LOG.info(() -> "CSV de benchmark gravado: " + destino.toAbsolutePath());
        return destino;
    }

    private static BufferedWriter abrirCsv(Path destino) throws IOException {
        criarPasta(destino);
        BufferedWriter w = Files.newBufferedWriter(destino, StandardCharsets.UTF_8);
        w.write('﻿');
        return w;
    }

    private static String linhaCsv(String... v) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(SEP);
            sb.append(CsvParser.escapar(v[i], SEP));
        }
        return sb.toString();
    }

    private static String num(double d) {
        return Formatos.decimal(d, 6).replace(".", "");
    }

    private static String str(Object o) {
        if (o == null) return "";
        return o instanceof Double d ? num(d) : o.toString();
    }

    // ---------------------------------------------------------------- EXCEL

    /**
     * Gera uma pasta de trabalho Excel com uma aba por secao disponivel.
     *
     * @param ctx     conteudo
     * @param destino arquivo .xlsx
     * @return caminho gravado
     * @throws ApsException se nao for possivel gravar
     */
    public Path exportarExcel(ContextoRelatorio ctx, Path destino) throws ApsException {
        try (Workbook wb = new XSSFWorkbook()) {
            Estilos es = new Estilos(wb);
            abaResumo(wb, es, ctx);
            abaQualidade(wb, es, ctx.base().getRelatorio());
            abaEstatisticas(wb, es, ctx);
            if (ctx.ordenacao() != null) abaOrdenacao(wb, es, ctx.ordenacao());
            if (ctx.comparativo() != null && !ctx.comparativo().isEmpty()) abaComparativo(wb, es, ctx.comparativo());
            if (ctx.benchmark() != null && !ctx.benchmark().isEmpty()) {
                abaBenchmark(wb, es, ctx.benchmark());
                abaComplexidade(wb, es, ctx.benchmark());
            }
            if (ctx.ml() != null) abaMl(wb, es, ctx.ml());
            criarPasta(destino);
            try (OutputStream out = Files.newOutputStream(destino)) {
                wb.write(out);
            }
        } catch (IOException e) {
            throw falha(destino, e);
        }
        LOG.info(() -> "Excel gravado: " + destino.toAbsolutePath());
        return destino;
    }

    private static final class Estilos {
        final CellStyle titulo, cabecalho, inteiro, decimal, texto;

        Estilos(Workbook wb) {
            org.apache.poi.ss.usermodel.Font ft = wb.createFont();
            ft.setBold(true);
            ft.setFontHeightInPoints((short) 14);
            titulo = wb.createCellStyle();
            titulo.setFont(ft);

            org.apache.poi.ss.usermodel.Font fc = wb.createFont();
            fc.setBold(true);
            fc.setColor(IndexedColors.WHITE.getIndex());
            cabecalho = wb.createCellStyle();
            cabecalho.setFont(fc);
            cabecalho.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
            cabecalho.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cabecalho.setBorderBottom(BorderStyle.THIN);

            inteiro = wb.createCellStyle();
            inteiro.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
            decimal = wb.createCellStyle();
            decimal.setDataFormat(wb.createDataFormat().getFormat("#,##0.000"));
            texto = wb.createCellStyle();
        }
    }

    /** Escritor sequencial de linhas em uma aba. */
    private static final class Aba {
        final Sheet sheet;
        final Estilos es;
        int linha;
        int maxColunas;

        Aba(Workbook wb, Estilos es, String nome) {
            this.sheet = wb.createSheet(nome);
            this.es = es;
        }

        void titulo(String t) {
            Row r = sheet.createRow(linha++);
            Cell c = r.createCell(0);
            c.setCellValue(t);
            c.setCellStyle(es.titulo);
        }

        void cabecalho(String... cols) {
            Row r = sheet.createRow(linha++);
            for (int i = 0; i < cols.length; i++) {
                Cell c = r.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(es.cabecalho);
            }
            maxColunas = Math.max(maxColunas, cols.length);
        }

        void linha(Object... vals) {
            Row r = sheet.createRow(linha++);
            for (int i = 0; i < vals.length; i++) {
                Cell c = r.createCell(i);
                Object v = vals[i];
                if (v == null) continue;
                if (v instanceof Integer || v instanceof Long) {
                    c.setCellValue(((Number) v).doubleValue());
                    c.setCellStyle(es.inteiro);
                } else if (v instanceof Number n) {
                    if (!Double.isNaN(n.doubleValue())) {
                        c.setCellValue(n.doubleValue());
                        c.setCellStyle(es.decimal);
                    }
                } else {
                    c.setCellValue(v.toString());
                }
            }
            maxColunas = Math.max(maxColunas, vals.length);
        }

        void vazia() {
            linha++;
        }

        void ajustar() {
            for (int i = 0; i < maxColunas; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 512, 60 * 256));
            }
        }
    }

    private void abaResumo(Workbook wb, Estilos es, ContextoRelatorio ctx) {
        Aba a = new Aba(wb, es, "Resumo");
        a.titulo(ctx.titulo());
        a.linha("Gerado em", LocalDateTime.now().format(Formatos.DATA_HORA));
        a.linha("Fontes", String.join(", ", nomes(ctx.base().getFontes())));
        a.linha("Filtro aplicado", ctx.filtro());
        a.vazia();
        Estatisticas est = new Estatisticas(ctx.focosFiltrados());
        a.cabecalho("Indicador", "Valor");
        a.linha("Total de focos", (long) est.total());
        est.porAno().forEach((ano, n) -> a.linha("Focos em " + ano, n));
        a.linha("Municipios afetados", (long) est.municipiosAfetados());
        a.linha("Periodo", ctx.base().dataInicial().format(Formatos.DATA) + " a " + ctx.base().dataFinal().format(Formatos.DATA));
        Map.Entry<YearMonth, Long> pico = est.mesPico();
        if (pico != null) a.linha("Mes com mais focos", pico.getKey() + " (" + pico.getValue() + " focos)");
        a.ajustar();
    }

    private void abaQualidade(Workbook wb, Estilos es, RelatorioCarga rel) {
        Aba a = new Aba(wb, es, "Qualidade dos dados");
        a.titulo("Relatorio de carga e limpeza");
        a.cabecalho("Arquivo", "Encoding", "Separador", "Linhas lidas", "Aceitas", "Rejeitadas", "Colunas");
        for (RelatorioCarga.ResumoArquivo r : rel.getArquivos()) {
            a.linha(r.arquivo().getFileName().toString(), r.encoding().name(), String.valueOf(r.separador()),
                    r.linhasLidas(), r.aceitas(), r.rejeitadas(), String.join(", ", r.colunas()));
        }
        a.vazia();
        a.linha("Duplicados removidos (foco_id repetido)", rel.getDuplicadosRemovidos());
        a.vazia();
        a.cabecalho("Motivo de rejeicao", "Quantidade");
        rel.getMotivos().forEach((m, n) -> a.linha(m, n));
        if (rel.getMotivos().isEmpty()) a.linha("Nenhuma linha rejeitada", 0L);
        a.vazia();
        a.cabecalho("Arquivo", "Linha", "Motivo", "Conteudo");
        for (RelatorioCarga.Rejeicao r : rel.getRejeicoes()) {
            a.linha(r.arquivo().getFileName().toString(), r.linha(), r.motivo(), r.conteudo());
        }
        a.ajustar();
    }

    private void abaEstatisticas(Workbook wb, Estilos es, ContextoRelatorio ctx) {
        Aba a = new Aba(wb, es, "Estatisticas");
        Estatisticas est = new Estatisticas(ctx.focosFiltrados());
        List<Integer> anos = new ArrayList<>(est.porAno().keySet());
        if (anos.size() >= 2) {
            int a1 = anos.get(anos.size() - 2), a2 = anos.get(anos.size() - 1);
            a.titulo("Comparativo mensal " + a1 + " x " + a2);
            a.cabecalho("Mes", String.valueOf(a1), String.valueOf(a2), "Variacao (%)");
            for (Estatisticas.LinhaComparativo l : est.comparativo(a1, a2)) {
                a.linha(l.mes() == 0 ? "TOTAL" : Estatisticas.MESES[l.mes() - 1], l.anoA(), l.anoB(), l.variacao());
            }
            a.vazia();
        }
        a.titulo("Focos por bioma");
        a.cabecalho("Bioma", "Focos", "%");
        for (Contagem c : est.porBioma()) a.linha(c.chave(), c.total(), 100.0 * c.total() / Math.max(1, est.total()));
        a.vazia();
        a.titulo("Top 20 municipios");
        a.cabecalho("Posicao", "Municipio", "Focos");
        int i = 1;
        for (Contagem c : est.topMunicipios(20)) a.linha((long) i++, c.chave(), c.total());
        a.ajustar();
    }

    private void abaOrdenacao(Workbook wb, Estilos es, ResultadoOrdenacao<FocoIncendio> r) {
        Aba a = new Aba(wb, es, "Ordenacao");
        a.titulo("Dados ordenados: " + r.algoritmo() + " | " + r.criterio());
        a.cabecalho("Algoritmo", "Criterio", "Cenario", "n", "Comparacoes", "Trocas", "Atribuicoes", "Acessos", "Tempo (ms)", "Verificado");
        a.linha(r.algoritmo(), r.criterio(), r.cenario().toString(), (long) r.tamanho(), r.metricas().comparacoes(),
                r.metricas().trocas(), r.metricas().atribuicoes(), r.metricas().acessos(), r.metricas().millis(),
                r.verificado() ? "sim" : "NAO");
        a.vazia();
        a.cabecalho("Posicao", "Data/hora (GMT)", "Municipio", "Bioma", "Latitude", "Longitude", "id_bdq");
        int i = 1;
        for (FocoIncendio f : r.dados()) {
            a.linha((long) i++, f.getDataHora().format(Formatos.DATA_HORA), f.getMunicipio(), f.getBioma(),
                    f.getLatitude(), f.getLongitude(), f.getIdBdq());
        }
        a.ajustar();
    }

    private void abaComparativo(Workbook wb, Estilos es, List<ResultadoOrdenacao<FocoIncendio>> lista) {
        Aba a = new Aba(wb, es, "Comparativo algoritmos");
        a.titulo("Todos os algoritmos sobre a mesma entrada");
        a.cabecalho("Algoritmo", "Criterio", "Cenario", "n", "Comparacoes", "Trocas", "Atribuicoes", "Acessos", "Tempo (ms)", "Verificado");
        for (ResultadoOrdenacao<FocoIncendio> r : lista) {
            a.linha(r.algoritmo(), r.criterio(), r.cenario().toString(), (long) r.tamanho(), r.metricas().comparacoes(),
                    r.metricas().trocas(), r.metricas().atribuicoes(), r.metricas().acessos(), r.metricas().millis(),
                    r.verificado() ? "sim" : "NAO");
        }
        a.ajustar();
    }

    private void abaBenchmark(Workbook wb, Estilos es, List<BenchmarkResult> res) {
        Aba a = new Aba(wb, es, "Benchmark");
        a.titulo("Benchmark (tempo = media de repeticoes apos aquecimento do JIT)");
        a.cabecalho("Algoritmo", "Criterio", "Cenario", "n", "Media (ms)", "Desvio (ms)", "Min (ms)", "Max (ms)",
                "Comparacoes", "Trocas", "Atribuicoes", "Acessos", "n log2 n", "n²/2", "Verificado");
        for (BenchmarkResult r : res) {
            a.linha(r.algoritmo(), r.criterio().rotulo(), r.cenario().toString(), (long) r.n(), r.mediaMs(), r.desvioMs(),
                    r.minNs() / 1e6, r.maxNs() / 1e6, r.comparacoes(), r.trocas(), r.atribuicoes(), r.acessos(),
                    r.referenciaNLogN(), r.referenciaN2(), r.verificado() ? "sim" : "NAO");
        }
        a.ajustar();
    }

    private void abaComplexidade(Workbook wb, Estilos es, List<BenchmarkResult> res) {
        Aba a = new Aba(wb, es, "Complexidade empirica");
        a.titulo("Expoente empirico k (regressao log-log: custo ~ n^k)");
        a.cabecalho("Algoritmo", "Criterio", "Cenario", "k (tempo)", "k (comparacoes)", "R² (tempo)", "Classe estimada");
        for (AnaliseComplexidade.Estimativa e : AnaliseComplexidade.estimar(res)) {
            a.linha(e.algoritmo(), e.criterio().rotulo(), e.cenario().toString(), e.expoenteTempo(),
                    e.expoenteComparacoes(), e.r2Tempo(), e.classificacao());
        }
        a.ajustar();
    }

    private void abaMl(Workbook wb, Estilos es, Preditor.ResultadoML ml) {
        Aba a = new Aba(wb, es, "Machine Learning");
        var reg = ml.regressao();
        a.titulo("Previsao de focos por municipio/mes - Random Forest (treino " + reg.anoTreino() + ", teste " + reg.anoTeste() + ")");
        a.cabecalho("Modelo", "MAE", "RMSE", "R²");
        linhaReg(a, "Random Forest (treino fixo)", reg.modelo());
        linhaReg(a, "Random Forest (janela expansivel)", reg.janelaExpansivel());
        linhaReg(a, "Baseline persistencia (lag1)", reg.persistencia());
        linhaReg(a, "Baseline media historica", reg.mediaHistorica());
        a.vazia();
        a.cabecalho("Mes", "Focos reais (estado)", "Previstos RF treino fixo", "Previstos RF janela expansivel");
        reg.realPorMes().forEach((m, v) -> a.linha(m.toString(), v, reg.previstoPorMes().get(m),
                reg.previstoJanelaPorMes().getOrDefault(m, 0.0)));
        a.vazia();
        a.cabecalho("Variavel", "Importancia (RF regressao)", "Importancia (RF classificacao)");
        for (int i = 0; i < BaseMensal.VARIAVEIS.length; i++) {
            a.linha(BaseMensal.VARIAVEIS[i], reg.importancia()[i], ml.classificacao().importancia()[i]);
        }
        a.vazia();
        var cls = ml.classificacao();
        a.titulo("Classificacao do nivel de atividade (baixo / medio / alto)");
        a.cabecalho("Metrica", "Random Forest", "Baseline (classe majoritaria)");
        a.linha("Acuracia", cls.metricas().acuracia(), cls.baselineAcuracia());
        a.linha("F1 macro", cls.metricas().f1Macro(), cls.baselineF1Macro());
        a.vazia();
        NivelAtividade[] niveis = NivelAtividade.values();
        a.cabecalho("Real \\ Previsto", niveis[0].toString(), niveis[1].toString(), niveis[2].toString(), "Precisao", "Revocacao", "F1");
        Metricas.Classificacao m = cls.metricas();
        for (int i = 0; i < niveis.length; i++) {
            a.linha(niveis[i].toString(), (long) m.matriz()[i][0], (long) m.matriz()[i][1], (long) m.matriz()[i][2],
                    m.precisao()[i], m.revocacao()[i], m.f1()[i]);
        }
        a.vazia();
        for (ClusterizacaoHotspots.Resultado c : List.of(ml.dbscan(), ml.kMeans())) {
            a.titulo("Hotspots - " + c.metodo() + (c.ruido() > 0 ? " | ruido: " + c.ruido() + " focos" : ""));
            a.cabecalho("#", "Focos", "Latitude", "Longitude", "Raio (km)", "Municipio principal", "Bioma", "Focos por ano");
            for (ClusterizacaoHotspots.Hotspot h : c.hotspots()) {
                a.linha((long) h.id(), (long) h.focos(), h.latitude(), h.longitude(), h.raioKm(), h.municipioPrincipal(),
                        h.biomaPredominante(), h.focosPorAno().toString());
            }
            a.vazia();
        }
        a.ajustar();
    }

    private static void linhaReg(Aba a, String nome, Metricas.Regressao m) {
        a.linha(nome, m.mae(), m.rmse(), m.r2());
    }

    // ------------------------------------------------------------------ PDF

    /**
     * Gera o relatorio em PDF (A4, retrato).
     *
     * @param ctx     conteudo
     * @param destino arquivo .pdf
     * @return caminho gravado
     * @throws ApsException se nao for possivel gravar
     */
    public Path exportarPdf(ContextoRelatorio ctx, Path destino) throws ApsException {
        Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
        try {
            criarPasta(destino);
            try (OutputStream out = Files.newOutputStream(destino)) {
                PdfWriter.getInstance(doc, out);
                doc.addTitle(Pdf.limpar(ctx.titulo()));
                doc.addAuthor("APS UNIP - Estrutura de Dados");
                doc.open();
                doc.add(new Paragraph(Pdf.limpar(ctx.titulo()), Pdf.TITULO));
                doc.add(new Paragraph("Gerado em " + LocalDateTime.now().format(Formatos.DATA_HORA)
                        + " | Fontes: " + String.join(", ", nomes(ctx.base().getFontes())), Pdf.PEQUENA));
                doc.add(new Paragraph("Filtro: " + Pdf.limpar(ctx.filtro()), Pdf.PEQUENA));

                secaoResumoPdf(doc, ctx);
                for (ContextoRelatorio.Grafico g : ctx.graficos()) {
                    doc.add(new Paragraph(Pdf.limpar(g.titulo()), Pdf.SUBTITULO));
                    Image img = Image.getInstance(g.imagem(), null);
                    img.scaleToFit(doc.getPageSize().getWidth() - 100, 300);
                    img.setAlignment(Element.ALIGN_CENTER);
                    doc.add(img);
                }
                if (ctx.comparativo() != null && !ctx.comparativo().isEmpty()) secaoComparativoPdf(doc, ctx.comparativo());
                if (ctx.benchmark() != null && !ctx.benchmark().isEmpty()) secaoBenchmarkPdf(doc, ctx.benchmark());
                if (ctx.ml() != null) secaoMlPdf(doc, ctx.ml());
                if (ctx.ordenacao() != null) secaoOrdenacaoPdf(doc, ctx.ordenacao());
                doc.close();
            }
        } catch (IOException | DocumentException e) {
            throw falha(destino, e);
        }
        LOG.info(() -> "PDF gravado: " + destino.toAbsolutePath());
        return destino;
    }

    private void secaoResumoPdf(Document doc, ContextoRelatorio ctx) {
        Estatisticas est = new Estatisticas(ctx.focosFiltrados());
        doc.add(new Paragraph("1. Resumo dos dados", Pdf.SUBTITULO));
        PdfPTable t = Pdf.tabela(new float[]{3, 2}, "Indicador", "Valor");
        Pdf.linha(t, "Total de focos", Formatos.inteiro(est.total()));
        est.porAno().forEach((ano, n) -> Pdf.linha(t, "Focos em " + ano, Formatos.inteiro(n)));
        Pdf.linha(t, "Municipios afetados", Formatos.inteiro(est.municipiosAfetados()));
        RelatorioCarga rel = ctx.base().getRelatorio();
        Pdf.linha(t, "Linhas lidas / rejeitadas / duplicadas", rel.getTotalLidas() + " / " + rel.getTotalRejeitadas()
                + " / " + rel.getDuplicadosRemovidos());
        doc.add(t);

        List<Integer> anos = new ArrayList<>(est.porAno().keySet());
        if (anos.size() >= 2) {
            int a1 = anos.get(anos.size() - 2), a2 = anos.get(anos.size() - 1);
            doc.add(new Paragraph("Comparativo mensal " + a1 + " x " + a2, Pdf.SUBTITULO));
            PdfPTable c = Pdf.tabela(new float[]{2, 2, 2, 2}, "Mes", String.valueOf(a1), String.valueOf(a2), "Variacao");
            for (Estatisticas.LinhaComparativo l : est.comparativo(a1, a2)) {
                Pdf.linha(c, l.mes() == 0 ? "TOTAL" : Estatisticas.MESES[l.mes() - 1], Formatos.inteiro(l.anoA()),
                        Formatos.inteiro(l.anoB()), Double.isNaN(l.variacao()) ? "-" : Formatos.decimal(l.variacao(), 1) + "%");
            }
            doc.add(c);
        }
        doc.add(new Paragraph("Focos por bioma e top 10 municipios", Pdf.SUBTITULO));
        PdfPTable b = Pdf.tabela(new float[]{3, 2}, "Bioma / Municipio", "Focos");
        for (Contagem x : est.porBioma()) Pdf.linha(b, x.chave(), Formatos.inteiro(x.total()));
        for (Contagem x : est.topMunicipios(10)) Pdf.linha(b, x.chave(), Formatos.inteiro(x.total()));
        doc.add(b);
    }

    private void secaoComparativoPdf(Document doc, List<ResultadoOrdenacao<FocoIncendio>> lista) {
        doc.add(new Paragraph("2. Comparativo de algoritmos (mesma entrada)", Pdf.SUBTITULO));
        doc.add(new Paragraph("Criterio: " + Pdf.limpar(lista.get(0).criterio()) + " | cenario: " + lista.get(0).cenario()
                + " | n = " + Formatos.inteiro(lista.get(0).tamanho()), Pdf.PEQUENA));
        PdfPTable t = Pdf.tabela(new float[]{3, 2.2f, 2, 2.2f, 2}, "Algoritmo", "Comparacoes", "Trocas", "Acessos", "Tempo");
        for (ResultadoOrdenacao<FocoIncendio> r : lista) {
            Pdf.linha(t, r.algoritmo(), Formatos.inteiro(r.metricas().comparacoes()), Formatos.inteiro(r.metricas().trocas()),
                    Formatos.inteiro(r.metricas().acessos()), Formatos.duracao(r.metricas().nanos()));
        }
        doc.add(t);
    }

    private void secaoBenchmarkPdf(Document doc, List<BenchmarkResult> res) {
        doc.add(new Paragraph("3. Benchmark", Pdf.SUBTITULO));
        PdfPTable t = Pdf.tabela(new float[]{3, 1.6f, 1.8f, 1.2f, 1.6f, 1.4f, 2, 2}, "Algoritmo", "Criterio", "Cenario", "n",
                "Media ms", "Desvio", "Comparacoes", "Trocas");
        for (BenchmarkResult r : res) {
            Pdf.linha(t, r.algoritmo(), r.criterio().rotulo(), r.cenario().toString(), Formatos.inteiro(r.n()),
                    Formatos.decimal(r.mediaMs(), 3), Formatos.decimal(r.desvioMs(), 3), Formatos.inteiro(r.comparacoes()),
                    Formatos.inteiro(r.trocas()));
        }
        doc.add(t);
        doc.add(new Paragraph("Expoente empirico (custo ~ n^k)", Pdf.SUBTITULO));
        PdfPTable e = Pdf.tabela(new float[]{3, 2, 2, 1.5f, 1.5f, 2}, "Algoritmo", "Criterio", "Cenario", "k tempo", "k comp.", "Classe");
        for (AnaliseComplexidade.Estimativa x : AnaliseComplexidade.estimar(res)) {
            Pdf.linha(e, x.algoritmo(), x.criterio().rotulo(), x.cenario().toString(), Formatos.decimal(x.expoenteTempo(), 2),
                    Double.isNaN(x.expoenteComparacoes()) ? "-" : Formatos.decimal(x.expoenteComparacoes(), 2), x.classificacao());
        }
        doc.add(e);
    }

    private void secaoMlPdf(Document doc, Preditor.ResultadoML ml) {
        doc.add(new Paragraph("4. Machine Learning", Pdf.SUBTITULO));
        var reg = ml.regressao();
        doc.add(new Paragraph("Previsao de focos por municipio/mes (Random Forest, " + ml.parametros().arvores()
                + " arvores). Treino: " + reg.anoTreino() + " | Teste: " + reg.anoTeste(), Pdf.NORMAL));
        PdfPTable t = Pdf.tabela(new float[]{4, 2, 2, 2}, "Modelo", "MAE", "RMSE", "R2");
        for (var e : List.of(Map.entry("Random Forest (treino fixo)", reg.modelo()),
                Map.entry("Random Forest (janela expansivel)", reg.janelaExpansivel()), Map.entry("Persistencia (lag1)", reg.persistencia()),
                Map.entry("Media historica", reg.mediaHistorica()))) {
            Pdf.linha(t, e.getKey(), Formatos.decimal(e.getValue().mae(), 3), Formatos.decimal(e.getValue().rmse(), 3),
                    Formatos.decimal(e.getValue().r2(), 3));
        }
        doc.add(t);
        var cls = ml.classificacao();
        doc.add(new Paragraph(String.format("Classificacao do nivel de atividade: acuracia %s (baseline %s), F1 macro %s (baseline %s)",
                Formatos.decimal(cls.metricas().acuracia(), 3), Formatos.decimal(cls.baselineAcuracia(), 3),
                Formatos.decimal(cls.metricas().f1Macro(), 3), Formatos.decimal(cls.baselineF1Macro(), 3)), Pdf.NORMAL));
        doc.add(new Paragraph("Hotspots - " + ml.dbscan().metodo(), Pdf.NORMAL));
        PdfPTable h = Pdf.tabela(new float[]{0.6f, 1.2f, 1.6f, 1.6f, 1.2f, 3, 2}, "#", "Focos", "Lat", "Lon", "Raio km", "Municipio", "Bioma");
        for (ClusterizacaoHotspots.Hotspot x : ml.dbscan().hotspots().subList(0, Math.min(15, ml.dbscan().hotspots().size()))) {
            Pdf.linha(h, String.valueOf(x.id()), Formatos.inteiro(x.focos()), Formatos.decimal(x.latitude(), 4),
                    Formatos.decimal(x.longitude(), 4), Formatos.decimal(x.raioKm(), 1), x.municipioPrincipal(), x.biomaPredominante());
        }
        doc.add(h);
    }

    private void secaoOrdenacaoPdf(Document doc, ResultadoOrdenacao<FocoIncendio> r) {
        doc.newPage();
        doc.add(new Paragraph("5. Dados ordenados", Pdf.SUBTITULO));
        doc.add(new Paragraph(Pdf.limpar(r.algoritmo() + " | " + r.criterio() + " | cenario: " + r.cenario()
                + " | n = " + Formatos.inteiro(r.tamanho())), Pdf.NORMAL));
        doc.add(new Paragraph(Pdf.limpar("Operacoes: " + r.metricas()) + (r.verificado() ? " | ordenacao verificada" : " | FALHA NA VERIFICACAO"), Pdf.PEQUENA));
        PdfPTable t = Pdf.tabela(new float[]{0.9f, 2.4f, 3.4f, 2.2f, 1.5f, 1.5f}, "#", "Data/hora GMT", "Municipio", "Bioma", "Lat", "Lon");
        int limite = Math.min(MAX_LINHAS_PDF, r.dados().size());
        for (int i = 0; i < limite; i++) {
            FocoIncendio f = r.dados().get(i);
            Pdf.linha(t, String.valueOf(i + 1), f.getDataHora().format(Formatos.DATA_HORA), f.getMunicipio(), f.getBioma(),
                    Formatos.decimal(f.getLatitude(), 4), Formatos.decimal(f.getLongitude(), 4));
        }
        doc.add(t);
        if (r.dados().size() > limite) {
            doc.add(new Paragraph("... exibidas as primeiras " + limite + " de " + Formatos.inteiro(r.dados().size())
                    + " linhas (a lista completa esta no Excel/CSV).", Pdf.PEQUENA));
        }
    }

    /** Utilitarios de formatacao do PDF. */
    static final class Pdf {
        static final Font TITULO = new Font(Font.HELVETICA, 15, Font.BOLD, new Color(0x8B, 0x1A, 0x1A));
        static final Font SUBTITULO = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(0x33, 0x33, 0x33));
        static final Font NORMAL = new Font(Font.HELVETICA, 9.5f);
        static final Font PEQUENA = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);
        static final Font CELULA = new Font(Font.HELVETICA, 7.5f);
        static final Font CABECALHO = new Font(Font.HELVETICA, 7.5f, Font.BOLD, Color.WHITE);

        private Pdf() { }

        static PdfPTable tabela(float[] larguras, String... cab) {
            PdfPTable t = new PdfPTable(larguras);
            t.setWidthPercentage(100);
            t.setSpacingBefore(4);
            t.setSpacingAfter(8);
            t.setHeaderRows(1);
            for (String c : cab) {
                PdfPCell cell = new PdfPCell(new Phrase(limpar(c), CABECALHO));
                cell.setBackgroundColor(new Color(0x8B, 0x1A, 0x1A));
                cell.setPadding(3);
                t.addCell(cell);
            }
            return t;
        }

        static void linha(PdfPTable t, String... v) {
            for (String s : v) {
                PdfPCell cell = new PdfPCell(new Phrase(limpar(s == null ? "" : s), CELULA));
                cell.setPadding(2.5f);
                t.addCell(cell);
            }
        }

        /** Troca simbolos fora do Cp1252 (fontes padrao do PDF) por equivalentes ASCII. */
        static String limpar(String s) {
            if (s == null) return "";
            return s.replace("→", "->").replace("↑", "(cresc.)").replace("↓", "(decresc.)")
                    .replace("≈", "~").replace("·", ".").replace("Ω", "Omega").replace("✔", "OK");
        }
    }

    // ------------------------------------------------------------ utilitarios

    private static List<String> nomes(List<Path> paths) {
        List<String> r = new ArrayList<>();
        for (Path p : paths) r.add(p.getFileName().toString());
        return r;
    }

    private static void criarPasta(Path destino) throws IOException {
        Path pai = destino.toAbsolutePath().getParent();
        if (pai != null) Files.createDirectories(pai);
    }

    private static ApsException falha(Path destino, Exception e) {
        String extra = e.getMessage() != null && e.getMessage().contains("used by another process")
                ? " O arquivo esta aberto em outro programa (feche o Excel/leitor de PDF)." : "";
        return new ApsException("Nao foi possivel gravar " + destino.toAbsolutePath() + "." + extra + " (" + e.getMessage() + ")", e);
    }
}
