package br.unip.aps.ui.componentes;

import javafx.scene.layout.Region;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;

/** Minigrafico de linha (sparkline) sem eixos, usado nos cards de KPI para mostrar a tendencia mensal. */
public class Sparkline extends Region {
    private final Polyline linha = new Polyline();
    private final Circle ponto = new Circle(2.5);
    private double[] valores = new double[0];

    public Sparkline(String classeCor) {
        linha.getStyleClass().addAll("sparkline-linha", classeCor);
        ponto.getStyleClass().add("sparkline-ponto");
        getChildren().addAll(linha, ponto);
        setPrefSize(96, 30);
        setMinSize(60, 24);
        setMaxHeight(30);
        setMouseTransparent(true);
    }

    public void setValores(double[] v) {
        this.valores = v == null ? new double[0] : v.clone();
        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        linha.getPoints().clear();
        if (valores.length < 2) {
            ponto.setVisible(false);
            return;
        }
        double w = getWidth(), h = getHeight(), pad = 3;
        double max = Double.NEGATIVE_INFINITY, min = Double.POSITIVE_INFINITY;
        for (double v : valores) {
            max = Math.max(max, v);
            min = Math.min(min, v);
        }
        double faixa = max - min == 0 ? 1 : max - min;
        double x = 0, y = 0;
        for (int i = 0; i < valores.length; i++) {
            x = pad + (w - 2 * pad) * i / (valores.length - 1);
            y = pad + (h - 2 * pad) * (1 - (valores[i] - min) / faixa);
            linha.getPoints().addAll(x, y);
        }
        ponto.setVisible(true);
        ponto.setCenterX(x);
        ponto.setCenterY(y);
    }
}
