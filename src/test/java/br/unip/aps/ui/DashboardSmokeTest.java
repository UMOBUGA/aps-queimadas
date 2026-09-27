package br.unip.aps.ui;

import br.unip.aps.Focos;
import br.unip.aps.app.Sessao;
import br.unip.aps.config.AppConfig;
import br.unip.aps.io.RelatorioCarga;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.ui.tema.GerenciadorTema;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Teste de fumaca da interface: monta o shell (sidebar, top bar, filtros), injeta uma base,
 * navega por todas as telas nos temas claro e escuro e aplica um filtro global, verificando que
 * nada lanca excecao. Requer ambiente grafico: e ignorado em CI/headless.
 */
@DisplayName("Dashboard (smoke test)")
class DashboardSmokeTest {

    @Test
    void navegaPorTodasAsTelasNosDoisTemas() throws Exception {
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
        AtomicInteger telas = new AtomicInteger();
        AtomicInteger filtrados = new AtomicInteger(-1);
        CountDownLatch feito = new CountDownLatch(1);
        Platform.runLater(() -> {
            GerenciadorTema tema = GerenciadorTema.get();
            GerenciadorTema.Tema original = tema.temaProperty().get();
            boolean anim = tema.animacoesProperty().get();
            try {
                tema.animacoesProperty().set(false);
                Stage stage = new Stage();
                UiContexto ctx = new UiContexto(new Sessao(AppConfig.carregar()), stage);
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/unip/aps/ui/main.fxml"));
                loader.setControllerFactory(ctx::criarController);
                Parent root = loader.load();
                MainController main = loader.getController();
                Scene scene = new Scene(root, 1280, 800);
                tema.registrar(scene);
                stage.setScene(scene);
                ctx.baseProperty().set(new BaseDeFocos(Focos.aleatorios(600, 1), new RelatorioCarga(), List.of(Path.of("t.csv"))));
                for (GerenciadorTema.Tema t : GerenciadorTema.Tema.values()) {
                    tema.temaProperty().set(t);
                    for (Pagina p : Pagina.values()) {
                        ctx.navegar(p);
                        root.applyCss();
                        root.layout();
                        if (main.noDe(p) != null) telas.incrementAndGet();
                    }
                }
                main.filterBar().selecionar(java.util.Set.of("Cerrado"), java.util.Set.of());
                filtrados.set(ctx.focosFiltradosProperty().get().size());
                ctx.encerrar();
                stage.close();
            } catch (Throwable t) {
                erro.set(t);
            } finally {
                tema.temaProperty().set(original);
                tema.animacoesProperty().set(anim);
                feito.countDown();
            }
        });
        assertTrue(feito.await(90, TimeUnit.SECONDS));
        assertNull(erro.get(), () -> "Falha ao montar o dashboard: " + erro.get());
        assertEquals(2 * Pagina.values().length, telas.get(), "todas as telas carregadas nos dois temas");
        assertTrue(filtrados.get() > 0 && filtrados.get() < 600, "filtro global por bioma aplicado: " + filtrados.get());
    }

    @AfterAll
    static void encerrarJavaFx() {
        try {
            Platform.exit();
        } catch (IllegalStateException ignorada) {
            // toolkit nao iniciado (teste ignorado)
        }
    }
}
