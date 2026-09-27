package br.unip.aps.ui.componentes;

import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;

/** Item de navegacao da sidebar. */
public class SidebarItem extends ToggleButton {
    private final String rotulo;

    public SidebarItem(String rotulo, String icone, String atalho) {
        super(rotulo, Icones.de(icone, 19));
        this.rotulo = rotulo;
        getStyleClass().setAll("sidebar-item");
        setMaxWidth(Double.MAX_VALUE);
        setMnemonicParsing(false);
        setTooltip(new Tooltip(rotulo + (atalho != null ? "  (" + atalho + ")" : "")));
        setAccessibleText(rotulo);
    }

    public void setRecolhido(boolean recolhido) {
        setContentDisplay(recolhido ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT);
    }

    public String getRotulo() {
        return rotulo;
    }
}
