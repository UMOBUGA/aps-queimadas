package br.unip.aps.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("GIF animado")
class GifAnimadoTest {

    private static List<BufferedImage> quadros(int n, int largura) {
        List<BufferedImage> r = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            BufferedImage img = new BufferedImage(largura, largura / 2, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setColor(new Color(40 * i % 255, 80, 200 - 30 * i % 200));
            g.fillRect(0, 0, largura, largura / 2);
            g.dispose();
            r.add(img);
        }
        return r;
    }

    @Test
    @DisplayName("grava todos os quadros, com o atraso pedido, repetição infinita e largura reduzida")
    void gravaSequencia(@TempDir Path pasta) throws Exception {
        Path gif = GifAnimado.gravar(quadros(4, 400), 700, 2000, 200, pasta.resolve("sub").resolve("teste.gif"));
        ImageReader leitor = ImageIO.getImageReadersByFormatName("gif").next();
        try (ImageInputStream in = ImageIO.createImageInputStream(gif.toFile())) {
            leitor.setInput(in);
            assertEquals(4, leitor.getNumImages(true));
            assertEquals(200, leitor.read(0).getWidth());
            assertEquals("70", atraso(leitor, 0));
            assertEquals("200", atraso(leitor, 3));
        } finally {
            leitor.dispose();
        }
    }

    @Test
    @DisplayName("sem quadros não há GIF")
    void semQuadros(@TempDir Path pasta) {
        assertThrows(IllegalArgumentException.class, () -> GifAnimado.gravar(List.of(), 700, 0, 0, pasta.resolve("x.gif")));
    }

    private static String atraso(ImageReader leitor, int i) throws Exception {
        IIOMetadataNode raiz = (IIOMetadataNode) leitor.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0");
        for (Node n = raiz.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeName().equals("GraphicControlExtension")) return ((IIOMetadataNode) n).getAttribute("delayTime");
        }
        return "";
    }
}
