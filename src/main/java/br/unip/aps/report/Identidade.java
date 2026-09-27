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
    public static final Color TEXTO = new Color(0x10, 0x18, 0x28);
    public static final Color TEXTO_2 = new Color(0x47, 0x54, 0x67);
    public static final Color TEXTO_3 = new Color(0x66, 0x70, 0x85);
    public static final Color BORDA = new Color(0xE4, 0xE7, 0xEC);
    public static final Color ZEBRA = new Color(0xFA, 0xFB, 0xFC);
    public static final String FONTE_EXCEL = "Inter";

    private static BaseFont regular;
    private static BaseFont semibold;
    private static boolean carregado;

    private Identidade() { }

    private static synchronized void carregar() {
        if (carregado) return;
        carregado = true;
        regular = fonte("Inter-Regular.ttf");
        semibold = fonte("Inter-SemiBold.ttf");
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
}
