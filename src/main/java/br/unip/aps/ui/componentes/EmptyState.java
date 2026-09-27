package br.unip.aps.ui.componentes;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Estado vazio: icone, titulo, explicacao e acoes. */
public class EmptyState extends VBox {
    private final Label titulo = new Label();
    private final Label texto = new Label();
    private final HBox acoes = new HBox(Espaco.S);

    public EmptyState(String icone, String titulo, String texto, boolean compacto, Node... botoes) {
        getStyleClass().add("empty-state");
        StackPane circulo = new StackPane(Icones.de(icone, compacto ? 22 : 34));
        circulo.getStyleClass().add("empty-icone");
        if (compacto) circulo.getStyleClass().add("pequeno");
        this.titulo.setText(titulo);
        this.titulo.getStyleClass().add("empty-titulo");
        this.texto.setText(texto);
        this.texto.getStyleClass().add("empty-texto");
        this.texto.setWrapText(true);
        this.texto.setMaxWidth(compacto ? 320 : 460);
        acoes.setAlignment(Pos.CENTER);
        acoes.getChildren().addAll(botoes);
        getChildren().addAll(circulo, this.titulo, this.texto);
        if (botoes.length > 0) getChildren().add(acoes);
        setAlignment(Pos.CENTER);
        setFillWidth(false);
    }

    public void setMensagem(String t, String x) {
        titulo.setText(t);
        texto.setText(x);
    }
}
