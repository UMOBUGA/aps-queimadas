package br.unip.aps.ui.componentes;

import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

/** Regras de layout globais. */
public final class Layout {
    private Layout() { }

    /** Aplica a regra "nao encolher" a toda a arvore (inclusive a nos adicionados depois). */
    public static void naoEncolher(Node raiz) {
        aplicar(raiz);
        if (raiz instanceof Parent p) {
            p.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) c -> {
                while (c.next()) for (Node n : c.getAddedSubList()) naoEncolher(n);
            });
            for (Node n : p.getChildrenUnmodifiable()) naoEncolher(n);
        }
    }

    private static void aplicar(Node n) {
        if (n instanceof ButtonBase b) {
            b.setMinWidth(Region.USE_PREF_SIZE);
        } else if (n instanceof Label l && (l.getStyleClass().contains("chip") || l.getStyleClass().contains("field-label")
                || l.getStyleClass().contains("section-label") || l.getStyleClass().contains("filtro-rotulo"))) {
            l.setMinWidth(Region.USE_PREF_SIZE);
        }
    }
}
