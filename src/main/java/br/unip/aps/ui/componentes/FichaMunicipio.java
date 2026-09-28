package br.unip.aps.ui.componentes;

import br.unip.aps.analysis.Contagem;
import br.unip.aps.analysis.Estatisticas;
import br.unip.aps.geo.PerfilMunicipio;
import br.unip.aps.util.Formatos;
import br.unip.aps.util.Textos;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Painel lateral com a ficha de um municipio: total, ranking, focos por mes, bioma e vizinhos com focos. */
public class FichaMunicipio extends VBox {
    private static final int MAX_VIZINHOS = 6;

    private final Label nome = new Label();
    private final Label total = new Label();
    private final Label totalTexto = new Label();
    private final Label ranking = new Label();
    private final GridPane numeros = new GridPane();
    private final FaixaTermica faixa = new FaixaTermica();
    private final Label pico = new Label();
    private final BarrasHorizontais vizinhos = new BarrasHorizontais();
    private final Label vizinhosTexto = new Label();
    private final Button verNoMapa = new Button("Ver no mapa");
    private final TextField campoComparar = new TextField();
    private final ListView<String> sugestoes = new ListView<>();
    private List<String> municipios = List.of();
    private PerfilMunicipio perfil;
    private Consumer<PerfilMunicipio> aoVerNoMapa;
    private BiConsumer<PerfilMunicipio, String> aoComparar;

    public FichaMunicipio() {
        getStyleClass().add("ficha-painel");
        setId("fichaMunicipio");
        setVisible(false);
        setManaged(false);
        setMaxWidth(Region.USE_PREF_SIZE);
        setPrefWidth(392);

        Label sobre = new Label("FICHA DO MUNICÍPIO");
        sobre.getStyleClass().add("t-overline");
        nome.getStyleClass().add("ficha-nome");
        nome.setWrapText(true);
        Button fechar = new Button(null, Icones.de(Icones.FECHAR, 16));
        fechar.getStyleClass().add("btn-icon");
        fechar.setId("btnFecharFicha");
        fechar.setAccessibleText("Fechar a ficha");
        fechar.setOnAction(e -> fechar());
        VBox titulos = new VBox(2, sobre, nome);
        HBox.setHgrow(titulos, Priority.ALWAYS);
        HBox cabecalho = new HBox(8, titulos, fechar);
        cabecalho.setAlignment(Pos.TOP_LEFT);

        total.getStyleClass().add("ficha-numero");
        totalTexto.getStyleClass().add("ficha-numero-texto");
        ranking.getStyleClass().add("ficha-texto");
        ranking.setWrapText(true);
        numeros.getStyleClass().add("ficha-numeros");
        numeros.setHgap(18);
        numeros.setVgap(10);

        faixa.getStyleClass().add("ficha-faixa");
        faixa.semLegenda();
        pico.getStyleClass().add("t-small");
        pico.setWrapText(true);
        vizinhosTexto.getStyleClass().add("t-small");
        vizinhosTexto.setWrapText(true);

        verNoMapa.getStyleClass().add("btn-primary");
        verNoMapa.setGraphic(Icones.de(Icones.ALVO, 15));
        verNoMapa.setId("btnVerNoMapa");
        verNoMapa.setOnAction(e -> {
            if (aoVerNoMapa != null && perfil != null) aoVerNoMapa.accept(perfil);
        });
        Layout.naoEncolher(verNoMapa);

        campoComparar.setId("campoComparar");
        campoComparar.setPromptText("Digite outro município…");
        campoComparar.setAccessibleText("Comparar com outro município");
        campoComparar.textProperty().addListener((o, a, n) -> filtrarSugestoes(n));
        campoComparar.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && !sugestoes.getItems().isEmpty()) {
                String s = sugestoes.getSelectionModel().getSelectedItem();
                comparar(s == null ? sugestoes.getItems().get(0) : s);
                e.consume();
            } else if (e.getCode() == KeyCode.DOWN && !sugestoes.getItems().isEmpty()) {
                sugestoes.requestFocus();
                sugestoes.getSelectionModel().selectFirst();
                e.consume();
            }
        });
        sugestoes.setId("sugestoesComparar");
        sugestoes.getStyleClass().add("ficha-sugestoes");
        sugestoes.setFixedCellSize(30);
        sugestoes.setVisible(false);
        sugestoes.setManaged(false);
        sugestoes.setOnMouseClicked(e -> comparar(sugestoes.getSelectionModel().getSelectedItem()));
        sugestoes.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                comparar(sugestoes.getSelectionModel().getSelectedItem());
                e.consume();
            }
        });
        HBox linhaComparar = new HBox(8, Icones.de(Icones.COMPARAR, 16), campoComparar);
        linhaComparar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(campoComparar, Priority.ALWAYS);

        VBox conteudo = new VBox(16,
                new VBox(0, total, totalTexto), ranking, numeros,
                secao("Focos por mês", "Clique num mês para vê-lo no mapa."), faixa, pico,
                secao("Vizinhos com focos", "Municípios que fazem fronteira, pela malha do IBGE."), vizinhos, vizinhosTexto,
                verNoMapa, secao("Comparar", "Coloque este município lado a lado com outro."), linhaComparar, sugestoes);
        conteudo.getStyleClass().add("ficha-conteudo");
        ScrollPane rolagem = new ScrollPane(conteudo);
        rolagem.setFitToWidth(true);
        rolagem.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rolagem.getStyleClass().add("ficha-rolagem");
        VBox.setVgrow(rolagem, Priority.ALWAYS);
        getChildren().addAll(cabecalho, rolagem);
        setSpacing(12);
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                fechar();
                e.consume();
            }
        });
    }

    private static VBox secao(String titulo, String sub) {
        Label t = new Label(titulo);
        t.getStyleClass().add("ficha-secao");
        Label s = new Label(sub);
        s.getStyleClass().add("t-small");
        s.setWrapText(true);
        VBox v = new VBox(1, t, s);
        v.getStyleClass().add("ficha-secao-cabecalho");
        return v;
    }

    /** Preenche e abre a ficha, deslizando da direita. */
    public void mostrar(PerfilMunicipio p) {
        perfil = p;
        nome.setText(p.nome());
        total.setText(Formatos.inteiro(p.total()));
        String periodo = p.porAno().isEmpty() ? "" : p.porAno().size() == 1 ? " em " + p.porAno().get(0).chave()
                : " de " + p.porAno().get(0).chave() + " a " + p.porAno().get(p.porAno().size() - 1).chave();
        totalTexto.setText((p.total() == 1 ? "foco" : "focos") + periodo);
        ranking.setText(p.total() == 0 ? "Nenhum foco registrado no período."
                : p.posicao() + "º município com mais focos, entre " + Formatos.inteiro(p.municipiosComFocos()) + " com registro.");
        preencherNumeros(p);

        Map<YearMonth, Long> serie = new LinkedHashMap<>();
        for (PerfilMunicipio.Mes m : p.porMes()) serie.put(YearMonth.of(m.ano(), m.mes()), m.focos());
        faixa.setDados(serie);
        faixa.setSelecao(null, null);
        PerfilMunicipio.Mes pk = p.pico();
        pico.setText(pk == null ? "" : "Pico em " + Estatisticas.MESES[pk.mes() - 1] + "/" + pk.ano() + ", com "
                + Formatos.inteiro(pk.focos()) + (pk.focos() == 1 ? " foco." : " focos."));

        List<BarrasHorizontais.Item> itens = new ArrayList<>();
        for (int i = 0; i < Math.min(MAX_VIZINHOS, p.vizinhosComFocos().size()); i++) {
            Contagem c = p.vizinhosComFocos().get(i);
            itens.add(new BarrasHorizontais.Item(c.chave(), c.total(), Formatos.inteiro(c.total()), null, i == 0, null));
        }
        vizinhos.setItens(itens);
        vizinhos.setVisible(!itens.isEmpty());
        vizinhos.setManaged(!itens.isEmpty());
        vizinhosTexto.setText(p.vizinhos() == 0 ? "Sem vizinhos na malha do IBGE."
                : p.vizinhosComFocos().size() + " de " + p.vizinhos() + " vizinhos tiveram focos"
                + (p.vizinhosComFocos().size() > MAX_VIZINHOS ? "; aqui estão os " + MAX_VIZINHOS + " com mais." : "."));
        verNoMapa.setDisable(Double.isNaN(p.latitude()));
        campoComparar.clear();
        setAccessibleText("Ficha de " + p.nome() + ": " + total.getText() + " " + totalTexto.getText() + ". " + ranking.getText());

        boolean jaAberta = isVisible();
        setVisible(true);
        setManaged(true);
        toFront();
        if (!jaAberta && Movimento.ativo()) {
            setOpacity(0);
            setTranslateX(28);
            new Timeline(new KeyFrame(Duration.millis(260), new KeyValue(opacityProperty(), 1, Movimento.SAIDA),
                    new KeyValue(translateXProperty(), 0, Movimento.SAIDA))).play();
        }
    }

    private void preencherNumeros(PerfilMunicipio p) {
        numeros.getChildren().clear();
        int col = 0;
        for (Contagem a : p.porAno()) numeros.add(numero(Formatos.inteiro(a.total()), a.chave()), col++, 0);
        if (p.porAno().size() == 2) {
            long de = p.porAno().get(0).total(), ate = p.porAno().get(1).total();
            String v = de == 0 ? "—" : (ate >= de ? "+" : "−") + Formatos.decimal(Math.abs(100.0 * (ate - de) / de), 0) + "%";
            numeros.add(numero(v, "variação"), col, 0);
        }
        numeros.add(numero(p.biomaPrincipal().isEmpty() ? "—" : p.biomaPrincipal(), "bioma principal"), 0, 1, 2, 1);
        numeros.add(numero(Double.isNaN(p.densidade()) ? "—" : Formatos.decimal(p.densidade(), 1), "por 1.000 km²"), 2, 1);
    }

    private static VBox numero(String valor, String rotulo) {
        Label v = new Label(valor);
        v.getStyleClass().add("ficha-valor");
        Label r = new Label(rotulo);
        r.getStyleClass().add("ficha-rotulo");
        return new VBox(0, v, r);
    }

    private void filtrarSugestoes(String texto) {
        String q = Textos.semAcentos(texto == null ? "" : texto.strip()).toLowerCase(Locale.ROOT);
        List<String> inicio = new ArrayList<>(), meio = new ArrayList<>();
        if (!q.isEmpty()) {
            for (String m : municipios) {
                if (perfil != null && m.equalsIgnoreCase(perfil.nome())) continue;
                String k = Textos.semAcentos(m).toLowerCase(Locale.ROOT);
                if (k.startsWith(q)) inicio.add(m);
                else if (k.contains(q)) meio.add(m);
            }
        }
        inicio.addAll(meio);
        List<String> itens = inicio.subList(0, Math.min(6, inicio.size()));
        sugestoes.getItems().setAll(itens);
        sugestoes.setPrefHeight(itens.size() * 30 + 4);
        sugestoes.setVisible(!itens.isEmpty());
        sugestoes.setManaged(!itens.isEmpty());
    }

    private void comparar(String outro) {
        if (outro == null || perfil == null || aoComparar == null) return;
        sugestoes.setVisible(false);
        sugestoes.setManaged(false);
        aoComparar.accept(perfil, outro);
    }

    /** Nomes que o campo "Comparar" sugere (normalmente os municipios da base). */
    public void setMunicipios(List<String> nomes) {
        this.municipios = List.copyOf(nomes);
    }

    /** Chamado com a ficha atual e o nome escolhido para comparar. */
    public void setOnComparar(BiConsumer<PerfilMunicipio, String> acao) {
        this.aoComparar = acao;
    }

    /** Digita no campo de comparacao (usado pelos testes e pela captura de telas). */
    public void digitarComparacao(String texto) {
        campoComparar.setText(texto);
    }

    public void fechar() {
        setVisible(false);
        setManaged(false);
    }

    public boolean aberta() {
        return isVisible();
    }

    /** Municipio exibido, ou null. */
    public PerfilMunicipio perfil() {
        return perfil;
    }

    public void setOnVerNoMapa(Consumer<PerfilMunicipio> acao) {
        this.aoVerNoMapa = acao;
    }

    /** Clique num mes da faixa (nulos quando a selecao e limpa). */
    public void setOnMes(BiConsumer<YearMonth, YearMonth> acao) {
        faixa.setOnSelecionar(acao);
    }
}
