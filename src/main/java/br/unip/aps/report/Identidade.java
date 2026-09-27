package br.unip.aps.report;

import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Identidade visual dos relatorios exportados (PDF e Excel), espelhando os tokens do design
 * system da interface ({@code ui/css/tokens.css} e {@code theme-light.css}): mesma fonte (Inter),
 * mesma cor de destaque (brasa) e mesmos neutros.
 */
public final class Identidade {

    private static final Logger LOG = Logger.getLogger(Identidade.class.getName());

    /** Brasa 700: cabecalhos de tabela (contraste 5,18:1 com texto branco). */
    public static final Color BRASA = new Color(0xC2, 0x41, 0x0C);
    /** Brasa 600: filetes e destaques graficos. */
    public static final Color BRASA_GRAFICO = new Color(0xE8, 0x59, 0x0C);
    /** Slate 900: texto principal. */
    public static final Color TEXTO = new Color(0x10, 0x18, 0x28);
    /** Slate 600: texto secundario. */
    public static final Color TEXTO_2 = new Color(0x47, 0x54, 0x67);
    /** Slate 500: texto terciario (rodape, legendas). */
    public static final Color TEXTO_3 = new Color(0x66, 0x70, 0x85);
    /** Slate 150: bordas. */
    public static final Color BORDA = new Color(0xE4, 0xE7, 0xEC);
    /** Slate 25: linhas zebradas. */
    public static final Color ZEBRA = new Color(0xFA, 0xFB, 0xFC);
    /** Nome da fonte usada no Excel (o Excel usa uma fonte substituta se a Inter nao estiver instalada). */
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

    /** @return {@code true} se a Inter foi embarcada (Unicode completo; sem necessidade de substituir simbolos) */
    public static boolean interDisponivel() {
        carregar();
        return regular != null && semibold != null;
    }

    /**
     * @param tamanho  tamanho em pt
     * @param destaque {@code true} para SemiBold
     * @param cor      cor
     * @return fonte do PDF (Inter embarcada, ou Helvetica como alternativa)
     */
    public static Font pdf(float tamanho, boolean destaque, Color cor) {
        carregar();
        BaseFont bf = destaque ? semibold : regular;
        if (bf != null) return new Font(bf, tamanho, Font.NORMAL, cor);
        return new Font(Font.HELVETICA, tamanho, destaque ? Font.BOLD : Font.NORMAL, cor);
    }
}
