package br.unip.aps.ui;

import br.unip.aps.busca.ServicoGeografico;
import br.unip.aps.geo.MalhaMunicipal;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.ui.componentes.BarrasHorizontais;
import br.unip.aps.ui.componentes.ChartCard;
import br.unip.aps.ui.componentes.Icones;
import br.unip.aps.ui.componentes.KpiCard;
import br.unip.aps.util.Formatos;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/** Secoes da tela Estruturas com Trie (autocompletar), arvore k-d (focos num raio) e grafo de vizinhos (propagacao). */
final class EstruturasEspaciaisPainel {
    private final UiContexto ctx;
    private final ServicoGeografico servico = new ServicoGeografico();

    private final TextField tfPrefixo = new TextField();
    private final ListView<String> lista = new ListView<>();
    private final KpiCard kTrie = new KpiCard("Trie", "mdi2f-file-tree");
    private final KpiCard kLinearNomes = new KpiCard("Testar todos os nomes", "mdi2m-magnify");

    private final ComboBox<String> cbCentro = new ComboBox<>();
    private final Slider slRaio = new Slider(5, 100, 30);
    private final Label lblRaio = new Label();
    private final KpiCard kKd = new KpiCard("Árvore k-d", "mdi2f-file-tree-outline");
    private final KpiCard kTodos = new KpiCard("Calcular todas as distâncias", "mdi2m-map-marker-distance");
    private final KpiCard kAchados = new KpiCard("Focos no raio", "mdi2f-fire");
    private final Button btnMapa = new Button("Ver no mapa");

    private final ComboBox<String> cbOrigem = new ComboBox<>();
    private final BarrasHorizontais camadas = new BarrasHorizontais();
    private final GridPane gradePropagacao = new GridPane();

    private List<FocoIncendio> focos = List.of();
    private ServicoGeografico.Raio ultimoRaio;

    EstruturasEspaciaisPainel(UiContexto ctx) {
        this.ctx = ctx;
    }

    void montar(ChartCard cTrie, ChartCard cRaio, ChartCard cGrafo) {
        tfPrefixo.setPromptText("Digite o começo de um município (ex.: são j)");
        tfPrefixo.setPrefWidth(320);
        tfPrefixo.setAccessibleText("Prefixo do município");
        tfPrefixo.textProperty().addListener((o, a, n) -> atualizarPrefixo());
        lista.setPrefHeight(220);
        lista.setPlaceholder(new Label("Nenhum município começa assim"));
        for (KpiCard k : List.of(kTrie, kLinearNomes)) k.valor("—").contexto("caracteres examinados", KpiCard.Tendencia.NEUTRA);
        VBox placar = new VBox(18, kTrie, kLinearNomes);
        HBox corpoTrie = new HBox(28, new VBox(10, tfPrefixo, lista), placar);
        cTrie.conteudo(corpoTrie);
        cTrie.estado(ChartCard.Estado.CONTEUDO);

        cbCentro.setPrefWidth(240);
        cbCentro.setPromptText("Centro (município)");
        cbCentro.setAccessibleText("Município no centro do raio");
        slRaio.setMajorTickUnit(25);
        slRaio.setShowTickLabels(true);
        slRaio.setPrefWidth(260);
        slRaio.setAccessibleText("Raio em quilômetros");
        slRaio.valueProperty().addListener((o, a, n) -> {
            lblRaio.setText(Math.round(n.doubleValue()) + " km");
            if (!slRaio.isValueChanging()) atualizarRaio();
        });
        slRaio.valueChangingProperty().addListener((o, a, mudando) -> {
            if (!mudando) atualizarRaio();
        });
        lblRaio.setText(Math.round(slRaio.getValue()) + " km");
        lblRaio.getStyleClass().add("chip");
        cbCentro.valueProperty().addListener((o, a, n) -> atualizarRaio());
        btnMapa.getStyleClass().add("btn-secondary");
        btnMapa.setGraphic(Icones.de(Icones.MUNICIPIO, 16));
        btnMapa.setDisable(true);
        btnMapa.setOnAction(e -> verNoMapa());
        FlowPane controles = new FlowPane(12, 10, cbCentro, new Label("Raio"), slRaio, lblRaio, btnMapa);
        controles.setAlignment(Pos.CENTER_LEFT);
        for (KpiCard k : List.of(kKd, kTodos, kAchados)) k.valor("—");
        HBox placarRaio = new HBox(28, kKd, kTodos, kAchados);
        cRaio.conteudo(new VBox(20, controles, placarRaio));
        cRaio.estado(ChartCard.Estado.CONTEUDO);

        cbOrigem.setPrefWidth(240);
        cbOrigem.setPromptText("Município de origem");
        cbOrigem.setAccessibleText("Município de origem da busca em largura");
        cbOrigem.valueProperty().addListener((o, a, n) -> atualizarCamadas());
        gradePropagacao.setHgap(32);
        gradePropagacao.setVgap(10);
        Label tituloCamadas = new Label("Focos por distância em fronteiras (busca em largura)");
        tituloCamadas.getStyleClass().add("field-label");
        Label tituloProp = new Label("O fogo pula para o vizinho?");
        tituloProp.getStyleClass().add("field-label");
        HBox corpoGrafo = new HBox(40, new VBox(12, cbOrigem, tituloCamadas, camadas), new VBox(12, tituloProp, gradePropagacao));
        cGrafo.conteudo(corpoGrafo);
        cGrafo.estado(ChartCard.Estado.CARREGANDO);
        ctx.registrarGrafico("Vizinhança e propagação (grafo)", cGrafo);
        this.cGrafo = cGrafo;
    }

    private ChartCard cGrafo;

    void atualizar(List<FocoIncendio> base) {
        this.focos = base;
        List<String> nomes = new ArrayList<>();
        for (MalhaMunicipal.Municipio m : MalhaMunicipal.sp().municipios()) nomes.add(m.nome());
        List<String> ordenados = br.unip.aps.sorting.Ordenacoes.ordenar(nomes, br.unip.aps.util.Textos.collator()::compare);
        cbCentro.getItems().setAll(ordenados);
        cbOrigem.getItems().setAll(ordenados);
        if (!base.isEmpty()) {
            String lider = new br.unip.aps.analysis.Estatisticas(base).topMunicipios(1).get(0).chave();
            MalhaMunicipal.Municipio m = MalhaMunicipal.sp().buscar(lider);
            if (m != null) {
                cbCentro.setValue(m.nome());
                cbOrigem.setValue(m.nome());
            }
        }
        if (tfPrefixo.getText().isBlank()) tfPrefixo.setText("São J");
        atualizarPrefixo();
        List<FocoIncendio> copia = base;
        ctx.executarEmSegundoPlano("Grafo de municípios vizinhos", () -> servico.propagacao(copia), this::mostrarPropagacao);
    }

    private void atualizarPrefixo() {
        if (focos.isEmpty()) return;
        ServicoGeografico.Prefixo p = servico.prefixo(focos, tfPrefixo.getText(), 50);
        List<String> nomes = new ArrayList<>();
        for (String n : p.nomes()) nomes.add(VisaoGeralController.capitalizar(n));
        lista.getItems().setAll(nomes);
        kTrie.valor(Formatos.inteiro(p.passosTrie()))
                .contexto("nós visitados para achar " + Formatos.inteiro(p.total()) + " nomes", KpiCard.Tendencia.BOA);
        kLinearNomes.valor(Formatos.inteiro(p.comparacoesLinear()))
                .contexto("caracteres comparados em " + Formatos.inteiro(p.nomesDistintos()) + " nomes", KpiCard.Tendencia.NEUTRA);
    }

    private void atualizarRaio() {
        String centro = cbCentro.getValue();
        if (centro == null || focos.isEmpty()) return;
        ultimoRaio = servico.raio(focos, centro, Math.round(slRaio.getValue()));
        kKd.valor(Formatos.inteiro(ultimoRaio.distanciasArvore()))
                .contexto("distâncias calculadas · altura " + ultimoRaio.alturaArvore(), KpiCard.Tendencia.BOA);
        kTodos.valor(Formatos.inteiro(ultimoRaio.distanciasLinear())).contexto("uma por foco", KpiCard.Tendencia.NEUTRA);
        kAchados.valor(Formatos.inteiro(ultimoRaio.focos().size()))
                .contexto("a até " + Math.round(ultimoRaio.raioKm()) + " km de " + ultimoRaio.municipio(), KpiCard.Tendencia.NEUTRA);
        btnMapa.setDisable(false);
    }

    private void verNoMapa() {
        MalhaMunicipal.Municipio m = MalhaMunicipal.sp().buscar(cbCentro.getValue());
        if (m == null || ultimoRaio == null) return;
        ctx.mostrarCirculoNoMapa(m.latitude(), m.longitude(), ultimoRaio.raioKm(),
                Formatos.inteiro(ultimoRaio.focos().size()) + " focos a até " + Math.round(ultimoRaio.raioKm()) + " km");
    }

    private void atualizarCamadas() {
        String origem = cbOrigem.getValue();
        if (origem == null || focos.isEmpty()) return;
        List<FocoIncendio> copia = focos;
        ctx.executarEmSegundoPlano("Busca em largura", () -> servico.camadas(copia, origem, 4), cs -> {
            List<BarrasHorizontais.Item> itens = new ArrayList<>();
            for (ServicoGeografico.Camada c : cs) {
                String rotulo = c.distancia() == 0 ? "O próprio município" : c.distancia() == 1 ? "Vizinhos (1 fronteira)"
                        : c.distancia() + " fronteiras";
                itens.add(new BarrasHorizontais.Item(rotulo, c.focos(), Formatos.inteiro(c.focos()), c.distancia() == 0 ? "lider" : "",
                        c.distancia() == 0, rotulo + ": " + Formatos.inteiro(c.municipios()) + " municípios, " + Formatos.inteiro(c.comFocos())
                        + " com focos, " + Formatos.inteiro(c.focos()) + " focos"));
            }
            camadas.setItens(itens);
        });
    }

    private void mostrarPropagacao(ServicoGeografico.Propagacao p) {
        gradePropagacao.getChildren().clear();
        linha(0, "Surgiram focos quando um vizinho queimou no mês anterior", Formatos.decimal(100 * p.taxaComVizinho(), 1) + "%",
                Formatos.inteiro(p.novosComVizinho()) + " de " + Formatos.inteiro(p.casosComVizinho()) + " casos");
        linha(1, "Surgiram focos sem vizinho queimando", Formatos.decimal(100 * p.taxaSemVizinho(), 1) + "%",
                Formatos.inteiro(p.novosSemVizinho()) + " de " + Formatos.inteiro(p.casosSemVizinho()) + " casos");
        linha(2, "Razão", Double.isNaN(p.razao()) ? "—" : Formatos.decimal(p.razao(), 1) + "×",
                "grafo com " + Formatos.inteiro(p.municipiosNoGrafo()) + " municípios e " + Formatos.inteiro(p.arestas()) + " fronteiras");
        Label nota = new Label("Associação, não causa: vizinhos dividem clima e uso do solo. Caso = município sem focos num mês; "
                + "olha-se se ele teve focos no mês seguinte.");
        nota.getStyleClass().add("nota-metodo");
        nota.setWrapText(true);
        nota.setMaxWidth(460);
        gradePropagacao.add(nota, 0, 3, 3, 1);
        cGrafo.estado(ChartCard.Estado.CONTEUDO);
        atualizarCamadas();
    }

    private void linha(int i, String rotulo, String valor, String detalhe) {
        Label r = new Label(rotulo);
        r.getStyleClass().add("comparar-rotulo");
        r.setWrapText(true);
        r.setMaxWidth(220);
        Label v = new Label(valor);
        v.getStyleClass().add("comparar-valor");
        Label d = new Label(detalhe);
        d.getStyleClass().add("t-small");
        gradePropagacao.addRow(i, r, v, d);
    }
}
