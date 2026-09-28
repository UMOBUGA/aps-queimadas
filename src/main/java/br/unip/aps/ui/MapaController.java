package br.unip.aps.ui;

import br.unip.aps.ml.ClusterizacaoHotspots;
import br.unip.aps.ml.Preditor;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.ui.componentes.Icones;
import br.unip.aps.ui.tema.GerenciadorTema;
import br.unip.aps.util.Formatos;
import br.unip.aps.util.Json;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.util.List;
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

        engine = webView.getEngine();
        engine.setUserAgent(engine.getUserAgent() + " APS-Queimadas-UNIP/1.0");
        engine.setOnAlert(e -> {
            LOG.info("mapa: " + e.getData());
            if (e.getData() != null && e.getData().startsWith("OFFLINE")) {
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
        lblContagem.setText(Formatos.inteiro(focos.size()));
        lblContagemTexto.setText(focos.size() == 1 ? "foco no mapa" : "focos no mapa");
        if (b == null) return;
        List<Integer> anos = b.anos();
        js("APS.setFocos(" + json(b.biomas(), focos, anos.isEmpty() ? 0 : anos.get(anos.size() - 1)) + ")");
        enviarCoropletico();
    }

    private static final String[] CORES_ESCURO = {"#420A68", "#932667", "#DD513A", "#FCA50A", "#FCFFA4"};
    private static final String[] CORES_CLARO = {"#F6C26B", "#E0691C", "#A8263B", "#6E1B5E", "#2E0A4F"};

    private void enviarCoropletico() {
        if (!paginaPronta || tgMunicipios == null || !tgMunicipios.isSelected()) return;
        br.unip.aps.geo.MalhaMunicipal malha = br.unip.aps.geo.MalhaMunicipal.sp();
        java.util.Map<String, Integer> contagem = new java.util.HashMap<>();
        for (FocoIncendio f : ctx.focosFiltradosProperty().get()) {
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
                    .append(',').append(f.getAno()).append(']');
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
