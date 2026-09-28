package br.unip.aps.ui.componentes;

import br.unip.aps.analysis.Contagem;
import br.unip.aps.analysis.Estatisticas;
import br.unip.aps.geo.PerfilMunicipio;
import br.unip.aps.util.Formatos;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Painel que coloca dois municipios lado a lado: totais, ranking, anos, bioma, densidade e focos mes a mes. */
public class ComparacaoMunicipios extends VBox {
    private final Label titulo = new Label();
    private final GridPane tabela = new GridPane();
    private final BarrasPareadas barras = new BarrasPareadas();
    private final Label legendaA = new Label();
    private final Label legendaB = new Label();
    private final Label resumo = new Label();
    private final Button voltar = new Button();
    private PerfilMunicipio a, b;
    private BiConsumer<PerfilMunicipio, PerfilMunicipio> aoVerNoMapa;
    private Consumer<PerfilMunicipio> aoVoltar;

    public ComparacaoMunicipios() {
        getStyleClass().addAll("ficha-painel", "comparacao-painel");
        setId("comparacaoMunicipios");
        setVisible(false);
        setManaged(false);
        setMaxWidth(Region.USE_PREF_SIZE);
        setPrefWidth(560);
        setSpacing(12);

        Label sobre = new Label("COMPARAÇÃO DE MUNICÍPIOS");
        sobre.getStyleClass().add("t-overline");
        titulo.getStyleClass().add("ficha-nome");
        titulo.setWrapText(true);
        Button fechar = new Button(null, Icones.de(Icones.FECHAR, 16));
        fechar.getStyleClass().add("btn-icon");
        fechar.setId("btnFecharComparacao");
        fechar.setAccessibleText("Fechar a comparação");
        fechar.setOnAction(e -> fechar());
        VBox titulos = new VBox(2, sobre, titulo);
        HBox.setHgrow(titulos, Priority.ALWAYS);
        HBox cabecalho = new HBox(8, titulos, fechar);
        cabecalho.setAlignment(Pos.TOP_LEFT);

        tabela.getStyleClass().add("comparacao-tabela");
        tabela.setHgap(16);
        tabela.setVgap(9);
        ColumnConstraints c0 = new ColumnConstraints(118);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        c1.setPercentWidth(40);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        c2.setPercentWidth(40);
        tabela.getColumnConstraints().addAll(c0, c1, c2);

        legendaA.getStyleClass().addAll("comparacao-legenda", "comparacao-a");
        legendaB.getStyleClass().addAll("comparacao-legenda", "comparacao-b");
        HBox legendas = new HBox(16, legendaA, legendaB);
        resumo.getStyleClass().add("ficha-texto");
        resumo.setWrapText(true);

        Button verNoMapa = new Button("Ver os dois no mapa");
        verNoMapa.setId("btnVerComparacaoNoMapa");
        verNoMapa.getStyleClass().add("btn-primary");
        verNoMapa.setGraphic(Icones.de(Icones.ALVO, 15));
        verNoMapa.setOnAction(e -> {
            if (aoVerNoMapa != null && a != null) aoVerNoMapa.accept(a, b);
        });
        voltar.getStyleClass().add("btn-ghost");
        voltar.setId("btnVoltarFicha");
        voltar.setOnAction(e -> {
            if (aoVoltar != null && a != null) aoVoltar.accept(a);
        });
        Layout.naoEncolher(verNoMapa);
        Layout.naoEncolher(voltar);
        HBox acoes = new HBox(10, verNoMapa, voltar);
        acoes.setAlignment(Pos.CENTER_LEFT);

        VBox conteudo = new VBox(16, tabela, resumo,
                secao("Focos por mês", "Mesma escala para os dois municípios."), legendas, barras, acoes);
        conteudo.getStyleClass().add("ficha-conteudo");
        ScrollPane rolagem = new ScrollPane(conteudo);
        rolagem.setFitToWidth(true);
        rolagem.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rolagem.getStyleClass().add("ficha-rolagem");
        VBox.setVgrow(rolagem, Priority.ALWAYS);
        getChildren().addAll(cabecalho, rolagem);
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
        VBox v = new VBox(1, t, s);
        v.getStyleClass().add("ficha-secao-cabecalho");
        return v;
    }

    /** Preenche e abre o painel com o municipio A (laranja) e o B (azul). */
    public void mostrar(PerfilMunicipio pa, PerfilMunicipio pb) {
        a = pa;
        b = pb;
        titulo.setText(pa.nome() + " × " + pb.nome());
        legendaA.setText(pa.nome());
        legendaB.setText(pb.nome());
        voltar.setText("Voltar à ficha de " + pa.nome());
        preencherTabela();
        barras.setDados(pa.porMes(), pb.porMes());
        resumo.setText(resumo(pa, pb));
        setAccessibleText("Comparação entre " + pa.nome() + " e " + pb.nome() + ". " + resumo.getText());

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

    /** Frase de leitura rapida: quem teve mais focos, quantas vezes mais, e se os dois fazem fronteira. */
    static String resumo(PerfilMunicipio pa, PerfilMunicipio pb) {
        StringBuilder s = new StringBuilder();
        if (pa.total() == pb.total()) {
            s.append("Os dois tiveram ").append(Formatos.inteiro(pa.total())).append(pa.total() == 1 ? " foco." : " focos.");
        } else {
            PerfilMunicipio maior = pa.total() > pb.total() ? pa : pb, menor = maior == pa ? pb : pa;
            s.append(maior.nome()).append(" teve ");
            if (menor.total() == 0) s.append(Formatos.inteiro(maior.total())).append(" focos; ").append(menor.nome()).append(", nenhum.");
            else s.append(Formatos.decimal((double) maior.total() / menor.total(), 1)).append(" vezes os focos de ").append(menor.nome()).append('.');
        }
        for (Contagem v : pa.vizinhosComFocos()) {
            if (v.chave().equals(pb.nome())) {
                s.append(" Os dois fazem fronteira.");
                break;
            }
        }
        return s.toString();
    }

    private void preencherTabela() {
        tabela.getChildren().clear();
        int r = 0;
        tabela.add(celula("", "comparacao-rotulo"), 0, r);
        tabela.add(celula(a.nome(), "comparacao-nome", "comparacao-a"), 1, r);
        tabela.add(celula(b.nome(), "comparacao-nome", "comparacao-b"), 2, r++);
        r = linha(r, "Focos", Formatos.inteiro(a.total()), Formatos.inteiro(b.total()), "comparacao-numero");
        r = linha(r, "No ranking", posicao(a), posicao(b), "comparacao-valor");
        for (int i = 0; i < a.porAno().size() && i < b.porAno().size(); i++) {
            r = linha(r, a.porAno().get(i).chave(), Formatos.inteiro(a.porAno().get(i).total()),
                    Formatos.inteiro(b.porAno().get(i).total()), "comparacao-valor");
        }
        r = linha(r, "Variação", variacao(a), variacao(b), "comparacao-valor");
        r = linha(r, "Bioma principal", vazio(a.biomaPrincipal()), vazio(b.biomaPrincipal()), "comparacao-texto");
        r = linha(r, "Por 1.000 km²", densidade(a), densidade(b), "comparacao-valor");
        linha(r, "Pico", pico(a), pico(b), "comparacao-texto");
    }

    private int linha(int r, String rotulo, String va, String vb, String classe) {
        tabela.add(celula(rotulo, "comparacao-rotulo"), 0, r);
        tabela.add(celula(va, classe), 1, r);
        tabela.add(celula(vb, classe), 2, r);
        return r + 1;
    }

    private static Label celula(String texto, String... classes) {
        Label l = new Label(texto);
        l.getStyleClass().addAll(classes);
        l.setWrapText(true);
        GridPane.setHalignment(l, HPos.LEFT);
        return l;
    }

    private static String posicao(PerfilMunicipio p) {
        return p.total() == 0 ? "—" : p.posicao() + "º de " + Formatos.inteiro(p.municipiosComFocos());
    }

    private static String variacao(PerfilMunicipio p) {
        double v = p.variacao();
        return Double.isNaN(v) ? "—" : (v >= 0 ? "+" : "−") + Formatos.decimal(Math.abs(v), 0) + "%";
    }

    private static String densidade(PerfilMunicipio p) {
        return Double.isNaN(p.densidade()) ? "—" : Formatos.decimal(p.densidade(), 1);
    }

    private static String pico(PerfilMunicipio p) {
        PerfilMunicipio.Mes m = p.pico();
        return m == null ? "—" : Estatisticas.MESES[m.mes() - 1] + "/" + m.ano() + " (" + Formatos.inteiro(m.focos()) + ")";
    }

    private static String vazio(String s) {
        return s.isEmpty() ? "—" : s;
    }

    public void fechar() {
        setVisible(false);
        setManaged(false);
    }

    public boolean aberta() {
        return isVisible();
    }

    /** Municipio A (o da ficha de origem), ou null. */
    public PerfilMunicipio a() {
        return a;
    }

    /** Municipio B (o escolhido para comparar), ou null. */
    public PerfilMunicipio b() {
        return b;
    }

    public void setOnVerNoMapa(BiConsumer<PerfilMunicipio, PerfilMunicipio> acao) {
        this.aoVerNoMapa = acao;
    }

    public void setOnVoltar(Consumer<PerfilMunicipio> acao) {
        this.aoVoltar = acao;
    }

    private static final class BarrasPareadas extends Pane {
        private static final double ALTURA = 110;
        private final List<Region> barrasA = new ArrayList<>();
        private final List<Region> barrasB = new ArrayList<>();
        private final List<Label> rotulos = new ArrayList<>();
        private final List<Node> alvos = new ArrayList<>();
        private final List<Long> valoresA = new ArrayList<>();
        private final List<Long> valoresB = new ArrayList<>();
        private long maximo = 1;

        BarrasPareadas() {
            getStyleClass().add("comparacao-barras");
            setMinWidth(0);
        }

        void setDados(List<PerfilMunicipio.Mes> ma, List<PerfilMunicipio.Mes> mb) {
            getChildren().clear();
            barrasA.clear();
            barrasB.clear();
            rotulos.clear();
            alvos.clear();
            valoresA.clear();
            valoresB.clear();
            maximo = 1;
            int n = Math.min(ma.size(), mb.size());
            for (int i = 0; i < n; i++) {
                maximo = Math.max(maximo, Math.max(ma.get(i).focos(), mb.get(i).focos()));
            }
            for (int i = 0; i < n; i++) {
                PerfilMunicipio.Mes x = ma.get(i), y = mb.get(i);
                Region ra = new Region();
                ra.getStyleClass().addAll("comparacao-barra", "comparacao-a");
                Region rb = new Region();
                rb.getStyleClass().addAll("comparacao-barra", "comparacao-b");
                Region alvo = new Region();
                alvo.getStyleClass().add("comparacao-alvo");
                Tooltip t = new Tooltip(Estatisticas.MESES[x.mes() - 1] + "/" + x.ano() + ": " + Formatos.inteiro(x.focos())
                        + " × " + Formatos.inteiro(y.focos()));
                t.setShowDelay(Duration.millis(60));
                Tooltip.install(alvo, t);
                Label l = new Label(x.mes() == 1 ? String.valueOf(x.ano()) : Estatisticas.MESES[x.mes() - 1]);
                l.getStyleClass().add("faixa-rotulo");
                if (x.mes() == 1) l.getStyleClass().add("ano");
                l.setVisible(x.mes() % 3 == 1);
                barrasA.add(ra);
                barrasB.add(rb);
                alvos.add(alvo);
                rotulos.add(l);
                valoresA.add(x.focos());
                valoresB.add(y.focos());
            }
            getChildren().addAll(barrasA);
            getChildren().addAll(barrasB);
            getChildren().addAll(rotulos);
            getChildren().addAll(alvos);
            requestLayout();
        }

        @Override
        protected double computePrefHeight(double w) {
            return ALTURA + 22;
        }

        @Override
        protected double computeMinHeight(double w) {
            return computePrefHeight(w);
        }

        @Override
        protected double computePrefWidth(double h) {
            return 480;
        }

        @Override
        protected void layoutChildren() {
            int n = barrasA.size();
            if (n == 0) return;
            double passo = getWidth() / n;
            double larg = Math.max(1, (passo - 3) / 2);
            for (int i = 0; i < n; i++) {
                double x = i * passo;
                double ha = valoresA.get(i) == 0 ? 0 : Math.max(2, ALTURA * valoresA.get(i) / maximo);
                double hb = valoresB.get(i) == 0 ? 0 : Math.max(2, ALTURA * valoresB.get(i) / maximo);
                barrasA.get(i).resizeRelocate(x, ALTURA - ha, larg, ha);
                barrasB.get(i).resizeRelocate(x + larg + 1, ALTURA - hb, larg, hb);
                alvos.get(i).resizeRelocate(x, 0, passo, ALTURA);
                Label l = rotulos.get(i);
                l.autosize();
                l.relocate(x, ALTURA + 4);
            }
        }
    }
}
