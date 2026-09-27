package br.unip.aps.ui.componentes;

import br.unip.aps.ui.tema.GerenciadorTema;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;

import java.util.List;
import java.util.Optional;

/** Dialogos modais com a identidade do sistema (nada de {@code Alert} padrao cru). */
public final class Dialogos {
    private Dialogos() { }

    private enum Tipo {
        ERRO("erro", Icones.ERRO), ALERTA("alerta", Icones.ALERTA), INFO("info", Icones.INFO), PERGUNTA("pergunta", Icones.PERGUNTA);

        final String classe;
        final String icone;

        Tipo(String classe, String icone) {
            this.classe = classe;
            this.icone = icone;
        }
    }

    private static Alert criar(Window dono, Tipo tipo, String titulo, String texto, ButtonType... botoes) {
        Alert a = new Alert(Alert.AlertType.NONE, texto, botoes);
        if (dono != null) a.initOwner(dono);
        a.setTitle("APS Queimadas");
        a.setHeaderText(titulo);
        StackPane icone = new StackPane(Icones.de(tipo.icone, 22));
        icone.getStyleClass().addAll("dialog-icone", tipo.classe);
        a.setGraphic(icone);
        DialogPane p = a.getDialogPane();
        GerenciadorTema.get().aplicar(p);
        p.getStyleClass().add("app-dialog");
        p.setMinWidth(480);
        p.setPrefWidth(520);
        if (p.getContent() == null) {
            Label l = new Label(texto);
            l.setWrapText(true);
            l.getStyleClass().add("t-muted");
            p.setContent(l);
        }
        for (ButtonType b : a.getButtonTypes()) {
            var n = p.lookupButton(b);
            if (n == null) continue;
            boolean primario = b.getButtonData() == ButtonBar.ButtonData.OK_DONE || b.getButtonData() == ButtonBar.ButtonData.YES;
            n.getStyleClass().add(primario ? "btn-primary" : "btn-secondary");
        }
        return a;
    }

    public static void info(Window dono, String titulo, String texto) {
        criar(dono, Tipo.INFO, titulo, texto, new ButtonType("Entendi", ButtonBar.ButtonData.OK_DONE)).show();
    }

    public static void aviso(Window dono, String titulo, String texto) {
        criar(dono, Tipo.ALERTA, titulo, texto, new ButtonType("Entendi", ButtonBar.ButtonData.OK_DONE)).show();
    }

    /** Erro com detalhes tecnicos opcionais (expansiveis). */
    public static void erro(Window dono, String titulo, String texto, String detalhes) {
        Alert a = criar(dono, Tipo.ERRO, titulo, texto, new ButtonType("Fechar", ButtonBar.ButtonData.OK_DONE));
        if (detalhes != null) {
            TextArea ta = new TextArea(detalhes);
            ta.setEditable(false);
            ta.getStyleClass().add("t-mono");
            ta.setPrefRowCount(12);
            a.getDialogPane().setExpandableContent(ta);
        }
        a.show();
    }

    public static boolean confirmar(Window dono, String titulo, String texto, String confirmar) {
        ButtonType sim = new ButtonType(confirmar, ButtonBar.ButtonData.YES);
        ButtonType nao = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        Optional<ButtonType> r = criar(dono, Tipo.PERGUNTA, titulo, texto, sim, nao).showAndWait();
        return r.isPresent() && r.get() == sim;
    }

    /** Pergunta com varias opcoes. */
    public static int escolher(Window dono, String titulo, String texto, String... opcoes) {
        ButtonType[] bs = new ButtonType[opcoes.length + 1];
        for (int i = 0; i < opcoes.length; i++) {
            bs[i] = new ButtonType(opcoes[i], i == 0 ? ButtonBar.ButtonData.OK_DONE : ButtonBar.ButtonData.OTHER);
        }
        bs[opcoes.length] = new ButtonType("Agora não", ButtonBar.ButtonData.CANCEL_CLOSE);
        Optional<ButtonType> r = criar(dono, Tipo.ALERTA, titulo, texto, bs).showAndWait();
        if (r.isEmpty()) return -1;
        for (int i = 0; i < opcoes.length; i++) if (r.get() == bs[i]) return i;
        return -1;
    }

    /** Mostra os dados de um grafico em tabela. */
    public static void tabela(Window dono, String titulo, String[] cabecalho, List<String[]> linhas) {
        Alert a = criar(dono, Tipo.INFO, titulo, null, new ButtonType("Fechar", ButtonBar.ButtonData.OK_DONE));
        TableView<String[]> t = new TableView<>(FXCollections.observableArrayList(linhas));
        t.getStyleClass().add("data-table");
        for (int i = 0; i < cabecalho.length; i++) {
            final int c = i;
            TableColumn<String[], String> col = new TableColumn<>(cabecalho[i]);
            col.setSortable(false);
            col.setCellValueFactory(cd -> new ReadOnlyStringWrapper(c < cd.getValue().length ? cd.getValue()[c] : ""));
            col.setPrefWidth(i == 0 ? 220 : 130);
            if (i > 0) col.getStyleClass().add("numerica");
            t.getColumns().add(col);
        }
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        t.setPrefHeight(Math.min(460, 60 + linhas.size() * 34));
        a.getDialogPane().setContent(t);
        a.getDialogPane().setPrefWidth(620);
        a.show();
    }
}
