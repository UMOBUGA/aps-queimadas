package br.unip.aps.ui;

import br.unip.aps.analysis.Contagem;
import br.unip.aps.busca.ServicoConsultas;
import br.unip.aps.estruturas.ArvoreAVL;
import br.unip.aps.estruturas.ExternalMergeSort;
import br.unip.aps.estruturas.MedidorMemoria;
import br.unip.aps.estruturas.MergeSortParalelo;
import br.unip.aps.io.RepositorioDados;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.sorting.AlgoritmoTipo;
import br.unip.aps.sorting.CriterioOrdenacao;
import br.unip.aps.sorting.OperationCounter;
import br.unip.aps.sorting.Ordem;
import br.unip.aps.sorting.Ordenacoes;
import br.unip.aps.ui.componentes.ArvoreVisual;
import br.unip.aps.ui.componentes.BarrasHorizontais;
import br.unip.aps.ui.componentes.ChartCard;
import br.unip.aps.ui.componentes.Graficos;
import br.unip.aps.ui.componentes.Icones;
import br.unip.aps.ui.componentes.KpiCard;
import br.unip.aps.util.Formatos;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Tela Estruturas e Busca. */
public class EstruturasController implements Pagina.Controlador {
    private final UiContexto ctx;

    @FXML private TextFlow tfManchete;
    @FXML private ChartCard cConsultas, cTotal, cArvore, cTopK, cMemoria, cExterno, cParalelo, cTrie, cRaio, cGrafo;
    @FXML private ToggleButton tgMunicipio, tgIntervalo;
    @FXML private ComboBox<String> cbMunicipio;
    @FXML private HBox boxDatas;
    @FXML private DatePicker dpDe, dpAte;
    @FXML private Spinner<Integer> spConsultas;
    @FXML private Button btnConsultar;
    @FXML private KpiCard kLinear, kBinaria, kAvl, kHash;
    @FXML private javafx.scene.layout.FlowPane fpControles;
    @FXML private br.unip.aps.ui.componentes.GradeResponsiva gradePlacar;

    private final BarrasHorizontais barrasTotal = new BarrasHorizontais();
    private final BarrasHorizontais barrasTopK = new BarrasHorizontais();
    private final BarrasHorizontais barrasMemoria = new BarrasHorizontais();
    private final ArvoreVisual<String> arvore = new ArvoreVisual<>(EstruturasController::abreviar);
    private final Label lblArvore = new Label();
    private final Label lblTopK = new Label();
    private final GridPane gradeExterno = new GridPane();
    private final LineChart<Number, Number> grafParalelo = new LineChart<>(new NumberAxis(), new NumberAxis());
    private final Label lblParalelo = new Label();
    private Spinner<Integer> spK;
    private Spinner<Integer> spMemoria;
    private ArvoreAVL<String, String> avlDemo;
    private List<String> sequencia = List.of();
    private int proximo;
    private Timeline reproducao;
    private boolean pendente = true;
    private final EstruturasEspaciaisPainel espaciais;

    public EstruturasController(UiContexto ctx) {
        this.ctx = ctx;
        this.espaciais = new EstruturasEspaciaisPainel(ctx);
    }

    @FXML
    private void initialize() {
        ToggleGroup tipo = new ToggleGroup();
        tgMunicipio.setToggleGroup(tipo);
        tgIntervalo.setToggleGroup(tipo);
        tipo.selectedToggleProperty().addListener((o, a, n) -> {
            if (n == null) a.setSelected(true);
            atualizarTipo();
        });
        spConsultas.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100_000, 1_000, 100));
        btnConsultar.setGraphic(Icones.de(Icones.BUSCA, 16));
        atualizarTipo();
        barrasTotal.setEscalaLog(true);
        barrasMemoria.setEscalaLog(true);
        ((VBox) fpControles.getParent()).getChildren().removeAll(fpControles, gradePlacar);
        VBox corpo = new VBox(20, fpControles, gradePlacar);
        cConsultas.conteudo(corpo);
        cConsultas.estado(ChartCard.Estado.CONTEUDO);
        cTotal.conteudo(barrasTotal);
        for (KpiCard k : List.of(kLinear, kBinaria, kAvl, kHash)) k.valor("—").contexto("comparações por consulta", KpiCard.Tendencia.NEUTRA);

        montarArvore();
        montarTopK();
        cMemoria.conteudo(barrasMemoria);
        cMemoria.estado(ChartCard.Estado.CONTEUDO);
        montarExterno();
        montarParalelo();
        espaciais.montar(cTrie, cRaio, cGrafo);
        ctx.registrarGrafico("Consultas: custo total", cTotal);
        ctx.registrarGrafico("Árvore AVL", cArvore);
        ctx.registrarGrafico("Top K com heap", cTopK);
        ctx.registrarGrafico("Memória por algoritmo", cMemoria);
        ctx.registrarGrafico("Merge Sort paralelo", cParalelo);
        ctx.baseProperty().addListener((o, a, b) -> {
            pendente = true;
            prepararBase(b);
        });
        prepararBase(ctx.baseProperty().get());
    }

    private void atualizarTipo() {
        boolean municipio = tgMunicipio.isSelected();
        cbMunicipio.setVisible(municipio);
        cbMunicipio.setManaged(municipio);
        boxDatas.setVisible(!municipio);
        boxDatas.setManaged(!municipio);
    }

    private void prepararBase(BaseDeFocos b) {
        if (b == null) return;
        cbMunicipio.setItems(FXCollections.observableArrayList(b.municipios()));
        if (!b.municipios().isEmpty()) cbMunicipio.getSelectionModel().select("ANDRADINA".equals(b.municipios().get(0)) ? 0 : Math.max(0, b.municipios().indexOf("ANDRADINA")));
        dpDe.setValue(LocalDate.of(2024, 8, 1));
        dpAte.setValue(LocalDate.of(2024, 8, 31));
        reiniciarArvore();
        espaciais.atualizar(b.getFocos());
    }

    @Override
    public void aoExibir() {
        if (!pendente || ctx.baseProperty().get() == null) return;
        pendente = false;
        consultar();
        calcularTopK();
        medirMemoria();
        executarExternoFundo();
        medirParaleloFundo();
    }

    @FXML
    private void consultar() {
        BaseDeFocos b = ctx.baseProperty().get();
        if (b == null) return;
        List<FocoIncendio> focos = b.getFocos();
        int n = spConsultas.getValue();
        boolean porMunicipio = tgMunicipio.isSelected();
        String municipio = cbMunicipio.getValue();
        LocalDate de = dpDe.getValue(), ate = dpAte.getValue();
        ctx.executar("Consultando com as quatro estruturas", () -> {
            ServicoConsultas s = new ServicoConsultas();
            if (porMunicipio) {
                List<String> alvos = n == 1 && municipio != null ? List.of(municipio) : ServicoConsultas.sortearMunicipios(focos, n, 42);
                return s.porMunicipio(focos, alvos);
            }
            List<LocalDateTime[]> alvos = n == 1 && de != null && ate != null
                    ? List.<LocalDateTime[]>of(new LocalDateTime[]{de.atStartOfDay(), ate.plusDays(1).atStartOfDay().minusNanos(1)})
                    : ServicoConsultas.sortearIntervalos(focos, n, 7, 42);
            return s.porIntervalo(focos, alvos);
        }, r -> exibirConsultas(r, n, porMunicipio));
    }

    private void exibirConsultas(List<ServicoConsultas.Custo> r, int n, boolean porMunicipio) {
        KpiCard[] cards = {kLinear, kBinaria, kAvl, kHash};
        ServicoConsultas.Custo linear = r.get(0);
        for (int i = 0; i < r.size(); i++) {
            ServicoConsultas.Custo c = r.get(i);
            if (!c.suportado()) {
                cards[i].valor("—").contexto("não responde intervalos", KpiCard.Tendencia.NEUTRA).dica(c.detalhe());
                continue;
            }
            cards[i].valor(Formatos.decimal(c.comparacoesPorConsulta(), c.comparacoesPorConsulta() < 100 ? 1 : 0))
                    .contexto(i == 0 ? "comparações por consulta, sem preparo"
                                    : "por consulta · preparo " + Formatos.inteiro(c.construcao().comparacoes()),
                            KpiCard.Tendencia.NEUTRA)
                    .dica(c.metodo().resumo() + (c.detalhe().isBlank() ? "" : "\n" + c.detalhe())
                            + "\nResultados encontrados: " + Formatos.inteiro(c.resultados())
                            + (i > 0 ? "\nCompensa a partir de " + Formatos.decimal(c.pontoDeEquilibrio(linear), 1) + " consultas" : ""));
        }
        List<ServicoConsultas.Custo> ord = new ArrayList<>();
        for (ServicoConsultas.Custo c : r) if (c.suportado()) ord.add(c);
        Ordenacoes.ordenar(ord, Comparator.comparingLong(ServicoConsultas.Custo::comparacoesTotais));
        List<BarrasHorizontais.Item> itens = new ArrayList<>();
        List<String[]> linhas = new ArrayList<>();
        for (int i = 0; i < ord.size(); i++) {
            ServicoConsultas.Custo c = ord.get(i);
            itens.add(new BarrasHorizontais.Item(c.metodo().rotulo(), c.comparacoesTotais(), Formatos.inteiro(c.comparacoesTotais()),
                    i == 0 ? "lider" : "suave", i == 0, c.metodo().rotulo() + ": " + Formatos.inteiro(c.construcao().comparacoes())
                    + " no preparo + " + Formatos.inteiro(c.consultas().comparacoes()) + " nas consultas · " + Formatos.duracao(c.nanosTotais())));
            linhas.add(new String[]{c.metodo().rotulo(), Formatos.inteiro(c.construcao().comparacoes()),
                    Formatos.decimal(c.comparacoesPorConsulta(), 1), Formatos.inteiro(c.comparacoesTotais()), Formatos.duracao(c.nanosTotais()),
                    Formatos.inteiro(c.resultados())});
        }
        barrasTotal.setItens(itens);
        cTotal.setSubtitulo(Formatos.inteiro(n) + (n == 1 ? " consulta" : " consultas") + (porMunicipio ? " por município" : " por intervalo de datas")
                + " · preparo incluído · escala logarítmica");
        cTotal.setDados(new String[]{"Método", "Preparo (comparações)", "Por consulta", "Total", "Tempo", "Resultados"}, () -> linhas);
        cTotal.estado(ChartCard.Estado.CONTEUDO);

        ServicoConsultas.Custo bin = r.get(1);
        tfManchete.getChildren().setAll(
                texto("Para " + Formatos.inteiro(n) + (n == 1 ? " consulta" : " consultas") + (porMunicipio ? " por município" : " por intervalo")
                        + ", a busca sequencial faz ", false),
                texto(Formatos.inteiro(linear.comparacoesTotais()) + " comparações", true),
                texto("; ordenando uma vez e usando busca binária, são ", false),
                texto(Formatos.inteiro(bin.comparacoesTotais()), true),
                texto(", já contando o Merge Sort. A ordenação se paga a partir de ", false),
                texto(Formatos.decimal(bin.pontoDeEquilibrio(linear), 1) + " consultas", true),
                texto(".", false));
    }

    private static Text texto(String s, boolean forte) {
        Text t = new Text(s);
        t.getStyleClass().add(forte ? "manchete-apoio-forte" : "manchete-apoio-texto");
        return t;
    }

    private void montarArvore() {
        Button proximoBtn = new Button("Inserir próximo", Icones.de(Icones.ADICIONAR, 16));
        proximoBtn.getStyleClass().add("btn-secondary");
        proximoBtn.setOnAction(e -> inserirProximo());
        Button reproduzir = new Button("Reproduzir", Icones.de(Icones.EXECUTAR, 16));
        reproduzir.getStyleClass().add("btn-primary");
        reproduzir.setOnAction(e -> reproduzir());
        Button reiniciar = new Button("Reiniciar", Icones.de(Icones.REINICIAR, 16));
        reiniciar.getStyleClass().add("btn-ghost");
        reiniciar.setOnAction(e -> reiniciarArvore());
        lblArvore.getStyleClass().add("t-small");
        lblArvore.setWrapText(true);
        lblArvore.setMaxWidth(Double.MAX_VALUE);
        lblArvore.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        HBox acoes = new HBox(8, proximoBtn, reproduzir, reiniciar);
        acoes.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(12, acoes, arvore, lblArvore);
        cArvore.conteudo(box);
        cArvore.estado(ChartCard.Estado.CONTEUDO);
    }

    private void reiniciarArvore() {
        if (reproducao != null) reproducao.stop();
        BaseDeFocos b = ctx.baseProperty().get();
        List<String> nomes = new ArrayList<>();
        if (b != null) {
            for (Contagem c : new br.unip.aps.analysis.Estatisticas(b.getFocos()).topMunicipios(15)) nomes.add(c.chave());
            Ordenacoes.ordenar(nomes, Comparator.naturalOrder());
        }
        sequencia = nomes;
        proximo = 0;
        avlDemo = new ArvoreAVL<>(Comparator.naturalOrder(), new OperationCounter());
        String[] ultimaRotacao = {null};
        avlDemo.setOuvinte((t, pivo) -> ultimaRotacao[0] = t + "|" + pivo);
        arvore.setUserData(ultimaRotacao);
        arvore.mostrar(null, null, null);
        lblArvore.setText("Os 15 municípios com mais focos, inseridos em ordem alfabética (o pior caso de uma árvore de busca sem balanceamento).");
    }

    private void inserirProximo() {
        if (avlDemo == null || proximo >= sequencia.size()) return;
        String[] ultima = (String[]) arvore.getUserData();
        ultima[0] = null;
        String chave = sequencia.get(proximo++);
        avlDemo.inserir(chave, chave);
        String pivo = null, descricao = "sem rotação";
        if (ultima[0] != null) {
            String[] p = ultima[0].split("\\|", 2);
            descricao = "rotação " + p[0] + " em " + abreviar(p[1]);
            pivo = p[1];
        }
        arvore.mostrar(avlDemo.raiz(), chave, pivo);
        double log2 = Math.log(avlDemo.nos() + 1) / Math.log(2);
        lblArvore.setText("Inserido " + chave + " (" + descricao + ") · " + avlDemo.nos() + " nós · altura " + avlDemo.altura()
                + " (mínimo possível " + (int) Math.ceil(log2) + ") · " + avlDemo.rotacoesSimples() + " rotações simples e "
                + avlDemo.rotacoesDuplas() + " duplas · " + avlDemo.contador().getComparacoes() + " comparações");
    }

    private void reproduzir() {
        if (proximo >= sequencia.size()) reiniciarArvore();
        if (reproducao != null) reproducao.stop();
        reproducao = new Timeline(new KeyFrame(Duration.millis(700), e -> inserirProximo()));
        reproducao.setCycleCount(sequencia.size() - proximo);
        reproducao.play();
    }

    private static String abreviar(String s) {
        String t = s.replace("SANTO ", "S. ").replace("SANTA ", "S. ").replace("SÃO ", "S. ");
        return t.length() <= 9 ? t : t.substring(0, 8) + "…";
    }

    private void montarTopK() {
        spK = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 10));
        spK.setPrefWidth(84);
        spK.valueProperty().addListener((o, a, n) -> calcularTopK());
        Label rot = new Label("K");
        rot.getStyleClass().add("field-label");
        HBox topo = new HBox(8, rot, spK);
        topo.setAlignment(Pos.CENTER_LEFT);
        lblTopK.getStyleClass().add("t-small");
        lblTopK.setWrapText(true);
        lblTopK.setMaxWidth(Double.MAX_VALUE);
        lblTopK.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        cTopK.setExtra(topo);
        cTopK.conteudo(new VBox(12, lblTopK, barrasTopK));
        cTopK.estado(ChartCard.Estado.CONTEUDO);
    }

    private void calcularTopK() {
        BaseDeFocos b = ctx.baseProperty().get();
        if (b == null || spK == null) return;
        ServicoConsultas.TopK t = new ServicoConsultas().topK(b.getFocos(), spK.getValue());
        List<BarrasHorizontais.Item> itens = new ArrayList<>();
        List<String[]> linhas = new ArrayList<>();
        for (int i = 0; i < t.porHeap().size(); i++) {
            Contagem c = t.porHeap().get(i);
            itens.add(new BarrasHorizontais.Item(VisaoGeralController.capitalizar(c.chave()), c.total(), Formatos.inteiro(c.total()),
                    i == 0 ? "lider" : "", i == 0, (i + 1) + "º " + c.chave() + ": " + c.total() + " focos"));
            linhas.add(new String[]{(i + 1) + "º", c.chave(), Formatos.inteiro(c.total())});
        }
        barrasTopK.setItens(itens);
        lblTopK.setText("Heap: " + Formatos.inteiro(t.custoHeap().comparacoes()) + " comparações · ordenar os "
                + Formatos.inteiro(t.candidatos()) + " municípios: " + Formatos.inteiro(t.custoOrdenacao().comparacoes())
                + " · mesmo resultado: " + (t.porHeap().equals(t.porOrdenacao()) ? "sim" : "não")
                + " · contagem com tabela hash: " + Formatos.inteiro(t.custoContagem().comparacoes()) + " comparações");
        cTopK.setDados(new String[]{"Posição", "Município", "Focos"}, () -> linhas);
    }

    private void medirMemoria() {
        BaseDeFocos b = ctx.baseProperty().get();
        if (b == null || !MedidorMemoria.suportado()) {
            cMemoria.estado(ChartCard.Estado.VAZIO);
            return;
        }
        FocoIncendio[] a = b.getFocos().toArray(new FocoIncendio[0]);
        CriterioOrdenacao crit = CriterioOrdenacao.DATA;
        List<AlgoritmoTipo> rapidos = new ArrayList<>();
        for (AlgoritmoTipo t : AlgoritmoTipo.values()) if (!t.quadratico()) rapidos.add(t);
        ctx.executarEmSegundoPlano("Memória por algoritmo",
                () -> MedidorMemoria.medir(a, crit.comparador(Ordem.CRESCENTE), crit.chaveNumerica(Ordem.CRESCENTE), rapidos),
                r -> {
                    List<MedidorMemoria.Medicao> ord = new ArrayList<>(r);
                    Ordenacoes.ordenar(ord, Comparator.comparingLong(MedidorMemoria.Medicao::bytesAlocados));
                    List<BarrasHorizontais.Item> itens = new ArrayList<>();
                    List<String[]> linhas = new ArrayList<>();
                    for (MedidorMemoria.Medicao m : ord) {
                        itens.add(new BarrasHorizontais.Item(m.algoritmo() + "  " + m.espacoTeorico(), m.bytesAlocados(),
                                Formatos.bytes(m.bytesAlocados()), m.bytesAlocados() == 0 ? "" : "suave", false,
                                m.algoritmo() + ": teórico " + m.espacoTeorico() + ", medido " + Formatos.inteiro(m.bytesAlocados())
                                        + " bytes (" + m.bytesPorElemento() + " por elemento)"));
                        linhas.add(new String[]{m.algoritmo(), m.espacoTeorico(), Formatos.inteiro(m.bytesAlocados()), String.valueOf(m.bytesPorElemento())});
                    }
                    barrasMemoria.setItens(itens);
                    cMemoria.setDados(new String[]{"Algoritmo", "Espaço teórico", "Bytes alocados", "Bytes por elemento"}, () -> linhas);
                    cMemoria.estado(ChartCard.Estado.CONTEUDO);
                });
    }

    private void montarExterno() {
        spMemoria = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(500, 1_000_000, 2_000, 500));
        spMemoria.setEditable(true);
        spMemoria.setPrefWidth(120);
        Label rot = new Label("Registros por run");
        rot.getStyleClass().add("field-label");
        Button executar = new Button("Ordenar", Icones.de(Icones.EXECUTAR, 16));
        executar.getStyleClass().add("btn-secondary");
        executar.setOnAction(e -> executarExterno());
        HBox topo = new HBox(8, rot, spMemoria, executar);
        topo.setAlignment(Pos.CENTER_LEFT);
        gradeExterno.setHgap(24);
        gradeExterno.setVgap(8);
        Label ajuda = new Label("Usa os CSVs do Brasil em data/brasil (modo estruturas --brasil 2019-2024); sem eles, os CSVs de SP carregados.");
        ajuda.getStyleClass().add("t-small");
        ajuda.setWrapText(true);
        cExterno.conteudo(new VBox(14, topo, gradeExterno, ajuda));
        cExterno.estado(ChartCard.Estado.CONTEUDO);
    }

    private void executarExterno() {
        BaseDeFocos b = ctx.baseProperty().get();
        int m = spMemoria.getValue();
        ctx.executar("External Merge Sort", prog -> {
            List<Path> csvs = new ArrayList<>();
            Path brasil = Path.of("data", "brasil");
            if (Files.isDirectory(brasil)) {
                try (var s = Files.list(brasil)) {
                    s.filter(p -> p.getFileName().toString().endsWith(".csv")).forEach(csvs::add);
                }
            }
            if (csvs.isEmpty()) csvs.addAll(b != null && !ctx.sessao().dadosEmbarcados() ? b.getFontes() : RepositorioDados.extrairEmbarcados());
            Ordenacoes.ordenar(csvs, Comparator.comparing(p -> p.getFileName().toString()));
            Path saida = Files.createTempFile("aps-ordenado", ".csv");
            try {
                return new ExternalMergeSort().ordenar(csvs, saida, m, msg -> prog.accept(-1.0, msg));
            } finally {
                Files.deleteIfExists(saida);
            }
        }, this::exibirExterno);
    }

    private void executarExternoFundo() {
        int m = spMemoria.getValue();
        ctx.executarEmSegundoPlano("External Merge Sort (SP)", () -> {
            List<Path> csvs = RepositorioDados.extrairEmbarcados();
            Path saida = Files.createTempFile("aps-ordenado", ".csv");
            try {
                return new ExternalMergeSort().ordenar(csvs, saida, m, null);
            } finally {
                Files.deleteIfExists(saida);
            }
        }, this::exibirExterno);
    }

    private void medirParaleloFundo() {
        BaseDeFocos b = ctx.baseProperty().get();
        if (b == null) return;
        ctx.executarEmSegundoPlano("Merge Sort paralelo", () -> medirSpeedup(b, 2), this::exibirParalelo);
    }

    private static List<MergeSortParalelo.Medicao> medirSpeedup(BaseDeFocos b, int repeticoes) {
        List<FocoIncendio> f = b.getFocos();
        Long[] chaves = new Long[f.size() * 32];
        for (int r = 0; r < 32; r++) {
            for (int i = 0; i < f.size(); i++) {
                chaves[r * f.size() + i] = f.get(i).getDataHora().toEpochSecond(ZoneOffset.UTC) + r * 17L;
            }
        }
        int nucleos = Runtime.getRuntime().availableProcessors();
        List<Integer> ts = new ArrayList<>();
        for (int p = 1; p <= nucleos; p *= 2) ts.add(p);
        if (ts.get(ts.size() - 1) != nucleos) ts.add(nucleos);
        return MergeSortParalelo.speedup(chaves, Comparator.naturalOrder(), ts.stream().mapToInt(Integer::intValue).toArray(), 1, repeticoes);
    }

    private void exibirExterno(ExternalMergeSort.Resultado r) {
        gradeExterno.getChildren().clear();
        String[][] linhas = {
                {"Registros ordenados", Formatos.inteiro(r.registros()) + " de " + r.arquivos() + " arquivo(s), " + Formatos.bytes(r.bytesEntrada())},
                {"Limite de memória", Formatos.inteiro(r.registrosPorRun()) + " registros por run"},
                {"Runs em disco", String.valueOf(r.runs())},
                {"Fase 1: Merge Sort de cada run", Formatos.inteiro(r.fase1().comparacoes()) + " comparações · " + Formatos.duracao(r.nanosFase1())},
                {"Fase 2: intercalação " + r.runs() + "-way (heap)", Formatos.inteiro(r.fase2().comparacoes()) + " comparações · " + Formatos.duracao(r.nanosFase2())},
                {"Pico de heap observado", Formatos.bytes(r.memoriaPicoBytes())},
                {"Saída em ordem", r.ordenado() ? "sim" : "não"}};
        for (int i = 0; i < linhas.length; i++) {
            Label k = new Label(linhas[i][0]);
            k.getStyleClass().add("field-label");
            Label v = new Label(linhas[i][1]);
            v.getStyleClass().add("t-body");
            gradeExterno.addRow(i, k, v);
        }
    }

    private void montarParalelo() {
        NumberAxis x = (NumberAxis) grafParalelo.getXAxis(), y = (NumberAxis) grafParalelo.getYAxis();
        x.setLabel("threads");
        y.setLabel("speedup");
        x.setForceZeroInRange(false);
        grafParalelo.setAnimated(false);
        grafParalelo.setLegendVisible(false);
        grafParalelo.setCreateSymbols(true);
        Button medir = new Button("Medir speedup", Icones.de(Icones.EXECUTAR, 16));
        medir.getStyleClass().add("btn-secondary");
        medir.setOnAction(e -> medirParalelo());
        lblParalelo.getStyleClass().add("t-small");
        lblParalelo.setWrapText(true);
        lblParalelo.setMaxWidth(Double.MAX_VALUE);
        lblParalelo.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        cParalelo.setExtra(medir);
        cParalelo.conteudo(new VBox(10, grafParalelo, lblParalelo));
        cParalelo.adicionarLegenda("medido", "serie-2", "linha");
        cParalelo.adicionarLegenda("ideal (S = p)", "tracejada");
        cParalelo.estado(ChartCard.Estado.CONTEUDO);
        lblParalelo.setText("Medindo o tempo com 1, 2, 4… threads sobre 332 mil datas (a base de SP repetida 32 vezes)…");
    }

    private void medirParalelo() {
        BaseDeFocos b = ctx.baseProperty().get();
        if (b == null) return;
        ctx.executar("Merge Sort paralelo", () -> medirSpeedup(b, 3), this::exibirParalelo);
    }

    private void exibirParalelo(List<MergeSortParalelo.Medicao> r) {
        grafParalelo.getData().clear();
        XYChart.Series<Number, Number> medido = new XYChart.Series<>(), ideal = new XYChart.Series<>();
        List<String[]> linhas = new ArrayList<>();
        for (MergeSortParalelo.Medicao m : r) {
            medido.getData().add(new XYChart.Data<>(m.threads(), m.speedup()));
            ideal.getData().add(new XYChart.Data<>(m.threads(), m.threads()));
            linhas.add(new String[]{String.valueOf(m.threads()), Formatos.decimal(m.mediaMs(), 1) + " ms", Formatos.decimal(m.speedup(), 2) + "×",
                    Formatos.decimal(100 * m.eficiencia(), 0) + "%", m.threads() == 1 ? "—" : Formatos.decimal(m.fracaoSerial(), 3)});
        }
        grafParalelo.getData().add(ideal);
        grafParalelo.getData().add(medido);
        Graficos.classe(ideal, "serie-teorica");
        Graficos.classe(medido, "serie-2");
        Graficos.tooltips(medido, d -> d.getXValue() + " threads: speedup " + Formatos.decimal(d.getYValue().doubleValue(), 2) + "×");
        MergeSortParalelo.Medicao ultima = r.get(r.size() - 1);
        lblParalelo.setText("Com " + ultima.threads() + " threads: " + Formatos.decimal(ultima.speedup(), 2) + "× mais rápido; fração serial estimada (Karp–Flatt) "
                + Formatos.decimal(ultima.fracaoSerial(), 2) + ". Pela Lei de Amdahl, o ganho máximo seria 1 / fração serial ≈ "
                + Formatos.decimal(1 / Math.max(1e-9, ultima.fracaoSerial()), 1) + "×: as intercalações dos níveis de cima são sequenciais.");
        cParalelo.setDados(new String[]{"Threads", "Tempo médio", "Speedup", "Eficiência", "Fração serial"}, () -> linhas);
    }

    @Override
    public void demonstrar(Runnable concluido) {
        aoExibir();
        for (int i = 0; i < 15; i++) inserirProximo();
        concluido.run();
    }
}
