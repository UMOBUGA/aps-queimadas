package br.unip.aps.ui.componentes;

import br.unip.aps.sorting.Ordenacoes;
import br.unip.aps.util.Textos;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Paleta de comandos (Ctrl+K): busca aproximada por telas, acoes, municipios e algoritmos. */
public class PaletaComandos extends StackPane {
    /** Um item executavel da paleta. */
    public record Comando(String titulo, String detalhe, String categoria, String icone, Runnable acao) { }

    private record Pontuado(Comando comando, int pontos) { }

    private final Supplier<List<Comando>> fonte;
    private final TextField busca = new TextField();
    private final ListView<Comando> lista = new ListView<>();
    private final Label vazio = new Label("Nada encontrado. Tente o nome de uma tela, de um município ou de um algoritmo.");
    private List<Comando> todos = List.of();

    public PaletaComandos(Supplier<List<Comando>> fonte) {
        this.fonte = fonte;
        getStyleClass().add("paleta-fundo");
        setVisible(false);
        setManaged(false);
        busca.setPromptText("Buscar telas, ações, municípios, algoritmos…");
        busca.getStyleClass().add("paleta-busca");
        busca.setAccessibleText("Buscar comando");
        HBox campo = new HBox(10, Icones.de(Icones.BUSCA, 18), busca);
        campo.setAlignment(Pos.CENTER_LEFT);
        campo.getStyleClass().add("paleta-campo");
        HBox.setHgrow(busca, Priority.ALWAYS);
        lista.getStyleClass().add("paleta-lista");
        lista.setCellFactory(v -> new Celula());
        lista.setPrefHeight(380);
        lista.setFocusTraversable(false);
        vazio.getStyleClass().add("t-small");
        vazio.setWrapText(true);
        vazio.setPadding(new Insets(16));
        Label dica = new Label("↑ ↓ para navegar · Enter para executar · Esc para fechar");
        dica.getStyleClass().add("paleta-dica");
        VBox painel = new VBox(0, campo, lista, dica);
        painel.getStyleClass().add("paleta-painel");
        painel.setMaxWidth(640);
        painel.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(painel, Pos.TOP_CENTER);
        StackPane.setMargin(painel, new Insets(96, 24, 24, 24));
        getChildren().add(painel);
        setOnMouseClicked(e -> {
            if (e.getTarget() == this) fechar();
        });
        busca.textProperty().addListener((o, a, n) -> filtrar(n));
        busca.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) fechar();
            else if (e.getCode() == KeyCode.DOWN) mover(1);
            else if (e.getCode() == KeyCode.UP) mover(-1);
            else if (e.getCode() == KeyCode.ENTER) executar(lista.getSelectionModel().getSelectedItem());
            else return;
            e.consume();
        });
        lista.setOnMouseClicked(e -> {
            if (e.getClickCount() >= 1) executar(lista.getSelectionModel().getSelectedItem());
        });
    }

    public void abrir() {
        todos = fonte.get();
        busca.clear();
        filtrar("");
        setVisible(true);
        setManaged(true);
        toFront();
        Platform.runLater(busca::requestFocus);
    }

    public void fechar() {
        setVisible(false);
        setManaged(false);
    }

    public boolean aberta() {
        return isVisible();
    }

    private void mover(int d) {
        int n = lista.getItems().size();
        if (n == 0) return;
        int i = Math.floorMod(lista.getSelectionModel().getSelectedIndex() + d, n);
        lista.getSelectionModel().select(i);
        lista.scrollTo(Math.max(0, i - 3));
    }

    private void executar(Comando c) {
        if (c == null) return;
        fechar();
        Platform.runLater(c.acao());
    }

    private void filtrar(String texto) {
        String q = Textos.semAcentos(texto == null ? "" : texto.strip());
        List<Pontuado> r = new ArrayList<>();
        for (Comando c : todos) {
            int p = q.isEmpty() ? (c.categoria().equals("Município") || c.categoria().equals("Ficha") ? -1 : 0) : pontuar(q, Textos.semAcentos(c.titulo() + " " + c.categoria()));
            if (p >= 0) r.add(new Pontuado(c, p));
        }
        Ordenacoes.ordenar(r, (a, b) -> Integer.compare(b.pontos(), a.pontos()));
        List<Comando> itens = new ArrayList<>();
        for (int i = 0; i < Math.min(40, r.size()); i++) itens.add(r.get(i).comando());
        lista.getItems().setAll(itens);
        if (!itens.isEmpty()) lista.getSelectionModel().select(0);
        lista.setPlaceholder(vazio);
    }

    /** Pontua a busca como subsequencia: bonus para letras consecutivas e para inicio de palavra; -1 se nao casa. */
    static int pontuar(String consulta, String alvo) {
        if (alvo.contains(consulta)) return 1000 - alvo.indexOf(consulta) + (alvo.startsWith(consulta) ? 200 : 0);
        int pontos = 0, j = 0, seguidas = 0;
        for (int i = 0; i < consulta.length(); i++) {
            char c = consulta.charAt(i);
            if (c == ' ') continue;
            int achou = alvo.indexOf(c, j);
            if (achou < 0) return -1;
            seguidas = achou == j ? seguidas + 1 : 0;
            pontos += 10 + 5 * seguidas + (achou == 0 || alvo.charAt(achou - 1) == ' ' ? 15 : 0);
            j = achou + 1;
        }
        return pontos;
    }

    private static final class Celula extends ListCell<Comando> {
        @Override
        protected void updateItem(Comando c, boolean vazio) {
            super.updateItem(c, vazio);
            if (vazio || c == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label t = new Label(c.titulo());
            t.getStyleClass().add("paleta-titulo");
            Label d = new Label(c.detalhe());
            d.getStyleClass().add("paleta-detalhe");
            VBox textos = new VBox(1, t, d);
            HBox.setHgrow(textos, Priority.ALWAYS);
            Label cat = new Label(c.categoria());
            cat.getStyleClass().add("paleta-categoria");
            HBox linha = new HBox(12, Icones.de(c.icone(), 18), textos, cat);
            linha.setAlignment(Pos.CENTER_LEFT);
            setGraphic(linha);
            setText(null);
            setAccessibleText(c.categoria() + ": " + c.titulo());
        }
    }
}
