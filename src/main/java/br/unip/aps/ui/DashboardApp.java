package br.unip.aps.ui;

import br.unip.aps.app.Sessao;
import br.unip.aps.config.AppConfig;
import br.unip.aps.config.LogConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Aplicacao JavaFX do dashboard (padrao <b>MVC</b>).
 *
 * <ul>
 *   <li><b>Model</b>: {@link Sessao} e as classes de dominio (base de focos, resultados de
 *       ordenacao, benchmark e ML);</li>
 *   <li><b>View</b>: arquivos FXML em {@code src/main/resources/br/unip/aps/ui} + {@code dashboard.css};</li>
 *   <li><b>Controller</b>: classes {@code *Controller} deste pacote, que recebem o
 *       {@link UiContexto} por injecao de dependencia (controller factory).</li>
 * </ul>
 * Operacoes demoradas rodam em {@link javafx.concurrent.Task} fora da thread da interface.
 */
public class DashboardApp extends Application {

    private static final Logger LOG = Logger.getLogger(DashboardApp.class.getName());

    private UiContexto contexto;

    /**
     * Inicia o JavaFX (chamado por {@link br.unip.aps.Main}).
     *
     * @param args argumentos
     */
    public static void iniciar(String[] args) {
        launch(DashboardApp.class, args);
    }

    @Override
    public void start(Stage stage) {
        LogConfig.inicializar();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> LOG.log(Level.SEVERE, "Erro nao tratado em " + t.getName(), e));
        try {
            contexto = new UiContexto(new Sessao(AppConfig.carregar()), stage);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/unip/aps/ui/main.fxml"));
            loader.setControllerFactory(contexto::criarController);
            Parent root = loader.load();
            Scene scene = new Scene(root, 1360, 860);
            scene.getStylesheets().add(getClass().getResource("/br/unip/aps/ui/dashboard.css").toExternalForm());
            stage.setTitle("APS Queimadas — Análise de Performance de Algoritmos de Ordenação (INPE)");
            try (InputStream icone = getClass().getResourceAsStream("/br/unip/aps/ui/icone.png")) {
                if (icone != null) stage.getIcons().add(new Image(icone));
            }
            stage.setScene(scene);
            stage.setMinWidth(1000);
            stage.setMinHeight(680);
            stage.show();
            contexto.carregarDadosIniciais();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Falha ao iniciar o dashboard", e);
            Alert a = new Alert(Alert.AlertType.ERROR, "Nao foi possivel abrir o dashboard:\n" + e.getMessage()
                    + "\n\nVoce pode usar o modo console: java -jar aps-queimadas.jar cli");
            a.setHeaderText("Erro ao iniciar");
            a.showAndWait();
        }
    }

    @Override
    public void stop() {
        if (contexto != null) contexto.encerrar();
    }
}
