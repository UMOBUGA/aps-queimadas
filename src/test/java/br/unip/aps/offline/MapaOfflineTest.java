package br.unip.aps.offline;

import br.unip.aps.geo.MalhaMunicipal;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Abre o mapa no WebView e simula a queda da rede: Leaflet vem do JAR e o mundo (Natural Earth) e a malha do IBGE substituem os tiles. */
@DisplayName("Mapa sem internet")
class MapaOfflineTest {
    private static Stage palco;

    @Test
    void leafletCarregaDoJarEMalhaSubstituiOsTiles() throws Exception {
        br.unip.aps.FxTestes.iniciar();

        AtomicReference<String> resultado = new AtomicReference<>();
        AtomicReference<WritableImage> imagem = new AtomicReference<>();
        CountDownLatch feito = new CountDownLatch(1);
        Platform.runLater(() -> {
            WebView wv = new WebView();
            palco = new Stage();
            palco.setScene(new Scene(wv, 900, 640));
            palco.show();
            WebEngine eng = wv.getEngine();
            eng.getLoadWorker().stateProperty().addListener((o, a, s) -> {
                if (s == Worker.State.SUCCEEDED) {
                    eng.executeScript("APS.setMalha(" + MalhaMunicipal.sp().geojson() + ")");
                    eng.executeScript("APS.forcarOffline()");
                    resultado.set(eng.executeScript("(typeof L) + '|' + APS.pronto + '|' + APS.offline() + '|' "
                            + "+ document.body.classList.contains('offline') + '|' + (typeof MUNDO) + '|' "
                            + "+ (typeof MUNDO === 'object' && MUNDO.features.length > 150)").toString());
                    PauseTransition p = new PauseTransition(Duration.millis(800));
                    p.setOnFinished(e -> {
                        imagem.set(wv.snapshot(null, null));
                        palco.close();
                        feito.countDown();
                    });
                    p.play();
                } else if (s == Worker.State.FAILED) {
                    resultado.set("falhou");
                    feito.countDown();
                }
            });
            eng.load(getClass().getResource("/br/unip/aps/ui/mapa.html").toExternalForm());
        });
        assertTrue(feito.await(40, TimeUnit.SECONDS), "o mapa nao terminou de carregar");
        assertEquals("object|true|true|true|object|true", resultado.get(), "Leaflet|pronto|offline|classe|mapa-múndi|países");

        WritableImage img = imagem.get();
        Path saida = Path.of("target", "mapa-offline.png");
        Files.createDirectories(saida.getParent());
        ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", saida.toFile());
        Color mar = img.getPixelReader().getColor(890, 630);
        Color centro = img.getPixelReader().getColor(450, 360);
        assertNotEquals(mar, centro, "o estado de SP deveria aparecer sobre o fundo");
    }
}
