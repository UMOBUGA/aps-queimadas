package br.unip.aps.ui.componentes;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

import java.util.List;

/** Grade de colunas iguais que se ajusta a largura disponivel. */
@javafx.beans.DefaultProperty("children")
public class GradeResponsiva extends Pane {
    private double larguraMinima = 240;
    private int maxColunas = 4;
    private double hgap = 16;
    private double vgap = 16;

    /** Cria a grade com os valores padrao (colunas de 240 px, ate 4, espaco de 16 px). */
    public GradeResponsiva() {
        getStyleClass().add("grade-responsiva");
        widthProperty().addListener((o, a, n) -> {
            int c = colunas(n.doubleValue() - getInsets().getLeft() - getInsets().getRight());
            if (c != ultimasColunas) {
                ultimasColunas = c;
                if (getParent() != null) getParent().requestLayout();
                requestLayout();
            }
        });
    }

    private int ultimasColunas = -1;

    public GradeResponsiva(double larguraMinima, int maxColunas, double gap) {
        this();
        setLarguraMinima(larguraMinima);
        setMaxColunas(maxColunas);
        setGap(gap);
    }

    public void setLarguraMinima(double v) { larguraMinima = v; requestLayout(); }
    public double getLarguraMinima() { return larguraMinima; }
    public void setMaxColunas(int v) { maxColunas = Math.max(1, v); requestLayout(); }
    public int getMaxColunas() { return maxColunas; }
    public void setGap(double v) { hgap = v; vgap = v; requestLayout(); }
    public double getGap() { return hgap; }

    public GradeResponsiva com(Node... nos) {
        getChildren().addAll(nos);
        return this;
    }

    public int colunas(double largura) {
        int n = (int) Math.floor((largura + hgap) / (larguraMinima + hgap));
        return Math.max(1, Math.min(maxColunas, Math.min(n, Math.max(1, visiveis().size()))));
    }

    private List<Node> visiveis() {
        return getChildren().stream().filter(Node::isManaged).toList();
    }

    @Override
    protected void layoutChildren() {
        Insets in = getInsets();
        double w = getWidth() - in.getLeft() - in.getRight();
        List<Node> nos = visiveis();
        int cols = colunas(w);
        double cw = (w - hgap * (cols - 1)) / cols;
        double y = in.getTop();
        for (int i = 0; i < nos.size(); i += cols) {
            double h = 0;
            for (int j = i; j < Math.min(i + cols, nos.size()); j++) h = Math.max(h, nos.get(j).prefHeight(cw));
            for (int j = i; j < Math.min(i + cols, nos.size()); j++) {
                nos.get(j).resizeRelocate(in.getLeft() + (j - i) * (cw + hgap), y, cw, h);
            }
            y += h + vgap;
        }
    }

    @Override
    protected double computePrefHeight(double largura) {
        Insets in = getInsets();
        double w = (largura > 0 ? largura : getWidth()) - in.getLeft() - in.getRight();
        if (w <= 0) w = larguraMinima * maxColunas;
        List<Node> nos = visiveis();
        int cols = colunas(w);
        double cw = (w - hgap * (cols - 1)) / cols;
        double total = in.getTop() + in.getBottom();
        for (int i = 0; i < nos.size(); i += cols) {
            double h = 0;
            for (int j = i; j < Math.min(i + cols, nos.size()); j++) h = Math.max(h, nos.get(j).prefHeight(cw));
            total += h + (i + cols < nos.size() ? vgap : 0);
        }
        return total;
    }

    @Override
    protected double computeMinHeight(double largura) {
        return computePrefHeight(largura);
    }

    @Override
    protected double computePrefWidth(double altura) {
        return larguraMinima;
    }

    @Override
    protected double computeMinWidth(double altura) {
        return larguraMinima;
    }

    @Override
    public javafx.geometry.Orientation getContentBias() {
        return javafx.geometry.Orientation.HORIZONTAL;
    }
}
