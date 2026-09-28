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
    private static final double RAIO = 12;
    private static final double MARGEM = 56;
    private static final Font FONTE = Font.font("Inter SemiBold", FontWeight.NORMAL, 11);
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
        double dy = Math.min(80, (h - 2 * RAIO - 56) / Math.max(1, niveis - 1));
        for (int i = 0; i < nos.size(); i++) {
            Desenho<K> d = nos.get(i);
            double x = nos.size() == 1 ? w / 2 : MARGEM + i * dx;
            destinos.put(d.chave(), new Posicao(x, RAIO + 10 + profundidade.getOrDefault(d.chave(), 0) * dy));
        }
        desenhar();
    }

    private static String caber(String texto, double largura, javafx.scene.text.Text medidor) {
        medidor.setText(texto);
        if (medidor.getLayoutBounds().getWidth() <= largura) return texto;
        for (int n = texto.length() - 1; n > 1; n--) {
            String t = texto.substring(0, n).strip() + "…";
            medidor.setText(t);
            if (medidor.getLayoutBounds().getWidth() <= largura) return t;
        }
        return texto.substring(0, 1) + "…";
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
        g.setFont(FONTE);
        double w = canvas.getWidth(), espaco = nos.size() <= 1 ? w : 2 * (w - 2 * MARGEM) / (nos.size() - 1) - 8;
        javafx.scene.text.Text medidor = new javafx.scene.text.Text();
        medidor.setFont(FONTE);
        for (int i = 0; i < nos.size(); i++) {
            Desenho<K> d = nos.get(i);
            Posicao p = posicao(d.chave());
            if (p == null) continue;
            boolean ehNovo = d.chave().equals(destacado);
            boolean ehPivo = d.chave().equals(pivo);
            g.setFill(ehNovo ? novo : no);
            g.fillOval(p.x() - RAIO, p.y() - RAIO, 2 * RAIO, 2 * RAIO);
            g.setStroke(ehPivo ? rot : borda);
            g.setLineWidth(ehPivo ? 3 : 1.2);
            g.strokeOval(p.x() - RAIO, p.y() - RAIO, 2 * RAIO, 2 * RAIO);
            String r = caber(d.rotulo(), espaco, medidor);
            medidor.setText(r);
            double lw = medidor.getLayoutBounds().getWidth() + 10, ly = p.y() + RAIO + (i % 2 == 0 ? 11 : 27);
            double lx = Math.max(lw / 2 + 2, Math.min(w - lw / 2 - 2, p.x()));
            g.setFill(fundo.deriveColor(0, 1, 1, 0.88));
            g.fillRoundRect(lx - lw / 2, ly - 8, lw, 16, 8, 8);
            g.setFill(ehNovo ? novo : texto);
            g.fillText(r, lx, ly);
        }
    }
}
