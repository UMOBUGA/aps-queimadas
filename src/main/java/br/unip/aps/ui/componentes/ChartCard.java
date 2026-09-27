package br.unip.aps.ui.componentes;

import br.unip.aps.ui.tema.GerenciadorTema;
import javafx.animation.FadeTransition;
import javafx.animation.Animation;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

/**
 * Card de grafico: titulo, subtitulo explicativo, legenda em linha, chip de destaque opcional e
 * menu de acoes (exportar PNG, ver dados em tabela). Tem tres estados: conteudo, carregando
 * (skeleton) e vazio.
 */
public class ChartCard extends VBox {

    /** Estado visual do card. */
    public enum Estado { CONTEUDO, CARREGANDO, VAZIO }

    private final Label titulo = new Label();
    private final Label subtitulo = new Label();
    private final javafx.scene.layout.FlowPane legenda = new javafx.scene.layout.FlowPane(14, 6);
    private final HBox extras = new HBox(8);
    private final StackPane corpo = new StackPane();
    private final VBox skeleton = new VBox(10);
    private final EmptyState vazio;
    private Node conteudo;
    private String[] cabecalho;
    private Supplier<List<String[]>> dados;
    private Animation pulso;
    private MenuItem itemTabela;

    /**
     * @param titulo    titulo
     * @param subtitulo explicacao curta
     */
    public ChartCard(@javafx.beans.NamedArg("titulo") String titulo, @javafx.beans.NamedArg("subtitulo") String subtitulo) {
        getStyleClass().add("chart-card");
        this.titulo.setText(titulo);
        this.titulo.getStyleClass().add("card-title");
        this.subtitulo.setText(subtitulo);
        this.subtitulo.getStyleClass().add("card-subtitle");
        this.subtitulo.setWrapText(true);
        this.subtitulo.setMinHeight(Region.USE_PREF_SIZE);
        legenda.getStyleClass().add("legenda");

        VBox textos = new VBox(2, this.titulo, this.subtitulo);
        HBox.setHgrow(textos, Priority.ALWAYS);
        textos.setMinWidth(0);

        MenuItem png = new MenuItem("Exportar como PNG", Icones.de(Icones.IMAGEM, 16));
        png.setOnAction(e -> exportarPng());
        MenuItem tabela = new MenuItem("Ver dados em tabela", Icones.de(Icones.TABELA, 16));
        tabela.setOnAction(e -> verDados());
        MenuButton acoes = new MenuButton(null, Icones.de(Icones.MAIS_ACOES, 18), png, tabela);
        acoes.getStyleClass().add("btn-icon");
        acoes.setTooltip(new Tooltip("Ações do gráfico"));
        acoes.setAccessibleText("Ações do gráfico " + titulo);
        tabela.setDisable(true);
        this.itemTabela = tabela;

        extras.setAlignment(Pos.CENTER_RIGHT);
        extras.setMinWidth(Region.USE_PREF_SIZE);
        HBox header = new HBox(10, textos, extras, acoes);
        header.getStyleClass().add("chart-card-header");
        header.setAlignment(Pos.TOP_LEFT);

        montarSkeleton();
        vazio = new EmptyState(Icones.VAZIO_FILTRO, "Sem dados para os filtros",
                "Nenhum foco atende aos filtros atuais. Remova algum filtro na barra acima.", true);
        vazio.setVisible(false);
        corpo.getChildren().addAll(skeleton, vazio);
        VBox.setVgrow(corpo, Priority.ALWAYS);
        corpo.setMinHeight(120);

        getChildren().addAll(header, legenda, corpo);
        legenda.setVisible(false);
        legenda.setManaged(false);
    }

    private void montarSkeleton() {
        skeleton.setFillWidth(true);
        for (double f : new double[]{0.9, 0.7, 0.8, 0.55, 0.75}) {
            Region r = new Region();
            r.getStyleClass().add("skeleton");
            r.setPrefHeight(18);
            r.maxWidthProperty().bind(corpo.widthProperty().multiply(f));
            skeleton.getChildren().add(r);
        }
        skeleton.setAlignment(Pos.CENTER_LEFT);
        skeleton.setVisible(false);
    }

    /**
     * @param n conteudo (grafico ou componente)
     * @return este card
     */
    public ChartCard conteudo(Node n) {
        if (conteudo != null) corpo.getChildren().remove(conteudo);
        conteudo = n;
        corpo.getChildren().add(0, n);
        estado(Estado.CONTEUDO);
        return this;
    }

    /** @return conteudo atual */
    public Node getConteudo() {
        return conteudo;
    }

    /**
     * @param e novo estado
     */
    public void estado(Estado e) {
        if (conteudo != null) conteudo.setVisible(e == Estado.CONTEUDO);
        skeleton.setVisible(e == Estado.CARREGANDO);
        vazio.setVisible(e == Estado.VAZIO);
        if (pulso != null) pulso.stop();
        if (e == Estado.CARREGANDO && GerenciadorTema.get().animacoesProperty().get()) {
            FadeTransition f = new FadeTransition(Duration.millis(700), skeleton);
            f.setFromValue(1);
            f.setToValue(0.45);
            f.setAutoReverse(true);
            f.setCycleCount(Animation.INDEFINITE);
            pulso = f;
            f.play();
        } else {
            skeleton.setOpacity(1);
        }
    }

    /** Remove os itens da legenda. */
    public void limparLegenda() {
        legenda.getChildren().clear();
        legenda.setVisible(false);
        legenda.setManaged(false);
    }

    /**
     * Adiciona item de legenda (marca colorida + texto). A legenda fica sempre visivel quando ha
     * duas ou mais series, para que a identidade nunca dependa so da cor.
     *
     * @param texto        rotulo
     * @param classesMarca classes CSS da marca (ex.: {@code ano-recente}, {@code linha})
     */
    public void adicionarLegenda(String texto, String... classesMarca) {
        Region marca = new Region();
        marca.getStyleClass().add("legenda-marca");
        marca.getStyleClass().addAll(classesMarca);
        Label l = new Label(texto, marca);
        l.getStyleClass().add("legenda-item");
        legenda.getChildren().add(l);
        legenda.setVisible(true);
        legenda.setManaged(true);
    }

    /**
     * @param n no exibido a direita do titulo (ex.: chip "Pico: ago/2024 — 3.612")
     */
    public void setExtra(Node... n) {
        extras.getChildren().setAll(n);
    }

    /**
     * @param t novo subtitulo
     */
    public void setSubtitulo(String t) {
        subtitulo.setText(t);
    }

    /**
     * Define os dados exibidos em "Ver dados em tabela".
     *
     * @param cabecalho colunas
     * @param dados     fornecedor das linhas
     */
    public void setDados(String[] cabecalho, Supplier<List<String[]>> dados) {
        this.cabecalho = cabecalho;
        this.dados = dados;
        itemTabela.setDisable(dados == null);
    }

    /** @return titulo do card */
    public String getTitulo() {
        return titulo.getText();
    }

    private void verDados() {
        if (dados == null) return;
        Dialogos.tabela(getScene() == null ? null : getScene().getWindow(), titulo.getText(), cabecalho, dados.get());
    }

    private void exportarPng() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar gráfico como PNG");
        fc.setInitialFileName(titulo.getText().replaceAll("[^\\p{L}\\p{N}]+", "-").toLowerCase() + ".png");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagem PNG", "*.png"));
        File f = fc.showSaveDialog(getScene().getWindow());
        if (f == null) return;
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        WritableImage img = snapshot(sp, null);
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", f);
            Feedback.sucesso("Gráfico exportado", f.getName());
        } catch (IOException ex) {
            Feedback.erro("Não foi possível exportar", ex.getMessage());
        }
    }
}
