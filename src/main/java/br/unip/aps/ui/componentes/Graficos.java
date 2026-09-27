package br.unip.aps.ui.componentes;

import br.unip.aps.ui.tema.GerenciadorTema;
import br.unip.aps.util.Textos;
import javafx.scene.Node;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.util.function.Function;

/**
 * Utilitarios de graficos: classes CSS de serie (cor ligada a entidade), tooltips em todos os
 * pontos/barras e eixo logaritmico com rotulos 10^k.
 */
public final class Graficos {

    private Graficos() { }

    /**
     * Classe CSS de um bioma (Mata Atlântica -&gt; {@code bioma-mata-atlantica}). Biomas fora da
     * paleta caem em {@code bioma-outro}.
     *
     * @param bioma nome do bioma
     * @return classe CSS
     */
    public static String classeBioma(String bioma) {
        String s = Textos.semAcentos(bioma == null ? "" : bioma).replaceAll("[^a-z]+", "-").replaceAll("(^-|-$)", "");
        return switch (s) {
            case "mata-atlantica", "cerrado", "amazonia", "caatinga", "pantanal", "pampa" -> "bioma-" + s;
            default -> "bioma-outro";
        };
    }

    /**
     * Aplica a classe CSS a serie ja adicionada ao grafico e a todos os seus pontos (atuais e futuros).
     *
     * @param serie  serie
     * @param classe classe (ex.: {@code serie-ano-recente}, {@code serie-3})
     * @param <X>    tipo X
     * @param <Y>    tipo Y
     */
    public static <X, Y> void classe(XYChart.Series<X, Y> serie, String classe) {
        fixar(serie.getNode(), classe);
        serie.nodeProperty().addListener((o, a, n) -> fixar(n, classe));
        for (XYChart.Data<X, Y> d : serie.getData()) {
            fixar(d.getNode(), classe);
            d.nodeProperty().addListener((o, a, n) -> fixar(n, classe));
        }
    }

    /**
     * Garante a classe no no mesmo que o grafico reescreva as classes das series (o XYChart faz
     * {@code getStyleClass().setAll(...)} quando outra serie e adicionada, o que apagaria a cor
     * ligada a entidade e voltaria a colorir pela posicao).
     */
    private static void fixar(Node n, String classe) {
        if (n == null) return;
        if (!n.getStyleClass().contains(classe)) n.getStyleClass().add(classe);
        if (n.getProperties().containsKey("aps-classe-" + classe)) return;
        n.getProperties().put("aps-classe-" + classe, Boolean.TRUE);
        n.getStyleClass().addListener((javafx.collections.ListChangeListener<String>) c -> {
            if (!n.getStyleClass().contains(classe)) javafx.application.Platform.runLater(() -> {
                if (!n.getStyleClass().contains(classe)) n.getStyleClass().add(classe);
            });
        });
    }

    /**
     * Instala tooltip em cada ponto/barra da serie.
     *
     * @param serie serie
     * @param texto texto do tooltip a partir do dado
     * @param <X>   tipo X
     * @param <Y>   tipo Y
     */
    public static <X, Y> void tooltips(XYChart.Series<X, Y> serie, Function<XYChart.Data<X, Y>, String> texto) {
        for (XYChart.Data<X, Y> d : serie.getData()) {
            if (d.getNode() != null) tooltip(d.getNode(), texto.apply(d));
            d.nodeProperty().addListener((o, a, n) -> {
                if (n != null) tooltip(n, texto.apply(d));
            });
        }
    }

    /**
     * @param n     no
     * @param texto texto
     */
    public static void tooltip(Node n, String texto) {
        Tooltip t = new Tooltip(texto);
        t.setShowDelay(Duration.millis(80));
        t.setHideDelay(Duration.millis(50));
        Tooltip.install(n, t);
    }

    /**
     * Configura rotulos do eixo como potencias de 10 (os valores plotados ja estao em log10).
     *
     * @param eixo eixo numerico
     * @param log  {@code true} para rotulos 10^k; {@code false} para formato pt-BR comum
     */
    public static void eixoLog(NumberAxis eixo, boolean log) {
        eixo.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number v) {
                if (!log) return br.unip.aps.util.Formatos.inteiro(Math.round(v.doubleValue()));
                double k = v.doubleValue();
                if (Math.abs(k - Math.rint(k)) > 1e-6) return "";
                return abreviar(Math.pow(10, k));
            }

            @Override
            public Number fromString(String s) {
                return 0;
            }
        });
    }

    /**
     * @param v valor
     * @return valor abreviado pt-BR (1 mil, 2,5 mi, 3 bi)
     */
    public static String abreviar(double v) {
        double a = Math.abs(v);
        if (a >= 1e9) return br.unip.aps.util.Formatos.decimal(v / 1e9, a >= 1e10 ? 0 : 1) + " bi";
        if (a >= 1e6) return br.unip.aps.util.Formatos.decimal(v / 1e6, a >= 1e7 ? 0 : 1) + " mi";
        if (a >= 1e3) return br.unip.aps.util.Formatos.decimal(v / 1e3, a >= 1e4 ? 0 : 1) + " mil";
        if (a >= 1 || a == 0) return br.unip.aps.util.Formatos.inteiro(Math.round(v));
        return br.unip.aps.util.Formatos.decimal(v, 2);
    }

    /** @return {@code true} se as animacoes estao habilitadas nas preferencias */
    public static boolean animar() {
        return GerenciadorTema.get().animacoesProperty().get();
    }
}
