package br.unip.aps.util;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Grava uma sequencia de quadros como GIF animado em repeticao continua, so com o ImageIO do Java. */
public final class GifAnimado {
    private static final String FORMATO = "javax_imageio_gif_image_1.0";

    private GifAnimado() { }

    /** Grava os quadros com o atraso dado; o ultimo fica mais tempo na tela e larguras acima do limite sao reduzidas (0 = original). */
    public static Path gravar(List<BufferedImage> quadros, int atrasoMs, int pausaFinalMs, int larguraMax, Path destino) throws IOException {
        if (quadros.isEmpty()) throw new IllegalArgumentException("Nenhum quadro para gravar.");
        Path pasta = destino.toAbsolutePath().getParent();
        if (pasta != null) Files.createDirectories(pasta);
        ImageWriter escritor = ImageIO.getImageWritersByFormatName("gif").next();
        try (ImageOutputStream saida = ImageIO.createImageOutputStream(Files.newOutputStream(destino))) {
            escritor.setOutput(saida);
            escritor.prepareWriteSequence(null);
            for (int i = 0; i < quadros.size(); i++) {
                BufferedImage q = reduzir(quadros.get(i), larguraMax);
                int atraso = i == quadros.size() - 1 ? Math.max(atrasoMs, pausaFinalMs) : atrasoMs;
                ImageWriteParam param = escritor.getDefaultWriteParam();
                IIOMetadata meta = escritor.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(q), param);
                configurar(meta, atraso, i == 0);
                escritor.writeToSequence(new IIOImage(q, null, meta), param);
            }
            escritor.endWriteSequence();
        } finally {
            escritor.dispose();
        }
        return destino;
    }

    private static void configurar(IIOMetadata meta, int atrasoMs, boolean primeiro) throws IIOInvalidTreeException {
        IIOMetadataNode raiz = (IIOMetadataNode) meta.getAsTree(FORMATO);
        IIOMetadataNode controle = no(raiz, "GraphicControlExtension");
        controle.setAttribute("disposalMethod", "none");
        controle.setAttribute("userInputFlag", "FALSE");
        controle.setAttribute("transparentColorFlag", "FALSE");
        controle.setAttribute("delayTime", String.valueOf(Math.max(1, atrasoMs / 10)));
        controle.setAttribute("transparentColorIndex", "0");
        if (primeiro) {
            IIOMetadataNode app = new IIOMetadataNode("ApplicationExtension");
            app.setAttribute("applicationID", "NETSCAPE");
            app.setAttribute("authenticationCode", "2.0");
            app.setUserObject(new byte[]{1, 0, 0});
            no(raiz, "ApplicationExtensions").appendChild(app);
        }
        meta.setFromTree(FORMATO, raiz);
    }

    private static IIOMetadataNode no(IIOMetadataNode raiz, String nome) {
        for (int i = 0; i < raiz.getLength(); i++) {
            if (raiz.item(i).getNodeName().equalsIgnoreCase(nome)) return (IIOMetadataNode) raiz.item(i);
        }
        IIOMetadataNode novo = new IIOMetadataNode(nome);
        raiz.appendChild(novo);
        return novo;
    }

    private static BufferedImage reduzir(BufferedImage img, int larguraMax) {
        boolean reduz = larguraMax > 0 && img.getWidth() > larguraMax;
        int w = reduz ? larguraMax : img.getWidth();
        int h = reduz ? (int) Math.round(img.getHeight() * (double) larguraMax / img.getWidth()) : img.getHeight();
        BufferedImage rgb = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(img, 0, 0, w, h, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }
}
