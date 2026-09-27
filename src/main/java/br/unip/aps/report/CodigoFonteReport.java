package br.unip.aps.report;

import br.unip.aps.ApsException;
import br.unip.aps.sorting.Ordenacoes;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

/** Gera o "Relatorio com as linhas de codigo" exigido na APS. */
public final class CodigoFonteReport {
    private static final Logger LOG = Logger.getLogger(CodigoFonteReport.class.getName());

    private static final List<String> PASTAS = List.of("src/main/java", "src/main/resources", "src/test/java");
    private static final List<String> EXTENSOES = List.of(".java", ".fxml", ".css", ".html", ".properties");

    /** Arquivo do relatorio com a quantidade de linhas. */
    public record Arquivo(Path caminho, List<String> linhas) { }

    /** Coleta os arquivos-fonte do projeto (pom.xml + src), em ordem alfabetica de caminho. */
    public List<Arquivo> coletar(Path raiz) throws ApsException {
        List<Path> caminhos = new ArrayList<>();
        if (Files.isRegularFile(raiz.resolve("pom.xml"))) caminhos.add(raiz.resolve("pom.xml"));
        for (String pasta : PASTAS) {
            Path p = raiz.resolve(pasta);
            if (!Files.isDirectory(p)) continue;
            List<Path> daPasta = new ArrayList<>();
            try (Stream<Path> s = Files.walk(p)) {
                s.filter(Files::isRegularFile)
                        .filter(f -> EXTENSOES.stream().anyMatch(e -> f.getFileName().toString().endsWith(e)))
                        .forEach(daPasta::add);
            } catch (IOException e) {
                throw new ApsException("Falha ao listar " + p + ": " + e.getMessage(), e);
            }
            caminhos.addAll(Ordenacoes.ordenar(daPasta, Comparator.comparing(x -> raiz.relativize(x).toString().replace('\\', '/'))));
        }
        if (caminhos.isEmpty()) {
            throw new ApsException("Nenhum codigo-fonte encontrado em " + raiz.toAbsolutePath()
                    + ". Execute a partir da pasta raiz do projeto.");
        }
        List<Arquivo> r = new ArrayList<>();
        for (Path c : caminhos) {
            try {
                r.add(new Arquivo(raiz.relativize(c), Files.readAllLines(c, StandardCharsets.UTF_8)));
            } catch (IOException e) {
                throw new ApsException("Falha ao ler " + c + ": " + e.getMessage(), e);
            }
        }
        return r;
    }

    /** Gera o relatorio em PDF e TXT. */
    public long gerar(Path raiz, Path pdf, Path txt) throws ApsException {
        List<Arquivo> arquivos = coletar(raiz);
        long total = 0;
        for (Arquivo a : arquivos) total += a.linhas().size();
        gerarTxt(arquivos, txt, total);
        gerarPdf(arquivos, pdf, total);
        final long t = total;
        LOG.info(() -> "Relatorio de codigo: " + arquivos.size() + " arquivos, " + t + " linhas");
        return total;
    }

    private void gerarTxt(List<Arquivo> arquivos, Path txt, long total) throws ApsException {
        try {
            Files.createDirectories(txt.toAbsolutePath().getParent());
            try (BufferedWriter w = Files.newBufferedWriter(txt, StandardCharsets.UTF_8)) {
                w.write("RELATORIO COM AS LINHAS DE CODIGO - APS Queimadas (" + arquivos.size() + " arquivos, " + total + " linhas)\n");
                w.write("Gerado em " + LocalDateTime.now() + "\n\nSUMARIO\n");
                for (Arquivo a : arquivos) w.write(String.format("  %-90s %6d linhas%n", nome(a), a.linhas().size()));
                for (Arquivo a : arquivos) {
                    w.write("\n" + "=".repeat(100) + "\n" + nome(a) + "\n" + "=".repeat(100) + "\n");
                    int n = 1;
                    for (String l : a.linhas()) w.write(String.format("%5d | %s%n", n++, l));
                }
            }
        } catch (IOException e) {
            throw new ApsException("Nao foi possivel gravar " + txt + ": " + e.getMessage(), e);
        }
    }

    private void gerarPdf(List<Arquivo> arquivos, Path pdf, long total) throws ApsException {
        Document doc = new Document(PageSize.A4, 40, 30, 40, 40);
        try {
            Files.createDirectories(pdf.toAbsolutePath().getParent());
            try (OutputStream out = Files.newOutputStream(pdf)) {
                PdfWriter.getInstance(doc, out);
                doc.addTitle("Relatorio com as linhas de codigo - APS Queimadas");
                doc.open();
                Font mono = fonteMonoespacada(7f);
                Font titulo = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(0x8B, 0x1A, 0x1A));
                Font arquivo = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(0x33, 0x33, 0x33));
                doc.add(new Paragraph("Relatorio com as linhas de codigo", titulo));
                doc.add(new Paragraph("APS - Estrutura de Dados | " + arquivos.size() + " arquivos | " + total + " linhas", arquivo));
                doc.add(new Paragraph(" "));
                for (Arquivo a : arquivos) {
                    doc.add(new Paragraph(String.format("%-80s %6d", nome(a), a.linhas().size()), mono));
                }
                for (Arquivo a : arquivos) {
                    doc.newPage();
                    doc.add(new Paragraph(nome(a) + "  (" + a.linhas().size() + " linhas)", arquivo));
                    StringBuilder sb = new StringBuilder();
                    int n = 1;
                    for (String l : a.linhas()) {
                        sb.append(String.format("%4d  %s%n", n++, l.replace("\t", "    ")));
                    }
                    Paragraph p = new Paragraph(sb.toString(), mono);
                    p.setLeading(8.5f);
                    doc.add(p);
                }
                doc.close();
            }
        } catch (IOException | DocumentException e) {
            throw new ApsException("Nao foi possivel gravar " + pdf + ": " + e.getMessage(), e);
        }
    }

    private static Font fonteMonoespacada(float tamanho) {
        for (String f : List.of("C:/Windows/Fonts/consola.ttf", "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
                "/System/Library/Fonts/Menlo.ttc")) {
            if (Files.isRegularFile(Path.of(f)) && f.endsWith(".ttf")) {
                try {
                    return new Font(BaseFont.createFont(f, BaseFont.IDENTITY_H, BaseFont.EMBEDDED), tamanho);
                } catch (IOException | DocumentException ignorada) {
                }
            }
        }
        return new Font(Font.COURIER, tamanho);
    }

    private static String nome(Arquivo a) {
        return a.caminho().toString().replace('\\', '/');
    }
}
