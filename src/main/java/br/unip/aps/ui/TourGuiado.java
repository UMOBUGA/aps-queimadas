package br.unip.aps.ui;

import br.unip.aps.ui.componentes.Movimento;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

/** Tour de primeira execucao: cinco passos que apontam as regioes da tela; pode ser pulado e revisto pela paleta. */
final class TourGuiado {
    /** Um passo do tour: alvo destacado, titulo e texto. */
    record Passo(Supplier<Node> alvo, String titulo, String texto) { }

    private static final String CHAVE = "tourVisto";

    private final StackPane raiz;
    private final List<Passo> passos;
    private final Pane camada = new Pane();
    private final Rectangle destaque = new Rectangle();
    private final VBox cartao = new VBox(10);
    private final Label titulo = new Label();
    private final Label texto = new Label();
    private final Label contador = new Label();
    private final Button proximo = new Button();
    private int indice;

    TourGuiado(StackPane raiz, List<Passo> passos) {
        this.raiz = raiz;
        this.passos = passos;
        camada.getStyleClass().add("tour-camada");
        destaque.getStyleClass().add("tour-destaque");
        destaque.setArcWidth(10);
        destaque.setArcHeight(10);
        destaque.setMouseTransparent(true);
        titulo.getStyleClass().add("tour-titulo");
        texto.getStyleClass().add("tour-texto");
        texto.setWrapText(true);
        contador.getStyleClass().add("tour-contador");
        Button pular = new Button("Pular tour");
        pular.getStyleClass().add("btn-ghost");
        pular.setOnAction(e -> fechar());
        proximo.getStyleClass().add("btn-primary");
        proximo.setOnAction(e -> avancar());
        Region esp = new Region();
        HBox.setHgrow(esp, javafx.scene.layout.Priority.ALWAYS);
        HBox acoes = new HBox(8, contador, esp, pular, proximo);
        acoes.setAlignment(Pos.CENTER_LEFT);
        cartao.getChildren().addAll(titulo, texto, acoes);
        cartao.getStyleClass().add("tour-cartao");
        cartao.setPrefWidth(380);
        camada.getChildren().addAll(destaque, cartao);
        camada.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ESCAPE -> fechar();
                case ENTER, RIGHT -> avancar();
                default -> { }
            }
        });
    }

    static boolean jaVisto() {
        return Preferences.userNodeForPackage(TourGuiado.class).getBoolean(CHAVE, false);
    }

    void iniciar() {
        indice = 0;
        if (!raiz.getChildren().contains(camada)) raiz.getChildren().add(camada);
        camada.setFocusTraversable(true);
        camada.requestFocus();
        mostrar();
    }

    private void avancar() {
        if (indice >= passos.size() - 1) {
            fechar();
            return;
        }
        indice++;
        mostrar();
    }

    private void fechar() {
        raiz.getChildren().remove(camada);
        Preferences.userNodeForPackage(TourGuiado.class).putBoolean(CHAVE, true);
    }

    private void mostrar() {
        Passo p = passos.get(indice);
        titulo.setText(p.titulo());
        texto.setText(p.texto());
        contador.setText((indice + 1) + " de " + passos.size());
        proximo.setText(indice == passos.size() - 1 ? "Começar" : "Próximo");
        raiz.applyCss();
        raiz.layout();
        Node alvo = p.alvo().get();
        Bounds b = alvo == null || alvo.getScene() == null ? null : raiz.sceneToLocal(alvo.localToScene(alvo.getBoundsInLocal()));
        double w = raiz.getWidth(), h = raiz.getHeight();
        cartao.applyCss();
        cartao.autosize();
        double cw = cartao.prefWidth(-1), ch = cartao.prefHeight(cw);
        if (b == null) {
            destaque.setVisible(false);
            cartao.relocate((w - cw) / 2, (h - ch) / 2);
        } else {
            destaque.setVisible(true);
            destaque.setX(b.getMinX() - 6);
            destaque.setY(b.getMinY() - 6);
            destaque.setWidth(b.getWidth() + 12);
            destaque.setHeight(b.getHeight() + 12);
            double x = b.getMaxX() + 20 + cw < w ? b.getMaxX() + 20 : Math.max(16, Math.min(w - cw - 16, b.getMinX()));
            double y = b.getMaxX() + 20 + cw < w ? Math.max(16, Math.min(h - ch - 16, b.getMinY()))
                    : (b.getMaxY() + 20 + ch < h ? b.getMaxY() + 20 : Math.max(16, b.getMinY() - ch - 20));
            cartao.relocate(x, y);
        }
        Movimento.entradaEscalonada(cartao);
    }
}
