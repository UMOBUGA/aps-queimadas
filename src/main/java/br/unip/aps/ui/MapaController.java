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

/**
 * Tela "Mapa": focos sobre mapa Leaflet (WebView) com mapas-base CARTO claro/escuro que
 * acompanham o tema, modos Pontos / Agrupado (cluster) / Calor (heatmap), cor por bioma ou ano e
 * sobreposicao dos hotspots do DBSCAN. Os focos exibidos seguem o filtro global.
 *
 * <p>Comunicacao Java → JavaScript via {@link WebEngine#executeScript} ({@code window.APS}).</p>
 */
public class MapaController implements Pagina.Controlador {

    private static final Logger LOG = Logger.getLogger(MapaController.class.getName());

    private final UiContexto ctx;

    @FXML private ToggleButton tgPontos, tgCluster, tgCalor, tgBioma, tgAno, tgHotspots;
    @FXML private Label lblContagem;
    @FXML private StackPane moldura;
    @FXML private WebView webView;

    private WebEngine engine;
    private boolean paginaPronta;
    private boolean pendente = true;
    private boolean visivel;

    /** @param ctx contexto injetado */
    public MapaController(UiContexto ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        moldura.getStyleClass().add("map-frame");
        Rectangle clip = new Rectangle();
        clip.setArcWidth(28);
        clip.setArcHeight(28);
        clip.widthProperty().bind(moldura.widthProperty());
        clip.heightProperty().bind(moldura.heightProperty());
        moldura.setClip(clip);

        ToggleGroup modo = new ToggleGroup(), cor = new ToggleGroup();
        for (ToggleButton t : List.of(tgPontos, tgCluster, tgCalor)) t.setToggleGroup(modo);
        for (ToggleButton t : List.of(tgBioma, tgAno)) t.setToggleGroup(cor);
        tgPontos.setGraphic(Icones.de(Icones.PONTOS, 15));
        tgCluster.setGraphic(Icones.de(Icones.CAMADAS, 15));
        tgCalor.setGraphic(Icones.de(Icones.MARCA, 15));
        tgHotspots.setGraphic(Icones.de(Icones.ALVO, 15));
        lblContagem.setGraphic(Icones.de(Icones.MARCA, 14));
        modo.selectedToggleProperty().addListener((o, a, n) -> {
            if (n == null) a.setSelected(true);
            else js("APS.setModo('" + (n == tgCluster ? "cluster" : n == tgCalor ? "calor" : "pontos") + "')");
        });
        cor.selectedToggleProperty().addListener((o, a, n) -> {
            if (n == null) a.setSelected(true);
            else js("APS.setCorPor('" + (n == tgAno ? "ano" : "bioma") + "')");
        });

        engine = webView.getEngine();
        engine.setUserAgent(engine.getUserAgent() + " APS-Queimadas-UNIP/1.0");
        engine.setOnAlert(e -> LOG.info("mapa: " + e.getData()));
        engine.getLoadWorker().stateProperty().addListener((o, a, s) -> {
            if (s == Worker.State.SUCCEEDED) {
                paginaPronta = true;
                aplicarTema();
                if (visivel) enviarFocos();
                enviarHotspots();
            } else if (s == Worker.State.FAILED) {
                lblContagem.setText("Mapa indisponível (sem internet?)");
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
        GerenciadorTema.get().temaProperty().addListener((o, a, n) -> aplicarTema());
    }

    @Override
    public void aoExibir() {
        visivel = true;
        if (pendente) enviarFocos();
    }

    @Override
    public void aoOcultar() {
        visivel = false;
    }

    /**
     * @param modo "pontos", "cluster" ou "calor" (demonstracoes e capturas)
     */
    void modo(String modo) {
        (modo.equals("cluster") ? tgCluster : modo.equals("calor") ? tgCalor : tgPontos).setSelected(true);
    }

    private void aplicarTema() {
        js("APS.setTema(" + GerenciadorTema.get().escuro() + ")");
    }

    private void enviarFocos() {
        if (!paginaPronta) return;
        pendente = false;
        BaseDeFocos b = ctx.baseProperty().get();
        List<FocoIncendio> focos = ctx.focosFiltradosProperty().get();
        lblContagem.setText(Formatos.inteiro(focos.size()) + " focos no mapa");
        if (b == null) return;
        List<Integer> anos = b.anos();
        js("APS.setFocos(" + json(b.biomas(), focos, anos.isEmpty() ? 0 : anos.get(anos.size() - 1)) + ")");
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

    private void js(String codigo) {
        if (!paginaPronta) return;
        try {
            engine.executeScript("window.APS && " + codigo + ";");
        } catch (RuntimeException e) {
            LOG.warning("Falha ao executar script no mapa: " + e.getMessage());
            lblContagem.setText("Mapa indisponível (verifique a internet)");
        }
    }
}
