package br.unip.aps.report;

import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/** Cores e fontes dos relatorios PDF e Excel. */
public final class Identidade {
    private static final Logger LOG = Logger.getLogger(Identidade.class.getName());

    public static final Color BRASA = new Color(0xC2, 0x41, 0x0C);
    public static final Color BRASA_GRAFICO = new Color(0xE8, 0x59, 0x0C);
    public static final Color TINTA = new Color(0x1A, 0x14, 0x11);
    public static final Color CARVAO = new Color(0x0A, 0x08, 0x07);
    public static final Color TEXTO = new Color(0x10, 0x18, 0x28);
    public static final Color TEXTO_2 = new Color(0x47, 0x54, 0x67);
    public static final Color TEXTO_3 = new Color(0x66, 0x70, 0x85);
    public static final Color BORDA = new Color(0xE4, 0xE7, 0xEC);
    public static final Color ZEBRA = new Color(0xFA, 0xFB, 0xFC);
    public static final Color PAPEL = new Color(0xFB, 0xF8, 0xF4);
    public static final Color ANO_ANTERIOR = new Color(0x8A, 0x94, 0xA6);
    public static final String FONTE_EXCEL = "Inter";

    private static final Color[] INFERNO = {new Color(0x00, 0x00, 0x04), new Color(0x42, 0x0A, 0x68),
            new Color(0x93, 0x26, 0x67), new Color(0xDD, 0x51, 0x3A), new Color(0xFC, 0xA5, 0x0A), new Color(0xFC, 0xFF, 0xA4)};

    private static BaseFont regular;
    private static BaseFont semibold;
    private static BaseFont display;
    private static BaseFont displayForte;
    private static boolean carregado;

    private Identidade() { }

    private static synchronized void carregar() {
        if (carregado) return;
        carregado = true;
        regular = fonte("Inter-Regular.ttf");
        semibold = fonte("Inter-SemiBold.ttf");
        display = fonte("BigShouldersDisplay-ExtraBold.ttf");
        displayForte = fonte("BigShouldersDisplay-Black.ttf");
    }

    private static BaseFont fonte(String arquivo) {
        try (InputStream in = Identidade.class.getResourceAsStream("/fonts/" + arquivo)) {
            if (in == null) return null;
            byte[] bytes = in.readAllBytes();
            return BaseFont.createFont(arquivo, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
        } catch (IOException | RuntimeException e) {
            LOG.warning("Fonte " + arquivo + " indisponivel no PDF (usando Helvetica): " + e.getMessage());
            return null;
        }
    }

    public static boolean interDisponivel() {
        carregar();
        return regular != null && semibold != null;
    }

    public static Font pdf(float tamanho, boolean destaque, Color cor) {
        carregar();
        BaseFont bf = destaque ? semibold : regular;
        if (bf != null) return new Font(bf, tamanho, Font.NORMAL, cor);
        return new Font(Font.HELVETICA, tamanho, destaque ? Font.BOLD : Font.NORMAL, cor);
    }

    /** Fonte de titulos (Big Shoulders Display); cai para Helvetica negrito se nao carregar. */
    public static Font titulo(float tamanho, boolean forte, Color cor) {
        carregar();
        BaseFont bf = forte ? displayForte : display;
        if (bf != null) return new Font(bf, tamanho, Font.NORMAL, cor);
        return new Font(Font.HELVETICA, tamanho, Font.BOLD, cor);
    }

    /** Fonte base para desenho direto (graficos vetoriais e capa). */
    public static BaseFont base(boolean destaque) {
        carregar();
        BaseFont bf = destaque ? semibold : regular;
        if (bf != null) return bf;
        try {
            return BaseFont.createFont(destaque ? BaseFont.HELVETICA_BOLD : BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Fonte base de titulos para desenho direto. */
    public static BaseFont baseTitulo(boolean forte) {
        carregar();
        BaseFont bf = forte ? displayForte : display;
        return bf != null ? bf : base(true);
    }

    /** Cor da escala termica inferno para t entre 0 e 1. */
    public static Color inferno(double t) {
        double x = Math.max(0, Math.min(1, t)) * (INFERNO.length - 1);
        int i = (int) Math.min(INFERNO.length - 2, Math.floor(x));
        double f = x - i;
        Color a = INFERNO[i], b = INFERNO[i + 1];
        return new Color((int) Math.round(a.getRed() + f * (b.getRed() - a.getRed())),
                (int) Math.round(a.getGreen() + f * (b.getGreen() - a.getGreen())),
                (int) Math.round(a.getBlue() + f * (b.getBlue() - a.getBlue())));
    }
}
