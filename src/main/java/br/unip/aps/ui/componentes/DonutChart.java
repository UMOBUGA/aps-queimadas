package br.unip.aps.ui.componentes;

import br.unip.aps.util.Formatos;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.List;

/**
 * Grafico de rosca (donut) com o total no centro e legenda lateral com valores e percentuais.
 * Substitui o PieChart padrao (que sobrepunha os rotulos). Cada segmento tem 2 px de folga e
 * tooltip com valor exato e percentual; a cor vem da classe CSS do item (ex.: bioma).
 */
public class DonutChart extends HBox {

    /**
     * Item do grafico.
     *
     * @param nome   rotulo
     * @param valor  quantidade
     * @param classe classe CSS de cor (ex.: {@code bioma-cerrado})
     */
    public record Item(String nome, long valor, String classe) { }

    private static final double RAIO = 74;
    private static final double ESPESSURA = 22;

    private final Group anel = new Group();
    private final Text total = new Text();
    private final Text rotuloTotal = new Text("focos");
    private final VBox legendaLateral = new VBox();

    /** Cria o donut vazio. */
    public DonutChart() {
        setSpacing(Espaco.XL);
        setAlignment(Pos.CENTER_LEFT);
        total.getStyleClass().add("donut-total");
        rotuloTotal.getStyleClass().add("donut-legenda-rotulo");
        VBox centro = new VBox(0, total, rotuloTotal);
        centro.setAlignment(Pos.CENTER);
        centro.setMouseTransparent(true);
        double lado = 2 * (RAIO + ESPESSURA);
        StackPane grafico = new StackPane(anel, centro);
        grafico.setMinSize(lado, lado);
        grafico.setPrefSize(lado, lado);
        grafico.setMaxSize(lado, lado);
        HBox.setHgrow(legendaLateral, Priority.ALWAYS);
        legendaLateral.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(grafico, legendaLateral);
    }

    /**
     * @param itens itens (ja na ordem desejada, normalmente decrescente)
     */
    public void setItens(List<Item> itens) {
        anel.getChildren().clear();
        legendaLateral.getChildren().clear();
        long soma = 0;
        for (Item i : itens) soma += i.valor();
        total.setText(Formatos.inteiro(soma));
        Arc trilho = new Arc(0, 0, RAIO, RAIO, 0, 360);
        trilho.setType(ArcType.OPEN);
        trilho.setStrokeWidth(ESPESSURA);
        trilho.getStyleClass().add("donut-trilho");
        anel.getChildren().add(trilho);
        if (soma == 0) return;

        double inicio = 90;
        double folga = itens.size() > 1 ? 1.2 : 0;
        boolean animar = Graficos.animar();
        for (Item i : itens) {
            double ext = 360.0 * i.valor() / soma;
            double pct = 100.0 * i.valor() / soma;
            Arc a = new Arc(0, 0, RAIO, RAIO, inicio - folga / 2, -(Math.max(0.5, ext - folga)));
            a.setType(ArcType.OPEN);
            a.setStrokeWidth(ESPESSURA);
            a.getStyleClass().addAll("donut-segmento", i.classe());
            String dica = i.nome() + ": " + Formatos.inteiro(i.valor()) + " focos (" + Formatos.decimal(pct, 1) + "%)";
            Graficos.tooltip(a, dica);
            a.setOnMouseEntered(e -> a.setStrokeWidth(ESPESSURA + 5));
            a.setOnMouseExited(e -> a.setStrokeWidth(ESPESSURA));
            if (animar) {
                double alvo = a.getLength();
                a.setLength(0);
                new Timeline(new KeyFrame(Duration.millis(320), new KeyValue(a.lengthProperty(), alvo))).play();
            }
            anel.getChildren().add(a);
            inicio -= ext;

            Region ponto = new Region();
            ponto.getStyleClass().addAll("legenda-marca", i.classe());
            Label nome = new Label(i.nome());
            nome.getStyleClass().add("donut-item-nome");
            Region esp = new Region();
            HBox.setHgrow(esp, Priority.ALWAYS);
            Label valor = new Label(Formatos.inteiro(i.valor()));
            valor.getStyleClass().add("donut-item-valor");
            Label p = new Label(Formatos.decimal(pct, 1) + "%");
            p.getStyleClass().add("donut-item-pct");
            HBox linha = new HBox(10, ponto, nome, esp, valor, p);
            linha.getStyleClass().add("donut-item");
            linha.setAlignment(Pos.CENTER_LEFT);
            Graficos.tooltip(linha, dica);
            legendaLateral.getChildren().add(linha);
        }
    }
}
