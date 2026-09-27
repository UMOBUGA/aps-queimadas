package br.unip.aps.ui;

import atlantafx.base.controls.ToggleSwitch;
import br.unip.aps.config.AppConfig;
import br.unip.aps.ui.componentes.Feedback;
import br.unip.aps.ui.componentes.Icones;
import br.unip.aps.ui.tema.GerenciadorTema;
import br.unip.aps.util.Formatos;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

/** Tela Configuracoes. */
public class ConfiguracoesController implements Pagina.Controlador {
    private final UiContexto ctx;

    @FXML private ToggleButton tgClaro, tgEscuro;
    @FXML private ToggleSwitch swAnimacoes, swSidebar;
    @FXML private Button btnRestaurar;
    @FXML private GridPane gridAtalhos, gridDados;

    public ConfiguracoesController(UiContexto ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        GerenciadorTema t = GerenciadorTema.get();
        ToggleGroup g = new ToggleGroup();
        tgClaro.setToggleGroup(g);
        tgEscuro.setToggleGroup(g);
        tgClaro.setGraphic(Icones.de(Icones.TEMA_CLARO, 15));
        tgEscuro.setGraphic(Icones.de(Icones.TEMA_ESCURO, 15));
        sincronizarTema();
        t.temaProperty().addListener((o, a, n) -> sincronizarTema());
        g.selectedToggleProperty().addListener((o, a, n) -> {
            if (n == null) {
                a.setSelected(true);
                return;
            }
            t.temaProperty().set(n == tgEscuro ? GerenciadorTema.Tema.ESCURO : GerenciadorTema.Tema.CLARO);
        });
        swAnimacoes.selectedProperty().bindBidirectional(t.animacoesProperty());
        swSidebar.selectedProperty().bindBidirectional(t.sidebarRecolhidaProperty());
        btnRestaurar.setGraphic(Icones.de(Icones.RECARREGAR, 16));

        String[][] atalhos = {
                {"Ctrl + 1 … 7", "Ir para Visão geral, Ordenação, Estruturas, Benchmark, Mapa, ML, Qualidade"},
                {"Ctrl + O", "Abrir CSV"}, {"Ctrl + R", "Recarregar dados"}, {"Ctrl + E", "Exportar relatório"},
                {"Ctrl + T", "Alternar tema claro/escuro"}, {"Ctrl + B", "Recolher/expandir menu"}, {"F1", "Sobre"},
                {"Enter", "Ordenar (na tela de Ordenação)"}};
        for (int i = 0; i < atalhos.length; i++) {
            Label k = new Label(atalhos[i][0]);
            k.getStyleClass().add("atalho");
            Label d = new Label(atalhos[i][1]);
            d.getStyleClass().add("t-small");
            gridAtalhos.add(new HBox(k), 0, i);
            gridAtalhos.add(d, 1, i);
        }

        AppConfig c = ctx.sessao().config();
        String[][] dados = {
                {"UF e anos", c.uf() + " · " + c.anos()},
                {"Pasta dos CSVs", c.diretorioDados().toAbsolutePath().toString()},
                {"Pasta de relatórios", c.diretorioRelatorios().toAbsolutePath().toString()},
                {"Aviso O(n²) a partir de", Formatos.inteiro(c.limiteQuadratico()) + " elementos"},
                {"Fonte", c.urlInpe()}};
        for (int i = 0; i < dados.length; i++) {
            Label k = new Label(dados[i][0]);
            k.getStyleClass().add("field-label");
            Label v = new Label(dados[i][1]);
            v.getStyleClass().add("t-small");
            v.setWrapText(true);
            gridDados.add(k, 0, i);
            gridDados.add(v, 1, i);
        }
    }

    private void sincronizarTema() {
        boolean escuro = GerenciadorTema.get().escuro();
        tgEscuro.setSelected(escuro);
        tgClaro.setSelected(!escuro);
    }

    @FXML
    private void restaurar() {
        GerenciadorTema.get().restaurarPadroes();
        Feedback.info("Preferências restauradas", "Tema claro, animações ligadas e menu expandido.");
    }
}
