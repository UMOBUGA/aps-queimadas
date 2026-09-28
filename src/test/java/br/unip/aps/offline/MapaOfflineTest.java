package br.unip.aps.offline;

import br.unip.aps.geo.MalhaMunicipal;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Abre o mapa no WebView e simula a queda da rede: Leaflet vem do JAR e a malha do IBGE substitui os tiles. */
@DisplayName("Mapa sem internet")
class MapaOfflineTest {
    private static WebView mantido;

    @Test
    void leafletCarregaDoJarEMalhaSubstituiOsTiles() throws Exception {
        br.unip.aps.FxTestes.iniciar();

        AtomicReference<String> resultado = new AtomicReference<>();
        CountDownLatch feito = new CountDownLatch(1);
        Platform.runLater(() -> {
            WebView wv = new WebView();
            mantido = wv;
            WebEngine eng = wv.getEngine();
            eng.getLoadWorker().stateProperty().addListener((o, a, s) -> {
                if (s == Worker.State.SUCCEEDED) {
                    eng.executeScript("APS.setMalha(" + MalhaMunicipal.sp().geojson() + ")");
                    eng.executeScript("APS.forcarOffline()");
                    resultado.set(eng.executeScript("(typeof L) + '|' + APS.pronto + '|' + APS.offline() + '|' "
                            + "+ document.body.classList.contains('offline')").toString());
                    feito.countDown();
                } else if (s == Worker.State.FAILED) {
                    resultado.set("falhou");
                    feito.countDown();
                }
            });
            eng.load(getClass().getResource("/br/unip/aps/ui/mapa.html").toExternalForm());
        });
        assertTrue(feito.await(40, TimeUnit.SECONDS), "o mapa nao terminou de carregar");
        assertEquals("object|true|true|true", resultado.get());
    }

}
