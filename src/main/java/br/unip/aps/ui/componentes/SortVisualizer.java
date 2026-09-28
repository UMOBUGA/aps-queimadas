package br.unip.aps.ui.componentes;

import br.unip.aps.sorting.AlgoritmoTipo;
import br.unip.aps.sorting.GravadorPassos;
import br.unip.aps.util.Formatos;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Visualizacao animada de um algoritmo de ordenacao em uma amostra pequena (barras). */
public class SortVisualizer extends VBox {
    private static final int N = 32;

    private final Pane palco = new Pane();
    private final List<Region> barras = new ArrayList<>();
    private final Label lblPasso = new Label();
    private final Label lblComp = new Label();
    private final Label lblMov = new Label();
    private final VBox codigo = new VBox(2);
    private final Label lblOperacao = new Label();
    private Pseudocodigo pseudo;
    private final Button play = new Button();
    private final Slider velocidade = new Slider(1, 60, 22);
    private final Timeline timeline = new Timeline();
    private int[] inicial = new int[N];
    private int[] atual = new int[N];
    private List<GravadorPassos.Passo> eventos = List.of();
    private int passo;
    private long comparacoes;
    private long movimentos;
    private AlgoritmoTipo algoritmo = AlgoritmoTipo.BUBBLE;
    private final Random rnd = new Random(7);

    /** Cria o visualizador com uma permutacao aleatoria de 1..32. */
    public SortVisualizer() {
        setSpacing(Espaco.M);
        palco.getStyleClass().add("viz-palco");
        palco.setPrefHeight(260);
        palco.setMinHeight(200);
        VBox.setVgrow(palco, Priority.ALWAYS);
        for (int i = 0; i < N; i++) {
            Region r = new Region();
            r.getStyleClass().add("viz-barra");
            r.setManaged(false);
            barras.add(r);
        }
        palco.getChildren().addAll(barras);
        palco.widthProperty().addListener((o, a, n) -> desenhar());
        palco.heightProperty().addListener((o, a, n) -> desenhar());

        play.getStyleClass().add("btn-primary");
        play.setOnAction(e -> alternar());
        Button reiniciar = new Button("Reiniciar", Icones.de(Icones.REINICIAR, 16));
        reiniciar.getStyleClass().add("btn-secondary");
        reiniciar.setOnAction(e -> preparar(false));
        Button embaralhar = new Button("Nova amostra", Icones.de(Icones.RECARREGAR, 16));
        embaralhar.getStyleClass().add("btn-ghost");
        embaralhar.setOnAction(e -> preparar(true));
        velocidade.setPrefWidth(160);
        velocidade.setTooltip(new Tooltip("Velocidade da animação"));
        Label vel = new Label("Velocidade");
        vel.getStyleClass().add("field-label");
        for (Label l : List.of(lblPasso, lblComp, lblMov)) l.getStyleClass().add("chip");
        lblComp.setGraphic(Icones.de(Icones.COMPARACOES, 14));
        lblMov.setGraphic(Icones.de(Icones.TROCAS, 14));
        Button anterior = new Button(null, Icones.de(Icones.PASSO_ANTERIOR, 16));
        anterior.getStyleClass().add("btn-secondary");
        anterior.setTooltip(new Tooltip("Voltar um passo"));
        anterior.setAccessibleText("Voltar um passo");
        anterior.setOnAction(e -> voltarPasso());
        Button seguinte = new Button(null, Icones.de(Icones.PASSO_SEGUINTE, 16));
        seguinte.getStyleClass().add("btn-secondary");
        seguinte.setTooltip(new Tooltip("Avançar um passo"));
        seguinte.setAccessibleText("Avançar um passo");
        seguinte.setOnAction(e -> avancarPasso());
        Region esp = new Region();
        HBox.setHgrow(esp, Priority.ALWAYS);
        HBox controles = new HBox(8, play, anterior, seguinte, reiniciar, embaralhar, esp, vel, velocidade);
        controles.setAlignment(Pos.CENTER_LEFT);
        HBox contadores = new HBox(8, lblPasso, lblComp, lblMov, legenda());
        contadores.setAlignment(Pos.CENTER_LEFT);
        codigo.getStyleClass().add("pseudocodigo");
        codigo.setMinWidth(280);
        codigo.setPrefWidth(320);
        codigo.setMaxWidth(360);
        palco.setPrefWidth(640);
        lblOperacao.getStyleClass().add("pseudocodigo-operacao");
        lblOperacao.setWrapText(true);
        VBox lateral = new VBox(10, codigo, lblOperacao);
        HBox.setHgrow(palco, Priority.ALWAYS);
        HBox area = new HBox(20, palco, lateral);
        VBox.setVgrow(area, Priority.ALWAYS);
        getChildren().addAll(controles, area, contadores);
        timeline.setCycleCount(Timeline.INDEFINITE);
        velocidade.valueProperty().addListener((o, a, n) -> ajustarRitmo());
        embaralharInicial();
        preparar(false);
    }

    private HBox legenda() {
        HBox h = new HBox(12);
        h.setAlignment(Pos.CENTER_LEFT);
        for (String[] s : new String[][]{{"comparando", "Comparando"}, {"trocando", "Movendo"}, {"ordenada", "Ordenado"}}) {
            Region m = new Region();
            m.getStyleClass().addAll("viz-barra", s[0]);
            m.setMinSize(10, 10);
            m.setMaxSize(10, 10);
            Label l = new Label(s[1], m);
            l.getStyleClass().add("legenda-item");
            h.getChildren().add(l);
        }
        return h;
    }

    /** Define o algoritmo e prepara a gravacao. */
    public void setAlgoritmo(AlgoritmoTipo tipo) {
        this.algoritmo = tipo;
        preparar(false);
    }

    private void embaralharInicial() {
        inicial = new int[N];
        for (int i = 0; i < N; i++) inicial[i] = i + 1;
        for (int i = N - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = inicial[i];
            inicial[i] = inicial[j];
            inicial[j] = t;
        }
    }

    private void montarCodigo() {
        pseudo = Pseudocodigo.de(algoritmo);
        codigo.getChildren().clear();
        for (int i = 0; i < pseudo.linhas().size(); i++) {
            Label l = new Label(String.format("%2d  %s", i + 1, pseudo.linhas().get(i)));
            l.getStyleClass().add("pseudocodigo-linha");
            l.setMaxWidth(Double.MAX_VALUE);
            codigo.getChildren().add(l);
        }
        codigo.setAccessibleText("Pseudocódigo do " + algoritmo.nome());
    }

    private void destacarLinha(int linha) {
        for (int i = 0; i < codigo.getChildren().size(); i++) {
            codigo.getChildren().get(i).getStyleClass().remove("atual");
            if (i == linha) codigo.getChildren().get(i).getStyleClass().add("atual");
        }
    }

    private void voltarPasso() {
        timeline.stop();
        int alvo = Math.max(0, passo - 1);
        atual = inicial.clone();
        passo = 0;
        comparacoes = 0;
        movimentos = 0;
        barras.forEach(b -> b.getStyleClass().removeAll("comparando", "trocando", "ordenada"));
        while (passo < alvo - 1) aplicar(eventos.get(passo++), false);
        if (alvo > 0) avancar();
        else {
            destacarLinha(-1);
            lblOperacao.setText("Início: vetor embaralhado com " + N + " valores.");
            atualizarRotulos();
            desenhar();
        }
        pausado();
    }

    private void avancarPasso() {
        timeline.stop();
        avancar();
        pausado();
    }

    private void pausado() {
        if (passo < eventos.size()) {
            play.setText(passo == 0 ? "Reproduzir" : "Continuar");
            play.setGraphic(Icones.de(Icones.EXECUTAR, 16));
        }
    }

    private void preparar(boolean novaAmostra) {
        timeline.stop();
        montarCodigo();
        destacarLinha(-1);
        lblOperacao.setText("Início: vetor embaralhado com " + N + " valores. Use os botões de passo para seguir linha a linha.");
        if (novaAmostra) embaralharInicial();
        atual = inicial.clone();
        eventos = GravadorPassos.gravar(algoritmo, inicial);
        passo = 0;
        comparacoes = 0;
        movimentos = 0;
        barras.forEach(b -> b.getStyleClass().removeAll("comparando", "trocando", "ordenada"));
        play.setText("Reproduzir");
        play.setGraphic(Icones.de(Icones.EXECUTAR, 16));
        atualizarRotulos();
        desenhar();
    }

    private void alternar() {
        if (timeline.getStatus() == Timeline.Status.RUNNING) {
            timeline.pause();
            play.setText("Continuar");
            play.setGraphic(Icones.de(Icones.EXECUTAR, 16));
        } else {
            if (passo >= eventos.size()) preparar(false);
            ajustarRitmo();
            timeline.play();
            play.setText("Pausar");
            play.setGraphic(Icones.de(Icones.PAUSAR, 16));
        }
    }

    private void ajustarRitmo() {
        boolean rodando = timeline.getStatus() == Timeline.Status.RUNNING;
        timeline.stop();
        double ms = Math.max(4, 260 - velocidade.getValue() * 4.2);
        timeline.getKeyFrames().setAll(new KeyFrame(Duration.millis(ms), e -> avancar()));
        if (rodando) timeline.play();
    }

    private void avancar() {
        barras.forEach(b -> b.getStyleClass().removeAll("comparando", "trocando"));
        if (passo >= eventos.size()) {
            timeline.stop();
            barras.forEach(b -> {
                if (!b.getStyleClass().contains("ordenada")) b.getStyleClass().add("ordenada");
            });
            destacarLinha(-1);
            lblOperacao.setText("Fim: vetor ordenado em " + Formatos.inteiro(eventos.size()) + " passos.");
            play.setText("Repetir");
            play.setGraphic(Icones.de(Icones.REINICIAR, 16));
            atualizarRotulos();
            return;
        }
        aplicar(eventos.get(passo++), true);
        atualizarRotulos();
        desenhar();
    }

    private void aplicar(GravadorPassos.Passo e, boolean visual) {
        switch (e.tipo()) {
            case GravadorPassos.COMPARA -> {
                comparacoes++;
                if (visual) {
                    marcar(e.i(), "comparando");
                    marcar(e.j(), "comparando");
                    destacarLinha(pseudo.comparacao());
                    lblOperacao.setText(e.i() < 0 ? "Compara o valor guardado com o elemento da vez"
                            : "Compara a[" + e.i() + "] = " + atual[e.i()] + " com a[" + e.j() + "] = " + atual[e.j()]);
                }
            }
            case GravadorPassos.TROCA -> {
                movimentos++;
                int t = atual[e.i()];
                atual[e.i()] = atual[e.j()];
                atual[e.j()] = t;
                if (visual) {
                    marcar(e.i(), "trocando");
                    marcar(e.j(), "trocando");
                    destacarLinha(pseudo.troca());
                    lblOperacao.setText("Troca a[" + e.i() + "] e a[" + e.j() + "]: agora " + atual[e.i()] + " e " + atual[e.j()]);
                }
            }
            default -> {
                movimentos++;
                atual[e.i()] = e.valor();
                if (visual) {
                    marcar(e.i(), "trocando");
                    destacarLinha(pseudo.escrita());
                    lblOperacao.setText("Escreve " + e.valor() + " em a[" + e.i() + "]");
                }
            }
        }
    }

    private void marcar(int i, String classe) {
        if (i >= 0 && i < N) barras.get(i).getStyleClass().add(classe);
    }

    private void atualizarRotulos() {
        lblPasso.setText("Passo " + Formatos.inteiro(passo) + " de " + Formatos.inteiro(eventos.size()));
        lblComp.setText(Formatos.inteiro(comparacoes) + " comparações");
        lblMov.setText(Formatos.inteiro(movimentos) + " trocas/escritas");
    }

    private void desenhar() {
        double w = palco.getWidth() - 24, h = palco.getHeight() - 24;
        if (w <= 0 || h <= 0) return;
        double gap = 3;
        double bw = (w - gap * (N - 1)) / N;
        for (int i = 0; i < N; i++) {
            double bh = h * atual[i] / N;
            barras.get(i).resizeRelocate(12 + i * (bw + gap), 12 + h - bh, Math.max(2, bw), bh);
        }
    }

    /** Inicia (ou continua) a reproducao. */
    public void reproduzir() {
        if (timeline.getStatus() != Timeline.Status.RUNNING) alternar();
    }

    /** Interrompe a animacao (ao sair da tela). */
    public void parar() {
        timeline.stop();
        if (passo < eventos.size()) {
            play.setText(passo == 0 ? "Reproduzir" : "Continuar");
            play.setGraphic(Icones.de(Icones.EXECUTAR, 16));
        }
    }
}
