package br.unip.aps.ui.componentes;

import br.unip.aps.ui.tema.GerenciadorTema;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/** Seletor visual do mapa-base: miniaturas reais de cada estilo (Esri), escolha persistente e aviso sem internet. */
public final class SeletorMapaBase {
    /** Um estilo de mapa-base; {@code camadas} sao servicos de tiles da Esri sobrepostos na ordem. */
    public record Opcao(String id, String nome, String descricao, List<String> camadas) { }

    private static final String ESRI = "https://server.arcgisonline.com/ArcGIS/rest/services/";
    private static final String TILE = "/MapServer/tile/6/36/23";
    private static final String ROTULOS = "Reference/World_Boundaries_and_Places";

    /** Estilos disponiveis; o Neutro acompanha o tema claro ou escuro. */
    public static final List<Opcao> OPCOES = List.of(
            new Opcao("neutro", "Neutro", "Cinza discreto; os focos se destacam", List.of()),
            new Opcao("ruas", "Ruas e cidades", "Estradas, cidades e fronteiras", List.of("World_Street_Map")),
            new Opcao("relevo", "Relevo", "Serras, rios, vegetação e cidades", List.of("World_Topo_Map")),
            new Opcao("terreno", "Terreno", "Relevo sombreado, com nomes", List.of("World_Terrain_Base", ROTULOS)),
            new Opcao("satelite", "Satélite", "Imagem real vista do espaço", List.of("World_Imagery")),
            new Opcao("hibrido", "Satélite com nomes", "Satélite com cidades e fronteiras", List.of("World_Imagery", ROTULOS)),
            new Opcao("atlas", "Atlas", "Estilo National Geographic", List.of("NatGeo_World_Map")));

    private static final double LARGURA = 132, ALTURA = 84;

    private final ReadOnlyStringWrapper selecionada = new ReadOnlyStringWrapper();
    private final Button botao = new Button("Mapa-base");
    private final VBox painel = new VBox(14);
    private final List<Button> cartoes = new ArrayList<>();
    private final StackPane miniaturaBotao = new StackPane();
    private boolean miniaturasCarregadas;
    private boolean offline;

    public SeletorMapaBase(String inicial) {
        selecionada.set(opcao(inicial).id());
        botao.setId("btnMapaBase");
        botao.getStyleClass().addAll("btn-secondary", "mapa-base-botao");
        botao.setGraphic(miniaturaBotao);
        miniaturaBotao.getStyleClass().add("mapa-base-botao-miniatura");
        botao.setAccessibleText("Escolher o mapa-base");
        botao.setTooltip(new Tooltip("Estilo do mapa de fundo"));
        botao.setMinWidth(Region.USE_PREF_SIZE);
        botao.setOnAction(e -> alternar());

        Label titulo = new Label("Mapa-base");
        titulo.getStyleClass().add("mapa-base-titulo");
        Label sub = new Label("O mundo inteiro em estilos diferentes. Os focos não mudam; só o fundo.");
        sub.getStyleClass().add("t-small");
        sub.setWrapText(true);
        GridPane grade = new GridPane();
        grade.setHgap(12);
        grade.setVgap(12);
        for (int i = 0; i < OPCOES.size(); i++) {
            Button c = cartao(OPCOES.get(i));
            cartoes.add(c);
            grade.add(c, i % 4, i / 4);
        }
        Label fonte = new Label("Mapas da Esri; sem internet, o programa desenha os países (Natural Earth) e os municípios (IBGE).");
        fonte.getStyleClass().add("mapa-base-rodape");
        fonte.setWrapText(true);
        painel.getChildren().addAll(new VBox(2, titulo, sub), grade, fonte);
        painel.getStyleClass().add("mapa-base-painel");
        painel.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        painel.setVisible(false);
        painel.setManaged(false);
        painel.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                fechar();
                botao.requestFocus();
                e.consume();
            }
        });
        GerenciadorTema.get().temaProperty().addListener((o, a, n) -> {
            miniaturasCarregadas = false;
            if (painel.isVisible()) carregarMiniaturas();
            atualizarBotao();
        });
        atualizarBotao();
    }

    private static Opcao opcao(String id) {
        for (Opcao o : OPCOES) if (o.id().equals(id)) return o;
        return OPCOES.get(0);
    }

    private Button cartao(Opcao o) {
        StackPane miniatura = new StackPane();
        miniatura.getStyleClass().add("mapa-base-miniatura");
        miniatura.setPrefSize(LARGURA, ALTURA);
        miniatura.setMinSize(LARGURA, ALTURA);
        miniatura.setMaxSize(LARGURA, ALTURA);
        Rectangle recorte = new Rectangle(LARGURA, ALTURA);
        recorte.setArcWidth(10);
        recorte.setArcHeight(10);
        miniatura.setClip(recorte);
        Label marca = new Label(null, Icones.de(Icones.SUCESSO, 16));
        marca.getStyleClass().add("mapa-base-marca");
        StackPane.setAlignment(marca, Pos.TOP_RIGHT);
        Label nome = new Label(o.nome());
        nome.getStyleClass().add("mapa-base-nome");
        Label desc = new Label(o.descricao());
        desc.getStyleClass().add("mapa-base-descricao");
        desc.setWrapText(true);
        desc.setPrefWidth(LARGURA);
        desc.setMaxWidth(LARGURA);
        desc.setMinHeight(32);
        desc.setPrefHeight(32);
        desc.setMaxHeight(32);
        desc.setAlignment(Pos.TOP_LEFT);
        VBox corpo = new VBox(6, new StackPane(miniatura, marca), nome, desc);
        corpo.setPrefWidth(LARGURA);
        corpo.setMaxWidth(LARGURA);
        Button b = new Button(null, corpo);
        b.setId("base-" + o.id());
        b.getStyleClass().add("mapa-base-cartao");
        b.setUserData(o);
        b.setAccessibleText("Mapa-base " + o.nome() + ": " + o.descricao());
        b.setOnAction(e -> escolher(o.id()));
        b.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                escolher(o.id());
                e.consume();
            }
        });
        b.setMinHeight(Region.USE_PREF_SIZE);
        b.setMaxHeight(Region.USE_PREF_SIZE);
        return b;
    }

    private List<String> camadasDaMiniatura(Opcao o) {
        if (!o.camadas().isEmpty()) return o.camadas();
        return List.of(GerenciadorTema.get().escuro() ? "Canvas/World_Dark_Gray_Base" : "Canvas/World_Light_Gray_Base");
    }

    private void carregarMiniaturas() {
        if (miniaturasCarregadas) return;
        miniaturasCarregadas = true;
        for (Button b : cartoes) preencher(miniaturaDe(b), camadasDaMiniatura((Opcao) b.getUserData()), LARGURA, ALTURA);
    }

    private static StackPane miniaturaDe(Button b) {
        return (StackPane) ((StackPane) ((VBox) b.getGraphic()).getChildren().get(0)).getChildren().get(0);
    }

    private static void preencher(StackPane alvo, List<String> camadas, double largura, double altura) {
        alvo.getChildren().clear();
        for (String servico : camadas) {
            Image img = new Image(ESRI + servico + TILE, true);
            ImageView v = new ImageView(img);
            v.setViewport(new Rectangle2D(32, 64, 192, 192 * altura / largura));
            v.setFitWidth(largura);
            v.setFitHeight(altura);
            v.setSmooth(true);
            v.setOpacity(0);
            img.progressProperty().addListener((o, a, p) -> {
                if (p.doubleValue() >= 1 && !img.isError()) v.setOpacity(1);
            });
            alvo.getChildren().add(v);
        }
    }

    private void atualizarBotao() {
        preencher(miniaturaBotao, camadasDaMiniatura(opcao(selecionada.get())), 26, 18);
        Rectangle r = new Rectangle(26, 18);
        r.setArcWidth(6);
        r.setArcHeight(6);
        miniaturaBotao.setClip(r);
        miniaturaBotao.setMinSize(26, 18);
        miniaturaBotao.setMaxSize(26, 18);
        for (Button c : cartoes) {
            boolean atual = ((Opcao) c.getUserData()).id().equals(selecionada.get());
            c.getStyleClass().remove("selecionado");
            if (atual) c.getStyleClass().add("selecionado");
        }
    }

    /** Escolhe um estilo (ignora os que exigem internet quando o mapa esta offline). */
    public void escolher(String id) {
        Opcao o = opcao(id);
        if (offline && !o.camadas().isEmpty()) return;
        selecionada.set(o.id());
        atualizarBotao();
        fechar();
    }

    /** Sem internet so o Neutro fica disponivel (o mapa usa a malha local). */
    public void setOffline(boolean offline) {
        this.offline = offline;
        for (Button c : cartoes) {
            boolean precisaRede = !((Opcao) c.getUserData()).camadas().isEmpty();
            c.setDisable(offline && precisaRede);
            c.setTooltip(offline && precisaRede ? new Tooltip("Requer internet") : null);
        }
    }

    private void alternar() {
        if (aberto()) fechar();
        else abrir();
    }

    public boolean aberto() {
        return painel.isVisible();
    }

    public void abrir() {
        carregarMiniaturas();
        atualizarBotao();
        painel.setManaged(true);
        painel.setVisible(true);
        if (GerenciadorTema.get().animacoesProperty().get()) {
            painel.setOpacity(0);
            painel.setTranslateY(-6);
            new Timeline(new KeyFrame(Duration.millis(180),
                    new KeyValue(painel.opacityProperty(), 1, Movimento.SAIDA),
                    new KeyValue(painel.translateYProperty(), 0, Movimento.SAIDA))).play();
        } else {
            painel.setOpacity(1);
            painel.setTranslateY(0);
        }
        for (Button c : cartoes) {
            if (c.getStyleClass().contains("selecionado")) c.requestFocus();
        }
    }

    public void fechar() {
        painel.setVisible(false);
        painel.setManaged(false);
    }

    /** Fecha o painel quando o clique acontece fora dele e do botao. */
    public void fecharAoClicarFora(Node raiz) {
        raiz.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (!aberto()) return;
            Node alvo = e.getTarget() instanceof Node n ? n : null;
            for (Node x = alvo; x != null; x = x.getParent()) {
                if (x == painel || x == botao) return;
            }
            fechar();
        });
    }

    public Button botao() {
        return botao;
    }

    public VBox painel() {
        return painel;
    }

    public ReadOnlyStringProperty selecionadaProperty() {
        return selecionada.getReadOnlyProperty();
    }
}
