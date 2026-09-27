package br.unip.aps.ui.componentes;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;

/** Marca do sistema: chama em gradiente termico com tres barras de grafico (fogo + dados). */
public final class Logo {
    private static final String CHAMA = "M12 1.5 C12.6 4.8 15.5 6.6 17.2 9.4 C19.4 13 18.6 17.8 15 19.9 C11.6 21.9 6.9 20.8 5.3 17.2 "
            + "C4.2 14.7 4.9 11.9 6.6 9.9 C6.9 11.6 7.8 12.8 9.2 13.3 C8.2 9.7 10.2 5.6 12 1.5 Z";

    private Logo() {
    }

    /** Marca desenhada num quadrado de lado {@code tamanho}. */
    public static Node criar(double tamanho) {
        SVGPath chama = new SVGPath();
        chama.setContent(CHAMA);
        chama.setFill(new LinearGradient(0, 1, 0, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#932667")), new Stop(0.4, Color.web("#EB6A26")),
                new Stop(0.75, Color.web("#FCA50A")), new Stop(1, Color.web("#FCFFA4"))));
        Group g = new Group(chama, barra(9.1, 2.6), barra(11.2, 4.4), barra(13.3, 3.3));
        double escala = tamanho / 24.0;
        g.setScaleX(escala);
        g.setScaleY(escala);
        Group externo = new Group(g);
        externo.setAccessibleText("Queimadas");
        return externo;
    }

    private static Rectangle barra(double x, double altura) {
        Rectangle r = new Rectangle(x, 18.6 - altura, 1.6, altura);
        r.setArcWidth(0.6);
        r.setArcHeight(0.6);
        r.setFill(Color.web("#0A0807", 0.88));
        return r;
    }
}
