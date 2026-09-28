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

    @FXML private ToggleButton tgClaro, tgEscuro, tgTexto100, tgTexto115, tgTexto130;
    @FXML private ToggleSwitch swAnimacoes, swSidebar, swCompacto, swDaltonico, swContraste;
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
        swCompacto.selectedProperty().bindBidirectional(t.compactoProperty());
        swDaltonico.selectedProperty().bindBidirectional(t.daltonicoProperty());
        swContraste.selectedProperty().bindBidirectional(t.altoContrasteProperty());
        ToggleGroup texto = new ToggleGroup();
        for (ToggleButton b : new ToggleButton[]{tgTexto100, tgTexto115, tgTexto130}) b.setToggleGroup(texto);
        sincronizarTexto();
        t.escalaTextoProperty().addListener((o, a, n) -> sincronizarTexto());
        texto.selectedToggleProperty().addListener((o, a, n) -> {
            if (n == null) {
                a.setSelected(true);
                return;
            }
            t.escalaTextoProperty().set(n == tgTexto130 ? 130 : n == tgTexto115 ? 115 : 100);
        });
        btnRestaurar.setGraphic(Icones.de(Icones.RECARREGAR, 16));

        String[][] atalhos = {
                {"Ctrl + K", "Buscar telas, ações, municípios e algoritmos"},
                {"Ctrl + 1 … 7", "Ir para Visão geral, Ordenação, Estruturas, Benchmark, Mapa, ML, Qualidade"},
                {"F5", "Modo apresentação (setas navegam, Esc sai)"},
                {"Ctrl + O", "Abrir CSV"}, {"Ctrl + R", "Recarregar dados"}, {"Ctrl + E", "Exportar relatório"},
                {"Ctrl + T", "Alternar tema claro/escuro"}, {"Ctrl + B", "Recolher/expandir menu"}, {"F1", "Sobre"},
                {"Enter", "Ordenar (na tela de Ordenação)"}};
        for (int i = 0; i < atalhos.length; i++) {
            Label k = new Label(atalhos[i][0]);
            k.getStyleClass().add("atalho");
            k.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
            Label d = new Label(atalhos[i][1]);
            d.getStyleClass().add("t-small");
            gridAtalhos.add(new HBox(k), 0, i);
            gridAtalhos.add(d, 1, i);
        }

        AppConfig c = ctx.sessao().config();
        String[][] dados = {
                {"UF e anos", c.uf() + " · " + c.anos()},
                {"Pasta dos CSVs", relativo(c.diretorioDados())},
                {"Pasta de relatórios", relativo(c.diretorioRelatorios())},
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

    private void sincronizarTexto() {
        int e = GerenciadorTema.get().escalaTextoProperty().get();
        (e == 130 ? tgTexto130 : e == 115 ? tgTexto115 : tgTexto100).setSelected(true);
    }

    private void sincronizarTema() {
        boolean escuro = GerenciadorTema.get().escuro();
        tgEscuro.setSelected(escuro);
        tgClaro.setSelected(!escuro);
    }

    @FXML
    private void restaurar() {
        GerenciadorTema.get().restaurarPadroes();
        Feedback.info("Preferências restauradas", "Tema escuro, animações ligadas, menu expandido e acessibilidade no padrão.");
    }

    private static String relativo(java.nio.file.Path p) {
        java.nio.file.Path base = java.nio.file.Path.of("").toAbsolutePath();
        java.nio.file.Path abs = p.toAbsolutePath().normalize();
        return abs.startsWith(base) ? base.relativize(abs).toString().replace('\\', '/') + "/" : abs.toString();
    }
}
