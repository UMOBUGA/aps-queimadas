package br.unip.aps.ui.componentes;

import br.unip.aps.analysis.Estatisticas;
import br.unip.aps.ui.tema.GerenciadorTema;
import br.unip.aps.util.Formatos;
import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/** Faixa termica: um retangulo por mes da base, colorido pela quantidade de focos (escala inferno). */
public class FaixaTermica extends HBox {
    private static final double[] POS = {0.0, 0.12, 0.25, 0.4, 0.55, 0.7, 0.85, 1.0};
    private static final Color[] INFERNO = {
            Color.web("#1C1310"), Color.web("#2A0B4A"), Color.web("#5C1370"), Color.web("#932667"),
            Color.web("#C73E4C"), Color.web("#EB6A26"), Color.web("#FBA40A"), Color.web("#FCFFA4")};
    private static final double ALTURA = 30;
    private static final double GAP = 2;

    private final Tira tira = new Tira();
    private final List<YearMonth> meses = new ArrayList<>();
    private final List<Rectangle> celulas = new ArrayList<>();
    private final List<Label> rotulos = new ArrayList<>();
    private final Rectangle selecao = new Rectangle();
    private final Rectangle cursor = new Rectangle();
    private int posCursor = -1;
    private final List<Long> valores = new ArrayList<>();
    private BiConsumer<YearMonth, YearMonth> aoSelecionar;
    private YearMonth de, ate;
    private boolean revelada;

    public FaixaTermica() {
        getStyleClass().add("faixa-termica");
        setAlignment(Pos.TOP_LEFT);
        StackPane trilho = new StackPane(tira);
        trilho.getStyleClass().add("faixa-trilho");
        HBox.setHgrow(trilho, Priority.ALWAYS);
        trilho.setMinWidth(0);
        selecao.getStyleClass().add("faixa-selecao");
        selecao.setManaged(false);
        selecao.setMouseTransparent(true);
        selecao.setVisible(false);
        getChildren().addAll(trilho, legenda());
        setSpacing(18);
        cursor.getStyleClass().add("faixa-cursor");
        cursor.setManaged(false);
        cursor.setMouseTransparent(true);
        cursor.setVisible(false);
        setFocusTraversable(true);
        setAccessibleRole(javafx.scene.AccessibleRole.SLIDER);
        setAccessibleText("Faixa térmica: focos por mês. Use as setas para escolher um mês e Enter para filtrar.");
        focusedProperty().addListener((o, a, f) -> {
            if (f && posCursor < 0 && !meses.isEmpty()) posCursor = meses.size() - 1;
            cursor.setVisible(f && posCursor >= 0);
            anunciar();
            tira.requestLayout();
        });
        setOnKeyPressed(e -> {
            if (meses.isEmpty()) return;
            switch (e.getCode()) {
                case LEFT -> posCursor = Math.max(0, posCursor - 1);
                case RIGHT -> posCursor = Math.min(meses.size() - 1, posCursor + 1);
                case HOME -> posCursor = 0;
                case END -> posCursor = meses.size() - 1;
                case ENTER, SPACE -> clicar(meses.get(posCursor), e.isShiftDown());
                case ESCAPE -> {
                    setSelecao(null, null);
                    if (aoSelecionar != null) aoSelecionar.accept(null, null);
                }
                default -> {
                    return;
                }
            }
            e.consume();
            anunciar();
            tira.requestLayout();
        });
    }

    private HBox legenda() {
        Rectangle escala = new Rectangle(84, 10);
        Stop[] stops = new Stop[POS.length];
        for (int i = 0; i < POS.length; i++) stops[i] = new Stop(POS[i], INFERNO[i]);
        escala.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, stops));
        escala.setArcWidth(2);
        escala.setArcHeight(2);
        Label menos = new Label("menos");
        Label mais = new Label("mais focos");
        menos.getStyleClass().add("faixa-legenda");
        mais.getStyleClass().add("faixa-legenda");
        HBox l = new HBox(7, menos, escala, mais);
        l.setAlignment(Pos.CENTER_LEFT);
        l.setMinWidth(Region.USE_PREF_SIZE);
        l.setPadding(new javafx.geometry.Insets(10, 0, 0, 0));
        Tooltip.install(l, new Tooltip("Clique em um mês para filtrar o período; Shift+clique estende a seleção; clique de novo para limpar."));
        return l;
    }

    /** Recebe a serie mensal completa da base (em ordem cronologica). */
    public void setDados(Map<YearMonth, Long> serie) {
        meses.clear();
        valores.clear();
        celulas.clear();
        rotulos.clear();
        tira.getChildren().clear();
        long max = 1;
        for (long v : serie.values()) max = Math.max(max, v);
        for (Map.Entry<YearMonth, Long> e : serie.entrySet()) {
            YearMonth ym = e.getKey();
            long v = e.getValue();
            Rectangle r = new Rectangle();
            r.getStyleClass().add("faixa-celula");
            r.setFill(cor(v == 0 ? 0 : Math.sqrt((double) v / max)));
            r.setArcWidth(2);
            r.setArcHeight(2);
            Tooltip t = new Tooltip(Estatisticas.MESES[ym.getMonthValue() - 1] + "/" + ym.getYear() + ": " + Formatos.inteiro(v)
                    + (v == 1 ? " foco" : " focos"));
            t.setShowDelay(Duration.millis(80));
            Tooltip.install(r, t);
            r.addEventHandler(MouseEvent.MOUSE_CLICKED, ev -> clicar(ym, ev.isShiftDown()));
            meses.add(ym);
            valores.add(v);
            celulas.add(r);
            Label l = new Label(ym.getMonthValue() == 1 ? String.valueOf(ym.getYear()) : Estatisticas.MESES[ym.getMonthValue() - 1]);
            l.getStyleClass().add("faixa-rotulo");
            if (ym.getMonthValue() == 1) l.getStyleClass().add("ano");
            l.setManaged(false);
            rotulos.add(l);
        }
        tira.getChildren().addAll(celulas);
        tira.getChildren().addAll(rotulos);
        tira.getChildren().addAll(selecao, cursor);
        atualizarSelecao();
        if (!revelada && GerenciadorTema.get().animacoesProperty().get()) {
            for (int i = 0; i < celulas.size(); i++) {
                Rectangle r = celulas.get(i);
                r.setOpacity(0);
                FadeTransition f = new FadeTransition(Duration.millis(220), r);
                f.setDelay(Duration.millis(18L * i));
                f.setToValue(1);
                f.play();
            }
        }
        revelada = true;
        tira.requestLayout();
    }

    private void anunciar() {
        if (posCursor < 0 || posCursor >= meses.size()) return;
        YearMonth ym = meses.get(posCursor);
        setAccessibleText("Faixa térmica, " + Estatisticas.MESES[ym.getMonthValue() - 1] + " de " + ym.getYear() + ": "
                + Formatos.inteiro(valores.get(posCursor)) + " focos. Enter filtra o mês; Shift+Enter estende; Esc limpa.");
    }

    /** Destaca o periodo filtrado (nulos removem o destaque). */
    public void setSelecao(YearMonth de, YearMonth ate) {
        this.de = de;
        this.ate = ate;
        atualizarSelecao();
    }

    public void setOnSelecionar(BiConsumer<YearMonth, YearMonth> acao) {
        this.aoSelecionar = acao;
    }

    private void clicar(YearMonth ym, boolean estender) {
        YearMonth nDe, nAte;
        if (estender && de != null) {
            nDe = ym.isBefore(de) ? ym : de;
            nAte = ym.isBefore(de) ? (ate == null ? de : ate) : ym;
        } else if (ym.equals(de) && ym.equals(ate)) {
            nDe = null;
            nAte = null;
        } else {
            nDe = ym;
            nAte = ym;
        }
        setSelecao(nDe, nAte);
        if (aoSelecionar != null) aoSelecionar.accept(nDe, nAte);
    }

    private void atualizarSelecao() {
        boolean ativa = de != null && !meses.isEmpty();
        selecao.setVisible(ativa);
        for (int i = 0; i < celulas.size(); i++) {
            YearMonth ym = meses.get(i);
            boolean dentro = !ativa || (!ym.isBefore(de) && !ym.isAfter(ate == null ? de : ate));
            if (revelada) celulas.get(i).setOpacity(dentro ? 1 : 0.3);
        }
        tira.requestLayout();
    }

    static Color cor(double t) {
        t = Math.max(0, Math.min(1, t));
        for (int i = 1; i < POS.length; i++) {
            if (t <= POS[i]) {
                double f = (t - POS[i - 1]) / (POS[i] - POS[i - 1]);
                return INFERNO[i - 1].interpolate(INFERNO[i], f);
            }
        }
        return INFERNO[INFERNO.length - 1];
    }

    private final class Tira extends javafx.scene.layout.Pane {
        Tira() {
            setMinWidth(0);
        }

        @Override
        protected double computePrefHeight(double w) {
            return ALTURA + 20;
        }

        @Override
        protected double computeMinHeight(double w) {
            return computePrefHeight(w);
        }

        @Override
        protected double computePrefWidth(double h) {
            return 400;
        }

        @Override
        protected void layoutChildren() {
            int n = celulas.size();
            if (n == 0) return;
            double w = (getWidth() - GAP * (n - 1)) / n;
            int iDe = -1, iAte = -1;
            for (int i = 0; i < n; i++) {
                double x = i * (w + GAP);
                Rectangle r = celulas.get(i);
                r.setX(x);
                r.setY(0);
                r.setWidth(Math.max(1, w));
                r.setHeight(ALTURA);
                Label l = rotulos.get(i);
                l.autosize();
                l.relocate(x + 1, ALTURA + 4);
                boolean cabe = w >= 26 || meses.get(i).getMonthValue() % 3 == 1;
                l.setVisible(cabe);
                if (de != null) {
                    YearMonth ym = meses.get(i);
                    YearMonth fim = ate == null ? de : ate;
                    if (iDe < 0 && !ym.isBefore(de)) iDe = i;
                    if (!ym.isAfter(fim)) iAte = i;
                }
            }
            if (posCursor >= 0 && posCursor < n) {
                cursor.setX(posCursor * (w + GAP));
                cursor.setY(0);
                cursor.setWidth(Math.max(1, w));
                cursor.setHeight(ALTURA);
            }
            if (iDe >= 0 && iAte >= iDe) {
                selecao.setX(iDe * (w + GAP));
                selecao.setY(0);
                selecao.setWidth((iAte - iDe + 1) * (w + GAP) - GAP);
                selecao.setHeight(ALTURA);
            }
        }
    }
}
