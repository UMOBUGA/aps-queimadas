package br.unip.aps.ui.componentes;

import atlantafx.base.controls.CustomTextField;
import br.unip.aps.analysis.Estatisticas;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.ui.FiltroGlobal;
import br.unip.aps.util.Formatos;
import br.unip.aps.util.Textos;
import javafx.animation.PauseTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Barra de filtros global, fixa no topo do conteudo. */
public class FilterBar extends VBox {
    private final ObjectProperty<FiltroGlobal> filtro = new SimpleObjectProperty<>(FiltroGlobal.VAZIO);
    private final MenuButton mbBioma = new MenuButton();
    private final MenuButton mbAno = new MenuButton();
    private final CustomTextField tfMunicipio = new CustomTextField();
    private final ComboBox<YearMonth> cbDe = new ComboBox<>();
    private final ComboBox<YearMonth> cbAte = new ComboBox<>();
    private final Label contagem = new Label();
    private final FlowPane chips = new FlowPane();
    private final ContextMenu sugestoes = new ContextMenu();
    private final List<CheckBox> checksBioma = new ArrayList<>();
    private final List<CheckBox> checksAno = new ArrayList<>();
    private final PauseTransition debounce = new PauseTransition(Duration.millis(380));
    private List<String> municipios = List.of();
    private boolean municipioExato;
    private boolean atualizando;

    /** Cria a barra (sem opcoes ate {@link #setOpcoes(BaseDeFocos)}). */
    public FilterBar() {
        getStyleClass().add("filter-bar");

        mbBioma.getStyleClass().add("btn-secondary");
        mbBioma.setGraphic(Icones.de(Icones.BIOMA, 16));
        mbAno.getStyleClass().add("btn-secondary");
        mbAno.setGraphic(Icones.de(Icones.CALENDARIO, 16));
        mbBioma.setTooltip(new Tooltip("Filtrar por bioma (seleção múltipla)"));
        mbAno.setTooltip(new Tooltip("Filtrar por ano (seleção múltipla)"));

        tfMunicipio.setLeft(Icones.de(Icones.BUSCA, 16));
        tfMunicipio.setPromptText("Buscar município…");
        tfMunicipio.setPrefWidth(220);
        tfMunicipio.setMinWidth(140);
        HBox.setHgrow(tfMunicipio, Priority.SOMETIMES);
        mbBioma.setMinWidth(Region.USE_PREF_SIZE);
        mbAno.setMinWidth(Region.USE_PREF_SIZE);
        tfMunicipio.getStyleClass().add("busca-municipio");
        tfMunicipio.setAccessibleText("Buscar município");
        tfMunicipio.textProperty().addListener((o, a, n) -> {
            if (atualizando) return;
            municipioExato = false;
            mostrarSugestoes(n);
            debounce.playFromStart();
        });
        tfMunicipio.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                sugestoes.hide();
                aplicar();
            } else if (e.getCode() == KeyCode.DOWN && sugestoes.isShowing()) {
                sugestoes.getSkin().getNode().requestFocus();
            }
        });
        debounce.setOnFinished(e -> aplicar());

        StringConverter<YearMonth> conv = new StringConverter<>() {
            @Override
            public String toString(YearMonth ym) {
                return ym == null ? "" : Estatisticas.MESES[ym.getMonthValue() - 1] + "/" + ym.getYear();
            }

            @Override
            public YearMonth fromString(String s) {
                return null;
            }
        };
        for (ComboBox<YearMonth> cb : List.of(cbDe, cbAte)) {
            cb.setConverter(conv);
            cb.setPrefWidth(118);
            cb.setVisibleRowCount(12);
            cb.setOnAction(e -> aplicar());
        }
        cbDe.setPromptText("Início");
        cbAte.setPromptText("Fim");
        cbDe.setTooltip(new Tooltip("Mês inicial do período"));
        cbAte.setTooltip(new Tooltip("Mês final do período"));
        Label seta = new Label("→");
        seta.getStyleClass().add("filtro-rotulo");

        Button limpar = new Button("Limpar", Icones.de(Icones.FECHAR, 15));
        limpar.getStyleClass().add("btn-ghost");
        limpar.setOnAction(e -> limpar());
        limpar.setTooltip(new Tooltip("Remover todos os filtros"));

        contagem.getStyleClass().add("contagem-filtro");
        contagem.setMinWidth(Region.USE_PREF_SIZE);
        Region esp = new Region();
        HBox.setHgrow(esp, Priority.ALWAYS);

        Label rotulo = new Label("Filtros", Icones.de(Icones.FILTRO, 16));
        rotulo.getStyleClass().add("filtro-rotulo");
        HBox periodo = new HBox(6, cbDe, seta, cbAte);
        periodo.setAlignment(Pos.CENTER_LEFT);
        HBox linha = new HBox(8, rotulo, mbBioma, mbAno, tfMunicipio, periodo, limpar, esp, contagem);
        linha.setAlignment(Pos.CENTER_LEFT);

        chips.getStyleClass().add("filtros-ativos");
        chips.setHgap(6);
        chips.setVgap(6);
        chips.setVisible(false);
        chips.setManaged(false);
        getChildren().addAll(linha, chips);
        setSpacing(8);
        filtro.addListener((o, a, n) -> atualizarChips());
        atualizarRotulos();
    }

    public ObjectProperty<FiltroGlobal> filtroProperty() {
        return filtro;
    }

    public void setContagem(int n) {
        contagem.setText(Formatos.inteiro(n) + (n == 1 ? " foco" : " focos"));
    }

    /** Preenche as opcoes com os valores presentes na base. */
    public void setOpcoes(BaseDeFocos b) {
        atualizando = true;
        mbBioma.getItems().clear();
        mbAno.getItems().clear();
        checksBioma.clear();
        checksAno.clear();
        cbDe.getItems().clear();
        cbAte.getItems().clear();
        tfMunicipio.clear();
        if (b != null) {
            for (String bioma : b.biomas()) mbBioma.getItems().add(itemCheck(bioma, checksBioma, Graficos.classeBioma(bioma)));
            for (Integer ano : b.anos()) mbAno.getItems().add(itemCheck(String.valueOf(ano), checksAno, null));
            municipios = b.municipios();
            List<YearMonth> meses = new ArrayList<>();
            for (YearMonth ym = YearMonth.from(b.dataInicial()).withMonth(1); !ym.isAfter(YearMonth.from(b.dataFinal()).withMonth(12)); ym = ym.plusMonths(1)) {
                meses.add(ym);
            }
            cbDe.setItems(FXCollections.observableArrayList(meses));
            cbAte.setItems(FXCollections.observableArrayList(meses));
        }
        cbDe.setValue(null);
        cbAte.setValue(null);
        atualizando = false;
        filtro.set(FiltroGlobal.VAZIO);
        atualizarRotulos();
    }

    private CustomMenuItem itemCheck(String texto, List<CheckBox> lista, String classeMarca) {
        CheckBox cb = new CheckBox(texto);
        if (classeMarca != null) {
            Region ponto = new Region();
            ponto.getStyleClass().addAll("ponto-bioma", classeMarca);
            cb.setGraphic(ponto);
        }
        cb.setOnAction(e -> aplicar());
        lista.add(cb);
        CustomMenuItem it = new CustomMenuItem(cb);
        it.setHideOnClick(false);
        return it;
    }

    private void mostrarSugestoes(String texto) {
        sugestoes.getItems().clear();
        if (texto == null || texto.isBlank()) {
            sugestoes.hide();
            return;
        }
        String alvo = Textos.semAcentos(texto.strip());
        List<MenuItem> itens = new ArrayList<>();
        for (String m : municipios) {
            if (Textos.semAcentos(m).contains(alvo)) {
                MenuItem it = new MenuItem(m, Icones.de(Icones.MUNICIPIO, 15));
                it.setOnAction(e -> {
                    atualizando = true;
                    tfMunicipio.setText(m);
                    tfMunicipio.positionCaret(m.length());
                    atualizando = false;
                    municipioExato = true;
                    aplicar();
                });
                itens.add(it);
                if (itens.size() == 8) break;
            }
        }
        if (itens.isEmpty()) {
            MenuItem nada = new MenuItem("Nenhum município encontrado");
            nada.setDisable(true);
            itens.add(nada);
        }
        sugestoes.getItems().setAll(itens);
        if (!sugestoes.isShowing() && tfMunicipio.getScene() != null) sugestoes.show(tfMunicipio, Side.BOTTOM, 0, 4);
    }

    private void aplicar() {
        if (atualizando) return;
        Set<String> biomas = new LinkedHashSet<>();
        for (CheckBox c : checksBioma) if (c.isSelected()) biomas.add(c.getText());
        Set<Integer> anos = new LinkedHashSet<>();
        for (CheckBox c : checksAno) if (c.isSelected()) anos.add(Integer.parseInt(c.getText()));
        filtro.set(new FiltroGlobal(anos, biomas, tfMunicipio.getText(), municipioExato, cbDe.getValue(), cbAte.getValue()));
        atualizarRotulos();
    }

    /** Seleciona biomas e anos programaticamente (demonstracoes e capturas de tela). */
    public void selecionar(Set<String> biomas, Set<Integer> anos) {
        atualizando = true;
        checksBioma.forEach(c -> c.setSelected(biomas.contains(c.getText())));
        checksAno.forEach(c -> c.setSelected(anos.contains(Integer.parseInt(c.getText()))));
        atualizando = false;
        aplicar();
    }

    /** Define o periodo (mes inicial e final); nulos removem o filtro de periodo. */
    public void periodo(YearMonth de, YearMonth ate) {
        atualizando = true;
        cbDe.setValue(de);
        cbAte.setValue(ate);
        atualizando = false;
        aplicar();
    }

    /** Remove todos os filtros. */
    public void limpar() {
        atualizando = true;
        checksBioma.forEach(c -> c.setSelected(false));
        checksAno.forEach(c -> c.setSelected(false));
        tfMunicipio.clear();
        cbDe.setValue(null);
        cbAte.setValue(null);
        municipioExato = false;
        atualizando = false;
        aplicar();
    }

    private void atualizarRotulos() {
        mbBioma.setText(rotuloMultiplo("Bioma", checksBioma));
        mbAno.setText(rotuloMultiplo("Ano", checksAno));
    }

    private static String rotuloMultiplo(String nome, List<CheckBox> checks) {
        List<String> sel = new ArrayList<>();
        for (CheckBox c : checks) if (c.isSelected()) sel.add(c.getText());
        if (sel.isEmpty()) return nome + ": todos";
        if (sel.size() == 1) return nome + ": " + sel.get(0);
        return nome + ": " + sel.size() + " selecionados";
    }

    private void atualizarChips() {
        FiltroGlobal f = filtro.get();
        chips.getChildren().clear();
        if (!f.biomas().isEmpty()) chips.getChildren().add(Chip.removivel("Bioma: " + String.join(", ", f.biomas()),
                () -> {
                    checksBioma.forEach(c -> c.setSelected(false));
                    aplicar();
                }));
        if (!f.anos().isEmpty()) chips.getChildren().add(Chip.removivel("Ano: " + String.join(", ", f.anos().stream().map(String::valueOf).toList()),
                () -> {
                    checksAno.forEach(c -> c.setSelected(false));
                    aplicar();
                }));
        if (f.municipio() != null) chips.getChildren().add(Chip.removivel("Município: " + f.municipio(), () -> {
            atualizando = true;
            tfMunicipio.clear();
            atualizando = false;
            municipioExato = false;
            aplicar();
        }));
        if (f.de() != null || f.ate() != null) {
            String d = f.descricoes().get(f.descricoes().size() - 1);
            chips.getChildren().add(Chip.removivel(d, () -> {
                atualizando = true;
                cbDe.setValue(null);
                cbAte.setValue(null);
                atualizando = false;
                aplicar();
            }));
        }
        boolean tem = !chips.getChildren().isEmpty();
        chips.setVisible(tem);
        chips.setManaged(tem);
    }
}
