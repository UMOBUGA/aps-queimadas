package br.unip.aps.ui.componentes;

import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;

/**
 * Item de navegacao da sidebar: icone + rotulo, estado ativo destacado e modo recolhido (so
 * icone, com tooltip). E um {@link ToggleButton}, portanto navegavel por teclado.
 */
public class SidebarItem extends ToggleButton {

    private final String rotulo;

    /**
     * @param rotulo nome da tela
     * @param icone  codigo do icone
     * @param atalho atalho de teclado exibido no tooltip (ex.: "Ctrl+1")
     */
    public SidebarItem(String rotulo, String icone, String atalho) {
        super(rotulo, Icones.de(icone, 19));
        this.rotulo = rotulo;
        getStyleClass().setAll("sidebar-item");
        setMaxWidth(Double.MAX_VALUE);
        setMnemonicParsing(false);
        setTooltip(new Tooltip(rotulo + (atalho != null ? "  (" + atalho + ")" : "")));
        setAccessibleText(rotulo);
    }

    /**
     * @param recolhido {@code true} para mostrar so o icone
     */
    public void setRecolhido(boolean recolhido) {
        setContentDisplay(recolhido ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT);
    }

    /** @return rotulo da tela */
    public String getRotulo() {
        return rotulo;
    }
}
