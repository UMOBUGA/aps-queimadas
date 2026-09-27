package br.unip.aps.ui;

import br.unip.aps.Focos;
import br.unip.aps.app.Sessao;
import br.unip.aps.config.AppConfig;
import br.unip.aps.io.RelatorioCarga;
import br.unip.aps.model.BaseDeFocos;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Teste de fumaca da interface: carrega todas as telas FXML com seus controllers, injeta uma base
 * e verifica que nada lanca excecao. Requer ambiente grafico: e ignorado em CI/headless.
 */
@DisplayName("Dashboard (smoke test)")
class DashboardSmokeTest {

    @Test
    void carregaTodasAsTelas() throws Exception {
        assumeFalse(System.getenv("CI") != null || java.awt.GraphicsEnvironment.isHeadless(), "sem ambiente grafico");
        CountDownLatch iniciado = new CountDownLatch(1);
        try {
            Platform.startup(iniciado::countDown);
        } catch (IllegalStateException jaIniciado) {
            iniciado.countDown();
        }
        assertTrue(iniciado.await(20, TimeUnit.SECONDS));
        Platform.setImplicitExit(false);

        AtomicReference<Throwable> erro = new AtomicReference<>();
        AtomicReference<Integer> abas = new AtomicReference<>();
        CountDownLatch feito = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                Stage stage = new Stage();
                UiContexto ctx = new UiContexto(new Sessao(AppConfig.carregar()), stage);
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/unip/aps/ui/main.fxml"));
                loader.setControllerFactory(ctx::criarController);
                Parent root = loader.load();
                Scene scene = new Scene(root, 1200, 800);
                scene.getStylesheets().add(getClass().getResource("/br/unip/aps/ui/dashboard.css").toExternalForm());
                stage.setScene(scene);
                ctx.baseProperty().set(new BaseDeFocos(Focos.aleatorios(500, 1), new RelatorioCarga(), List.of(Path.of("t.csv"))));
                abas.set(((TabPane) root.lookup(".tab-pane")).getTabs().size());
                ctx.encerrar();
                stage.close();
            } catch (Throwable t) {
                erro.set(t);
            } finally {
                feito.countDown();
            }
        });
        assertTrue(feito.await(60, TimeUnit.SECONDS));
        assertNull(erro.get(), () -> "Falha ao montar o dashboard: " + erro.get());
        assertEquals(6, abas.get());
    }

    @org.junit.jupiter.api.AfterAll
    static void encerrarJavaFx() {
        try {
            Platform.exit();
        } catch (IllegalStateException ignorada) {
            // toolkit nao iniciado (teste ignorado)
        }
    }
}
