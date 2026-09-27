package br.unip.aps.ui.componentes;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.List;

/** Barras horizontais com rotulo a esquerda e valor escrito no fim de cada barra (sem eixo). */
public class BarrasHorizontais extends GridPane {
    /** Item. */
    public record Item(String rotulo, double valor, String texto, String classe, boolean destaque, String dica) { }

    private double maximoFixo = Double.NaN;
    private boolean escalaLog;

    /** Cria o componente vazio. */
    public BarrasHorizontais() {
        getStyleClass().add("barras-horizontais");
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(90);
        c1.setPrefWidth(Region.USE_COMPUTED_SIZE);
        c1.setMaxWidth(260);
        c1.setHalignment(HPos.LEFT);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        c2.setMinWidth(60);
        ColumnConstraints c3 = new ColumnConstraints();
        c3.setMinWidth(56);
        c3.setHalignment(HPos.RIGHT);
        getColumnConstraints().addAll(c1, c2, c3);
    }

    /** Comprimento proporcional a log10(1 + valor): permite comparar valores de ordens de grandeza diferentes. */
    public void setEscalaLog(boolean log) {
        this.escalaLog = log;
    }

    public void setMaximo(double max) {
        this.maximoFixo = max;
    }

    public void setItens(List<Item> itens) {
        getChildren().clear();
        double max = Double.isNaN(maximoFixo) ? 0 : maximoFixo;
        if (Double.isNaN(maximoFixo)) for (Item i : itens) max = Math.max(max, i.valor());
        if (max <= 0) max = 1;
        boolean animar = Graficos.animar();
        for (int r = 0; r < itens.size(); r++) {
            Item it = itens.get(r);
            Label rot = new Label(it.rotulo());
            rot.getStyleClass().add("barra-rotulo");
            if (it.destaque()) rot.getStyleClass().add("destaque");
            rot.setMinWidth(Region.USE_PREF_SIZE);
            rot.setMaxWidth(260);

            StackPane trilho = new StackPane();
            trilho.getStyleClass().add("barra-trilho");
            trilho.setAlignment(Pos.CENTER_LEFT);
            Region fill = new Region();
            fill.getStyleClass().add("barra-fill");
            if (it.classe() != null && !it.classe().isBlank()) fill.getStyleClass().addAll(it.classe().split(" "));
            double frac = escalaLog ? Math.log10(1 + Math.max(0, it.valor())) / Math.log10(1 + max)
                    : it.valor() / max;
            frac = Math.max(0, Math.min(1, frac));
            if (animar) {
                javafx.beans.property.SimpleDoubleProperty f = new javafx.beans.property.SimpleDoubleProperty(0);
                fill.maxWidthProperty().bind(trilho.widthProperty().multiply(f));
                new Timeline(new KeyFrame(Duration.millis(280 + r * 18), new KeyValue(f, frac))).play();
            } else {
                fill.maxWidthProperty().bind(trilho.widthProperty().multiply(frac));
            }
            fill.setMinWidth(frac > 0 ? 3 : 0);
            trilho.getChildren().add(fill);

            Label val = new Label(it.texto());
            val.getStyleClass().add("barra-valor");
            if (it.dica() != null) {
                Graficos.tooltip(trilho, it.dica());
                Graficos.tooltip(rot, it.dica());
            }
            add(rot, 0, r);
            add(trilho, 1, r);
            add(val, 2, r);
        }
    }
}
