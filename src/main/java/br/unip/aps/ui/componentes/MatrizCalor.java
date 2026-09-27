package br.unip.aps.ui.componentes;

import br.unip.aps.util.Formatos;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/**
 * Matriz de confusao como heatmap: intensidade proporcional ao percentual da classe real
 * (normalizacao por linha), exibindo contagem e percentual em cada celula.
 */
public class MatrizCalor extends GridPane {

    /** Cria o componente vazio. */
    public MatrizCalor() {
        setHgap(4);
        setVgap(4);
    }

    /**
     * @param rotulos rotulos das classes
     * @param matriz  matriz [real][previsto]
     */
    public void setDados(String[] rotulos, int[][] matriz) {
        getChildren().clear();
        getColumnConstraints().clear();
        ColumnConstraints c0 = new ColumnConstraints(120);
        getColumnConstraints().add(c0);
        for (int j = 0; j < rotulos.length; j++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setHgrow(Priority.ALWAYS);
            c.setMinWidth(90);
            c.setFillWidth(true);
            getColumnConstraints().add(c);
        }
        Label canto = new Label("real ↓   previsto →");
        canto.getStyleClass().add("matriz-cabecalho");
        add(canto, 0, 0);
        for (int j = 0; j < rotulos.length; j++) {
            Label l = new Label(rotulos[j]);
            l.getStyleClass().add("matriz-cabecalho");
            l.setMaxWidth(Double.MAX_VALUE);
            add(l, j + 1, 0);
        }
        for (int i = 0; i < rotulos.length; i++) {
            Label r = new Label(rotulos[i]);
            r.getStyleClass().add("matriz-cabecalho");
            r.setAlignment(Pos.CENTER_LEFT);
            add(r, 0, i + 1);
            long soma = 0;
            for (int v : matriz[i]) soma += v;
            for (int j = 0; j < rotulos.length; j++) {
                double pct = soma == 0 ? 0 : 100.0 * matriz[i][j] / soma;
                int faixa = matriz[i][j] == 0 ? 0 : Math.min(5, 1 + (int) (pct / 20));
                Label c = new Label(Formatos.inteiro(matriz[i][j]) + "\n" + Formatos.decimal(pct, 1) + "%");
                c.getStyleClass().addAll("celula-heat", "heat-" + faixa);
                c.setMaxWidth(Double.MAX_VALUE);
                c.setMinHeight(58);
                c.setAlignment(Pos.CENTER);
                c.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                Graficos.tooltip(c, "Real: " + rotulos[i] + " · Previsto: " + rotulos[j] + "\n"
                        + Formatos.inteiro(matriz[i][j]) + " casos (" + Formatos.decimal(pct, 1) + "% da classe real)"
                        + (i == j ? " — acerto" : " — erro"));
                add(c, j + 1, i + 1);
            }
        }
    }
}
