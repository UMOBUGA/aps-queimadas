package br.unip.aps.ui.componentes;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

import java.util.List;

/**
 * Grade responsiva: distribui os filhos em colunas de MESMA largura, recalculando o numero de
 * colunas conforme a largura disponivel (ex.: KPIs em 4/3/2 colunas; graficos em 2/1). Todas as
 * celulas de uma linha recebem a mesma altura, o que mantem os cards alinhados.
 */
@javafx.beans.DefaultProperty("children")
public class GradeResponsiva extends Pane {

    private double larguraMinima = 240;
    private int maxColunas = 4;
    private double hgap = 16;
    private double vgap = 16;

    /** Cria a grade com os valores padrao (colunas de 240 px, ate 4, espaco de 16 px). */
    public GradeResponsiva() {
        getStyleClass().add("grade-responsiva");
        // quando a largura muda o numero de colunas, a altura preferida muda: avisa o pai
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

    /**
     * @param larguraMinima largura minima de cada coluna (px)
     * @param maxColunas    numero maximo de colunas
     * @param gap           espaco horizontal e vertical entre celulas
     */
    public GradeResponsiva(double larguraMinima, int maxColunas, double gap) {
        this();
        setLarguraMinima(larguraMinima);
        setMaxColunas(maxColunas);
        setGap(gap);
    }

    /** @param v largura minima de cada coluna (px) */
    public void setLarguraMinima(double v) { larguraMinima = v; requestLayout(); }
    /** @return largura minima de cada coluna */
    public double getLarguraMinima() { return larguraMinima; }
    /** @param v numero maximo de colunas */
    public void setMaxColunas(int v) { maxColunas = Math.max(1, v); requestLayout(); }
    /** @return numero maximo de colunas */
    public int getMaxColunas() { return maxColunas; }
    /** @param v espaco entre celulas (px) */
    public void setGap(double v) { hgap = v; vgap = v; requestLayout(); }
    /** @return espaco entre celulas */
    public double getGap() { return hgap; }

    /**
     * @param nos celulas
     * @return a propria grade (encadeamento)
     */
    public GradeResponsiva com(Node... nos) {
        getChildren().addAll(nos);
        return this;
    }

    /**
     * @param largura largura disponivel
     * @return numero de colunas para essa largura
     */
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
