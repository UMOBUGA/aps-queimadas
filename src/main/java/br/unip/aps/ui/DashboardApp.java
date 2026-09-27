package br.unip.aps.ui;

import br.unip.aps.app.Sessao;
import br.unip.aps.config.AppConfig;
import br.unip.aps.config.LogConfig;
import br.unip.aps.ui.componentes.Dialogos;
import br.unip.aps.ui.tema.GerenciadorTema;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Aplicacao JavaFX do dashboard (padrao MVC). */
public class DashboardApp extends Application {
    private static final Logger LOG = Logger.getLogger(DashboardApp.class.getName());

    private UiContexto contexto;

    /** Inicia o JavaFX (chamado por {@link br.unip.aps.Main}). */
    public static void iniciar(String[] args) {
        launch(DashboardApp.class, args);
    }

    @Override
    public void start(Stage stage) {
        LogConfig.inicializar();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> LOG.log(Level.SEVERE, "Erro nao tratado em " + t.getName(), e));
        GerenciadorTema tema = GerenciadorTema.get();
        Stage splash = splash(tema);
        long inicio = System.currentTimeMillis();
        try {
            contexto = new UiContexto(new Sessao(AppConfig.carregar()), stage);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/br/unip/aps/ui/main.fxml"));
            loader.setControllerFactory(contexto::criarController);
            Parent root = loader.load();
            MainController main = loader.getController();
            var tela = Screen.getPrimary().getVisualBounds();
            Scene scene = new Scene(root, Math.min(1440, tela.getWidth() * 0.92), Math.min(900, tela.getHeight() * 0.92));
            tema.registrar(scene);
            stage.setTitle("APS Queimadas — Análise de Performance de Algoritmos de Ordenação");
            stage.getIcons().add(icone());
            stage.setScene(scene);
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            contexto.carregarDadosIniciais(() -> {
                long espera = Math.max(0, 900 - (System.currentTimeMillis() - inicio));
                PauseTransition p = new PauseTransition(Duration.millis(espera));
                p.setOnFinished(e -> {
                    stage.show();
                    fecharSplash(splash);
                    String capturas = System.getProperty("aps.capturas");
                    if (capturas != null) new CapturaTelas(contexto, main, stage, Path.of(capturas)).executar();
                });
                p.play();
            });
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Falha ao iniciar o dashboard", e);
            splash.close();
            Dialogos.erro(null, "Não foi possível abrir o dashboard", e.getMessage()
                    + "\n\nVocê pode usar o modo console: java -jar aps-queimadas.jar cli", null);
        }
    }

    private Stage splash(GerenciadorTema tema) {
        StackPane marca = new StackPane(br.unip.aps.ui.componentes.Logo.criar(44));
        marca.getStyleClass().addAll("brand-mark", "marca-splash");
        Label nome = new Label("QUEIMADAS");
        nome.getStyleClass().add("splash-titulo");
        HBox titulo = new HBox(12, marca, nome);
        titulo.setAlignment(Pos.CENTER_LEFT);
        Label sub = new Label("Focos de incêndio em São Paulo e a performance dos algoritmos de ordenação");
        sub.getStyleClass().add("splash-sub");
        VBox topo = new VBox(4, titulo, sub);
        ProgressBar barra = new ProgressBar(-1);
        barra.setMaxWidth(Double.MAX_VALUE);
        Label status = new Label("Carregando focos do INPE · SP 2023–2024");
        status.getStyleClass().add("splash-sub");
        VBox card = new VBox(18, topo, barra, status);
        card.getStyleClass().add("splash-card");
        StackPane raiz = new StackPane(card);
        raiz.getStyleClass().add("splash-root");
        Scene s = new Scene(raiz);
        s.setFill(Color.TRANSPARENT);
        tema.registrar(s);
        Stage st = new Stage(StageStyle.TRANSPARENT);
        st.setScene(s);
        st.getIcons().add(icone());
        st.show();
        return st;
    }

    private void fecharSplash(Stage splash) {
        if (!GerenciadorTema.get().animacoesProperty().get()) {
            splash.close();
            return;
        }
        FadeTransition f = new FadeTransition(Duration.millis(220), splash.getScene().getRoot());
        f.setToValue(0);
        f.setOnFinished(e -> splash.close());
        f.play();
    }

    static Image icone() {
        StackPane p = new StackPane(br.unip.aps.ui.componentes.Logo.criar(46));
        p.getStyleClass().setAll("icone-app");
        p.setMinSize(64, 64);
        p.setPrefSize(64, 64);
        new Scene(p, 64, 64, Color.TRANSPARENT);
        GerenciadorTema.get().aplicar(p);
        p.applyCss();
        p.layout();
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        return p.snapshot(sp, null);
    }

    @Override
    public void stop() {
        if (contexto != null) contexto.encerrar();
    }
}
