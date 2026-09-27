package br.unip.aps.ui.componentes;

import br.unip.aps.estruturas.ArvoreAVL;
import br.unip.aps.ui.tema.GerenciadorTema;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.geometry.VPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Desenha uma arvore AVL em Canvas; apos cada insercao os nos deslizam da posicao antiga para a nova. */
public class ArvoreVisual<K> extends Region {
    private static final double RAIO = 22;
    private static final double MARGEM = 44;
    private static final double DURACAO_MS = 320;

    private record Posicao(double x, double y) { }

    private record Desenho<K>(K chave, String rotulo, int altura, K pai) { }

    private final Canvas canvas = new Canvas();
    private final Function<K, String> rotulo;
    private final Map<K, Posicao> anteriores = new HashMap<>();
    private final Map<K, Posicao> destinos = new HashMap<>();
    private List<Desenho<K>> nos = new ArrayList<>();
    private K destacado;
    private K pivo;
    private double progresso = 1;
    private long inicio;
    private final AnimationTimer animacao = new AnimationTimer() {
        @Override
        public void handle(long agora) {
            if (inicio == 0) inicio = agora;
            progresso = Math.min(1, (agora - inicio) / (DURACAO_MS * 1e6));
            desenhar();
            if (progresso >= 1) stop();
        }
    };

    public ArvoreVisual(Function<K, String> rotulo) {
        this.rotulo = rotulo;
        getStyleClass().add("arvore-visual");
        getChildren().add(canvas);
        setMinHeight(300);
        setPrefHeight(340);
        widthProperty().addListener((o, a, n) -> relayout());
        heightProperty().addListener((o, a, n) -> relayout());
        GerenciadorTema.get().temaProperty().addListener((o, a, n) -> desenhar());
        setAccessibleText("Visualização da árvore AVL");
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        desenhar();
    }

    /** Mostra o estado atual; {@code destacado} e o no recem-inserido e {@code pivo} o no onde houve rotacao. */
    public void mostrar(ArvoreAVL.No<K, ?> raiz, K destacado, K pivo) {
        this.destacado = destacado;
        this.pivo = pivo;
        anteriores.clear();
        anteriores.putAll(destinos);
        nos = new ArrayList<>();
        coletar(raiz, null);
        relayout();
        boolean animar = GerenciadorTema.get().animacoesProperty().get() && !anteriores.isEmpty();
        progresso = animar ? 0 : 1;
        inicio = 0;
        if (animar) animacao.start();
        else desenhar();
        setAccessibleText("Árvore AVL com " + nos.size() + " nós" + (raiz == null ? "" : ", altura " + raiz.altura()));
    }

    private void coletar(ArvoreAVL.No<K, ?> n, K pai) {
        if (n == null) return;
        coletar(n.esquerda(), n.chave());
        nos.add(new Desenho<>(n.chave(), rotulo.apply(n.chave()), n.altura(), pai));
        coletar(n.direita(), n.chave());
    }

    private void relayout() {
        destinos.clear();
        if (nos.isEmpty()) {
            desenhar();
            return;
        }
        Map<K, Integer> profundidade = new HashMap<>();
        for (Desenho<K> d : nos) if (d.pai() == null) profundidade.put(d.chave(), 0);
        boolean mudou = true;
        while (mudou) {
            mudou = false;
            for (Desenho<K> d : nos) {
                if (profundidade.containsKey(d.chave()) || !profundidade.containsKey(d.pai())) continue;
                profundidade.put(d.chave(), profundidade.get(d.pai()) + 1);
                mudou = true;
            }
        }
        int niveis = 1;
        for (int p : profundidade.values()) niveis = Math.max(niveis, p + 1);
        double w = Math.max(1, getWidth()), h = Math.max(1, getHeight());
        double dx = (w - 2 * MARGEM) / Math.max(1, nos.size() - 1);
        double dy = Math.min(78, (h - 2 * RAIO - 20) / Math.max(1, niveis - 1));
        for (int i = 0; i < nos.size(); i++) {
            Desenho<K> d = nos.get(i);
            double x = nos.size() == 1 ? w / 2 : MARGEM + i * dx;
            destinos.put(d.chave(), new Posicao(x, RAIO + 10 + profundidade.getOrDefault(d.chave(), 0) * dy));
        }
        desenhar();
    }

    private Posicao posicao(K chave) {
        Posicao fim = destinos.get(chave);
        Posicao ini = anteriores.get(chave);
        if (ini == null || fim == null) return fim;
        double t = 1 - Math.pow(1 - progresso, 3);
        return new Posicao(ini.x() + (fim.x() - ini.x()) * t, ini.y() + (fim.y() - ini.y()) * t);
    }

    private void desenhar() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        boolean escuro = GerenciadorTema.get().escuro();
        Color fundo = escuro ? Color.web("#0A0807") : Color.web("#FFFFFF");
        Color linha = escuro ? Color.web("#5C4B41") : Color.web("#CFCAC4");
        Color no = escuro ? Color.web("#2B221E") : Color.web("#F3F2F0");
        Color borda = escuro ? Color.web("#8A7668") : Color.web("#8C8178");
        Color texto = escuro ? Color.web("#F6EEE7") : Color.web("#15100D");
        Color novo = escuro ? Color.web("#FF6B1A") : Color.web("#D9480F");
        Color rot = escuro ? Color.web("#FCA50A") : Color.web("#6E1B5E");
        g.setFill(fundo);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (nos.isEmpty()) return;
        g.setLineWidth(1.5);
        g.setStroke(linha);
        for (Desenho<K> d : nos) {
            if (d.pai() == null) continue;
            Posicao a = posicao(d.chave()), b = posicao(d.pai());
            if (a != null && b != null) g.strokeLine(a.x(), a.y(), b.x(), b.y());
        }
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        for (Desenho<K> d : nos) {
            Posicao p = posicao(d.chave());
            if (p == null) continue;
            boolean ehNovo = d.chave().equals(destacado);
            boolean ehPivo = d.chave().equals(pivo);
            g.setFill(ehNovo ? novo : no);
            g.fillOval(p.x() - RAIO, p.y() - RAIO, 2 * RAIO, 2 * RAIO);
            g.setStroke(ehPivo ? rot : borda);
            g.setLineWidth(ehPivo ? 3 : 1.2);
            g.strokeOval(p.x() - RAIO, p.y() - RAIO, 2 * RAIO, 2 * RAIO);
            g.setFill(ehNovo ? Color.web("#1A0B02") : texto);
            g.setFont(Font.font("Inter SemiBold", FontWeight.NORMAL, 10.5));
            g.fillText(d.rotulo(), p.x(), p.y());
        }
    }
}
