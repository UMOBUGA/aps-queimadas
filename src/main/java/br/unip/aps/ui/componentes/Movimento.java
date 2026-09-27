package br.unip.aps.ui.componentes;

import br.unip.aps.ui.tema.GerenciadorTema;
import br.unip.aps.util.Formatos;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.util.Duration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Movimento do sistema: curva de saida forte, 150-450 ms, e nada anima se o usuario desligou as animacoes. */
public final class Movimento {
    /** Desaceleracao forte (equivalente a cubic-bezier(0.16, 1, 0.3, 1)). */
    public static final Interpolator SAIDA = Interpolator.SPLINE(0.16, 1, 0.3, 1);
    private static final Pattern NUMERO = Pattern.compile("^([-+−]?)([\\d.]+)(,(\\d+))?(.*)$");
    private static final String CHAVE_ANIMACAO = "aps.contagem";

    private Movimento() {
    }

    public static boolean ativo() {
        return GerenciadorTema.get().animacoesProperty().get();
    }

    /** Secoes da pagina surgem de baixo para cima, uma apos a outra (40 ms de defasagem). */
    public static void entradaEscalonada(Parent raiz) {
        if (!ativo()) return;
        Parent conteudo = raiz instanceof ScrollPane sp && sp.getContent() instanceof Parent p ? p : raiz;
        ParallelTransition todas = new ParallelTransition();
        int i = 0;
        for (Node filho : conteudo.getChildrenUnmodifiable()) {
            if (!filho.isVisible() || i > 8) continue;
            filho.setOpacity(0);
            filho.setTranslateY(12);
            Timeline t = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(filho.opacityProperty(), 0), new KeyValue(filho.translateYProperty(), 12)),
                    new KeyFrame(Duration.millis(300), new KeyValue(filho.opacityProperty(), 1, SAIDA),
                            new KeyValue(filho.translateYProperty(), 0, SAIDA)));
            t.setDelay(Duration.millis(45L * i++));
            todas.getChildren().add(t);
        }
        todas.play();
    }

    /** Troca o texto numerico de um rotulo contando do valor atual ate o novo (formato pt-BR preservado). */
    public static void contar(Label rotulo, String novoTexto) {
        Object anterior = rotulo.getProperties().get(CHAVE_ANIMACAO);
        if (anterior instanceof Timeline t) t.stop();
        Matcher de = NUMERO.matcher(rotulo.getText() == null ? "" : rotulo.getText());
        Matcher para = NUMERO.matcher(novoTexto == null ? "" : novoTexto);
        if (!ativo() || rotulo.getScene() == null || !de.matches() || !para.matches() || !para.group(5).equals(de.group(5))) {
            rotulo.setText(novoTexto);
            return;
        }
        double v0 = valor(de), v1 = valor(para);
        if (v0 == v1) {
            rotulo.setText(novoTexto);
            return;
        }
        int casas = para.group(4) == null ? 0 : para.group(4).length();
        String sufixo = para.group(5);
        String sinal = para.group(1).equals("+") ? "+" : "";
        SimpleDoubleProperty p = new SimpleDoubleProperty(v0);
        p.addListener((o, a, n) -> rotulo.setText(sinal + (casas == 0 ? Formatos.inteiro(Math.round(n.doubleValue()))
                : Formatos.decimal(n.doubleValue(), casas)) + sufixo));
        Timeline t = new Timeline(new KeyFrame(Duration.millis(450), new KeyValue(p, v1, SAIDA)));
        t.setOnFinished(e -> rotulo.setText(novoTexto));
        rotulo.getProperties().put(CHAVE_ANIMACAO, t);
        t.play();
    }

    private static double valor(Matcher m) {
        double inteiro = Double.parseDouble(m.group(2).replace(".", "").isEmpty() ? "0" : m.group(2).replace(".", ""));
        double frac = m.group(4) == null ? 0 : Double.parseDouble("0." + m.group(4));
        double v = inteiro + frac;
        return m.group(1).equals("-") || m.group(1).equals("−") ? -v : v;
    }
}
