package br.unip.aps.ui.componentes;

import javafx.animation.PauseTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Sobreposicao de carregamento: escurece o conteudo e mostra progresso, mensagem e botao
 * Cancelar. So aparece se a tarefa demorar mais de 300 ms (evita "piscar" em operacoes rapidas).
 */
public class LoadingOverlay extends StackPane {

    private final PauseTransition atraso = new PauseTransition(Duration.millis(300));

    /**
     * @param ocupado   indica tarefa em andamento
     * @param mensagem  texto de status
     * @param progresso progresso 0..1 (ou negativo = indeterminado)
     * @param cancelar  acao do botao Cancelar
     */
    public LoadingOverlay(BooleanProperty ocupado, StringProperty mensagem, DoubleProperty progresso, Runnable cancelar) {
        getStyleClass().add("overlay");
        Label titulo = new Label("Processando…");
        titulo.getStyleClass().add("t-h2");
        Label msg = new Label();
        msg.textProperty().bind(mensagem);
        msg.getStyleClass().add("t-small");
        msg.setWrapText(true);
        msg.setMaxWidth(360);
        ProgressBar barra = new ProgressBar();
        barra.progressProperty().bind(progresso.map(p -> p.doubleValue() <= 0 ? -1.0 : p.doubleValue()));
        Button b = new Button("Cancelar", Icones.de(Icones.PARAR, 16));
        b.getStyleClass().add("btn-secondary");
        b.setOnAction(e -> cancelar.run());
        VBox card = new VBox(12, titulo, msg, barra, b);
        card.getStyleClass().add("overlay-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
        getChildren().add(card);
        setVisible(false);
        setManaged(false);
        atraso.setOnFinished(e -> {
            if (ocupado.get()) setVisible(true);
        });
        ocupado.addListener((o, a, n) -> {
            if (n) {
                atraso.playFromStart();
            } else {
                atraso.stop();
                setVisible(false);
            }
        });
    }
}
