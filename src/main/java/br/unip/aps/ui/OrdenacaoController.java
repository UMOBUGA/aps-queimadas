package br.unip.aps.ui;

import br.unip.aps.ApsException;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.sorting.AlgoritmoTipo;
import br.unip.aps.sorting.CenarioEntrada;
import br.unip.aps.sorting.Complexidade;
import br.unip.aps.sorting.CriterioOrdenacao;
import br.unip.aps.sorting.Criterios;
import br.unip.aps.sorting.Ordem;
import br.unip.aps.sorting.Ordenacoes;
import br.unip.aps.sorting.ResultadoOrdenacao;
import br.unip.aps.sorting.ServicoOrdenacao;
import br.unip.aps.ui.componentes.BarrasHorizontais;
import br.unip.aps.ui.componentes.ChartCard;
import br.unip.aps.ui.componentes.Chip;
import br.unip.aps.ui.componentes.Feedback;
import br.unip.aps.ui.componentes.Icones;
import br.unip.aps.ui.componentes.KpiCard;
import br.unip.aps.ui.componentes.SortVisualizer;
import br.unip.aps.util.Formatos;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Tela Ordenacao. */
public class OrdenacaoController implements Pagina.Controlador {
    private static final DataFormat FORMATO_INDICE = new DataFormat("application/x-aps-nivel");

    /** Nivel editavel da lista de criterios. */
    static final class NivelEditavel {
        final ObjectProperty<CriterioOrdenacao> criterio = new SimpleObjectProperty<>();
        final ObjectProperty<Ordem> ordem = new SimpleObjectProperty<>(Ordem.CRESCENTE);

        NivelEditavel(CriterioOrdenacao c, Ordem o) {
            criterio.set(c);
            ordem.set(o);
        }
    }

    private final UiContexto ctx;

    @FXML private ListView<NivelEditavel> lvCriterios;
    @FXML private Button btnAdicionar, btnOrdenar, btnComparar, btnCsv;
    @FXML private ComboBox<AlgoritmoTipo> cbAlgoritmo;
    @FXML private FlowPane chipsAlgoritmo;
    @FXML private Label lblDescricao, lblTitulo, lblResumo, lblBanner;
    @FXML private TextField tfAmostra;
    @FXML private ToggleButton tg1000, tg5000, tgTodos, tgDados, tgComparativo, tgVisualizar;
    @FXML private CheckBox chkAleatoria;
    @FXML private ComboBox<CenarioEntrada> cbCenario;
    @FXML private HBox chipsResultado, banner;
    @FXML private KpiCard mComparacoes, mTrocas, mAcessos, mTempo;
    @FXML private TableView<FocoIncendio> tabela;
    @FXML private VBox painelComparativo, painelVisualizacao;
    @FXML private javafx.scene.layout.StackPane areaVisoes;

    private final ObservableList<NivelEditavel> niveis = FXCollections.observableArrayList();
    private final List<CriterioOrdenacao> disponiveis = new ArrayList<>();
    private final BarrasHorizontais barrasComparativo = new BarrasHorizontais();
    {
        barrasComparativo.setEscalaLog(true);
    }
    private final TableView<ResultadoOrdenacao<FocoIncendio>> tabelaComparativo = new TableView<>();
    private final ToggleGroup metrica = new ToggleGroup();
    private final SortVisualizer visualizador = new SortVisualizer();
    private final Label lblVisualAlg = new Label();
    private ChartCard cardComparativo;
    private List<ResultadoOrdenacao<FocoIncendio>> comparativo = List.of();

    public OrdenacaoController(UiContexto ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        configurarCriterios(null);
        lvCriterios.setItems(niveis);
        lvCriterios.setCellFactory(lv -> new CelulaNivel());
        lvCriterios.setFocusTraversable(false);
        lvCriterios.setFixedCellSize(52);
        niveis.addListener((javafx.collections.ListChangeListener<NivelEditavel>) c -> {
            btnAdicionar.setDisable(niveis.size() >= 3);
            lvCriterios.setPrefHeight(Math.max(1, niveis.size()) * 52 + 6);
            lvCriterios.setMinHeight(Math.max(1, niveis.size()) * 52 + 6);
            atualizarAviso();
        });
        niveis.add(new NivelEditavel(CriterioOrdenacao.DATA, Ordem.CRESCENTE));
        btnAdicionar.setGraphic(Icones.de(Icones.ADICIONAR, 16));

        cbAlgoritmo.setItems(FXCollections.observableArrayList(AlgoritmoTipo.values()));
        cbAlgoritmo.setCellFactory(lv -> new CelulaAlgoritmo());
        cbAlgoritmo.setButtonCell(new CelulaAlgoritmo());
        cbAlgoritmo.getSelectionModel().select(AlgoritmoTipo.MERGE);
        cbAlgoritmo.valueProperty().addListener((o, a, n) -> {
            descreverAlgoritmo();
            atualizarAviso();
            visualizador.setAlgoritmo(n == AlgoritmoTipo.RADIX ? AlgoritmoTipo.RADIX : n);
            lblVisualAlg.setText(n.nome());
        });
        cbCenario.setItems(FXCollections.observableArrayList(CenarioEntrada.values()));
        cbCenario.getSelectionModel().selectFirst();

        ToggleGroup rapidos = new ToggleGroup();
        for (ToggleButton t : List.of(tg1000, tg5000, tgTodos)) t.setToggleGroup(rapidos);
        tg1000.setOnAction(e -> tfAmostra.setText("1000"));
        tg5000.setOnAction(e -> tfAmostra.setText("5000"));
        tgTodos.setOnAction(e -> tfAmostra.setText("0"));
        tfAmostra.textProperty().addListener((o, a, n) -> {
            String s = n == null ? "" : n.replace(".", "").strip();
            tg1000.setSelected(s.equals("1000"));
            tg5000.setSelected(s.equals("5000"));
            tgTodos.setSelected(s.equals("0") || s.isEmpty());
            atualizarAviso();
        });

        btnOrdenar.setGraphic(Icones.de(Icones.EXECUTAR, 16));
        btnComparar.setGraphic(Icones.de(Icones.COMPARAR, 16));
        btnCsv.setGraphic(Icones.de(Icones.CSV, 16));
        lblBanner.setGraphic(Icones.de(Icones.ALERTA, 18));

        configurarVisoes();
        javafx.scene.shape.Rectangle recorte = new javafx.scene.shape.Rectangle();
        recorte.widthProperty().bind(areaVisoes.widthProperty());
        recorte.heightProperty().bind(areaVisoes.heightProperty());
        areaVisoes.setClip(recorte);
        configurarTabelas();
        configurarMetricas();
        descreverAlgoritmo();

        ctx.baseProperty().addListener((o, a, b) -> {
            configurarCriterios(b);
            tabela.getItems().clear();
            comparativo = List.of();
            atualizarComparativo();
        });
        ctx.focosFiltradosProperty().addListener((o, a, n) -> atualizarAviso());
    }

    @Override
    public void aoOcultar() {
        visualizador.parar();
    }

    private void configurarCriterios(BaseDeFocos b) {
        disponiveis.clear();
        for (CriterioOrdenacao c : CriterioOrdenacao.values()) {
            if (!c.opcional() || (b != null && b.possuiCampo(c::valor))) disponiveis.add(c);
        }
        niveis.removeIf(n -> !disponiveis.contains(n.criterio.get()));
        if (niveis.isEmpty()) niveis.add(new NivelEditavel(CriterioOrdenacao.DATA, Ordem.CRESCENTE));
        lvCriterios.refresh();
    }

    @FXML
    private void adicionarCriterio() {
        if (niveis.size() >= 3) return;
        for (CriterioOrdenacao c : List.of(CriterioOrdenacao.BIOMA, CriterioOrdenacao.MUNICIPIO, CriterioOrdenacao.DATA)) {
            boolean usado = niveis.stream().anyMatch(n -> n.criterio.get() == c);
            if (!usado) {
                niveis.add(new NivelEditavel(c, Ordem.CRESCENTE));
                return;
            }
        }
        niveis.add(new NivelEditavel(disponiveis.get(0), Ordem.CRESCENTE));
    }

    /** Celula da lista de criterios. */
    private final class CelulaNivel extends ListCell<NivelEditavel> {
        private final Label alca = new Label(null, Icones.de(Icones.ARRASTAR, 18));
        private final Label numero = new Label();
        private final ComboBox<CriterioOrdenacao> combo = new ComboBox<>();
        private final Button ordem = new Button();
        private final Button remover = new Button(null, Icones.de(Icones.REMOVER, 16));
        private final HBox linha;

        CelulaNivel() {
            alca.getStyleClass().add("alca-arraste");
            alca.setTooltip(new Tooltip("Arraste para mudar a prioridade"));
            numero.getStyleClass().add("numero-nivel");
            combo.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(combo, Priority.ALWAYS);
            combo.setConverter(new StringConverter<>() {
                @Override
                public String toString(CriterioOrdenacao c) {
                    return c == null ? "" : c.rotulo();
                }

                @Override
                public CriterioOrdenacao fromString(String s) {
                    return null;
                }
            });
            ordem.getStyleClass().add("btn-icon");
            remover.getStyleClass().add("btn-icon");
            remover.setTooltip(new Tooltip("Remover critério"));
            linha = new HBox(6, alca, numero, combo, ordem, remover);
            linha.getStyleClass().add("criterio-linha");
            linha.setAlignment(Pos.CENTER_LEFT);

            setOnDragDetected(e -> {
                if (getItem() == null) return;
                Dragboard db = startDragAndDrop(TransferMode.MOVE);
                ClipboardContent cc = new ClipboardContent();
                cc.put(FORMATO_INDICE, getIndex());
                db.setContent(cc);
                db.setDragView(linha.snapshot(null, null));
                linha.getStyleClass().add("arrastando");
                e.consume();
            });
            setOnDragOver(e -> {
                if (e.getGestureSource() != this && e.getDragboard().hasContent(FORMATO_INDICE)) {
                    e.acceptTransferModes(TransferMode.MOVE);
                }
                e.consume();
            });
            setOnDragEntered(e -> {
                if (getItem() != null && e.getGestureSource() != this) linha.getStyleClass().add("alvo-drop");
            });
            setOnDragExited(e -> linha.getStyleClass().remove("alvo-drop"));
            setOnDragDropped(e -> {
                Object v = e.getDragboard().getContent(FORMATO_INDICE);
                boolean ok = false;
                if (v instanceof Integer origem && origem >= 0 && origem < niveis.size()) {
                    int destino = getItem() == null ? niveis.size() - 1 : getIndex();
                    NivelEditavel n = niveis.remove((int) origem);
                    niveis.add(Math.min(destino, niveis.size()), n);
                    ok = true;
                }
                e.setDropCompleted(ok);
                e.consume();
            });
            setOnDragDone(e -> linha.getStyleClass().remove("arrastando"));
        }

        @Override
        protected void updateItem(NivelEditavel n, boolean vazio) {
            super.updateItem(n, vazio);
            if (vazio || n == null) {
                setGraphic(null);
                return;
            }
            numero.setText(String.valueOf(getIndex() + 1));
            combo.setItems(FXCollections.observableArrayList(disponiveis));
            combo.setOnAction(null);
            combo.setValue(n.criterio.get());
            combo.setOnAction(e -> {
                n.criterio.set(combo.getValue());
                atualizarAviso();
            });
            atualizarOrdem(n);
            ordem.setOnAction(e -> {
                n.ordem.set(n.ordem.get() == Ordem.CRESCENTE ? Ordem.DECRESCENTE : Ordem.CRESCENTE);
                atualizarOrdem(n);
            });
            remover.setDisable(niveis.size() <= 1);
            remover.setOnAction(e -> niveis.remove(n));
            setGraphic(linha);
        }

        private void atualizarOrdem(NivelEditavel n) {
            boolean cresc = n.ordem.get() == Ordem.CRESCENTE;
            ordem.setGraphic(Icones.de(cresc ? Icones.CRESCENTE : Icones.DECRESCENTE, 16));
            ordem.setTooltip(new Tooltip(cresc ? "Crescente (clique para decrescente)" : "Decrescente (clique para crescente)"));
            ordem.setAccessibleText(cresc ? "Ordem crescente" : "Ordem decrescente");
        }
    }

    private Criterios criterios() {
        List<Criterios.Nivel> r = new ArrayList<>();
        for (NivelEditavel n : niveis) r.add(new Criterios.Nivel(n.criterio.get(), n.ordem.get()));
        return Criterios.composto(r);
    }

    /** Celula do combo de algoritmos. */
    private static final class CelulaAlgoritmo extends ListCell<AlgoritmoTipo> {
        @Override
        protected void updateItem(AlgoritmoTipo t, boolean vazio) {
            super.updateItem(t, vazio);
            if (vazio || t == null) {
                setText(null);
                setGraphic(null);
                return;
            }
            setText(t.nome());
            Label c = Chip.de(t.complexidade().casoMedio(), null, t.quadratico() ? Chip.Variante.ALERTA : Chip.Variante.NEUTRO);
            c.getStyleClass().add("chip-mono");
            setGraphic(c);
            setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
            setGraphicTextGap(10);
        }
    }

    private void descreverAlgoritmo() {
        AlgoritmoTipo t = cbAlgoritmo.getValue();
        if (t == null) return;
        Complexidade c = t.complexidade();
        chipsAlgoritmo.getChildren().setAll(
                chipMono("Médio " + c.casoMedio(), t.quadratico() ? Chip.Variante.ALERTA : Chip.Variante.DESTAQUE),
                chipMono("Melhor " + c.melhorCaso(), Chip.Variante.NEUTRO),
                chipMono("Pior " + c.piorCaso(), Chip.Variante.NEUTRO),
                chipMono("Espaço " + c.espaco(), Chip.Variante.NEUTRO),
                Chip.de(c.estavel() ? "Estável" : "Instável", c.estavel() ? Icones.SUCESSO : Icones.ALERTA,
                        c.estavel() ? Chip.Variante.SUCESSO : Chip.Variante.NEUTRO));
        lblDescricao.setText(DescricoesAlgoritmos.de(t) + (t.exigeChaveNumerica() ? " Só para um critério numérico (data, latitude, longitude, id)." : ""));
    }

    private static Label chipMono(String texto, Chip.Variante v) {
        Label l = Chip.de(texto, null, v);
        l.getStyleClass().add("chip-mono");
        return l;
    }

    private int amostra() {
        String s = tfAmostra.getText() == null ? "" : tfAmostra.getText().replace(".", "").replace(" ", "").strip();
        if (s.isEmpty()) return 0;
        try {
            int n = Integer.parseInt(s);
            if (n < 0) throw new NumberFormatException();
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Tamanho da amostra inválido: '" + tfAmostra.getText() + "'. Use um inteiro ≥ 0 (0 = todos).");
        }
    }

    private void atualizarAviso() {
        AlgoritmoTipo alg = cbAlgoritmo.getValue();
        int total = ctx.focosFiltradosProperty().get().size();
        String msg = null;
        try {
            int n = amostra();
            int efetivo = n == 0 ? total : Math.min(n, total);
            if (alg != null && alg.exigeChaveNumerica() && niveis.size() == 1 && !niveis.get(0).criterio.get().temChaveNumerica()
                    || alg != null && alg.exigeChaveNumerica() && niveis.size() > 1) {
                msg = alg.nome() + " não compara elementos: só ordena por UM critério numérico (Data/hora, Latitude, Longitude ou ID).";
            } else if (alg != null) {
                msg = ctx.sessao().servicoOrdenacao().avisoDesempenho(alg, efetivo);
                if (msg == null && alg.quadratico() && efetivo > 5_000) {
                    msg = alg.nome() + " é O(n²): com n = " + Formatos.inteiro(efetivo) + " fará cerca de "
                            + Formatos.inteiro((long) efetivo * (efetivo - 1) / 2) + " comparações e pode levar alguns segundos. "
                            + "É possível cancelar durante a execução.";
                }
            }
        } catch (IllegalArgumentException e) {
            msg = e.getMessage();
        }
        banner.setVisible(msg != null);
        banner.setManaged(msg != null);
        lblBanner.setText(msg);
    }

    private List<FocoIncendio> dados() throws ApsException {
        ctx.sessao().exigirBase();
        List<FocoIncendio> r = ctx.focosFiltradosProperty().get();
        if (r.isEmpty()) throw new ApsException("Nenhum foco atende aos filtros atuais. Remova algum filtro na barra acima.");
        return r;
    }

    @FXML
    private void ordenar() {
        try {
            List<FocoIncendio> dados = dados();
            AlgoritmoTipo alg = cbAlgoritmo.getValue();
            Criterios crit = criterios();
            int n = amostra();
            int efetivo = n == 0 ? dados.size() : Math.min(n, dados.size());
            String aviso = ctx.sessao().servicoOrdenacao().avisoDesempenho(alg, efetivo);
            if (aviso != null && !ctx.confirmar("Algoritmo quadrático em amostra grande", aviso + "\n\nDeseja continuar?")) return;
            ServicoOrdenacao.Solicitacao s = new ServicoOrdenacao.Solicitacao(alg, crit, n, chkAleatoria.isSelected(),
                    cbCenario.getValue(), 42L);
            ctx.executar("Ordenando " + Formatos.inteiro(efetivo) + " focos com " + alg.nome(),
                    () -> ctx.sessao().servicoOrdenacao().ordenar(dados, s), this::exibir);
        } catch (ApsException | IllegalArgumentException e) {
            ctx.erro("Não foi possível ordenar", e);
        }
    }

    private void exibir(ResultadoOrdenacao<FocoIncendio> r) {
        ctx.sessao().setUltimaOrdenacao(r);
        tabela.setItems(FXCollections.observableArrayList(r.dados()));
        tabela.scrollTo(0);
        lblTitulo.setText(r.algoritmo());
        lblResumo.setText(r.criterio() + "  ·  " + r.cenario() + "  ·  n = " + Formatos.inteiro(r.tamanho()) + " focos");
        AlgoritmoTipo t = cbAlgoritmo.getValue();
        chipsResultado.getChildren().setAll(
                chipMono(t.complexidade().casoMedio(), t.quadratico() ? Chip.Variante.ALERTA : Chip.Variante.DESTAQUE),
                r.verificado() ? Chip.de("Ordenação verificada", Icones.VERIFICADO, Chip.Variante.SUCESSO)
                        : Chip.de("Falha na verificação", Icones.ERRO, Chip.Variante.PERIGO));
        long n = r.tamanho();
        mComparacoes.valor(Formatos.inteiro(r.metricas().comparacoes()))
                .contexto(n < 2 ? "—" : Formatos.decimal(r.metricas().comparacoes() / (n * Math.log(n) / Math.log(2)), 2) + " × n·log₂n",
                        KpiCard.Tendencia.NEUTRA);
        mTrocas.valor(Formatos.inteiro(r.metricas().trocas()))
                .contexto(Formatos.inteiro(r.metricas().atribuicoes()) + " atribuições", KpiCard.Tendencia.NEUTRA);
        mAcessos.valor(Formatos.inteiro(r.metricas().acessos()))
                .contexto(Formatos.inteiro(r.metricas().leituras()) + " leituras", KpiCard.Tendencia.NEUTRA);
        mTempo.valor(Formatos.duracao(r.metricas().nanos()))
                .contexto("execução única", KpiCard.Tendencia.NEUTRA)
                .dica("Uma execução, sem aquecimento do JIT; para medições rigorosas use a tela Benchmark.");
        tgDados.setSelected(true);
        mostrarVisao();
        Feedback.sucesso("Ordenação concluída", r.algoritmo() + " · " + Formatos.inteiro(r.metricas().comparacoes()) + " comparações");
    }

    @FXML
    private void compararTodos() {
        try {
            List<FocoIncendio> dados = dados();
            Criterios crit = criterios();
            int n = amostra();
            CenarioEntrada cen = cbCenario.getValue();
            boolean aleatoria = chkAleatoria.isSelected();
            ctx.executar("Comparando todos os algoritmos", progresso -> {
                List<ResultadoOrdenacao<FocoIncendio>> r = new ArrayList<>();
                AlgoritmoTipo[] tipos = AlgoritmoTipo.values();
                for (int i = 0; i < tipos.length; i++) {
                    if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
                    AlgoritmoTipo t = tipos[i];
                    progresso.accept((double) i / tipos.length, t.nome());
                    if (t.exigeChaveNumerica() && crit.chaveNumerica() == null) continue;
                    r.add(ctx.sessao().servicoOrdenacao().ordenar(dados, new ServicoOrdenacao.Solicitacao(t, crit, n, aleatoria, cen, 42L)));
                }
                return r;
            }, r -> {
                comparativo = r;
                ctx.sessao().setUltimoComparativo(r);
                tabelaComparativo.setItems(FXCollections.observableArrayList(r));
                atualizarComparativo();
                tgComparativo.setSelected(true);
                mostrarVisao();
                Feedback.sucesso("Comparativo concluído", r.size() + " algoritmos sobre a mesma entrada");
            });
        } catch (ApsException | IllegalArgumentException e) {
            ctx.erro("Não foi possível comparar", e);
        }
    }

    private void configurarVisoes() {
        ToggleGroup g = new ToggleGroup();
        for (ToggleButton t : List.of(tgDados, tgComparativo, tgVisualizar)) t.setToggleGroup(g);
        tgDados.setGraphic(Icones.de(Icones.TABELA, 15));
        tgComparativo.setGraphic(Icones.de(Icones.COMPARAR, 15));
        tgVisualizar.setGraphic(Icones.de(Icones.ANIMAR, 15));
        g.selectedToggleProperty().addListener((o, a, n) -> {
            if (n == null) a.setSelected(true);
            else mostrarVisao();
        });

        cardComparativo = new ChartCard("Comparativo de algoritmos", "Mesma entrada para todos; menor é melhor. Execução única, sem aquecimento do JIT.");
        HBox seg = new HBox();
        seg.getStyleClass().add("segmented");
        for (String m : List.of("Comparações", "Trocas", "Acessos", "Tempo")) {
            ToggleButton t = new ToggleButton(m);
            t.setToggleGroup(metrica);
            t.setUserData(m);
            seg.getChildren().add(t);
        }
        metrica.getToggles().get(0).setSelected(true);
        metrica.selectedToggleProperty().addListener((o, a, n) -> {
            if (n == null) a.setSelected(true);
            else atualizarComparativo();
        });
        cardComparativo.setExtra(seg);
        cardComparativo.conteudo(barrasComparativo);
        cardComparativo.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        tabelaComparativo.setMinHeight(160);
        cardComparativo.estado(ChartCard.Estado.VAZIO);
        ctx.registrarGrafico("Comparativo de algoritmos", cardComparativo);
        tabelaComparativo.setPrefHeight(300);
        VBox.setVgrow(tabelaComparativo, Priority.ALWAYS);
        painelComparativo.getChildren().addAll(cardComparativo, tabelaComparativo);

        Label t = new Label("Visualização animada");
        t.getStyleClass().add("card-title");
        Label s = new Label("32 valores aleatórios ordenados pelo algoritmo escolhido. Cada passo é uma operação registrada pelo "
                + "InstrumentedArray: azul = comparação, brasa = troca/escrita, verde = concluído.");
        s.getStyleClass().add("card-subtitle");
        s.setWrapText(true);
        lblVisualAlg.getStyleClass().addAll("chip", "chip-accent");
        lblVisualAlg.setText(cbAlgoritmo.getValue().nome());
        Region esp = new Region();
        HBox.setHgrow(esp, Priority.ALWAYS);
        HBox cab = new HBox(10, new VBox(2, t, s), esp, lblVisualAlg);
        VBox.setVgrow(visualizador, Priority.ALWAYS);
        painelVisualizacao.getChildren().addAll(cab, visualizador);
        visualizador.setAlgoritmo(cbAlgoritmo.getValue());
    }

    private void mostrarVisao() {
        tabela.setVisible(tgDados.isSelected());
        painelComparativo.setVisible(tgComparativo.isSelected());
        painelVisualizacao.setVisible(tgVisualizar.isSelected());
        if (!tgVisualizar.isSelected()) visualizador.parar();
    }

    private void atualizarComparativo() {
        if (comparativo.isEmpty()) {
            barrasComparativo.getChildren().clear();
            cardComparativo.estado(ChartCard.Estado.VAZIO);
            return;
        }
        String m = (String) metrica.getSelectedToggle().getUserData();
        List<ResultadoOrdenacao<FocoIncendio>> ord = new ArrayList<>(comparativo);
        Ordenacoes.ordenar(ord, (a, b) -> Double.compare(valor(a, m), valor(b, m)));
        List<BarrasHorizontais.Item> itens = new ArrayList<>();
        for (int i = 0; i < ord.size(); i++) {
            ResultadoOrdenacao<FocoIncendio> r = ord.get(i);
            double v = valor(r, m);
            String txt = m.equals("Tempo") ? Formatos.duracao(r.metricas().nanos()) : Formatos.inteiro((long) v);
            itens.add(new BarrasHorizontais.Item(r.algoritmo(), v, txt, i == 0 ? "" : "suave", i == 0,
                    r.algoritmo() + " — " + m.toLowerCase() + ": " + txt + (i == 0 ? " (melhor)" : "")));
        }
        barrasComparativo.setItens(itens);
        cardComparativo.setSubtitulo(ord.get(0).criterio() + " · " + ord.get(0).cenario() + " · n = "
                + Formatos.inteiro(ord.get(0).tamanho()) + " · menor é melhor · escala logarítmica");
        List<String[]> linhas = new ArrayList<>();
        for (ResultadoOrdenacao<FocoIncendio> r : ord) {
            linhas.add(new String[]{r.algoritmo(), Formatos.inteiro(r.metricas().comparacoes()), Formatos.inteiro(r.metricas().trocas()),
                    Formatos.inteiro(r.metricas().acessos()), Formatos.duracao(r.metricas().nanos())});
        }
        cardComparativo.setDados(new String[]{"Algoritmo", "Comparações", "Trocas", "Acessos", "Tempo"}, () -> linhas);
        cardComparativo.estado(ChartCard.Estado.CONTEUDO);
    }

    private static double valor(ResultadoOrdenacao<FocoIncendio> r, String m) {
        return switch (m) {
            case "Trocas" -> r.metricas().trocas();
            case "Acessos" -> r.metricas().acessos();
            case "Tempo" -> r.metricas().nanos();
            default -> r.metricas().comparacoes();
        };
    }

    private void configurarTabelas() {
        Tabelas.preparar(tabela, "Execute uma ordenação para ver os focos ordenados aqui.");
        tabela.getColumns().add(Tabelas.indice());
        var colData = Tabelas.coluna("Data/hora (GMT)", (FocoIncendio f) -> f.getDataHora().format(Formatos.DATA_HORA), 150);
        colData.setMinWidth(150);
        tabela.getColumns().add(colData);
        tabela.getColumns().add(Tabelas.coluna("Município", FocoIncendio::getMunicipio, 200));
        tabela.getColumns().add(Tabelas.bioma("Bioma", FocoIncendio::getBioma, 160));
        tabela.getColumns().add(Tabelas.numero("Latitude", FocoIncendio::getLatitude, v -> Formatos.decimal(v, 5), 105));
        tabela.getColumns().add(Tabelas.numero("Longitude", FocoIncendio::getLongitude, v -> Formatos.decimal(v, 5), 105));
        tabela.getColumns().add(Tabelas.numero("id_bdq", FocoIncendio::getIdBdq, String::valueOf, 115));

        Tabelas.preparar(tabelaComparativo, "Clique em \"Comparar todos os algoritmos\".");
        tabelaComparativo.getColumns().add(Tabelas.coluna("Algoritmo", (ResultadoOrdenacao<FocoIncendio> r) -> r.algoritmo(), 180));
        tabelaComparativo.getColumns().add(Tabelas.numero("Comparações", r -> r.metricas().comparacoes(), Formatos::inteiro, 120));
        tabelaComparativo.getColumns().add(Tabelas.numero("Trocas", r -> r.metricas().trocas(), Formatos::inteiro, 110));
        tabelaComparativo.getColumns().add(Tabelas.numero("Atribuições", r -> r.metricas().atribuicoes(), Formatos::inteiro, 120));
        tabelaComparativo.getColumns().add(Tabelas.numero("Acessos", r -> r.metricas().acessos(), Formatos::inteiro, 120));
        tabelaComparativo.getColumns().add(Tabelas.numero("Tempo (ms)", r -> r.metricas().millis(), v -> Formatos.decimal(v, 3), 100));
        tabelaComparativo.getColumns().add(Tabelas.coluna("Estável", r -> estavel(r.algoritmo()), 80));
        tabelaComparativo.getColumns().add(Tabelas.coluna("Verificado", r -> r.verificado() ? "✔ sim" : "✘ falha", 90));
    }

    private void configurarMetricas() {
        for (KpiCard k : List.of(mComparacoes, mTrocas, mAcessos, mTempo)) k.valor("—").contexto("aguardando execução", KpiCard.Tendencia.NEUTRA);
        mComparacoes.icone(KpiCard.EstiloIcone.DESTAQUE);
    }

    private static String estavel(String nome) {
        for (AlgoritmoTipo t : AlgoritmoTipo.values()) {
            if (t.nome().equals(nome)) return t.complexidade().estavel() ? "sim" : "não";
        }
        return "?";
    }

    @FXML
    private void exportarCsv() {
        ResultadoOrdenacao<FocoIncendio> r = ctx.sessao().ultimaOrdenacao();
        if (r == null) {
            Feedback.alerta("Nada para exportar", "Execute uma ordenação primeiro.");
            return;
        }
        FileChooser fc = new FileChooser();
        Path sug = ctx.sessao().arquivoRelatorio("dados-ordenados", ".csv");
        File dir = sug.toAbsolutePath().getParent().toFile();
        if (!dir.isDirectory() && !dir.mkdirs()) dir = new File(System.getProperty("user.home"));
        fc.setInitialDirectory(dir);
        fc.setInitialFileName(sug.getFileName().toString());
        File f = fc.showSaveDialog(ctx.stage());
        if (f == null) return;
        ctx.executar("Exportando CSV", () -> ctx.sessao().exporter().exportarFocosCsv(r.dados(), f.toPath()),
                p -> Feedback.sucesso("CSV exportado", p.getFileName().toString()));
    }

    /** Seleciona o algoritmo (paleta de comandos). */
    void selecionarAlgoritmo(AlgoritmoTipo tipo) {
        cbAlgoritmo.getSelectionModel().select(tipo);
    }

    @Override
    public void demonstrar(Runnable concluido) {
        niveis.setAll(new NivelEditavel(CriterioOrdenacao.BIOMA, Ordem.CRESCENTE),
                new NivelEditavel(CriterioOrdenacao.MUNICIPIO, Ordem.CRESCENTE),
                new NivelEditavel(CriterioOrdenacao.DATA, Ordem.DECRESCENTE));
        cbAlgoritmo.getSelectionModel().select(AlgoritmoTipo.QUICK_3WAY);
        ordenar();
        concluido.run();
    }

    void demonstrarComparativo(Runnable concluido) {
        compararTodos();
        concluido.run();
    }

    void demonstrarVisualizacao() {
        cbAlgoritmo.getSelectionModel().select(AlgoritmoTipo.QUICK);
        tgVisualizar.setSelected(true);
        mostrarVisao();
        visualizador.setAlgoritmo(AlgoritmoTipo.QUICK);
        visualizador.reproduzir();
    }
}
