package br.unip.aps.ui.componentes;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Card de indicador (KPI) padronizado: rotulo em caixa-alta, valor grande, icone em badge
 * colorido suave, linha de contexto (com seta de tendencia e cor semantica) e sparkline opcional.
 *
 * <p>A cor so aparece por regra: {@link Tendencia#ALERTA} (ex.: aumento de focos) pinta o
 * contexto e o icone de vermelho; {@link Tendencia#BOA} de verde; o restante fica neutro.</p>
 */
public class KpiCard extends VBox {

    /** Semantica da linha de contexto. */
    public enum Tendencia { NEUTRA, ALERTA, BOA }

    /** Estilo do badge do icone. */
    public enum EstiloIcone {
        NEUTRO(null), DESTAQUE("icone-accent"), ALERTA("icone-alerta"), SUCESSO("icone-sucesso"), INFO("icone-info");

        final String classe;

        EstiloIcone(String classe) {
            this.classe = classe;
        }
    }

    private final Label rotulo = new Label();
    private final Label valor = new Label("—");
    private final Label contexto = new Label();
    private final StackPane badge = new StackPane();
    private final HBox base;
    private Sparkline sparkline;

    /**
     * @param titulo rotulo do indicador
     * @param icone  codigo do icone
     */
    public KpiCard(@javafx.beans.NamedArg("titulo") String titulo, @javafx.beans.NamedArg("icone") String icone) {
        getStyleClass().add("kpi-card");
        rotulo.setText(titulo.toUpperCase(java.util.Locale.of("pt", "BR")));
        rotulo.getStyleClass().add("kpi-rotulo");
        valor.getStyleClass().add("kpi-valor");
        valor.setMinWidth(Region.USE_PREF_SIZE);
        contexto.getStyleClass().add("kpi-contexto");
        contexto.setWrapText(false);
        badge.getStyleClass().add("kpi-icone");
        badge.getChildren().add(Icones.de(icone, 18));

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);
        HBox topo = new HBox(8, rotulo, espaco, badge);
        topo.setAlignment(Pos.CENTER_LEFT);

        Region esp2 = new Region();
        HBox.setHgrow(esp2, Priority.ALWAYS);
        base = new HBox(8, valor, esp2);
        base.setAlignment(Pos.BOTTOM_LEFT);

        getChildren().addAll(topo, base, contexto);
        setFocusTraversable(false);
    }

    /**
     * @param texto novo rotulo do indicador
     * @return este card
     */
    public KpiCard titulo(String texto) {
        rotulo.setText(texto.toUpperCase(java.util.Locale.of("pt", "BR")));
        return this;
    }

    /**
     * @param texto valor principal
     * @return este card
     */
    public KpiCard valor(String texto) {
        valor.setText(texto);
        valor.getStyleClass().remove("kpi-valor-texto");
        return this;
    }

    /**
     * Valor textual longo (ex.: nome de municipio): fonte menor para caber no card.
     *
     * @param texto valor
     * @return este card
     */
    public KpiCard valorTexto(String texto) {
        valor.setText(texto);
        if (!valor.getStyleClass().contains("kpi-valor-texto")) valor.getStyleClass().add("kpi-valor-texto");
        Tooltip.install(valor, new Tooltip(texto));
        return this;
    }

    /**
     * @param texto     linha de contexto
     * @param tendencia semantica (define cor e seta)
     * @return este card
     */
    public KpiCard contexto(String texto, Tendencia tendencia) {
        contexto.setText(texto);
        contexto.getStyleClass().removeAll("tendencia-alerta", "tendencia-boa");
        contexto.setGraphic(null);
        switch (tendencia) {
            case ALERTA -> {
                contexto.getStyleClass().add("tendencia-alerta");
                contexto.setGraphic(Icones.de(Icones.SUBINDO, 14));
            }
            case BOA -> {
                contexto.getStyleClass().add("tendencia-boa");
                contexto.setGraphic(Icones.de(Icones.DESCENDO, 14));
            }
            default -> { }
        }
        return this;
    }

    /**
     * @param estilo estilo do badge do icone
     * @return este card
     */
    public KpiCard icone(EstiloIcone estilo) {
        for (EstiloIcone e : EstiloIcone.values()) if (e.classe != null) badge.getStyleClass().remove(e.classe);
        if (estilo.classe != null) badge.getStyleClass().add(estilo.classe);
        return this;
    }

    /**
     * Adiciona (ou atualiza) um sparkline ao lado do valor.
     *
     * @param valores   serie
     * @param classeCor classe CSS da cor ({@code spark-recente}, {@code spark-anterior})
     * @return este card
     */
    public KpiCard sparkline(double[] valores, String classeCor) {
        if (sparkline == null) {
            sparkline = new Sparkline(classeCor);
            base.getChildren().add(sparkline);
        }
        sparkline.setValores(valores);
        sparkline.setVisible(true);
        sparkline.setManaged(true);
        return this;
    }

    /**
     * Oculta o sparkline (quando o indicador nao se aplica ao filtro atual).
     *
     * @return este card
     */
    public KpiCard semSparkline() {
        if (sparkline != null) {
            sparkline.setVisible(false);
            sparkline.setManaged(false);
        }
        return this;
    }

    /**
     * @param texto dica completa ao passar o mouse
     * @return este card
     */
    public KpiCard dica(String texto) {
        Tooltip.install(this, new Tooltip(texto));
        return this;
    }

    /** @return texto do valor (para testes e acessibilidade) */
    public String getValor() {
        return valor.getText();
    }
}
