package br.unip.aps.ui.componentes;

import br.unip.aps.ui.tema.GerenciadorTema;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

/** Notificacao nao bloqueante (toast) exibida no canto inferior direito, com icone e cor semanticos. */
public final class Toast extends HBox {
    /** Tipo semantico. */
    public enum Tipo {
        SUCESSO("sucesso", Icones.SUCESSO), ERRO("erro", Icones.ERRO), ALERTA("alerta", Icones.ALERTA), INFO("info", Icones.INFO);

        final String classe;
        final String icone;

        Tipo(String classe, String icone) {
            this.classe = classe;
            this.icone = icone;
        }
    }

    private Toast(Tipo tipo, String titulo, String texto, Runnable fechar) {
        getStyleClass().addAll("toast", tipo.classe);
        FontIcon ic = Icones.de(tipo.icone, 20);
        ic.getStyleClass().add("toast-icone");
        Label t = new Label(titulo);
        t.getStyleClass().add("toast-titulo");
        VBox textos = new VBox(2, t);
        if (texto != null && !texto.isBlank()) {
            Label d = new Label(texto);
            d.getStyleClass().add("toast-texto");
            d.setWrapText(true);
            d.setMaxWidth(320);
            textos.getChildren().add(d);
        }
        HBox.setHgrow(textos, Priority.ALWAYS);
        Button x = new Button();
        x.setGraphic(Icones.de(Icones.FECHAR, 15));
        x.getStyleClass().add("btn-icon");
        x.setAccessibleText("Fechar notificacao");
        x.setOnAction(e -> fechar.run());
        getChildren().addAll(ic, textos, x);
        setAlignment(Pos.CENTER_LEFT);
        setMaxHeight(USE_PREF_SIZE);
        setAccessibleText(titulo + (texto != null ? ". " + texto : ""));
    }

    /** Mostra um toast no container (VBox alinhado embaixo a direita). */
    public static void mostrar(VBox container, Tipo tipo, String titulo, String texto) {
        Toast[] ref = new Toast[1];
        Runnable fechar = () -> {
            if (ref[0] == null || !container.getChildren().contains(ref[0])) return;
            FadeTransition f = new FadeTransition(Duration.millis(180), ref[0]);
            f.setToValue(0);
            f.setOnFinished(e -> container.getChildren().remove(ref[0]));
            f.play();
        };
        Toast t = new Toast(tipo, titulo, texto, fechar);
        ref[0] = t;
        if (container.getChildren().size() >= 4) container.getChildren().remove(0);
        container.getChildren().add(t);
        boolean animar = GerenciadorTema.get().animacoesProperty().get();
        if (animar) {
            t.setOpacity(0);
            t.setTranslateY(12);
            FadeTransition in = new FadeTransition(Duration.millis(220), t);
            in.setToValue(1);
            TranslateTransition sobe = new TranslateTransition(Duration.millis(220), t);
            sobe.setToY(0);
            in.play();
            sobe.play();
        }
        PauseTransition espera = new PauseTransition(Duration.seconds(tipo == Tipo.ERRO ? 7 : 4.5));
        SequentialTransition seq = new SequentialTransition(espera);
        seq.setOnFinished(e -> fechar.run());
        seq.play();
    }
}
