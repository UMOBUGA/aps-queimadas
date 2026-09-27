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
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ConditionalFormattingThreshold;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xddf.usermodel.XDDFColor;
import org.apache.poi.xddf.usermodel.XDDFLineProperties;
import org.apache.poi.xddf.usermodel.XDDFShapeProperties;
import org.apache.poi.xddf.usermodel.XDDFSolidFillProperties;
import org.apache.poi.xddf.usermodel.chart.AxisCrosses;
import org.apache.poi.xddf.usermodel.chart.AxisOrientation;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.BarDirection;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.MarkerStyle;
import org.apache.poi.xddf.usermodel.chart.ScatterStyle;
import org.apache.poi.xddf.usermodel.chart.XDDFBarChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFLineChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFScatterChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFColorScaleFormatting;
import org.apache.poi.xssf.usermodel.XSSFConditionalFormattingRule;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFSheetConditionalFormatting;
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

/** Exportacao de relatorios em CSV, Excel (.xlsx, Apache POI) e PDF (OpenPDF). */
public final class ReportExporter {
    private static final Logger LOG = Logger.getLogger(ReportExporter.class.getName());
    private static final char SEP = ';';

    /** Exporta focos (por exemplo, o resultado de uma ordenacao) em CSV. */
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

    /** Exporta as medicoes do benchmark em CSV. */
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
            sb.append(CsvParser.escapar(semFormula(v[i]), SEP));
        }
        return sb.toString();
    }

    /** Neutraliza texto que o Excel executaria como formula (=, +, @, - nao numerico): prefixa com apostrofo. */
    static String semFormula(String v) {
        if (v == null || v.isEmpty()) return v;
        char c = v.charAt(0);
        boolean perigoso = c == '=' || c == '+' || c == '@' || c == '\t' || c == '\r'
                || (c == '-' && !v.matches("-\\d[\\d.,]*"));
        return perigoso ? "'" + v : v;
    }

    private static String num(double d) {
        return Formatos.decimal(d, 6).replace(".", "");
    }

    private static String str(Object o) {
        if (o == null) return "";
        return o instanceof Double d ? num(d) : o.toString();
    }

    /** Gera uma pasta de trabalho Excel com uma aba por secao, cabecalhos congelados, filtros e graficos nativos. */
    public Path exportarExcel(ContextoRelatorio ctx, Path destino) throws ApsException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Estilos es = new Estilos(wb);
            abaResumo(wb, es, ctx);
            abaSobre(wb, es, ctx);
            abaQualidade(wb, es, ctx.base().getRelatorio());
            abaEstatisticas(wb, es, ctx);
            if (ctx.ordenacao() != null) abaOrdenacao(wb, es, ctx.ordenacao());
            if (ctx.comparativo() != null && !ctx.comparativo().isEmpty()) abaComparativo(wb, es, ctx.comparativo());
            if (ctx.benchmark() != null && !ctx.benchmark().isEmpty()) {
                abaBenchmark(wb, es, ctx.benchmark());
                abaCurvas(wb, es, ctx.benchmark());
                abaComplexidade(wb, es, ctx.benchmark());
            }
            if (ctx.ml() != null) abaMl(wb, es, ctx.ml());
            wb.getProperties().getCoreProperties().setTitle(ctx.titulo());
            wb.getProperties().getCoreProperties().setCreator("APS Queimadas");
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
        final CellStyle titulo, subtitulo, nota, cabecalho, inteiro, decimal, percentual, texto, rotulo;

        Estilos(XSSFWorkbook wb) {
            titulo = wb.createCellStyle();
            titulo.setFont(fonte(wb, 16, true, Identidade.TEXTO));
            subtitulo = wb.createCellStyle();
            subtitulo.setFont(fonte(wb, 12, true, Identidade.TEXTO));
            nota = wb.createCellStyle();
            XSSFFont fn = fonte(wb, 9, false, Identidade.TEXTO_3);
            fn.setItalic(true);
            nota.setFont(fn);

            XSSFCellStyle cab = wb.createCellStyle();
            cab.setFont(fonte(wb, 10, true, Color.WHITE));
            cab.setFillForegroundColor(cor(Identidade.TINTA));
            cab.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cab.setBorderBottom(BorderStyle.MEDIUM);
            cab.setBottomBorderColor(cor(Identidade.BRASA_GRAFICO));
            cab.setVerticalAlignment(VerticalAlignment.CENTER);
            cabecalho = cab;

            XSSFFont corpo = fonte(wb, 10, false, Identidade.TEXTO);
            inteiro = corpo(wb, corpo, "#,##0");
            decimal = corpo(wb, corpo, "#,##0.000");
            percentual = corpo(wb, corpo, "0.0\"%\"");
            texto = corpo(wb, corpo, null);
            rotulo = corpo(wb, fonte(wb, 10, true, Identidade.TEXTO_2), null);
        }

        private static XSSFFont fonte(XSSFWorkbook wb, int tamanho, boolean negrito, Color c) {
            XSSFFont f = wb.createFont();
            f.setFontName(Identidade.FONTE_EXCEL);
            f.setFontHeightInPoints((short) tamanho);
            f.setBold(negrito);
            f.setColor(cor(c));
            return f;
        }

        private static CellStyle corpo(XSSFWorkbook wb, XSSFFont f, String formato) {
            XSSFCellStyle s = wb.createCellStyle();
            s.setFont(f);
            s.setBorderBottom(BorderStyle.HAIR);
            s.setBottomBorderColor(cor(Identidade.BORDA));
            if (formato != null) s.setDataFormat(wb.createDataFormat().getFormat(formato));
            return s;
        }
    }

    private static XSSFColor cor(Color c) {
        return new XSSFColor(new byte[]{(byte) c.getRed(), (byte) c.getGreen(), (byte) c.getBlue()}, null);
    }

    /** Escritor sequencial de linhas em uma aba. */
    private static final class Aba {
        final XSSFSheet sheet;
        final Estilos es;
        int linha;
        int maxColunas;
        int inicioFiltro = -1;
        int colunasFiltro;

        Aba(XSSFWorkbook wb, Estilos es, String nome) {
            this.sheet = wb.createSheet(nome);
            this.es = es;
            sheet.setDisplayGridlines(false);
            sheet.getPrintSetup().setLandscape(true);
            sheet.getPrintSetup().setPaperSize(org.apache.poi.ss.usermodel.PrintSetup.A4_PAPERSIZE);
            sheet.setFitToPage(true);
            sheet.getPrintSetup().setFitWidth((short) 1);
            sheet.getPrintSetup().setFitHeight((short) 0);
            sheet.getFooter().setLeft("APS Queimadas · dados INPE");
            sheet.getFooter().setRight("Página &P de &N");
        }

        void titulo(String t) {
            celula(t, es.titulo);
            sheet.getRow(linha - 1).setHeightInPoints(24);
        }

        void subtitulo(String t) {
            celula(t, es.subtitulo);
            sheet.getRow(linha - 1).setHeightInPoints(19);
        }

        void nota(String t) {
            celula(t, es.nota);
        }

        private void celula(String t, CellStyle s) {
            Row r = sheet.createRow(linha++);
            Cell c = r.createCell(0);
            c.setCellValue(t);
            c.setCellStyle(s);
        }

        void cabecalho(String... cols) {
            Row r = sheet.createRow(linha++);
            r.setHeightInPoints(20);
            for (int i = 0; i < cols.length; i++) {
                Cell c = r.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(es.cabecalho);
            }
            maxColunas = Math.max(maxColunas, cols.length);
        }

        /** Congela as linhas ate o cabecalho recem-escrito e liga o filtro automatico nessa tabela. */
        void congelarEFiltrar(int colunas) {
            sheet.createFreezePane(0, linha);
            inicioFiltro = linha - 1;
            colunasFiltro = colunas;
        }

        void linha(Object... vals) {
            Row r = sheet.createRow(linha++);
            for (int i = 0; i < vals.length; i++) {
                Cell c = r.createCell(i);
                Object v = vals[i];
                if (v == null) continue;
                if (v instanceof Pct p) {
                    if (!Double.isNaN(p.valor())) {
                        c.setCellValue(p.valor());
                        c.setCellStyle(es.percentual);
                    }
                } else if (v instanceof Integer || v instanceof Long) {
                    c.setCellValue(((Number) v).doubleValue());
                    c.setCellStyle(es.inteiro);
                } else if (v instanceof Number n) {
                    if (!Double.isNaN(n.doubleValue())) {
                        c.setCellValue(n.doubleValue());
                        c.setCellStyle(es.decimal);
                    }
                } else {
                    c.setCellValue(v.toString());
                    c.setCellStyle(i == 0 ? es.rotulo : es.texto);
                }
            }
            maxColunas = Math.max(maxColunas, vals.length);
        }

        void vazia() {
            linha++;
        }

        /** Escala de cor sequencial (claro para brasa) numa faixa de celulas de uma coluna. */
        void escalaCor(int col, int de, int ate) {
            if (ate < de) return;
            XSSFSheetConditionalFormatting scf = sheet.getSheetConditionalFormatting();
            XSSFConditionalFormattingRule regra = scf.createConditionalFormattingColorScaleRule();
            XSSFColorScaleFormatting cs = regra.getColorScaleFormatting();
            cs.getThresholds()[0].setRangeType(ConditionalFormattingThreshold.RangeType.MIN);
            cs.getThresholds()[1].setRangeType(ConditionalFormattingThreshold.RangeType.PERCENTILE);
            cs.getThresholds()[1].setValue(50d);
            cs.getThresholds()[2].setRangeType(ConditionalFormattingThreshold.RangeType.MAX);
            cs.setColors(new XSSFColor[]{cor(new Color(0xFF, 0xF7, 0xEC)), cor(new Color(0xFD, 0xBB, 0x84)), cor(new Color(0xE3, 0x4A, 0x33))});
            scf.addConditionalFormatting(new CellRangeAddress[]{new CellRangeAddress(de, ate, col, col)}, regra);
        }

        /** Barras de dados dentro das celulas de uma coluna. */
        void barrasDados(int col, int de, int ate) {
            if (ate < de) return;
            XSSFSheetConditionalFormatting scf = sheet.getSheetConditionalFormatting();
            XSSFConditionalFormattingRule regra = scf.createConditionalFormattingRule(cor(Identidade.BRASA_GRAFICO));
            scf.addConditionalFormatting(new CellRangeAddress[]{new CellRangeAddress(de, ate, col, col)}, regra);
        }

        void ajustar() {
            for (int i = 0; i < maxColunas; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(10 * 256, Math.min(sheet.getColumnWidth(i) + 768, 60 * 256)));
            }
            if (inicioFiltro >= 0 && linha - 1 > inicioFiltro) {
                sheet.setAutoFilter(new CellRangeAddress(inicioFiltro, linha - 1, 0, colunasFiltro - 1));
            }
        }
    }

    /** Valor numerico exibido com o sinal de porcentagem (ja multiplicado por 100). */
    private record Pct(double valor) { }

    private static XSSFChart grafico(XSSFSheet sh, String titulo, int col1, int lin1, int col2, int lin2) {
        XSSFDrawing d = sh.createDrawingPatriarch();
        XSSFClientAnchor an = d.createAnchor(0, 0, 0, 0, col1, lin1, col2, lin2);
        XSSFChart ch = d.createChart(an);
        ch.setTitleText(titulo);
        ch.setTitleOverlay(false);
        return ch;
    }

    private static XDDFShapeProperties traco(Color c, double largura) {
        XDDFShapeProperties p = new XDDFShapeProperties();
        XDDFLineProperties lp = new XDDFLineProperties();
        lp.setFillProperties(new XDDFSolidFillProperties(XDDFColor.from(new byte[]{(byte) c.getRed(), (byte) c.getGreen(), (byte) c.getBlue()})));
        lp.setWidth(largura);
        p.setLineProperties(lp);
        return p;
    }

    private static XDDFShapeProperties preenchimento(Color c) {
        XDDFShapeProperties p = new XDDFShapeProperties();
        p.setFillProperties(new XDDFSolidFillProperties(XDDFColor.from(new byte[]{(byte) c.getRed(), (byte) c.getGreen(), (byte) c.getBlue()})));
        return p;
    }

    private void abaResumo(XSSFWorkbook wb, Estilos es, ContextoRelatorio ctx) {
        Aba a = new Aba(wb, es, "Resumo");
        a.sheet.setTabColor(cor(Identidade.BRASA_GRAFICO));
        a.titulo(ctx.titulo());
        a.nota("Gerado em " + LocalDateTime.now().format(Formatos.DATA_HORA) + " · fontes: " + String.join(", ", nomes(ctx.base().getFontes())));
        a.nota("Recorte: " + ctx.filtro());
        a.vazia();
        Estatisticas est = new Estatisticas(ctx.focosFiltrados());
        a.cabecalho("Indicador", "Valor");
        a.linha("Total de focos", (long) est.total());
        est.porAno().forEach((ano, n) -> a.linha("Focos em " + ano, n));
        a.linha("Municípios afetados", (long) est.municipiosAfetados());
        if (ctx.base().dataInicial() != null) {
            a.linha("Período", ctx.base().dataInicial().format(Formatos.DATA) + " a " + ctx.base().dataFinal().format(Formatos.DATA));
        }
        Map.Entry<YearMonth, Long> pico = est.mesPico();
        if (pico != null) a.linha("Mês com mais focos", Estatisticas.MESES[pico.getKey().getMonthValue() - 1] + "/" + pico.getKey().getYear()
                + " (" + Formatos.inteiro(pico.getValue()) + " focos)");
        a.vazia();
        a.subtitulo("Principais achados");
        for (String s : new RelatorioPdf(ctx).achados(new ArrayList<>(est.porAno().keySet()))) a.linha("—", s);
        a.ajustar();
        a.sheet.setColumnWidth(0, 30 * 256);
        a.sheet.setColumnWidth(1, 120 * 256);
    }

    private void abaSobre(XSSFWorkbook wb, Estilos es, ContextoRelatorio ctx) {
        Aba a = new Aba(wb, es, "Sobre os dados");
        a.titulo("Sobre os dados");
        a.nota("De onde vêm os números desta planilha e como foram tratados.");
        a.vazia();
        a.cabecalho("Tópico", "Descrição");
        RelatorioCarga rel = ctx.base().getRelatorio();
        a.linha("Fonte", "Programa Queimadas do INPE (BDQueimadas), focos do satélite de referência (AQUA, passagem no início da tarde).");
        a.linha("Arquivos", String.join(", ", nomes(ctx.base().getFontes())));
        a.linha("Recorte", ctx.filtro());
        if (ctx.base().dataInicial() != null) {
            a.linha("Período", ctx.base().dataInicial().format(Formatos.DATA) + " a " + ctx.base().dataFinal().format(Formatos.DATA));
        }
        a.linha("Limpeza", Formatos.inteiro(rel.getTotalLidas()) + " linhas lidas, " + Formatos.inteiro(rel.getTotalAceitas()) + " aceitas, "
                + Formatos.inteiro(rel.getTotalRejeitadas()) + " rejeitadas e " + Formatos.inteiro(rel.getDuplicadosRemovidos())
                + " duplicadas removidas (detalhes na aba Qualidade dos dados).");
        a.linha("Horário", "data_pas está em GMT; as análises por hora usam a hora local de São Paulo (UTC−3).");
        a.linha("Viés do sensor", "Mesmo satélite em todos os anos: os anos são comparáveis, mas focos curtos, noturnos ou sob nuvens podem não "
                + "ser registrados. Os números indicam a tendência, não o total de queimadas.");
        a.linha("Ordenação", "Algoritmos implementados à mão; cada um acessa os dados por um vetor instrumentado que conta comparações, trocas, "
                + "atribuições e acessos. As ordenações trabalham sobre uma cópia e o resultado é verificado.");
        a.linha("Reprodução", "java -jar aps-queimadas-all.jar resultados");
        a.vazia();
        a.subtitulo("Colunas do arquivo do INPE");
        a.cabecalho("Coluna", "Significado");
        a.linha("id_bdq", "Identificador do registro no BDQueimadas");
        a.linha("foco_id", "Identificador único do foco (usado para remover duplicados)");
        a.linha("lat, lon", "Coordenadas do centro do pixel em graus decimais (WGS84)");
        a.linha("data_pas", "Data e hora da passagem do satélite, em GMT");
        a.linha("pais, estado, municipio", "Localização administrativa atribuída pelo INPE");
        a.linha("bioma", "Bioma do IBGE onde o foco foi detectado");
        a.ajustar();
        a.sheet.setColumnWidth(0, 24 * 256);
        a.sheet.setColumnWidth(1, 120 * 256);
    }

    private void abaQualidade(XSSFWorkbook wb, Estilos es, RelatorioCarga rel) {
        Aba a = new Aba(wb, es, "Qualidade dos dados");
        a.titulo("Relatório de carga e limpeza");
        a.cabecalho("Arquivo", "Encoding", "Separador", "Linhas lidas", "Aceitas", "Rejeitadas", "Colunas");
        for (RelatorioCarga.ResumoArquivo r : rel.getArquivos()) {
            a.linha(r.arquivo().getFileName().toString(), r.encoding().name(), String.valueOf(r.separador()),
                    r.linhasLidas(), r.aceitas(), r.rejeitadas(), String.join(", ", r.colunas()));
        }
        a.vazia();
        a.linha("Duplicados removidos (foco_id repetido)", rel.getDuplicadosRemovidos());
        a.vazia();
        a.cabecalho("Motivo de rejeição", "Quantidade");
        rel.getMotivos().forEach((m, n) -> a.linha(m, n));
        if (rel.getMotivos().isEmpty()) a.linha("Nenhuma linha rejeitada", 0L);
        a.vazia();
        a.cabecalho("Arquivo", "Linha", "Motivo", "Conteúdo");
        a.congelarEFiltrar(4);
        for (RelatorioCarga.Rejeicao r : rel.getRejeicoes()) {
            a.linha(r.arquivo().getFileName().toString(), r.linha(), r.motivo(), r.conteudo());
        }
        a.ajustar();
    }

    private void abaEstatisticas(XSSFWorkbook wb, Estilos es, ContextoRelatorio ctx) {
        Aba a = new Aba(wb, es, "Estatisticas");
        Estatisticas est = new Estatisticas(ctx.focosFiltrados());
        List<Integer> anos = new ArrayList<>(est.porAno().keySet());
        if (anos.size() >= 2) {
            int a1 = anos.get(anos.size() - 2), a2 = anos.get(anos.size() - 1);
            a.titulo("Comparativo mensal " + a1 + " × " + a2);
            a.cabecalho("Mês", String.valueOf(a1), String.valueOf(a2), "Variação");
            int cab = a.linha - 1;
            for (Estatisticas.LinhaComparativo l : est.comparativo(a1, a2)) {
                a.linha(l.mes() == 0 ? "Total" : Estatisticas.MESES[l.mes() - 1], l.anoA(), l.anoB(), new Pct(l.variacao()));
            }
            a.escalaCor(1, cab + 1, cab + 12);
            a.escalaCor(2, cab + 1, cab + 12);
            XSSFChart ch = grafico(a.sheet, "Focos por mês", 5, cab, 14, cab + 18);
            ch.getOrAddLegend().setPosition(LegendPosition.BOTTOM);
            XDDFCategoryAxis x = ch.createCategoryAxis(AxisPosition.BOTTOM);
            XDDFValueAxis y = ch.createValueAxis(AxisPosition.LEFT);
            y.setCrosses(AxisCrosses.AUTO_ZERO);
            XDDFDataSource<String> meses = XDDFDataSourcesFactory.fromStringCellRange(a.sheet, new CellRangeAddress(cab + 1, cab + 12, 0, 0));
            XDDFLineChartData dados = (XDDFLineChartData) ch.createData(ChartTypes.LINE, x, y);
            for (int c = 1; c <= 2; c++) {
                XDDFLineChartData.Series s = (XDDFLineChartData.Series) dados.addSeries(meses,
                        XDDFDataSourcesFactory.fromNumericCellRange(a.sheet, new CellRangeAddress(cab + 1, cab + 12, c, c)));
                s.setTitle(String.valueOf(c == 1 ? a1 : a2), new CellReference(a.sheet.getSheetName(), cab, c, true, true));
                s.setSmooth(false);
                s.setMarkerStyle(MarkerStyle.NONE);
                s.setShapeProperties(traco(c == 2 ? Identidade.BRASA_GRAFICO : Identidade.ANO_ANTERIOR, c == 2 ? 2.5 : 1.5));
            }
            ch.plot(dados);
            while (a.linha < cab + 20) a.vazia();
        }
        a.subtitulo("Focos por bioma");
        a.cabecalho("Bioma", "Focos", "% do total");
        int ib = a.linha;
        for (Contagem c : est.porBioma()) a.linha(c.chave(), c.total(), new Pct(100.0 * c.total() / Math.max(1, est.total())));
        a.barrasDados(1, ib, a.linha - 1);
        a.vazia();
        a.subtitulo("Top 20 municípios");
        a.cabecalho("Posição", "Município", "Focos");
        int it = a.linha;
        int i = 1;
        List<Contagem> top = est.topMunicipios(20);
        for (Contagem c : top) a.linha((long) i++, c.chave(), c.total());
        a.barrasDados(2, it, a.linha - 1);
        if (!top.isEmpty()) {
            XSSFChart ch = grafico(a.sheet, "Os 20 municípios com mais focos", 5, it - 1, 14, it + 22);
            XDDFCategoryAxis x = ch.createCategoryAxis(AxisPosition.LEFT);
            x.setOrientation(AxisOrientation.MAX_MIN);
            XDDFValueAxis y = ch.createValueAxis(AxisPosition.BOTTOM);
            y.setCrosses(AxisCrosses.AUTO_ZERO);
            XDDFBarChartData barras = (XDDFBarChartData) ch.createData(ChartTypes.BAR, x, y);
            barras.setBarDirection(BarDirection.BAR);
            barras.setVaryColors(false);
            XDDFBarChartData.Series s = (XDDFBarChartData.Series) barras.addSeries(
                    XDDFDataSourcesFactory.fromStringCellRange(a.sheet, new CellRangeAddress(it, it + top.size() - 1, 1, 1)),
                    XDDFDataSourcesFactory.fromNumericCellRange(a.sheet, new CellRangeAddress(it, it + top.size() - 1, 2, 2)));
            s.setTitle("Focos", null);
            s.setShapeProperties(preenchimento(Identidade.BRASA_GRAFICO));
            ch.plot(barras);
        }
        a.ajustar();
    }

    private void abaOrdenacao(XSSFWorkbook wb, Estilos es, ResultadoOrdenacao<FocoIncendio> r) {
        Aba a = new Aba(wb, es, "Ordenacao");
        a.titulo("Dados ordenados: " + r.algoritmo() + " · " + r.criterio());
        a.cabecalho("Algoritmo", "Critério", "Cenário", "n", "Comparações", "Trocas", "Atribuições", "Acessos", "Tempo (ms)", "Verificado");
        a.linha(r.algoritmo(), r.criterio(), r.cenario().toString(), (long) r.tamanho(), r.metricas().comparacoes(),
                r.metricas().trocas(), r.metricas().atribuicoes(), r.metricas().acessos(), r.metricas().millis(),
                r.verificado() ? "sim" : "NÃO");
        a.vazia();
        a.cabecalho("Posição", "Data/hora (GMT)", "Município", "Bioma", "Latitude", "Longitude", "id_bdq");
        a.congelarEFiltrar(7);
        int i = 1;
        for (FocoIncendio f : r.dados()) {
            a.linha((long) i++, f.getDataHora().format(Formatos.DATA_HORA), f.getMunicipio(), f.getBioma(),
                    f.getLatitude(), f.getLongitude(), f.getIdBdq());
        }
        a.ajustar();
    }

    private void abaComparativo(XSSFWorkbook wb, Estilos es, List<ResultadoOrdenacao<FocoIncendio>> lista) {
        Aba a = new Aba(wb, es, "Comparativo algoritmos");
        a.titulo("Todos os algoritmos sobre a mesma entrada");
        a.cabecalho("Algoritmo", "Critério", "Cenário", "n", "Comparações", "Trocas", "Atribuições", "Acessos", "Tempo (ms)", "Verificado");
        a.congelarEFiltrar(10);
        int de = a.linha;
        for (ResultadoOrdenacao<FocoIncendio> r : lista) {
            a.linha(r.algoritmo(), r.criterio(), r.cenario().toString(), (long) r.tamanho(), r.metricas().comparacoes(),
                    r.metricas().trocas(), r.metricas().atribuicoes(), r.metricas().acessos(), r.metricas().millis(),
                    r.verificado() ? "sim" : "NÃO");
        }
        a.escalaCor(4, de, a.linha - 1);
        a.escalaCor(8, de, a.linha - 1);
        a.ajustar();
    }

    private void abaBenchmark(XSSFWorkbook wb, Estilos es, List<BenchmarkResult> res) {
        Aba a = new Aba(wb, es, "Benchmark");
        a.titulo("Benchmark (tempo = média das repetições após o aquecimento do JIT)");
        a.cabecalho("Algoritmo", "Critério", "Cenário", "n", "Média (ms)", "Desvio (ms)", "Min (ms)", "Max (ms)",
                "Comparações", "Trocas", "Atribuições", "Acessos", "n log2 n", "n²/2", "Verificado");
        a.congelarEFiltrar(15);
        for (BenchmarkResult r : res) {
            a.linha(r.algoritmo(), r.criterio().rotulo(), r.cenario().toString(), (long) r.n(), r.mediaMs(), r.desvioMs(),
                    r.minNs() / 1e6, r.maxNs() / 1e6, r.comparacoes(), r.trocas(), r.atribuicoes(), r.acessos(),
                    r.referenciaNLogN(), r.referenciaN2(), r.verificado() ? "sim" : "NÃO");
        }
        a.ajustar();
    }

    private void abaCurvas(XSSFWorkbook wb, Estilos es, List<BenchmarkResult> res) {
        BenchmarkResult ref = RelatorioPdf.referencia(res);
        List<String> algs = new ArrayList<>();
        List<Integer> ns = new ArrayList<>();
        Map<String, Double> valores = new java.util.HashMap<>();
        for (BenchmarkResult r : res) {
            if (r.criterio() != ref.criterio() || r.cenario() != ref.cenario()) continue;
            if (!algs.contains(r.algoritmo())) algs.add(r.algoritmo());
            if (!ns.contains(r.n())) ns.add(r.n());
            valores.put(r.algoritmo() + "|" + r.n(), r.mediaMs());
        }
        for (int i = 1; i < ns.size(); i++) {
            int x = ns.get(i), j = i - 1;
            while (j >= 0 && ns.get(j) > x) {
                ns.set(j + 1, ns.get(j));
                j--;
            }
            ns.set(j + 1, x);
        }
        Aba a = new Aba(wb, es, "Curvas");
        a.titulo("Tempo médio (ms) por tamanho de entrada");
        a.nota("Critério " + ref.criterio().rotulo() + ", cenário " + ref.cenario() + ". Gráfico em escala log-log: a inclinação é o expoente k do custo.");
        String[] cab = new String[algs.size() + 1];
        cab[0] = "n";
        for (int i = 0; i < algs.size(); i++) cab[i + 1] = algs.get(i);
        a.cabecalho(cab);
        int cabLinha = a.linha - 1;
        for (int n : ns) {
            Object[] l = new Object[algs.size() + 1];
            l[0] = (long) n;
            for (int i = 0; i < algs.size(); i++) l[i + 1] = valores.getOrDefault(algs.get(i) + "|" + n, Double.NaN);
            a.linha(l);
        }
        a.ajustar();
        if (ns.size() < 2 || algs.isEmpty()) return;
        Map<String, Double> k = RelatorioPdf.expoentes(res);
        XSSFChart ch = grafico(a.sheet, "Tempo médio por n (log-log)", 0, a.linha + 1, Math.max(10, algs.size() + 1), a.linha + 28);
        ch.getOrAddLegend().setPosition(LegendPosition.RIGHT);
        XDDFValueAxis x = ch.createValueAxis(AxisPosition.BOTTOM);
        XDDFValueAxis y = ch.createValueAxis(AxisPosition.LEFT);
        x.setLogBase(10);
        y.setLogBase(10);
        x.setTitle("n");
        y.setTitle("ms");
        y.setCrosses(AxisCrosses.MIN);
        x.setCrosses(AxisCrosses.MIN);
        XDDFScatterChartData dados = (XDDFScatterChartData) ch.createData(ChartTypes.SCATTER, x, y);
        dados.setStyle(ScatterStyle.LINE_MARKER);
        XDDFNumericalDataSource<Double> xs = XDDFDataSourcesFactory.fromNumericCellRange(a.sheet,
                new CellRangeAddress(cabLinha + 1, cabLinha + ns.size(), 0, 0));
        Color[] quentes = {new Color(0xE8, 0x59, 0x0C), new Color(0xB9, 0x1C, 0x1C), new Color(0xF5, 0x9E, 0x0B), new Color(0x7C, 0x2D, 0x12),
                new Color(0xDB, 0x27, 0x77), new Color(0x92, 0x40, 0x0E), new Color(0xEA, 0x58, 0x0C)};
        Color[] frios = {new Color(0x47, 0x54, 0x67), new Color(0x8A, 0x94, 0xA6), new Color(0x1D, 0x29, 0x39)};
        int q = 0, f = 0;
        for (int i = 0; i < algs.size(); i++) {
            XDDFScatterChartData.Series s = (XDDFScatterChartData.Series) dados.addSeries(xs,
                    XDDFDataSourcesFactory.fromNumericCellRange(a.sheet, new CellRangeAddress(cabLinha + 1, cabLinha + ns.size(), i + 1, i + 1)));
            s.setTitle(algs.get(i), new CellReference(a.sheet.getSheetName(), cabLinha, i + 1, true, true));
            s.setSmooth(false);
            s.setMarkerStyle(MarkerStyle.CIRCLE);
            boolean quad = k.getOrDefault(algs.get(i), 0.0) >= 1.5;
            s.setShapeProperties(traco(quad ? frios[f++ % frios.length] : quentes[q++ % quentes.length], 1.75));
        }
        ch.plot(dados);
    }

    private void abaComplexidade(XSSFWorkbook wb, Estilos es, List<BenchmarkResult> res) {
        Aba a = new Aba(wb, es, "Complexidade empirica");
        a.titulo("Expoente empírico k (regressão log-log: custo ≈ c·nᵏ)");
        a.cabecalho("Algoritmo", "Critério", "Cenário", "k (tempo)", "k (comparações)", "R² (tempo)", "Classe estimada");
        a.congelarEFiltrar(7);
        int de = a.linha;
        for (AnaliseComplexidade.Estimativa e : AnaliseComplexidade.estimar(res)) {
            a.linha(e.algoritmo(), e.criterio().rotulo(), e.cenario().toString(), e.expoenteTempo(),
                    e.expoenteComparacoes(), e.r2Tempo(), e.classificacao());
        }
        a.escalaCor(3, de, a.linha - 1);
        a.ajustar();
    }

    private void abaMl(XSSFWorkbook wb, Estilos es, Preditor.ResultadoML ml) {
        Aba a = new Aba(wb, es, "Machine Learning");
        var reg = ml.regressao();
        a.titulo("Previsão de focos por município/mês — Random Forest (treino " + reg.anoTreino() + ", teste " + reg.anoTeste() + ")");
        a.cabecalho("Modelo", "MAE", "RMSE", "R²");
        linhaReg(a, "Random Forest (treino fixo)", reg.modelo());
        linhaReg(a, "Random Forest (janela expansível)", reg.janelaExpansivel());
        linhaReg(a, "Baseline persistência (lag1)", reg.persistencia());
        linhaReg(a, "Baseline média histórica", reg.mediaHistorica());
        a.vazia();
        a.cabecalho("Mês", "Focos reais (estado)", "Previstos RF treino fixo", "Previstos RF janela expansível");
        int cab = a.linha - 1;
        reg.realPorMes().forEach((m, v) -> a.linha(m.toString(), v, reg.previstoPorMes().get(m),
                reg.previstoJanelaPorMes().getOrDefault(m, 0.0)));
        int fim = a.linha - 1;
        if (fim > cab) {
            XSSFChart ch = grafico(a.sheet, "Focos no estado: real e previsto", 6, cab, 15, cab + 18);
            ch.getOrAddLegend().setPosition(LegendPosition.BOTTOM);
            XDDFCategoryAxis x = ch.createCategoryAxis(AxisPosition.BOTTOM);
            XDDFValueAxis y = ch.createValueAxis(AxisPosition.LEFT);
            y.setCrosses(AxisCrosses.AUTO_ZERO);
            XDDFDataSource<String> meses = XDDFDataSourcesFactory.fromStringCellRange(a.sheet, new CellRangeAddress(cab + 1, fim, 0, 0));
            XDDFLineChartData dados = (XDDFLineChartData) ch.createData(ChartTypes.LINE, x, y);
            for (int c = 1; c <= 2; c++) {
                XDDFLineChartData.Series s = (XDDFLineChartData.Series) dados.addSeries(meses,
                        XDDFDataSourcesFactory.fromNumericCellRange(a.sheet, new CellRangeAddress(cab + 1, fim, c, c)));
                s.setTitle(c == 1 ? "Real" : "Previsto", new CellReference(a.sheet.getSheetName(), cab, c, true, true));
                s.setSmooth(false);
                s.setMarkerStyle(MarkerStyle.NONE);
                s.setShapeProperties(traco(c == 1 ? Identidade.BRASA_GRAFICO : Identidade.ANO_ANTERIOR, c == 1 ? 2.5 : 1.5));
            }
            ch.plot(dados);
        }
        a.vazia();
        a.cabecalho("Variável", "Importância (RF regressão)", "Importância (RF classificação)");
        int iv = a.linha;
        for (int i = 0; i < BaseMensal.VARIAVEIS.length; i++) {
            a.linha(BaseMensal.VARIAVEIS[i], reg.importancia()[i], ml.classificacao().importancia()[i]);
        }
        a.barrasDados(1, iv, a.linha - 1);
        a.vazia();
        var cls = ml.classificacao();
        a.subtitulo("Classificação do nível de atividade (baixo / médio / alto)");
        a.cabecalho("Métrica", "Random Forest", "Baseline (classe majoritária)");
        a.linha("Acurácia", cls.metricas().acuracia(), cls.baselineAcuracia());
        a.linha("F1 macro", cls.metricas().f1Macro(), cls.baselineF1Macro());
        a.vazia();
        NivelAtividade[] niveis = NivelAtividade.values();
        a.cabecalho("Real \\ Previsto", niveis[0].toString(), niveis[1].toString(), niveis[2].toString(), "Precisão", "Revocação", "F1");
        Metricas.Classificacao m = cls.metricas();
        int im = a.linha;
        for (int i = 0; i < niveis.length; i++) {
            a.linha(niveis[i].toString(), (long) m.matriz()[i][0], (long) m.matriz()[i][1], (long) m.matriz()[i][2],
                    m.precisao()[i], m.revocacao()[i], m.f1()[i]);
        }
        for (int c = 1; c <= 3; c++) a.escalaCor(c, im, a.linha - 1);
        a.vazia();
        for (ClusterizacaoHotspots.Resultado c : List.of(ml.dbscan(), ml.kMeans())) {
            a.subtitulo("Hotspots · " + c.metodo() + (c.ruido() > 0 ? " · ruído: " + c.ruido() + " focos" : ""));
            a.cabecalho("#", "Focos", "Latitude", "Longitude", "Raio (km)", "Município principal", "Bioma", "Focos por ano");
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

    /** Gera o relatorio em PDF (A4, retrato) com capa, sumario, resumo executivo e notas metodologicas. */
    public Path exportarPdf(ContextoRelatorio ctx, Path destino) throws ApsException {
        try {
            criarPasta(destino);
            try (OutputStream out = Files.newOutputStream(destino)) {
                new RelatorioPdf(ctx).gerar(out);
            }
        } catch (IOException | DocumentException e) {
            throw falha(destino, e);
        }
        LOG.info(() -> "PDF gravado: " + destino.toAbsolutePath());
        return destino;
    }

    /** Utilitarios de formatacao do PDF. */
    static final class Pdf {
        static final Font SOBRETITULO = Identidade.pdf(7.5f, true, Identidade.BRASA);
        static final Font TITULO = Identidade.pdf(16, true, Identidade.TEXTO);
        static final Font SUBTITULO = Identidade.pdf(11.5f, true, Identidade.TEXTO);
        static final Font NORMAL = Identidade.pdf(9.5f, false, Identidade.TEXTO_2);
        static final Font PEQUENA = Identidade.pdf(8, false, Identidade.TEXTO_3);
        static final Font CELULA = Identidade.pdf(7.5f, false, Identidade.TEXTO);
        static final Font CABECALHO = Identidade.pdf(7.5f, true, Color.WHITE);

        private Pdf() { }

        static PdfPTable tabela(float[] larguras, String... cab) {
            PdfPTable t = new PdfPTable(larguras);
            t.setWidthPercentage(100);
            t.setSpacingBefore(4);
            t.setSpacingAfter(8);
            t.setHeaderRows(1);
            for (String c : cab) {
                PdfPCell cell = new PdfPCell(new Phrase(limpar(c), CABECALHO));
                cell.setBackgroundColor(Identidade.TINTA);
                cell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
                cell.setPaddingTop(5);
                cell.setPaddingBottom(6);
                cell.setPaddingLeft(5);
                cell.setPaddingRight(5);
                t.addCell(cell);
            }
            return t;
        }

        static void linha(PdfPTable t, String... v) {
            for (String s : v) {
                PdfPCell cell = new PdfPCell(new Phrase(limpar(s == null ? "" : s), CELULA));
                cell.setBorder(com.lowagie.text.Rectangle.BOTTOM);
                cell.setBorderColor(Identidade.BORDA);
                cell.setBorderWidthBottom(0.6f);
                cell.setPaddingTop(3.5f);
                cell.setPaddingBottom(5);
                cell.setPaddingLeft(5);
                cell.setPaddingRight(5);
                t.addCell(cell);
            }
        }

        /** Alinha a direita as colunas numericas a partir de {@code desde}, inclusive o cabecalho. */
        static void alinharNumeros(PdfPTable t, int desde) {
            for (com.lowagie.text.pdf.PdfPRow r : t.getRows()) {
                PdfPCell[] cells = r.getCells();
                for (int i = desde; i < cells.length; i++) if (cells[i] != null) cells[i].setHorizontalAlignment(Element.ALIGN_RIGHT);
            }
        }

        static String limpar(String s) {
            if (s == null) return "";
            if (Identidade.interDisponivel()) return s;
            return s.replace("→", "->").replace("↑", "(cresc.)").replace("↓", "(decresc.)")
                    .replace("≈", "~").replace("·", ".").replace("Ω", "Omega").replace("✔", "OK");
        }
    }

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
