package br.unip.aps.ui;

import br.unip.aps.geo.MalhaMunicipal;
import br.unip.aps.geo.PerfilMunicipio;
import br.unip.aps.ml.ClusterizacaoHotspots;
import br.unip.aps.ml.Preditor;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.ui.componentes.ComparacaoMunicipios;
import br.unip.aps.ui.componentes.Feedback;
import br.unip.aps.ui.componentes.FichaMunicipio;
import br.unip.aps.ui.componentes.Icones;
import br.unip.aps.ui.componentes.Layout;
import br.unip.aps.ui.componentes.LinhaDoTempo;
import br.unip.aps.ui.tema.GerenciadorTema;
import br.unip.aps.util.Formatos;
import br.unip.aps.util.Json;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Transform;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** Tela Mapa. */
public class MapaController implements Pagina.Controlador {
    private static final Logger LOG = Logger.getLogger(MapaController.class.getName());

    private final UiContexto ctx;

    @FXML private ToggleButton tgPontos, tgCluster, tgCalor, tgMunicipios, tgBioma, tgAno, tgHotspots;
    @FXML private ToggleButton tgDensidade, tgContagem, tgJenks, tgQuantis;
    @FXML private javafx.scene.layout.HBox boxCoro;
    @FXML private Label lblContagem, lblContagemTexto;
    @FXML private StackPane moldura;
    @FXML private WebView webView;
    @FXML private javafx.scene.layout.HBox controles;

    private final java.util.prefs.Preferences prefs = preferencias();
    private br.unip.aps.ui.componentes.SeletorMapaBase seletorBase;
    private final LinhaDoTempo linha = new LinhaDoTempo();
    private final FichaMunicipio ficha = new FichaMunicipio();
    private final ComparacaoMunicipios comparacao = new ComparacaoMunicipios();
    private boolean gravando;
    private final Button btnExportar = new Button("PNG");

    /** Escala da imagem exportada: o dobro dos pixels da tela, bom para impressao. */
    static final double ESCALA_PNG = 2;

    private WebEngine engine;
    private boolean paginaPronta;
    private boolean pendente = true;
    private boolean visivel;
    private boolean modoOffline;

    public MapaController(UiContexto ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(moldura.widthProperty());
        clip.heightProperty().bind(moldura.heightProperty());
        moldura.setClip(clip);

        ToggleGroup modo = new ToggleGroup(), cor = new ToggleGroup();
        for (ToggleButton t : List.of(tgPontos, tgCluster, tgCalor, tgMunicipios)) t.setToggleGroup(modo);
        ToggleGroup medida = new ToggleGroup(), classes = new ToggleGroup();
        tgDensidade.setToggleGroup(medida);
        tgContagem.setToggleGroup(medida);
        tgJenks.setToggleGroup(classes);
        tgQuantis.setToggleGroup(classes);
        for (ToggleGroup g : List.of(medida, classes)) {
            g.selectedToggleProperty().addListener((o, a, n) -> {
                if (n == null) a.setSelected(true);
                else enviarCoropletico();
            });
        }
        tgMunicipios.setGraphic(Icones.de(Icones.MAPA, 15));
        tgDensidade.setTooltip(new javafx.scene.control.Tooltip("Focos por 1.000 km² de área do município (IBGE): compara municípios de tamanhos diferentes"));
        tgJenks.setTooltip(new javafx.scene.control.Tooltip("Quebras naturais de Jenks: classes que minimizam a variância interna"));
        tgQuantis.setTooltip(new javafx.scene.control.Tooltip("Quantis: cada classe com o mesmo número de municípios"));
        for (ToggleButton t : List.of(tgBioma, tgAno)) t.setToggleGroup(cor);
        tgPontos.setGraphic(Icones.de(Icones.PONTOS, 15));
        tgCluster.setGraphic(Icones.de(Icones.CAMADAS, 15));
        tgCalor.setGraphic(Icones.de(Icones.MARCA, 15));
        tgHotspots.setGraphic(Icones.de(Icones.ALVO, 15));
        modo.selectedToggleProperty().addListener((o, a, n) -> {
            if (n == null) a.setSelected(true);
            else {
                boolean coro = n == tgMunicipios;
                boxCoro.setVisible(coro);
                boxCoro.setManaged(coro);
                if (coro) enviarCoropletico();
                js("APS.setModo('" + (n == tgCluster ? "cluster" : n == tgCalor ? "calor" : coro ? "coropletico" : "pontos") + "')");
            }
        });
        cor.selectedToggleProperty().addListener((o, a, n) -> {
            if (n == null) a.setSelected(true);
            else js("APS.setCorPor('" + (n == tgAno ? "ano" : "bioma") + "')");
        });

        seletorBase = new br.unip.aps.ui.componentes.SeletorMapaBase(prefs.get("mapaBase", "neutro"));
        controles.getChildren().add(0, seletorBase.botao());
        javafx.scene.layout.VBox painelBase = seletorBase.painel();
        StackPane.setAlignment(painelBase, javafx.geometry.Pos.TOP_RIGHT);
        StackPane.setMargin(painelBase, new javafx.geometry.Insets(78, 64, 0, 0));
        moldura.getChildren().add(painelBase);
        seletorBase.fecharAoClicarFora(moldura);
        seletorBase.selecionadaProperty().addListener((o, a, id) -> {
            prefs.put("mapaBase", id);
            js("APS.setBase('" + id + "')");
        });
        painelBase.visibleProperty().addListener((o, a, v) -> {
            if (v) ficha.fechar();
        });

        StackPane.setAlignment(linha, Pos.BOTTOM_LEFT);
        StackPane.setMargin(linha, new Insets(0, 0, 26, 20));
        linha.setOnMudar((de, ate) -> {
            enviarPeriodo();
            atualizarContagem();
            enviarCoropletico();
        });
        StackPane.setAlignment(ficha, Pos.TOP_RIGHT);
        StackPane.setMargin(ficha, new Insets(78, 16, 118, 0));
        ficha.setMaxHeight(Double.MAX_VALUE);
        ficha.setOnMes(linha::setPeriodo);
        ficha.setOnVerNoMapa(p -> js("APS.destacar(" + Json.texto(chaveIbge(p)) + ", true)"));
        ficha.visibleProperty().addListener((o, a, v) -> {
            if (!v && !comparacao.isVisible()) js("APS.destacar(null)");
        });
        ficha.setOnComparar((p, outro) -> comparar(p.nomeInpe(), outro));
        StackPane.setAlignment(comparacao, Pos.TOP_RIGHT);
        StackPane.setMargin(comparacao, new Insets(78, 16, 118, 0));
        comparacao.setMaxHeight(Double.MAX_VALUE);
        comparacao.setOnVerNoMapa((a, b) -> js("APS.destacar([" + Json.texto(chaveIbge(a)) + "," + Json.texto(chaveIbge(b)) + "], true)"));
        comparacao.setOnVoltar(p -> abrirFicha(p.nomeInpe()));
        comparacao.visibleProperty().addListener((o, a, v) -> {
            if (!v && !ficha.isVisible()) js("APS.destacar(null)");
        });
        linha.setOnGravar(this::escolherArquivoGif);
        moldura.getChildren().addAll(linha, ficha, comparacao);

        btnExportar.setId("btnExportarMapa");
        btnExportar.getStyleClass().add("btn-secondary");
        btnExportar.setGraphic(Icones.de(Icones.IMAGEM, 15));
        btnExportar.setTooltip(new Tooltip("Salvar o mapa como imagem PNG em alta resolução (2×), sem os controles"));
        btnExportar.setAccessibleText("Exportar o mapa em PNG");
        btnExportar.setOnAction(e -> escolherArquivoPng());
        Layout.naoEncolher(btnExportar);
        controles.getChildren().add(btnExportar);

        engine = webView.getEngine();
        engine.setUserAgent(engine.getUserAgent() + " APS-Queimadas-UNIP/1.0");
        engine.setOnAlert(e -> {
            String msg = e.getData();
            if (msg != null && msg.startsWith("FICHA:")) {
                String nome = msg.substring(6).strip();
                if (!nome.isEmpty() && nome.length() <= 80) abrirFicha(nome);
                return;
            }
            LOG.info("mapa: " + msg);
            if (msg != null && msg.startsWith("OFFLINE")) {
                modoOffline = true;
                seletorBase.setOffline(true);
            }
        });
        engine.getLoadWorker().stateProperty().addListener((o, a, s) -> {
            if (s == Worker.State.SUCCEEDED) {
                paginaPronta = true;
                js("APS.setMalha(" + br.unip.aps.geo.MalhaMunicipal.sp().geojson() + ")");
                if (Boolean.getBoolean("aps.mapa.offline")) js("APS.forcarOffline()");
                aplicarTema();
                js("APS.setDaltonico(" + GerenciadorTema.get().daltonicoProperty().get() + ")");
                js("APS.setBase('" + seletorBase.selecionadaProperty().get() + "')");
                if (visivel) enviarFocos();
                enviarPeriodo();
                enviarHotspots();
                enviarCirculo();
            } else if (s == Worker.State.FAILED) {
                indisponivel();
            }
        });
        engine.load(getClass().getResource("/br/unip/aps/ui/mapa.html").toExternalForm());

        ctx.focosFiltradosProperty().addListener((o, a, n) -> {
            pendente = true;
            if (visivel) enviarFocos();
        });
        ctx.mlProperty().addListener((o, a, r) -> {
            tgHotspots.setDisable(r == null);
            tgHotspots.setSelected(r != null);
            enviarHotspots();
        });
        tgHotspots.selectedProperty().addListener((o, a, n) -> enviarHotspots());
        ctx.circuloMapaProperty().addListener((o, a, n) -> enviarCirculo());
        GerenciadorTema.get().temaProperty().addListener((o, a, n) -> aplicarTema());
        GerenciadorTema.get().daltonicoProperty().addListener((o, a, n) -> js("APS.setDaltonico(" + n + ")"));
    }

    private static java.util.prefs.Preferences preferencias() {
        try {
            return java.util.prefs.Preferences.userNodeForPackage(MapaController.class);
        } catch (SecurityException e) {
            return java.util.prefs.Preferences.userRoot().node("aps-queimadas-temp");
        }
    }

    /** Seletor de mapa-base (usado pela captura de telas e pelos testes de interacao). */
    br.unip.aps.ui.componentes.SeletorMapaBase seletorBase() {
        return seletorBase;
    }

    @Override
    public void aoExibir() {
        visivel = true;
        if (pendente) enviarFocos();
    }

    @Override
    public void aoOcultar() {
        visivel = false;
        seletorBase.fechar();
        comparacao.fechar();
        linha.pausar();
    }

    void forcarOffline() {
        js("APS.forcarOffline()");
    }

    LinhaDoTempo linhaDoTempo() {
        return linha;
    }

    FichaMunicipio ficha() {
        return ficha;
    }

    /** Abre a ficha do municipio (nome do INPE ou do IBGE); avisa se ele nao existe na base nem na malha. */
    void abrirFicha(String municipio) {
        BaseDeFocos b = ctx.baseProperty().get();
        if (b == null) return;
        PerfilMunicipio p;
        try {
            p = PerfilMunicipio.de(b.getFocos(), municipio);
        } catch (IllegalArgumentException e) {
            Feedback.info("Município não encontrado", "Não há dados de " + municipio + " na base carregada.");
            return;
        }
        seletorBase.fechar();
        comparacao.fechar();
        ficha.setMunicipios(nomesDosMunicipios(b));
        ficha.mostrar(p);
        js("APS.destacar(" + Json.texto(chaveIbge(p)) + ", false)");
    }

    ComparacaoMunicipios comparacao() {
        return comparacao;
    }

    /** Abre o painel com os dois municipios lado a lado e destaca os dois no mapa. */
    void comparar(String municipioA, String municipioB) {
        BaseDeFocos b = ctx.baseProperty().get();
        if (b == null) return;
        PerfilMunicipio pa, pb;
        try {
            pa = PerfilMunicipio.de(b.getFocos(), municipioA);
            pb = PerfilMunicipio.de(b.getFocos(), municipioB);
        } catch (IllegalArgumentException e) {
            Feedback.info("Município não encontrado", "Confira o nome e tente de novo.");
            return;
        }
        seletorBase.fechar();
        ficha.fechar();
        comparacao.mostrar(pa, pb);
        js("APS.destacar([" + Json.texto(chaveIbge(pa)) + "," + Json.texto(chaveIbge(pb)) + "], false)");
    }

    private static List<String> nomesDosMunicipios(BaseDeFocos b) {
        List<String> r = new ArrayList<>();
        for (String m : b.municipios()) {
            MalhaMunicipal.Municipio mm = MalhaMunicipal.sp().buscar(m);
            r.add(mm == null ? br.unip.aps.util.Textos.nomeProprio(m) : mm.nome());
        }
        return r;
    }

    private void escolherArquivoGif() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Salvar a linha do tempo em GIF");
        fc.setInitialFileName("linha-do-tempo-focos.gif");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("GIF animado", "*.gif"));
        File arq = fc.showSaveDialog(ctx.stage());
        if (arq != null) gravarLinhaDoTempo(arq.toPath(), p -> Feedback.sucesso("GIF gravado", p.toAbsolutePath().toString()));
    }

    /** Passa pelos meses, fotografa o mapa em cada um e grava tudo como GIF em segundo plano. */
    void gravarLinhaDoTempo(Path destino, java.util.function.Consumer<Path> aoTerminar) {
        List<YearMonth> meses = linha.meses();
        if (meses.isEmpty() || gravando) return;
        gravando = true;
        linha.pausar();
        YearMonth deAntes = linha.inicio(), ateAntes = linha.fim();
        List<Node> esconder = List.of(controles, ficha, comparacao, seletorBase.painel());
        List<Boolean> antes = new ArrayList<>();
        for (Node n : esconder) {
            antes.add(n.isVisible());
            n.setVisible(false);
        }
        List<java.awt.image.BufferedImage> quadros = new ArrayList<>();
        Runnable[] passo = new Runnable[1];
        passo[0] = () -> {
            int i = quadros.size();
            if (i >= meses.size()) {
                for (int k = 0; k < esconder.size(); k++) esconder.get(k).setVisible(antes.get(k));
                linha.setPeriodo(deAntes, ateAntes);
                linha.setGravacao("Salvando…");
                ctx.executar("Gravando a linha do tempo em GIF", () -> {
                    try {
                        return br.unip.aps.util.GifAnimado.gravar(quadros, 700, 2200, 1280, destino);
                    } catch (IOException e) {
                        LOG.warning("Falha ao gravar o GIF: " + e.getMessage());
                        return null;
                    }
                }, p -> {
                    gravando = false;
                    linha.setGravacao(null);
                    if (p == null) Feedback.erro("Não foi possível gravar o GIF", "Verifique se a pasta permite gravação.");
                    else aoTerminar.accept(p);
                });
                return;
            }
            linha.setGravacao((i + 1) + "/" + meses.size());
            linha.setPeriodo(meses.get(i), meses.get(i));
            javafx.animation.PauseTransition espera = new javafx.animation.PauseTransition(javafx.util.Duration.millis(380));
            espera.setOnFinished(e -> {
                quadros.add(SwingFXUtils.fromFXImage(moldura.snapshot(new SnapshotParameters(), null), null));
                passo[0].run();
            });
            espera.play();
        };
        passo[0].run();
    }

    private static String chaveIbge(PerfilMunicipio p) {
        MalhaMunicipal.Municipio m = MalhaMunicipal.sp().buscar(p.nome());
        return m == null ? MalhaMunicipal.chave(p.nome()) : m.chave();
    }

    private void escolherArquivoPng() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Salvar o mapa em PNG");
        fc.setInitialFileName("mapa-focos" + (linha.inicio() == null ? "" : "-" + linha.inicio()) + ".png");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagem PNG", "*.png"));
        File arq = fc.showSaveDialog(ctx.stage());
        if (arq == null) return;
        try {
            Path salvo = exportarPng(arq.toPath());
            Feedback.sucesso("Mapa exportado", salvo.toAbsolutePath().toString());
        } catch (IOException e) {
            Feedback.erro("Não foi possível salvar a imagem", "Verifique se a pasta permite gravação. Detalhe: " + e.getMessage());
        }
    }

    /** Grava o mapa em PNG no dobro da resolucao da tela, escondendo controles, linha do tempo e ficha. */
    Path exportarPng(Path destino) throws IOException {
        List<Node> esconder = List.of(controles, linha, ficha, comparacao, seletorBase.painel());
        List<Boolean> antes = new ArrayList<>();
        for (Node n : esconder) {
            antes.add(n.isVisible());
            n.setVisible(false);
        }
        WritableImage img;
        try {
            SnapshotParameters sp = new SnapshotParameters();
            sp.setTransform(Transform.scale(ESCALA_PNG, ESCALA_PNG));
            img = moldura.snapshot(sp, null);
        } finally {
            for (int i = 0; i < esconder.size(); i++) esconder.get(i).setVisible(antes.get(i));
        }
        Path pasta = destino.toAbsolutePath().getParent();
        if (pasta != null) java.nio.file.Files.createDirectories(pasta);
        ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", destino.toFile());
        return destino;
    }

    private void indisponivel() {
        lblContagem.setText("—");
        lblContagemTexto.setText("Mapa indisponível: verifique a internet");
    }

    boolean modoOffline() {
        return modoOffline;
    }

    void modo(String modo) {
        (modo.equals("cluster") ? tgCluster : modo.equals("calor") ? tgCalor : modo.equals("municipios") ? tgMunicipios : tgPontos).setSelected(true);
    }

    private void aplicarTema() {
        js("APS.setTema(" + GerenciadorTema.get().escuro() + ")");
        enviarCoropletico();
    }

    private void enviarFocos() {
        if (!paginaPronta) return;
        pendente = false;
        BaseDeFocos b = ctx.baseProperty().get();
        List<FocoIncendio> focos = ctx.focosFiltradosProperty().get();
        atualizarContagem();
        if (b == null) return;
        List<Integer> anos = b.anos();
        linha.setDados(serieMensal(anos, focos));
        js("APS.setFocos(" + json(b.biomas(), focos, anos.isEmpty() ? 0 : anos.get(anos.size() - 1)) + ")");
        enviarPeriodo();
        enviarCoropletico();
    }

    private static Map<YearMonth, Long> serieMensal(List<Integer> anos, List<FocoIncendio> focos) {
        Map<YearMonth, Long> serie = new LinkedHashMap<>();
        if (anos.isEmpty()) return serie;
        for (int a = anos.get(0); a <= anos.get(anos.size() - 1); a++) {
            for (int m = 1; m <= 12; m++) serie.put(YearMonth.of(a, m), 0L);
        }
        for (FocoIncendio f : focos) serie.merge(YearMonth.of(f.getAno(), f.getMes()), 1L, Long::sum);
        return serie;
    }

    private List<FocoIncendio> focosNoPeriodo() {
        List<FocoIncendio> focos = ctx.focosFiltradosProperty().get();
        YearMonth de = linha.inicio(), ate = linha.fim();
        if (de == null) return focos;
        List<FocoIncendio> r = new ArrayList<>();
        for (FocoIncendio f : focos) {
            YearMonth ym = YearMonth.of(f.getAno(), f.getMes());
            if (!ym.isBefore(de) && !ym.isAfter(ate)) r.add(f);
        }
        return r;
    }

    private void atualizarContagem() {
        int n = focosNoPeriodo().size();
        lblContagem.setText(Formatos.inteiro(n));
        YearMonth de = linha.inicio(), ate = linha.fim();
        String quando = de == null ? "no mapa" : de.equals(ate) ? "em " + LinhaDoTempo.rotulo(de, ate)
                : "de " + LinhaDoTempo.rotulo(de, de) + " a " + LinhaDoTempo.rotulo(ate, ate);
        lblContagemTexto.setText((n == 1 ? "foco " : "focos ") + quando);
    }

    private void enviarPeriodo() {
        YearMonth de = linha.inicio(), ate = linha.fim();
        if (de == null) js("APS.setPeriodo(null)");
        else js("APS.setPeriodo(" + chaveMes(de) + "," + chaveMes(ate) + "," + Json.texto(LinhaDoTempo.rotulo(de, ate)) + ")");
    }

    private static int chaveMes(YearMonth ym) {
        return ym.getYear() * 100 + ym.getMonthValue();
    }

    private static final String[] CORES_ESCURO = {"#420A68", "#932667", "#DD513A", "#FCA50A", "#FCFFA4"};
    private static final String[] CORES_CLARO = {"#F6C26B", "#E0691C", "#A8263B", "#6E1B5E", "#2E0A4F"};

    private void enviarCoropletico() {
        if (!paginaPronta || tgMunicipios == null || !tgMunicipios.isSelected()) return;
        br.unip.aps.geo.MalhaMunicipal malha = br.unip.aps.geo.MalhaMunicipal.sp();
        java.util.Map<String, Integer> contagem = new java.util.HashMap<>();
        for (FocoIncendio f : focosNoPeriodo()) {
            contagem.merge(br.unip.aps.geo.MalhaMunicipal.chave(f.getMunicipio()), 1, Integer::sum);
        }
        boolean densidade = tgDensidade.isSelected();
        java.util.Map<String, Double> valores = new java.util.LinkedHashMap<>();
        for (java.util.Map.Entry<String, Integer> e : contagem.entrySet()) {
            br.unip.aps.geo.MalhaMunicipal.Municipio m = malha.buscar(e.getKey());
            if (m == null) continue;
            valores.put(e.getKey(), densidade ? e.getValue() * 1000.0 / m.areaKm2() : e.getValue());
        }
        double[] v = valores.values().stream().mapToDouble(Double::doubleValue).toArray();
        br.unip.aps.analysis.Classificacao.Metodo metodo = tgJenks.isSelected()
                ? br.unip.aps.analysis.Classificacao.Metodo.JENKS : br.unip.aps.analysis.Classificacao.Metodo.QUANTIS;
        double[] lim = br.unip.aps.analysis.Classificacao.limites(v, 5, metodo);
        String[] paleta = GerenciadorTema.get().escuro() ? CORES_ESCURO : CORES_CLARO;
        int desloc = paleta.length - lim.length;
        StringBuilder sb = new StringBuilder("{\"titulo\":").append(Json.texto(densidade ? "Focos por 1.000 km²" : "Focos por município"));
        sb.append(",\"metodo\":").append(Json.texto((metodo == br.unip.aps.analysis.Classificacao.Metodo.JENKS
                ? "Quebras naturais (Jenks)" : "Quantis") + " · sem cor = sem focos · área: IBGE"));
        sb.append(",\"cores\":[");
        for (int i = 0; i < lim.length; i++) sb.append(i > 0 ? "," : "").append(Json.texto(paleta[i + desloc]));
        sb.append("],\"rotulos\":[");
        double anterior = minimo(v);
        for (int i = 0; i < lim.length; i++) {
            String de = densidade ? Formatos.decimal(anterior, 1) : Formatos.inteiro(Math.round(i == 0 ? anterior : anterior + 1));
            String ate = densidade ? Formatos.decimal(lim[i], 1) : Formatos.inteiro(Math.round(lim[i]));
            sb.append(i > 0 ? "," : "").append(Json.texto(de.equals(ate) ? ate : de + " – " + ate));
            anterior = lim[i];
        }
        sb.append("],\"valores\":{");
        StringBuilder classes = new StringBuilder("{"), formatos = new StringBuilder("{");
        boolean primeiro = true;
        for (java.util.Map.Entry<String, Double> e : valores.entrySet()) {
            String k = Json.texto(e.getKey());
            String sep = primeiro ? "" : ",";
            int n = contagem.get(e.getKey());
            sb.append(sep).append(k).append(':').append(Json.numero(e.getValue(), 4));
            classes.append(sep).append(k).append(':').append(br.unip.aps.analysis.Classificacao.classe(e.getValue(), lim));
            formatos.append(sep).append(k).append(':').append(Json.texto(Formatos.inteiro(n) + (n == 1 ? " foco" : " focos")
                    + (densidade ? " · " + Formatos.decimal(e.getValue(), 1) + " por 1.000 km²" : "")));
            primeiro = false;
        }
        sb.append("},\"classeDe\":").append(classes).append("},\"formatar\":").append(formatos).append("}}");
        js("APS.setCoropletico(" + sb + ")");
    }

    private static double minimo(double[] v) {
        double m = Double.MAX_VALUE;
        for (double x : v) m = Math.min(m, x);
        return v.length == 0 ? 0 : m;
    }

    private static String json(List<String> biomas, List<FocoIncendio> focos, int anoRecente) {
        StringBuilder sb = new StringBuilder(focos.size() * 72 + 120);
        sb.append("{\"anoRecente\":").append(anoRecente).append(",\"biomas\":[");
        for (int i = 0; i < biomas.size(); i++) sb.append(i > 0 ? "," : "").append(Json.texto(biomas.get(i)));
        sb.append("],\"focos\":[");
        boolean primeiro = true;
        for (FocoIncendio f : focos) {
            if (!primeiro) sb.append(',');
            primeiro = false;
            sb.append('[').append(Json.numero(f.getLatitude(), 5)).append(',').append(Json.numero(f.getLongitude(), 5))
                    .append(',').append(Math.max(0, biomas.indexOf(f.getBioma())))
                    .append(',').append(Json.texto(f.getMunicipio()))
                    .append(',').append(Json.texto(f.getDataHora().format(Formatos.DATA_HORA)))
                    .append(',').append(f.getAno())
                    .append(',').append(f.getAno() * 100 + f.getMes()).append(']');
        }
        return sb.append("]}").toString();
    }

    private void enviarHotspots() {
        if (!paginaPronta) return;
        Preditor.ResultadoML r = ctx.mlProperty().get();
        if (r == null || !tgHotspots.isSelected()) {
            js("APS.setHotspots([])");
            return;
        }
        StringBuilder sb = new StringBuilder("[");
        boolean primeiro = true;
        for (ClusterizacaoHotspots.Hotspot h : r.dbscan().hotspots()) {
            if (!primeiro) sb.append(',');
            primeiro = false;
            sb.append('[').append(Json.numero(h.latitude(), 5)).append(',').append(Json.numero(h.longitude(), 5)).append(',')
                    .append(h.focos()).append(',').append(Json.numero(h.raioKm(), 2)).append(',').append(h.id()).append(',')
                    .append(Json.texto(h.municipioPrincipal())).append(',').append(Json.texto(h.biomaPredominante())).append(']');
        }
        js("APS.setHotspots(" + sb.append(']') + ")");
    }

    private void enviarCirculo() {
        double[] c = ctx.circuloMapaProperty().get();
        if (c == null) {
            js("APS.setCirculo(null)");
            return;
        }
        js("APS.setCirculo({lat:" + Json.numero(c[0], 5) + ",lon:" + Json.numero(c[1], 5) + ",raioKm:" + Json.numero(c[2], 2)
                + ",rotulo:" + Json.texto(ctx.rotuloCirculo()) + "})");
    }

    private void js(String codigo) {
        if (!paginaPronta) return;
        try {
            engine.executeScript("window.APS && " + codigo + ";");
        } catch (RuntimeException e) {
            LOG.warning("Falha ao executar script no mapa: " + e.getMessage());
            indisponivel();
        }
    }
}
