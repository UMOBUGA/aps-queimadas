package br.unip.aps.ui;

import br.unip.aps.ui.componentes.Graficos;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Region;

import java.util.function.Function;

/** Fabrica de colunas para as tabelas do design system ({@code .data-table}). */
final class Tabelas {
    private Tabelas() { }

    static <S> void preparar(TableView<S> t, String placeholder) {
        if (!t.getStyleClass().contains("data-table")) t.getStyleClass().add("data-table");
        Label l = new Label(placeholder);
        l.getStyleClass().add("t-subtle");
        t.setPlaceholder(l);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_SUBSEQUENT_COLUMNS);
    }

    static <S, T> TableColumn<S, T> coluna(String titulo, Function<S, T> valor, double largura) {
        TableColumn<S, T> c = new TableColumn<>(titulo);
        c.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue() == null ? null : valor.apply(cd.getValue())));
        c.setPrefWidth(largura);
        c.setMinWidth(largura * 0.85);
        c.setSortable(false);
        c.setReorderable(false);
        return c;
    }

    static <S, T extends Number> TableColumn<S, T> numero(String titulo, Function<S, T> valor,
                                                           Function<T, String> formatar, double largura) {
        TableColumn<S, T> c = coluna(titulo, valor, largura);
        c.setMinWidth(largura * 0.9);
        c.getStyleClass().add("numerica");
        c.setCellFactory(col -> new TableCell<>() {
            {
                getStyleClass().add("numerica");
            }

            @Override
            protected void updateItem(T item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio || item == null ? null : formatar.apply(item));
            }
        });
        return c;
    }

    static <S> TableColumn<S, Void> indice() {
        TableColumn<S, Void> c = new TableColumn<>("#");
        c.setSortable(false);
        c.setReorderable(false);
        c.setPrefWidth(64);
        c.setMaxWidth(90);
        c.getStyleClass().add("numerica");
        c.setCellFactory(col -> new TableCell<>() {
            {
                getStyleClass().add("indice");
            }

            @Override
            protected void updateItem(Void item, boolean vazio) {
                super.updateItem(item, vazio);
                setText(vazio ? null : br.unip.aps.util.Formatos.inteiro(getIndex() + 1));
            }
        });
        return c;
    }

    static <S> TableColumn<S, String> bioma(String titulo, Function<S, String> valor, double largura) {
        TableColumn<S, String> c = coluna(titulo, valor, largura);
        c.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean vazio) {
                super.updateItem(item, vazio);
                if (vazio || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Region ponto = new Region();
                ponto.getStyleClass().addAll("ponto-bioma", Graficos.classeBioma(item));
                Label l = new Label(item, ponto);
                l.getStyleClass().addAll("badge", "badge-bioma");
                setGraphic(l);
                setText(null);
            }
        });
        return c;
    }
}
