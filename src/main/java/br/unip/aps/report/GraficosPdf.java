package br.unip.aps.report;

import br.unip.aps.analysis.Estatisticas;
import br.unip.aps.benchmark.BenchmarkResult;
import br.unip.aps.util.Formatos;
import com.lowagie.text.BadElementException;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfTemplate;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Graficos vetoriais desenhados direto no PDF: nitidos em qualquer zoom e na impressao. */
final class GraficosPdf {
    private static final float FONTE = 7f;
    private static final Color CLARO = new Color(0xF0, 0xA2, 0x7A);

    private GraficosPdf() { }

    /** Uma serie por dado, com rotulo direto no fim da linha. */
    record Serie(String nome, double[] valores, boolean destaque) { }

    /** Linhas mensais (jan a dez) de cada ano, com o pico anotado. */
    static Image linhasMensais(PdfWriter w, float largura, float altura, List<Serie> series) {
        PdfTemplate t = w.getDirectContent().createTemplate(largura, altura);
        float esq = 42, dir = 40, topo = 14, base = 20;
        float pw = largura - esq - dir, ph = altura - topo - base;
        double max = 1;
        for (Serie s : series) for (double v : s.valores()) max = Math.max(max, v);
        double passo = passoBonito(max / 4), limite = Math.ceil(max / passo) * passo;
        grade(t, esq, base, pw, ph, limite, passo);
        for (int m = 0; m < 12; m++) {
            texto(t, Estatisticas.MESES[m], esq + m * pw / 11, base - 11, Element.ALIGN_CENTER, Identidade.TEXTO_3, false);
        }
        float[] finais = new float[series.size()];
        int ip = -1, mp = 0;
        double vp = -1;
        for (int k = 0; k < series.size(); k++) {
            Serie s = series.get(k);
            if (s.destaque()) continue;
            linha(t, s.valores(), esq, base, pw, ph, limite, Identidade.ANO_ANTERIOR, 1.2f);
        }
        for (int k = 0; k < series.size(); k++) {
            Serie s = series.get(k);
            if (s.destaque()) linha(t, s.valores(), esq, base, pw, ph, limite, Identidade.BRASA_GRAFICO, 1.9f);
            finais[k] = (float) (base + s.valores()[11] / limite * ph);
            for (int m = 0; m < 12; m++) {
                if (s.valores()[m] > vp) {
                    vp = s.valores()[m];
                    ip = k;
                    mp = m;
                }
            }
        }
        float[] ys = afastar(finais, 9);
        for (int k = 0; k < series.size(); k++) {
            Serie s = series.get(k);
            texto(t, s.nome(), esq + pw + 5, ys[k] - 2.5f, Element.ALIGN_LEFT,
                    s.destaque() ? Identidade.BRASA : Identidade.TEXTO_3, true);
        }
        if (ip >= 0 && vp > 0) {
            float x = esq + mp * pw / 11, y = (float) (base + vp / limite * ph);
            t.setColorFill(Identidade.BRASA);
            t.circle(x, y, 2.6f);
            t.fill();
            texto(t, Formatos.inteiro(Math.round(vp)) + " focos · " + Estatisticas.MESES[mp] + "/" + series.get(ip).nome(),
                    x + (mp > 8 ? -6 : 6), Math.min(y - 3, topo + ph + base - 8), mp > 8 ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT,
                    Identidade.TEXTO, true);
        }
        return imagem(t);
    }

    /** Barras horizontais com rotulo a esquerda e valor ao fim da barra; a primeira e a lider. */
    static Image barras(PdfWriter w, float largura, List<String> rotulos, List<Long> valores) {
        float linha = 15, altura = rotulos.size() * linha + 4;
        PdfTemplate t = w.getDirectContent().createTemplate(largura, altura);
        float esq = largura * 0.34f, dir = 44;
        long max = 1;
        for (long v : valores) max = Math.max(max, v);
        for (int i = 0; i < rotulos.size(); i++) {
            float y = altura - (i + 1) * linha + 3;
            float comp = (float) ((largura - esq - dir) * valores.get(i) / max);
            t.setColorFill(i == 0 ? Identidade.BRASA_GRAFICO : CLARO);
            t.roundRectangle(esq, y, Math.max(1, comp), linha - 5, 1.5f);
            t.fill();
            texto(t, rotulos.get(i), esq - 6, y + 2.5f, Element.ALIGN_RIGHT, i == 0 ? Identidade.TEXTO : Identidade.TEXTO_2, i == 0);
            texto(t, Formatos.inteiro(valores.get(i)), esq + comp + 5, y + 2.5f, Element.ALIGN_LEFT, Identidade.TEXTO, true);
        }
        return imagem(t);
    }

    /** Tempo medio por n em escala log-log, um traco por algoritmo; crescimento quadratico em cinza. */
    static Image curvasBenchmark(PdfWriter w, float largura, float altura, List<BenchmarkResult> res,
                                 Map<String, Boolean> quadratico) {
        Map<String, List<double[]>> pontos = new LinkedHashMap<>();
        double xmin = Double.MAX_VALUE, xmax = 0, ymin = Double.MAX_VALUE, ymax = 0;
        for (BenchmarkResult r : res) {
            if (r.mediaMs() <= 0) continue;
            pontos.computeIfAbsent(r.algoritmo(), k -> new ArrayList<>()).add(new double[]{Math.log10(r.n()), Math.log10(r.mediaMs())});
            xmin = Math.min(xmin, Math.log10(r.n()));
            xmax = Math.max(xmax, Math.log10(r.n()));
            ymin = Math.min(ymin, Math.log10(r.mediaMs()));
            ymax = Math.max(ymax, Math.log10(r.mediaMs()));
        }
        PdfTemplate t = w.getDirectContent().createTemplate(largura, altura);
        if (pontos.isEmpty()) return imagem(t);
        xmin = Math.floor(xmin);
        xmax = Math.ceil(xmax);
        ymin = Math.floor(ymin);
        ymax = Math.ceil(ymax);
        if (xmax == xmin) xmax++;
        if (ymax == ymin) ymax++;
        float esq = 46, dir = 92, topo = 10, base = 26;
        float pw = largura - esq - dir, ph = altura - topo - base;
        t.setLineWidth(0.5f);
        for (double d = ymin; d <= ymax + 1e-9; d++) {
            float y = (float) (base + (d - ymin) / (ymax - ymin) * ph);
            t.setColorStroke(Identidade.BORDA);
            t.moveTo(esq, y);
            t.lineTo(esq + pw, y);
            t.stroke();
            texto(t, Formatos.decimal(Math.pow(10, d), d < 0 ? (int) -d : 0) + " ms", esq - 4, y - 2.5f, Element.ALIGN_RIGHT, Identidade.TEXTO_3, false);
        }
        for (double d = xmin; d <= xmax + 1e-9; d++) {
            float x = (float) (esq + (d - xmin) / (xmax - xmin) * pw);
            texto(t, "n = " + abreviar(Math.pow(10, d)), x, base - 12, Element.ALIGN_CENTER, Identidade.TEXTO_3, false);
        }
        List<String> nomes = new ArrayList<>(pontos.keySet());
        float[] finais = new float[nomes.size()];
        for (int pass = 0; pass < 2; pass++) {
            for (int k = 0; k < nomes.size(); k++) {
                boolean quad = quadratico.getOrDefault(nomes.get(k), false);
                if ((pass == 0) != quad) continue;
                List<double[]> p = ordenarPorX(pontos.get(nomes.get(k)));
                t.setColorStroke(quad ? Identidade.ANO_ANTERIOR : Identidade.BRASA_GRAFICO);
                t.setLineWidth(quad ? 1.1f : 1.5f);
                t.setLineJoin(PdfContentByte.LINE_JOIN_ROUND);
                for (int i = 0; i < p.size(); i++) {
                    float x = (float) (esq + (p.get(i)[0] - xmin) / (xmax - xmin) * pw);
                    float y = (float) (base + (p.get(i)[1] - ymin) / (ymax - ymin) * ph);
                    if (i == 0) t.moveTo(x, y);
                    else t.lineTo(x, y);
                }
                t.stroke();
                double[] ultimo = p.get(p.size() - 1);
                finais[k] = (float) (base + (ultimo[1] - ymin) / (ymax - ymin) * ph);
            }
        }
        float[] ys = afastar(finais, 8.5f);
        for (int k = 0; k < nomes.size(); k++) {
            boolean quad = quadratico.getOrDefault(nomes.get(k), false);
            texto(t, nomes.get(k), esq + pw + 5, ys[k] - 2.5f, Element.ALIGN_LEFT, quad ? Identidade.TEXTO_3 : Identidade.BRASA, !quad);
        }
        return imagem(t);
    }

    private static List<double[]> ordenarPorX(List<double[]> p) {
        List<double[]> r = new ArrayList<>(p);
        for (int i = 1; i < r.size(); i++) {
            double[] x = r.get(i);
            int j = i - 1;
            while (j >= 0 && r.get(j)[0] > x[0]) {
                r.set(j + 1, r.get(j));
                j--;
            }
            r.set(j + 1, x);
        }
        return r;
    }

    /** Afasta rotulos verticais para que fiquem a pelo menos {@code min} pontos um do outro, mantendo a ordem. */
    static float[] afastar(float[] ys, float min) {
        int n = ys.length;
        int[] ordem = new int[n];
        for (int i = 0; i < n; i++) ordem[i] = i;
        for (int i = 1; i < n; i++) {
            int x = ordem[i], j = i - 1;
            while (j >= 0 && ys[ordem[j]] > ys[x]) {
                ordem[j + 1] = ordem[j];
                j--;
            }
            ordem[j + 1] = x;
        }
        float[] r = ys.clone();
        for (int i = 1; i < n; i++) {
            if (r[ordem[i]] - r[ordem[i - 1]] < min) r[ordem[i]] = r[ordem[i - 1]] + min;
        }
        return r;
    }

    static double passoBonito(double bruto) {
        double ordem = Math.pow(10, Math.floor(Math.log10(Math.max(1e-9, bruto))));
        double f = bruto / ordem;
        return (f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10) * ordem;
    }

    private static void grade(PdfTemplate t, float esq, float base, float pw, float ph, double limite, double passo) {
        t.setLineWidth(0.5f);
        for (double v = 0; v <= limite + 1e-9; v += passo) {
            float y = (float) (base + v / limite * ph);
            t.setColorStroke(v == 0 ? Identidade.TEXTO_3 : Identidade.BORDA);
            t.moveTo(esq, y);
            t.lineTo(esq + pw, y);
            t.stroke();
            texto(t, Formatos.inteiro(Math.round(v)), esq - 5, y - 2.5f, Element.ALIGN_RIGHT, Identidade.TEXTO_3, false);
        }
    }

    private static void linha(PdfTemplate t, double[] v, float esq, float base, float pw, float ph, double limite, Color cor, float espessura) {
        t.setColorStroke(cor);
        t.setLineWidth(espessura);
        t.setLineJoin(PdfContentByte.LINE_JOIN_ROUND);
        t.setLineCap(PdfContentByte.LINE_CAP_ROUND);
        for (int m = 0; m < v.length; m++) {
            float x = esq + m * pw / (v.length - 1), y = (float) (base + v[m] / limite * ph);
            if (m == 0) t.moveTo(x, y);
            else t.lineTo(x, y);
        }
        t.stroke();
    }

    static void texto(PdfContentByte t, String s, float x, float y, int alinhamento, Color cor, boolean destaque) {
        BaseFont bf = Identidade.base(destaque);
        t.beginText();
        t.setFontAndSize(bf, FONTE);
        t.setColorFill(cor);
        t.showTextAligned(alinhamento, ReportExporter.Pdf.limpar(s), x, y, 0);
        t.endText();
    }

    private static Image imagem(PdfTemplate t) {
        try {
            Image img = Image.getInstance(t);
            img.setAlignment(Element.ALIGN_CENTER);
            return img;
        } catch (BadElementException e) {
            throw new IllegalStateException(e);
        }
    }

    static String abreviar(double v) {
        if (v >= 1e6) return Formatos.decimal(v / 1e6, v >= 1e7 ? 0 : 1) + " mi";
        if (v >= 1e3) return Formatos.decimal(v / 1e3, v >= 1e4 ? 0 : 1) + " mil";
        return Formatos.inteiro(Math.round(v));
    }
}
