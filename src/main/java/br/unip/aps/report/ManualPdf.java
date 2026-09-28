package br.unip.aps.report;

import br.unip.aps.ApsException;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.ListItem;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Gera o manual do usuario em PDF a partir de docs/MANUAL.md (titulos, paragrafos, listas, negrito, codigo e imagens). */
public final class ManualPdf {
    private static final Pattern IMAGEM = Pattern.compile("^!\\[([^]]*)]\\(([^)]+)\\)$");
    private static final Pattern DESTAQUE = Pattern.compile("\\*\\*(.+?)\\*\\*|`(.+?)`");

    private ManualPdf() { }

    /** Converte o Markdown em PDF; imagens sao resolvidas relativas a pasta do Markdown. */
    public static Path gerar(Path markdown, Path destino) throws ApsException {
        try {
            List<String> linhas = Files.readAllLines(markdown, StandardCharsets.UTF_8);
            Files.createDirectories(destino.toAbsolutePath().getParent());
            try (OutputStream out = Files.newOutputStream(destino)) {
                Document doc = new Document(PageSize.A4, 56, 56, 64, 60);
                PdfWriter w = PdfWriter.getInstance(doc, out);
                w.setPageEvent(new Rodape());
                doc.addTitle("APS Queimadas — Manual do usuário");
                doc.open();
                com.lowagie.text.List lista = null;
                for (String bruta : linhas) {
                    String l = bruta.strip();
                    if (!l.startsWith("- ") && lista != null) {
                        doc.add(lista);
                        lista = null;
                    }
                    if (l.isEmpty()) continue;
                    Matcher img = IMAGEM.matcher(l);
                    if (l.startsWith("# ")) {
                        doc.add(titulo(l.substring(2), 34));
                    } else if (l.startsWith("## ")) {
                        doc.add(new Chunk(new LineSeparator(1.6f, 100, Identidade.TINTA, Element.ALIGN_LEFT, -6)));
                        doc.add(titulo(l.substring(3), 20));
                    } else if (img.matches()) {
                        Path arquivo = markdown.toAbsolutePath().getParent().resolve(img.group(2)).normalize();
                        if (Files.exists(arquivo)) {
                            Image i = Image.getInstance(arquivo.toString());
                            i.scaleToFit(doc.getPageSize().getWidth() - 112, 300);
                            i.setAlignment(Element.ALIGN_CENTER);
                            i.setSpacingBefore(6);
                            i.setSpacingAfter(4);
                            doc.add(i);
                            Paragraph legenda = new Paragraph(img.group(1), Identidade.pdf(8, false, Identidade.TEXTO_3));
                            legenda.setAlignment(Element.ALIGN_CENTER);
                            legenda.setSpacingAfter(10);
                            doc.add(legenda);
                        }
                    } else if (l.startsWith("- ")) {
                        if (lista == null) {
                            lista = new com.lowagie.text.List(false, 12);
                            lista.setListSymbol(new Chunk("•  ", Identidade.pdf(10, true, Identidade.BRASA)));
                        }
                        ListItem item = new ListItem(formatar(l.substring(2), 10));
                        item.setLeading(15);
                        item.setSpacingAfter(4);
                        lista.add(item);
                    } else {
                        Paragraph p = new Paragraph(formatar(l, 10));
                        p.setLeading(15);
                        p.setSpacingAfter(8);
                        doc.add(p);
                    }
                }
                if (lista != null) doc.add(lista);
                doc.close();
            }
            return destino;
        } catch (IOException | DocumentException e) {
            throw new ApsException("Não foi possível gerar o manual em PDF: " + e.getMessage(), e);
        }
    }

    private static Paragraph titulo(String texto, float tamanho) {
        Paragraph p = new Paragraph(ReportExporter.Pdf.limpar(texto), Identidade.titulo(tamanho, tamanho > 30, Identidade.TEXTO));
        p.setLeading(tamanho * 1.15f);
        p.setSpacingBefore(tamanho > 30 ? 0 : 6);
        p.setSpacingAfter(8);
        return p;
    }

    /** Aplica **negrito** e `codigo` inline. */
    static Phrase formatar(String texto, float tamanho) {
        Phrase ph = new Phrase();
        Font normal = Identidade.pdf(tamanho, false, Identidade.TEXTO_2);
        Matcher m = DESTAQUE.matcher(texto);
        int i = 0;
        while (m.find()) {
            if (m.start() > i) ph.add(new Chunk(ReportExporter.Pdf.limpar(texto.substring(i, m.start())), normal));
            if (m.group(1) != null) ph.add(new Chunk(ReportExporter.Pdf.limpar(m.group(1)), Identidade.pdf(tamanho, true, Identidade.TEXTO)));
            else ph.add(new Chunk(m.group(2), new Font(Font.COURIER, tamanho - 0.5f, Font.NORMAL, Identidade.TEXTO)));
            i = m.end();
        }
        if (i < texto.length()) ph.add(new Chunk(ReportExporter.Pdf.limpar(texto.substring(i)), normal));
        return ph;
    }

    private static final class Rodape extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter w, Document d) {
            PdfContentByte cb = w.getDirectContent();
            Font f = Identidade.pdf(7.5f, false, Identidade.TEXTO_3);
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase("APS Queimadas · Manual do usuário", f), d.left(), d.bottom() - 24, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase("Página " + w.getPageNumber(), f), d.right(), d.bottom() - 24, 0);
        }
    }
}
