package br.unip.aps.ui.componentes;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

/** Chips: rotulos compactos para contexto (UF, anos, registros), estados (verificado, O(n²)) e filtros ativos removiveis. */
public final class Chip {
    /** Variante visual (mapeada para classes CSS). */
    public enum Variante {
        NEUTRO(""), DESTAQUE("chip-accent"), SUCESSO("chip-success"), ALERTA("chip-warning"),
        PERIGO("chip-danger"), INFO("chip-info");

        final String classe;

        Variante(String classe) {
            this.classe = classe;
        }
    }

    private Chip() { }

    public static Label de(String texto, String icone, Variante variante) {
        Label l = new Label(texto);
        l.getStyleClass().add("chip");
        l.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        if (!variante.classe.isEmpty()) l.getStyleClass().add(variante.classe);
        if (icone != null) l.setGraphic(Icones.de(icone, 14));
        return l;
    }

    public static Label de(String texto) {
        return de(texto, null, Variante.NEUTRO);
    }

    /** Altera texto e variante de um chip existente. */
    public static void atualizar(Label chip, String texto, Variante variante) {
        chip.setText(texto);
        for (Variante v : Variante.values()) if (!v.classe.isEmpty()) chip.getStyleClass().remove(v.classe);
        if (!variante.classe.isEmpty()) chip.getStyleClass().add(variante.classe);
    }

    /** Chip de filtro ativo com botao "x". */
    public static HBox removivel(String texto, Runnable remover) {
        Label l = new Label(texto);
        Button x = new Button();
        x.setGraphic(Icones.de(Icones.FECHAR, 13));
        x.getStyleClass().add("chip-fechar");
        x.setTooltip(new Tooltip("Remover filtro"));
        x.setAccessibleText("Remover filtro " + texto);
        x.setOnAction(e -> remover.run());
        HBox h = new HBox(l, x);
        h.getStyleClass().addAll("chip", "chip-accent", "chip-filtro");
        return h;
    }
}
