package br.unip.aps.ui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.function.Function;

/**
 * Atalhos para montar colunas de {@link TableView} por codigo.
 */
final class Tabelas {

    private Tabelas() { }

    /**
     * @param titulo  cabecalho
     * @param valor   extrator do valor exibido
     * @param largura largura preferida
     * @param <S>     tipo da linha
     * @param <T>     tipo da celula
     * @return coluna configurada
     */
    static <S, T> TableColumn<S, T> coluna(String titulo, Function<S, T> valor, double largura) {
        TableColumn<S, T> c = new TableColumn<>(titulo);
        c.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue() == null ? null : valor.apply(cd.getValue())));
        c.setPrefWidth(largura);
        // A ordenacao por clique no cabecalho do TableView usa FXCollections.sort (Collections.sort) internamente;
        // fica desabilitada para que toda ordenacao exibida venha dos algoritmos do projeto.
        c.setSortable(false);
        return c;
    }

    /**
     * Coluna numerica alinhada a direita com formatacao propria (ordenavel pelo valor numerico).
     *
     * @param titulo    cabecalho
     * @param valor     extrator
     * @param formatar  formatacao do texto
     * @param largura   largura
     * @param <S>       tipo da linha
     * @param <T>       tipo numerico
     * @return coluna
     */
    static <S, T extends Number> TableColumn<S, T> numero(String titulo, Function<S, T> valor,
                                                           Function<T, String> formatar, double largura) {
        TableColumn<S, T> c = coluna(titulo, valor, largura);
        c.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio || item == null ? null : formatar.apply(item));
            }
        });
        c.setStyle("-fx-alignment: CENTER-RIGHT;");
        return c;
    }

    /**
     * Coluna com a posicao (1, 2, 3...) da linha na tabela.
     *
     * @param <S> tipo da linha
     * @return coluna de indice
     */
    static <S> TableColumn<S, Void> indice() {
        TableColumn<S, Void> c = new TableColumn<>("#");
        c.setSortable(false);
        c.setPrefWidth(64);
        c.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio ? null : String.valueOf(getIndex() + 1));
            }
        });
        c.setStyle("-fx-alignment: CENTER-RIGHT;");
        return c;
    }
}
