package br.unip.aps.report;

import br.unip.aps.analysis.Contagem;
import br.unip.aps.analysis.Estatisticas;
import br.unip.aps.benchmark.AnaliseComplexidade;
import br.unip.aps.benchmark.BenchmarkResult;
import br.unip.aps.io.RelatorioCarga;
import br.unip.aps.ml.ClusterizacaoHotspots;
import br.unip.aps.ml.Preditor;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.sorting.ResultadoOrdenacao;
import br.unip.aps.util.Formatos;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfShading;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/** Relatorio PDF corporativo: capa, sumario, resumo executivo, graficos vetoriais e notas metodologicas. */
final class RelatorioPdf {
    private static final String[] MESES_EXTENSO = {"janeiro", "fevereiro", "março", "abril", "maio", "junho",
            "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"};
    private static final Color PAPEL_CAPA = new Color(0xF6, 0xEE, 0xE7);
    private static final Color SUAVE_CAPA = new Color(0xC4, 0xB3, 0xA7);
    private static final Color FOGO = new Color(0xFF, 0x6B, 0x1A);
    private static final Color FOGO_CLARO = new Color(0xFF, 0x8C, 0x4A);
    private static final int MAX_LINHAS = 300;

    private final ContextoRelatorio ctx;
    private final Estatisticas est;
    private final LocalDateTime geradoEm = LocalDateTime.now();
    private int figura;
    private int numeroSecao;

    /** Entrada do sumario: titulo da secao e a pagina em que ela comeca. */
    record Secao(String titulo, int pagina) { }

    RelatorioPdf(ContextoRelatorio ctx) {
        this.ctx = ctx;
        this.est = new Estatisticas(ctx.focosFiltrados());
    }

    /** Gera em duas passadas: a primeira descobre as paginas das secoes, a segunda escreve o sumario com elas. */
    List<Secao> gerar(OutputStream out) throws DocumentException, IOException {
        List<Secao> paginas = escrever(OutputStream.nullOutputStream(), null);
        escrever(out, paginas);
        return paginas;
    }

    private List<Secao> escrever(OutputStream out, List<Secao> sumario) throws DocumentException, IOException {
        figura = 0;
        numeroSecao = 0;
        Document doc = new Document(PageSize.A4, 56, 56, 70, 62);
        PdfWriter w = PdfWriter.getInstance(doc, out);
        Eventos ev = new Eventos(ReportExporter.Pdf.limpar(ctx.titulo()));
        w.setPageEvent(ev);
        doc.addTitle(ReportExporter.Pdf.limpar(ctx.titulo()));
        doc.addAuthor("APS UNIP - Estrutura de Dados");
        doc.addSubject("Focos de incêndio (INPE) e análise de algoritmos de ordenação");
        doc.addCreator("APS Queimadas");
        doc.open();
        capa(w);
        doc.add(new Chunk(" "));
        doc.newPage();
        sumario(doc, sumario);
        doc.newPage();

        secao(doc, "Resumo executivo");
        resumoExecutivo(doc, w);
        secao(doc, "Os focos no período");
        dados(doc, w);
        if (!ctx.graficos().isEmpty()) {
            secao(doc, "Gráficos do painel");
            graficosPainel(doc);
        }
        if (ctx.comparativo() != null && !ctx.comparativo().isEmpty()) {
            secao(doc, "Comparativo de algoritmos");
            comparativo(doc, ctx.comparativo());
        }
        if (ctx.benchmark() != null && !ctx.benchmark().isEmpty()) {
            secao(doc, "Benchmark e complexidade");
            benchmark(doc, w, ctx.benchmark());
        }
        if (ctx.ml() != null) {
            secao(doc, "Machine Learning");
            ml(doc, w, ctx.ml());
        }
        if (ctx.ordenacao() != null) {
            secao(doc, "Dados ordenados");
            ordenacao(doc, ctx.ordenacao());
        }
        secao(doc, "Notas metodológicas");
        notas(doc);
        doc.close();
        return ev.secoes;
    }

    private void secao(Document doc, String titulo) throws DocumentException {
        numeroSecao++;
        if (numeroSecao > 1) doc.newPage();
        doc.add(new Chunk(new LineSeparator(2.2f, 100, Identidade.TINTA, Element.ALIGN_LEFT, 0)));
        Paragraph num = new Paragraph(String.format("%02d", numeroSecao), Identidade.pdf(8.5f, true, Identidade.BRASA));
        num.setSpacingBefore(6);
        doc.add(num);
        Chunk c = new Chunk(ReportExporter.Pdf.limpar(titulo), Identidade.titulo(26, false, Identidade.TEXTO));
        c.setGenericTag(titulo);
        Paragraph p = new Paragraph(c);
        p.setLeading(30);
        p.setSpacingAfter(12);
        doc.add(p);
    }

    private static void subtitulo(Document doc, String texto) throws DocumentException {
        Paragraph p = new Paragraph(ReportExporter.Pdf.limpar(texto), Identidade.titulo(15, false, Identidade.TEXTO));
        p.setLeading(18);
        p.setSpacingBefore(12);
        p.setSpacingAfter(3);
        p.setKeepTogether(true);
        doc.add(p);
    }

    private static void texto(Document doc, String texto) throws DocumentException {
        Paragraph p = new Paragraph(ReportExporter.Pdf.limpar(texto), Identidade.pdf(9.5f, false, Identidade.TEXTO_2));
        p.setLeading(14);
        p.setSpacingAfter(6);
        doc.add(p);
    }

    private void figura(Document doc, Image img, String legenda) throws DocumentException {
        figura++;
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setKeepTogether(true);
        t.setSpacingBefore(6);
        t.setSpacingAfter(10);
        PdfPCell ci = new PdfPCell(img, true);
        ci.setBorder(Rectangle.NO_BORDER);
        ci.setPadding(0);
        t.addCell(ci);
        Phrase ph = new Phrase();
        ph.add(new Chunk("Figura " + figura + "  ", Identidade.pdf(8, true, Identidade.TEXTO)));
        ph.add(new Chunk(ReportExporter.Pdf.limpar(legenda), Identidade.pdf(8, false, Identidade.TEXTO_3)));
        PdfPCell cl = new PdfPCell(ph);
        cl.setBorder(Rectangle.TOP);
        cl.setBorderColor(Identidade.BORDA);
        cl.setPaddingTop(5);
        t.addCell(cl);
        doc.add(t);
    }

    private void capa(PdfWriter w) throws DocumentException, IOException {
        PdfContentByte cb = w.getDirectContentUnder();
        Rectangle pg = PageSize.A4;
        cb.setColorFill(Identidade.CARVAO);
        cb.rectangle(0, 0, pg.getWidth(), pg.getHeight());
        cb.fill();
        PdfContentByte c = w.getDirectContent();
        float m = 56;
        logo(w, c, m, pg.getHeight() - 92, 34);
        escrever(c, "APS QUEIMADAS", Identidade.titulo(20, true, PAPEL_CAPA), m + 42, pg.getHeight() - 74);
        escrever(c, "Focos de incêndio · INPE", Identidade.pdf(8.5f, false, SUAVE_CAPA), m + 42, pg.getHeight() - 87);

        String onde = ctx.base().estados().size() == 1 ? capitalizar(ctx.base().estados().get(0)) : "Brasil";
        escrever(c, ("Relatório · focos de incêndio em " + onde).toUpperCase(br.unip.aps.util.Textos.PT_BR),
                Identidade.pdf(8.5f, true, FOGO_CLARO), m, 610);
        ColumnText ct = new ColumnText(c);
        ct.setSimpleColumn(new Phrase(ReportExporter.Pdf.limpar(ctx.titulo()), Identidade.titulo(38, true, PAPEL_CAPA)),
                m, 420, pg.getWidth() - m, 602, 39, Element.ALIGN_LEFT);
        ct.go();

        escrever(c, Formatos.inteiro(est.total()), Identidade.titulo(118, true, FOGO), m - 4, 318);
        escrever(c, (est.total() == 1 ? "foco de incêndio" : "focos de incêndio") + " no recorte deste relatório",
                Identidade.titulo(18, false, PAPEL_CAPA), m, 292);
        faixaTermica(c, m, 222, pg.getWidth() - 2 * m, 34);

        float y = 150;
        Properties g = br.unip.aps.config.Grupo.carregar();
        List<String[]> meta = new ArrayList<>();
        if (ctx.base().dataInicial() != null) {
            meta.add(new String[]{"Período", ctx.base().dataInicial().format(Formatos.DATA) + " a " + ctx.base().dataFinal().format(Formatos.DATA)});
        }
        meta.add(new String[]{"Recorte", ctx.filtro()});
        meta.add(new String[]{"Gerado em", geradoEm.format(Formatos.DATA_HORA)});
        String inst = br.unip.aps.config.Grupo.valor(g, "instituicao"), curso = br.unip.aps.config.Grupo.valor(g, "curso");
        if (inst != null) meta.add(new String[]{"Instituição", inst + (curso != null ? " · " + curso : "")});
        String disc = br.unip.aps.config.Grupo.valor(g, "disciplina");
        if (disc != null) meta.add(new String[]{"Disciplina", disc});
        List<String> nomes = br.unip.aps.config.Grupo.integrantes(g);
        if (!nomes.isEmpty()) meta.add(new String[]{"Equipe", String.join(", ", nomes)});
        c.setColorStroke(new Color(0x3A, 0x2E, 0x28));
        c.setLineWidth(0.8f);
        c.moveTo(m, y + 16);
        c.lineTo(pg.getWidth() - m, y + 16);
        c.stroke();
        for (String[] kv : meta) {
            escrever(c, kv[0].toUpperCase(br.unip.aps.util.Textos.PT_BR), Identidade.pdf(7, true, SUAVE_CAPA), m, y);
            ColumnText v = new ColumnText(c);
            v.setSimpleColumn(new Phrase(ReportExporter.Pdf.limpar(kv[1]), Identidade.pdf(9, false, PAPEL_CAPA)),
                    m + 88, y - 16, pg.getWidth() - m, y + 9, 11, Element.ALIGN_LEFT);
            v.go();
            y -= 18;
        }
        w.setPageEmpty(false);
    }

    private void faixaTermica(PdfContentByte c, float x, float y, float largura, float altura) {
        Map<YearMonth, Long> serie = est.serieMensal();
        if (serie.isEmpty()) return;
        long max = 1;
        for (long v : serie.values()) max = Math.max(max, v);
        float passo = largura / serie.size();
        int i = 0;
        for (Map.Entry<YearMonth, Long> e : serie.entrySet()) {
            double t = e.getValue() == 0 ? 0 : 0.12 + 0.88 * Math.sqrt((double) e.getValue() / max);
            c.setColorFill(Identidade.inferno(t));
            c.rectangle(x + i * passo, y, passo - 1.2f, altura);
            c.fill();
            if (e.getKey().getMonthValue() == 1 || i == 0) {
                escrever(c, String.valueOf(e.getKey().getYear()), Identidade.pdf(7.5f, true, SUAVE_CAPA), x + i * passo, y - 11);
            }
            i++;
        }
        escrever(c, "menos focos", Identidade.pdf(7, false, SUAVE_CAPA), x, y + altura + 6);
        Phrase mais = new Phrase("mais focos", Identidade.pdf(7, false, SUAVE_CAPA));
        ColumnText.showTextAligned(c, Element.ALIGN_RIGHT, mais, x + largura, y + altura + 6, 0);
    }

    private static void logo(PdfWriter w, PdfContentByte c, float x, float y, float tamanho) {
        float s = tamanho / 24f;
        float[][] p = {{12, 1.5f}, {12.6f, 4.8f, 15.5f, 6.6f, 17.2f, 9.4f}, {19.4f, 13, 18.6f, 17.8f, 15, 19.9f},
                {11.6f, 21.9f, 6.9f, 20.8f, 5.3f, 17.2f}, {4.2f, 14.7f, 4.9f, 11.9f, 6.6f, 9.9f},
                {6.9f, 11.6f, 7.8f, 12.8f, 9.2f, 13.3f}, {8.2f, 9.7f, 10.2f, 5.6f, 12, 1.5f}};
        c.saveState();
        c.moveTo(x + p[0][0] * s, y + (24 - p[0][1]) * s);
        for (int i = 1; i < p.length; i++) {
            float[] q = p[i];
            c.curveTo(x + q[0] * s, y + (24 - q[1]) * s, x + q[2] * s, y + (24 - q[3]) * s, x + q[4] * s, y + (24 - q[5]) * s);
        }
        c.closePath();
        c.clip();
        c.newPath();
        c.paintShading(PdfShading.simpleAxial(w, x, y, x, y + tamanho, new Color(0xDD, 0x51, 0x3A), new Color(0xFC, 0xC8, 0x3A)));
        c.restoreState();
        c.setColorFill(new Color(0x0A, 0x08, 0x07));
        float[][] barras = {{9.1f, 2.6f}, {11.2f, 4.4f}, {13.3f, 3.3f}};
        for (float[] b : barras) {
            c.rectangle(x + b[0] * s, y + (24 - 18.6f) * s, 1.6f * s, b[1] * s);
            c.fill();
        }
    }

    private static void escrever(PdfContentByte c, String s, Font f, float x, float y) {
        ColumnText.showTextAligned(c, Element.ALIGN_LEFT, new Phrase(ReportExporter.Pdf.limpar(s), f), x, y, 0);
    }

    private void sumario(Document doc, List<Secao> secoes) throws DocumentException {
        doc.add(new Chunk(new LineSeparator(2.2f, 100, Identidade.TINTA, Element.ALIGN_LEFT, 0)));
        Paragraph t = new Paragraph("Sumário", Identidade.titulo(26, false, Identidade.TEXTO));
        t.setSpacingBefore(8);
        t.setSpacingAfter(14);
        doc.add(t);
        if (secoes == null) return;
        PdfPTable tab = new PdfPTable(new float[]{0.6f, 8, 1});
        tab.setWidthPercentage(100);
        int i = 1;
        for (Secao s : secoes) {
            tab.addCell(celulaSumario(String.format("%02d", i++), Identidade.pdf(10, true, Identidade.BRASA), Element.ALIGN_LEFT));
            tab.addCell(celulaSumario(s.titulo(), Identidade.pdf(11, false, Identidade.TEXTO), Element.ALIGN_LEFT));
            tab.addCell(celulaSumario(String.valueOf(s.pagina()), Identidade.pdf(11, true, Identidade.TEXTO), Element.ALIGN_RIGHT));
        }
        doc.add(tab);
        Paragraph nota = new Paragraph("Os números deste relatório vêm diretamente dos dados carregados e das execuções feitas no programa; "
                + "nenhum valor foi digitado à mão. A seção final explica as fontes, a limpeza e as escolhas de método.",
                Identidade.pdf(8.5f, false, Identidade.TEXTO_3));
        nota.setSpacingBefore(18);
        doc.add(nota);
    }

    private static PdfPCell celulaSumario(String s, Font f, int alinhamento) {
        PdfPCell c = new PdfPCell(new Phrase(ReportExporter.Pdf.limpar(s), f));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(Identidade.BORDA);
        c.setPaddingTop(7);
        c.setPaddingBottom(8);
        c.setHorizontalAlignment(alinhamento);
        return c;
    }

    private void resumoExecutivo(Document doc, PdfWriter w) throws DocumentException {
        List<Integer> anos = new ArrayList<>(est.porAno().keySet());
        List<String[]> kpis = new ArrayList<>();
        kpis.add(new String[]{Formatos.inteiro(est.total()), "focos no recorte"});
        Map.Entry<YearMonth, Long> pico = est.mesPico();
        if (pico != null) kpis.add(new String[]{Estatisticas.MESES[pico.getKey().getMonthValue() - 1] + "/" + String.valueOf(pico.getKey().getYear()).substring(2), "mês de pico"});
        kpis.add(new String[]{Formatos.inteiro(est.municipiosAfetados()), "municípios com focos"});
        if (!est.porBioma().isEmpty()) {
            Contagem b = est.porBioma().get(0);
            kpis.add(new String[]{Formatos.decimal(100.0 * b.total() / Math.max(1, est.total()), 1) + "%", "no bioma " + b.chave()});
        }
        PdfPTable k = new PdfPTable(kpis.size());
        k.setWidthPercentage(100);
        k.setSpacingAfter(14);
        for (String[] kv : kpis) {
            Paragraph n = new Paragraph(ReportExporter.Pdf.limpar(kv[0]), Identidade.titulo(30, true, Identidade.TEXTO));
            n.setLeading(32);
            Paragraph r = new Paragraph(ReportExporter.Pdf.limpar(kv[1]), Identidade.pdf(8, false, Identidade.TEXTO_2));
            r.setLeading(11);
            PdfPCell c = new PdfPCell();
            c.addElement(n);
            c.addElement(r);
            c.setBorder(Rectangle.TOP);
            c.setBorderColor(Identidade.TINTA);
            c.setBorderWidthTop(1.2f);
            c.setPaddingTop(2);
            c.setPaddingRight(10);
            k.addCell(c);
        }
        doc.add(k);

        subtitulo(doc, "Principais achados");
        com.lowagie.text.List lista = new com.lowagie.text.List(false, 12);
        lista.setListSymbol(new Chunk("—  ", Identidade.pdf(9.5f, true, Identidade.BRASA)));
        for (String a : achados(anos)) {
            com.lowagie.text.ListItem it = new com.lowagie.text.ListItem(ReportExporter.Pdf.limpar(a), Identidade.pdf(9.5f, false, Identidade.TEXTO));
            it.setLeading(14);
            it.setSpacingAfter(5);
            lista.add(it);
        }
        doc.add(lista);

        if (!anos.isEmpty()) {
            subtitulo(doc, "A temporada, mês a mês");
            figura(doc, GraficosPdf.linhasMensais(w, 483, 210, seriesMensais(anos)),
                    "Focos detectados por mês em cada ano do recorte; o ano mais recente em destaque e o pico anotado.");
        }
    }

    private List<GraficosPdf.Serie> seriesMensais(List<Integer> anos) {
        List<GraficosPdf.Serie> series = new ArrayList<>();
        for (int i = 0; i < anos.size(); i++) {
            long[] m = est.porMes(anos.get(i));
            double[] v = new double[12];
            for (int j = 0; j < 12; j++) v[j] = m[j];
            series.add(new GraficosPdf.Serie(String.valueOf(anos.get(i)), v, i == anos.size() - 1));
        }
        return series;
    }

    List<String> achados(List<Integer> anos) {
        List<String> r = new ArrayList<>();
        Map<Integer, Long> porAno = est.porAno();
        if (anos.size() >= 2) {
            int a1 = anos.get(anos.size() - 2), a2 = anos.get(anos.size() - 1);
            long v1 = porAno.get(a1), v2 = porAno.get(a2);
            if (v1 > 0) {
                double razao = (double) v2 / v1;
                r.add(a2 + " teve " + Formatos.inteiro(v2) + " focos, "
                        + (razao >= 1.95 ? Formatos.decimal(razao, 1) + " vezes" : (v2 >= v1 ? "+" : "") + Formatos.decimal(100 * (razao - 1), 1) + "% em relação a")
                        + (razao >= 1.95 ? " o total de " : " ") + a1 + " (" + Formatos.inteiro(v1) + ").");
            }
        } else if (anos.size() == 1) {
            r.add(anos.get(0) + " teve " + Formatos.inteiro(porAno.get(anos.get(0))) + " focos no recorte.");
        }
        Map.Entry<YearMonth, Long> pico = est.mesPico();
        if (pico != null) {
            long noAno = porAno.getOrDefault(pico.getKey().getYear(), 1L);
            r.add("O pico foi em " + MESES_EXTENSO[pico.getKey().getMonthValue() - 1] + " de " + pico.getKey().getYear() + ", com "
                    + Formatos.inteiro(pico.getValue()) + " focos: " + Formatos.decimal(100.0 * pico.getValue() / Math.max(1, noAno), 1) + "% do ano.");
        }
        if (!est.porBioma().isEmpty() && est.total() > 0) {
            Contagem b = est.porBioma().get(0);
            Contagem m = est.topMunicipios(1).get(0);
            r.add(b.chave() + " concentra " + Formatos.decimal(100.0 * b.total() / est.total(), 1) + "% dos focos; o município com mais focos é "
                    + capitalizar(m.chave()) + " (" + Formatos.inteiro(m.total()) + ").");
            long[] h = est.porHoraLocal();
            long tarde = h[13] + h[14] + h[15];
            r.add(Formatos.decimal(100.0 * tarde / est.total(), 1) + "% das detecções ocorreram entre 13h e 15h (hora local), "
                    + "horário de passagem do satélite de referência: a série mede tendência, não o total de queimadas.");
        }
        if (ctx.comparativo() != null && !ctx.comparativo().isEmpty()) {
            ResultadoOrdenacao<FocoIncendio> rapido = null, menos = null, lento = null;
            for (ResultadoOrdenacao<FocoIncendio> x : ctx.comparativo()) {
                if (rapido == null || x.metricas().nanos() < rapido.metricas().nanos()) rapido = x;
                if (lento == null || x.metricas().nanos() > lento.metricas().nanos()) lento = x;
                if (menos == null || x.metricas().comparacoes() < menos.metricas().comparacoes()) menos = x;
            }
            r.add("Sobre a mesma entrada (n = " + Formatos.inteiro(rapido.tamanho()) + "), o mais rápido foi o " + rapido.algoritmo()
                    + " (" + Formatos.duracao(rapido.metricas().nanos()) + ") e o mais lento o " + lento.algoritmo() + " ("
                    + Formatos.duracao(lento.metricas().nanos()) + "); o que menos comparou foi o " + menos.algoritmo() + " ("
                    + Formatos.inteiro(menos.metricas().comparacoes()) + " comparações).");
        }
        if (ctx.benchmark() != null && !ctx.benchmark().isEmpty()) {
            Map<String, Double> k = expoentes(ctx.benchmark());
            List<String> lin = new ArrayList<>(), quad = new ArrayList<>();
            double lmin = 9, lmax = 0, qmin = 9, qmax = 0;
            for (Map.Entry<String, Double> e : k.entrySet()) {
                if (e.getValue() >= 1.5) {
                    quad.add(e.getKey());
                    qmin = Math.min(qmin, e.getValue());
                    qmax = Math.max(qmax, e.getValue());
                } else {
                    lin.add(e.getKey());
                    lmin = Math.min(lmin, e.getValue());
                    lmax = Math.max(lmax, e.getValue());
                }
            }
            if (!lin.isEmpty() && !quad.isEmpty()) {
                r.add("O expoente empírico do tempo separa dois grupos: " + String.join(", ", lin) + " crescem perto de n log n (k entre "
                        + Formatos.decimal(lmin, 2) + " e " + Formatos.decimal(lmax, 2) + "), enquanto " + String.join(", ", quad)
                        + " crescem perto de n² (k entre " + Formatos.decimal(qmin, 2) + " e " + Formatos.decimal(qmax, 2) + ").");
            }
        }
        if (ctx.ml() != null) {
            var reg = ctx.ml().regressao();
            r.add("Na previsão de focos por município e mês, a Random Forest teve erro médio absoluto de " + Formatos.decimal(reg.modelo().mae(), 2)
                    + " focos, contra " + Formatos.decimal(reg.persistencia().mae(), 2) + " da persistência e "
                    + Formatos.decimal(reg.mediaHistorica().mae(), 2) + " da média histórica (teste em " + reg.anoTeste() + ").");
        }
        return r;
    }

    /** Expoente do tempo por algoritmo, no criterio e cenario mais medidos. */
    static Map<String, Double> expoentes(List<BenchmarkResult> res) {
        BenchmarkResult ref = referencia(res);
        Map<String, Double> r = new LinkedHashMap<>();
        for (AnaliseComplexidade.Estimativa e : AnaliseComplexidade.estimar(res)) {
            if (e.criterio() == ref.criterio() && e.cenario() == ref.cenario() && !Double.isNaN(e.expoenteTempo())) {
                r.put(e.algoritmo(), e.expoenteTempo());
            }
        }
        return r;
    }

    static BenchmarkResult referencia(List<BenchmarkResult> res) {
        Map<String, Integer> cont = new HashMap<>();
        BenchmarkResult melhor = res.get(0);
        int max = 0;
        for (BenchmarkResult r : res) {
            String chave = r.criterio() + "|" + r.cenario();
            int c = cont.merge(chave, 1, Integer::sum);
            boolean aleatorio = "ALEATORIO".equals(r.cenario().name());
            if (c > max || (c == max && aleatorio)) {
                max = c;
                melhor = r;
            }
        }
        return melhor;
    }

    private void dados(Document doc, PdfWriter w) throws DocumentException {
        List<Integer> anos = new ArrayList<>(est.porAno().keySet());
        if (anos.size() >= 2) {
            int a1 = anos.get(anos.size() - 2), a2 = anos.get(anos.size() - 1);
            subtitulo(doc, "Comparativo mensal " + a1 + " × " + a2);
            PdfPTable c = ReportExporter.Pdf.tabela(new float[]{2, 2, 2, 2}, "Mês", String.valueOf(a1), String.valueOf(a2), "Variação");
            for (Estatisticas.LinhaComparativo l : est.comparativo(a1, a2)) {
                ReportExporter.Pdf.linha(c, l.mes() == 0 ? "Total" : Estatisticas.MESES[l.mes() - 1], Formatos.inteiro(l.anoA()),
                        Formatos.inteiro(l.anoB()), Double.isNaN(l.variacao()) ? "–" : (l.variacao() > 0 ? "+" : "") + Formatos.decimal(l.variacao(), 1) + "%");
            }
            ReportExporter.Pdf.alinharNumeros(c, 1);
            doc.add(c);
        }
        List<Contagem> top = est.topMunicipios(10);
        if (!top.isEmpty()) {
            subtitulo(doc, "Onde queima");
            List<String> rot = new ArrayList<>();
            List<Long> val = new ArrayList<>();
            for (Contagem x : top) {
                rot.add(capitalizar(x.chave()));
                val.add(x.total());
            }
            figura(doc, GraficosPdf.barras(w, 483, rot, val), "Os 10 municípios com mais focos no recorte.");
        }
        subtitulo(doc, "Em que bioma");
        PdfPTable b = ReportExporter.Pdf.tabela(new float[]{4, 2, 2}, "Bioma", "Focos", "% do total");
        for (Contagem x : est.porBioma()) {
            ReportExporter.Pdf.linha(b, x.chave(), Formatos.inteiro(x.total()), Formatos.decimal(100.0 * x.total() / Math.max(1, est.total()), 1) + "%");
        }
        ReportExporter.Pdf.alinharNumeros(b, 1);
        doc.add(b);

        RelatorioCarga rel = ctx.base().getRelatorio();
        subtitulo(doc, "Qualidade da carga");
        PdfPTable q = ReportExporter.Pdf.tabela(new float[]{4, 2}, "Etapa", "Linhas");
        ReportExporter.Pdf.linha(q, "Lidas dos CSVs", Formatos.inteiro(rel.getTotalLidas()));
        ReportExporter.Pdf.linha(q, "Aceitas", Formatos.inteiro(rel.getTotalAceitas()));
        ReportExporter.Pdf.linha(q, "Rejeitadas (registradas com o motivo)", Formatos.inteiro(rel.getTotalRejeitadas()));
        ReportExporter.Pdf.linha(q, "Duplicadas removidas (mesmo foco_id)", Formatos.inteiro(rel.getDuplicadosRemovidos()));
        ReportExporter.Pdf.alinharNumeros(q, 1);
        doc.add(q);
    }

    private void graficosPainel(Document doc) throws DocumentException, IOException {
        texto(doc, "Capturas dos gráficos como estavam no painel no momento da exportação, com os filtros aplicados.");
        for (ContextoRelatorio.Grafico g : ctx.graficos()) {
            Image img = Image.getInstance(g.imagem(), null);
            figura(doc, img, g.titulo());
        }
    }

    private void comparativo(Document doc, List<ResultadoOrdenacao<FocoIncendio>> lista) throws DocumentException {
        texto(doc, "Os " + lista.size() + " algoritmos compatíveis com o critério ordenaram a mesma cópia dos dados. Critério: " + lista.get(0).criterio() + " · cenário: "
                + lista.get(0).cenario() + " · n = " + Formatos.inteiro(lista.get(0).tamanho()) + ".");
        PdfPTable t = ReportExporter.Pdf.tabela(new float[]{3, 2.2f, 2, 2.2f, 2, 1.4f}, "Algoritmo", "Comparações", "Trocas", "Acessos", "Tempo", "Verificado");
        for (ResultadoOrdenacao<FocoIncendio> r : lista) {
            ReportExporter.Pdf.linha(t, r.algoritmo(), Formatos.inteiro(r.metricas().comparacoes()), Formatos.inteiro(r.metricas().trocas()),
                    Formatos.inteiro(r.metricas().acessos()), Formatos.duracao(r.metricas().nanos()), r.verificado() ? "sim" : "NÃO");
        }
        ReportExporter.Pdf.alinharNumeros(t, 1);
        doc.add(t);
    }

    private void benchmark(Document doc, PdfWriter w, List<BenchmarkResult> res) throws DocumentException {
        BenchmarkResult ref = referencia(res);
        List<BenchmarkResult> curva = new ArrayList<>();
        for (BenchmarkResult r : res) if (r.criterio() == ref.criterio() && r.cenario() == ref.cenario()) curva.add(r);
        Map<String, Boolean> quad = new HashMap<>();
        for (Map.Entry<String, Double> e : expoentes(res).entrySet()) quad.put(e.getKey(), e.getValue() >= 1.5);
        texto(doc, "Tempo médio de cada algoritmo conforme o tamanho da entrada cresce. Em escala log-log, a inclinação da reta é o expoente k "
                + "do custo (≈ nᵏ): retas mais íngremes (cinza) são os algoritmos quadráticos.");
        figura(doc, GraficosPdf.curvasBenchmark(w, 483, 250, curva, quad),
                "Tempo médio por n, critério " + ref.criterio().rotulo() + ", cenário " + ref.cenario() + ", escala log-log.");

        subtitulo(doc, "Expoente empírico (custo ≈ nᵏ)");
        PdfPTable e = ReportExporter.Pdf.tabela(new float[]{2.6f, 1.5f, 2.5f, 1.1f, 1.1f, 1, 1.6f}, "Algoritmo", "Critério", "Cenário", "k tempo", "k comp.", "R²", "Classe");
        for (AnaliseComplexidade.Estimativa x : AnaliseComplexidade.estimar(res)) {
            ReportExporter.Pdf.linha(e, x.algoritmo(), x.criterio().rotulo(), x.cenario().toString(), Formatos.decimal(x.expoenteTempo(), 2),
                    Double.isNaN(x.expoenteComparacoes()) ? "–" : Formatos.decimal(x.expoenteComparacoes(), 2),
                    Formatos.decimal(x.r2Tempo(), 3), x.classificacao());
        }
        ReportExporter.Pdf.alinharNumeros(e, 3);
        doc.add(e);

        subtitulo(doc, "Medições");
        PdfPTable t = ReportExporter.Pdf.tabela(new float[]{2.6f, 1.4f, 2.5f, 1, 1.3f, 1.2f, 1.7f, 1.5f}, "Algoritmo", "Critério", "Cenário", "n",
                "Média ms", "Desvio", "Comparações", "Trocas");
        for (BenchmarkResult r : res) {
            ReportExporter.Pdf.linha(t, r.algoritmo(), r.criterio().rotulo(), r.cenario().toString(), Formatos.inteiro(r.n()),
                    Formatos.decimal(r.mediaMs(), 3), Formatos.decimal(r.desvioMs(), 3), Formatos.inteiro(r.comparacoes()),
                    Formatos.inteiro(r.trocas()));
        }
        ReportExporter.Pdf.alinharNumeros(t, 3);
        doc.add(t);
    }

    private void ml(Document doc, PdfWriter w, Preditor.ResultadoML ml) throws DocumentException {
        var reg = ml.regressao();
        texto(doc, "Previsão do número de focos por município e mês com Random Forest (" + ml.parametros().arvores() + " árvores, semente "
                + ml.parametros().semente() + "). Treino em " + reg.anoTreino() + ", teste em " + reg.anoTeste()
                + ": o modelo nunca vê o ano que precisa prever. Os baselines mostram quanto o modelo acrescenta.");
        PdfPTable t = ReportExporter.Pdf.tabela(new float[]{4, 2, 2, 2}, "Modelo", "MAE", "RMSE", "R²");
        for (var e : List.of(Map.entry("Random Forest (treino fixo)", reg.modelo()),
                Map.entry("Random Forest (janela expansível)", reg.janelaExpansivel()), Map.entry("Persistência (mês anterior)", reg.persistencia()),
                Map.entry("Média histórica", reg.mediaHistorica()))) {
            ReportExporter.Pdf.linha(t, e.getKey(), Formatos.decimal(e.getValue().mae(), 3), Formatos.decimal(e.getValue().rmse(), 3),
                    Formatos.decimal(e.getValue().r2(), 3));
        }
        ReportExporter.Pdf.alinharNumeros(t, 1);
        doc.add(t);
        double[] real = new double[12], prev = new double[12];
        boolean tem = false;
        for (Map.Entry<YearMonth, Double> e : reg.realPorMes().entrySet()) {
            if (e.getKey().getYear() != reg.anoTeste()) continue;
            real[e.getKey().getMonthValue() - 1] = e.getValue();
            Double p = reg.previstoPorMes().get(e.getKey());
            prev[e.getKey().getMonthValue() - 1] = p == null ? 0 : p;
            tem = true;
        }
        if (tem) {
            figura(doc, GraficosPdf.linhasMensais(w, 483, 190, List.of(new GraficosPdf.Serie("Previsto", prev, false),
                    new GraficosPdf.Serie("Real", real, true))), "Focos no estado em " + reg.anoTeste() + ": real e previsto (soma dos municípios).");
        }
        var cls = ml.classificacao();
        texto(doc, "Classificação do nível de atividade (baixo, médio, alto): acurácia " + Formatos.decimal(cls.metricas().acuracia(), 3)
                + " (classe majoritária: " + Formatos.decimal(cls.baselineAcuracia(), 3) + "), F1 macro " + Formatos.decimal(cls.metricas().f1Macro(), 3)
                + " (classe majoritária: " + Formatos.decimal(cls.baselineF1Macro(), 3) + ").");
        subtitulo(doc, "Hotspots · " + ml.dbscan().metodo());
        PdfPTable h = ReportExporter.Pdf.tabela(new float[]{0.6f, 1.2f, 1.6f, 1.6f, 1.2f, 3, 2}, "#", "Focos", "Lat", "Lon", "Raio km", "Município", "Bioma");
        List<ClusterizacaoHotspots.Hotspot> hs = ml.dbscan().hotspots();
        for (ClusterizacaoHotspots.Hotspot x : hs.subList(0, Math.min(15, hs.size()))) {
            ReportExporter.Pdf.linha(h, String.valueOf(x.id()), Formatos.inteiro(x.focos()), Formatos.decimal(x.latitude(), 4),
                    Formatos.decimal(x.longitude(), 4), Formatos.decimal(x.raioKm(), 1), capitalizar(x.municipioPrincipal()), x.biomaPredominante());
        }
        doc.add(h);
    }

    private void ordenacao(Document doc, ResultadoOrdenacao<FocoIncendio> r) throws DocumentException {
        texto(doc, r.algoritmo() + " · " + r.criterio() + " · cenário " + r.cenario() + " · n = " + Formatos.inteiro(r.tamanho())
                + ". Operações: " + r.metricas() + (r.verificado() ? ". Ordenação verificada." : ". FALHA NA VERIFICAÇÃO."));
        PdfPTable t = ReportExporter.Pdf.tabela(new float[]{0.9f, 2.4f, 3.4f, 2.2f, 1.5f, 1.5f}, "#", "Data/hora GMT", "Município", "Bioma", "Lat", "Lon");
        int limite = Math.min(MAX_LINHAS, r.dados().size());
        for (int i = 0; i < limite; i++) {
            FocoIncendio f = r.dados().get(i);
            ReportExporter.Pdf.linha(t, String.valueOf(i + 1), f.getDataHora().format(Formatos.DATA_HORA), f.getMunicipio(), f.getBioma(),
                    Formatos.decimal(f.getLatitude(), 4), Formatos.decimal(f.getLongitude(), 4));
        }
        doc.add(t);
        if (r.dados().size() > limite) {
            texto(doc, "Exibidas as primeiras " + limite + " de " + Formatos.inteiro(r.dados().size()) + " linhas; a lista completa está no Excel e no CSV.");
        }
    }

    private void notas(Document doc) throws DocumentException {
        RelatorioCarga rel = ctx.base().getRelatorio();
        List<String> fontes = new ArrayList<>();
        for (var p : ctx.base().getFontes()) fontes.add(p.getFileName().toString());
        nota(doc, "Fonte", "Programa Queimadas do INPE (BDQueimadas), focos do satélite de referência (AQUA, passagem no início da tarde). "
                + "Arquivos: " + String.join(", ", fontes) + ".");
        nota(doc, "Viés do sensor", "O satélite de referência é o mesmo em todos os anos, o que torna os anos comparáveis. Em compensação, um "
                + "foco curto, noturno ou coberto por nuvens pode não ser registrado; por isso os números indicam a tendência, e não o total de queimadas.");
        nota(doc, "Horário", "A coluna data_pas vem em GMT. As análises por hora usam a hora local de São Paulo (UTC−3).");
        nota(doc, "Limpeza", Formatos.inteiro(rel.getTotalLidas()) + " linhas lidas, " + Formatos.inteiro(rel.getTotalAceitas()) + " aceitas, "
                + Formatos.inteiro(rel.getTotalRejeitadas()) + " rejeitadas e " + Formatos.inteiro(rel.getDuplicadosRemovidos())
                + " duplicadas removidas. Linhas inválidas não interrompem a carga: ficam registradas com o motivo na aba Qualidade dos dados.");
        nota(doc, "Ordenação", "Os treze algoritmos foram implementados à mão, sem Collections.sort, Arrays.sort ou estruturas ordenadas da "
                + "biblioteca padrão (um teste de arquitetura impede o uso). Cada algoritmo acessa os dados apenas por um vetor instrumentado, "
                + "que conta comparações, trocas, atribuições e acessos. A ordenação trabalha sobre uma cópia e o resultado é verificado.");
        if (ctx.benchmark() != null && !ctx.benchmark().isEmpty()) {
            int rmin = Integer.MAX_VALUE, rmax = 0;
            for (BenchmarkResult r : ctx.benchmark()) {
                rmin = Math.min(rmin, r.repeticoes());
                rmax = Math.max(rmax, r.repeticoes());
            }
            nota(doc, "Benchmark", "Cada ponto é a média de " + (rmin == rmax ? String.valueOf(rmin) : rmin + " a " + rmax)
                    + " repetições, medidas depois do aquecimento do JIT, com entrada gerada por semente fixa. O expoente k vem da regressão "
                    + "linear de log(tempo) sobre log(n). Os tempos foram conferidos com o JMH (docs/resultados/jmh.md).");
        }
        if (ctx.ml() != null) {
            nota(doc, "Machine Learning", "Validação temporal: treino em anos anteriores e teste no ano seguinte, sem embaralhar. "
                    + "Semente " + ctx.ml().parametros().semente() + ". Métricas comparadas com baselines ingênuos (persistência, média histórica e classe majoritária).");
        }
        nota(doc, "Reprodução", "Todos os números podem ser refeitos com: java -jar aps-queimadas-all.jar resultados. "
                + "Relatório gerado em " + geradoEm.format(Formatos.DATA_HORA) + ".");
    }

    private static void nota(Document doc, String titulo, String texto) throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{1.3f, 5});
        t.setWidthPercentage(100);
        t.setKeepTogether(true);
        PdfPCell a = new PdfPCell(new Phrase(ReportExporter.Pdf.limpar(titulo), Identidade.pdf(9, true, Identidade.TEXTO)));
        PdfPCell b = new PdfPCell(new Phrase(ReportExporter.Pdf.limpar(texto), Identidade.pdf(9, false, Identidade.TEXTO_2)));
        for (PdfPCell c : List.of(a, b)) {
            c.setBorder(Rectangle.TOP);
            c.setBorderColor(Identidade.BORDA);
            c.setPaddingTop(6);
            c.setPaddingBottom(8);
            c.setLeading(0, 1.35f);
            t.addCell(c);
        }
        doc.add(t);
    }

    static String capitalizar(String s) {
        return br.unip.aps.util.Textos.nomeProprio(s);
    }

    /** Cabecalho, rodape com "pagina N de T" e registro das paginas das secoes para o sumario. */
    private static final class Eventos extends PdfPageEventHelper {
        private final String titulo;
        private final List<Secao> secoes = new ArrayList<>();
        private PdfTemplate total;

        Eventos(String titulo) {
            this.titulo = titulo;
        }

        @Override
        public void onOpenDocument(PdfWriter w, Document d) {
            total = w.getDirectContent().createTemplate(30, 12);
        }

        @Override
        public void onGenericTag(PdfWriter w, Document d, Rectangle r, String texto) {
            for (Secao s : secoes) if (s.titulo().equals(texto)) return;
            secoes.add(new Secao(texto, w.getPageNumber()));
        }

        @Override
        public void onEndPage(PdfWriter w, Document d) {
            if (w.getPageNumber() == 1) return;
            PdfContentByte cb = w.getDirectContent();
            Font peq = Identidade.pdf(7.5f, false, Identidade.TEXTO_3);
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase("APS QUEIMADAS", Identidade.pdf(7.5f, true, Identidade.BRASA)),
                    d.left(), d.top() + 26, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase(titulo, peq), d.right(), d.top() + 26, 0);
            cb.setColorStroke(Identidade.BORDA);
            cb.setLineWidth(0.6f);
            cb.moveTo(d.left(), d.top() + 18);
            cb.lineTo(d.right(), d.top() + 18);
            cb.stroke();
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("Análise de Performance de Algoritmos de Ordenação · dados INPE", peq), d.left(), d.bottom() - 24, 0);
            String pag = "Página " + w.getPageNumber() + " de ";
            float largura = Identidade.base(false).getWidthPoint(pag, 7.5f);
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(pag, peq), d.right() - largura - 14, d.bottom() - 24, 0);
            cb.addTemplate(total, d.right() - 14, d.bottom() - 24);
        }

        @Override
        public void onCloseDocument(PdfWriter w, Document d) {
            ColumnText.showTextAligned(total, Element.ALIGN_LEFT,
                    new Phrase(String.valueOf(w.getPageNumber() - 1), Identidade.pdf(7.5f, false, Identidade.TEXTO_3)), 0, 0, 0);
        }
    }
}
