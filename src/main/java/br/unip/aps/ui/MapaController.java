package br.unip.aps.ui;

import br.unip.aps.analysis.FiltroFocos;
import br.unip.aps.ml.ClusterizacaoHotspots;
import br.unip.aps.ml.Preditor;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.util.Formatos;
import br.unip.aps.util.Json;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Aba "Mapa": focos sobre mapa Leaflet/OpenStreetMap (WebView), com filtros por ano, bioma,
 * municipio e periodo, e sobreposicao dos hotspots encontrados pelo DBSCAN.
 *
 * <p>Comunicacao Java → JavaScript: os focos filtrados sao serializados em JSON e passados a
 * {@code window.APS.setFocos(...)} via {@link WebEngine#executeScript}.</p>
 */
public class MapaController {

    private static final Logger LOG = Logger.getLogger(MapaController.class.getName());
    private static final String TODOS = "(todos)";

    private final UiContexto ctx;

    @FXML private HBox boxAnos;
    @FXML private ComboBox<String> cbBioma;
    @FXML private TextField tfMunicipio;
    @FXML private DatePicker dpInicio, dpFim;
    @FXML private CheckBox chkHotspots;
    @FXML private Label lblContagem;
    @FXML private WebView webView;

    private WebEngine engine;
    private boolean paginaPronta;
    private String pendente;
    private final List<CheckBox> chkAnos = new ArrayList<>();

    /** @param ctx contexto injetado */
    public MapaController(UiContexto ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        engine = webView.getEngine();
        engine.setUserAgent(engine.getUserAgent() + " APS-Queimadas-UNIP/1.0");
        engine.setOnAlert(e -> LOG.info("mapa: " + e.getData()));
        engine.getLoadWorker().stateProperty().addListener((o, a, s) -> {
            if (s == Worker.State.SUCCEEDED) {
                paginaPronta = true;
                if (pendente != null) {
                    executarJs(pendente);
                    pendente = null;
                }
                enviarHotspots();
            } else if (s == Worker.State.FAILED) {
                lblContagem.setText("Falha ao carregar o mapa (sem internet?)");
            }
        });
        engine.load(getClass().getResource("/br/unip/aps/ui/mapa.html").toExternalForm());

        ctx.baseProperty().addListener((o, a, b) -> prepararFiltros(b));
        ctx.mlProperty().addListener((o, a, r) -> {
            chkHotspots.setDisable(r == null);
            chkHotspots.setSelected(r != null);
            enviarHotspots();
        });
        chkHotspots.selectedProperty().addListener((o, a, n) -> enviarHotspots());
    }

    private void prepararFiltros(BaseDeFocos b) {
        boxAnos.getChildren().clear();
        chkAnos.clear();
        List<String> biomas = new ArrayList<>(List.of(TODOS));
        if (b != null) {
            for (Integer ano : b.anos()) {
                CheckBox c = new CheckBox(String.valueOf(ano));
                c.setSelected(true);
                chkAnos.add(c);
                boxAnos.getChildren().add(c);
            }
            biomas.addAll(b.biomas());
        }
        cbBioma.setItems(FXCollections.observableArrayList(biomas));
        cbBioma.getSelectionModel().selectFirst();
        aplicar();
    }

    @FXML
    private void limpar() {
        chkAnos.forEach(c -> c.setSelected(true));
        cbBioma.getSelectionModel().selectFirst();
        tfMunicipio.clear();
        dpInicio.setValue(null);
        dpFim.setValue(null);
        aplicar();
    }

    @FXML
    private void aplicar() {
        BaseDeFocos b = ctx.baseProperty().get();
        if (b == null) return;
        FiltroFocos filtro;
        try {
            Set<Integer> anos = new HashSet<>();
            for (CheckBox c : chkAnos) if (c.isSelected()) anos.add(Integer.parseInt(c.getText()));
            if (anos.isEmpty()) {
                ctx.aviso("Filtro vazio", "Selecione ao menos um ano.");
                return;
            }
            String bioma = cbBioma.getValue();
            filtro = new FiltroFocos(anos, bioma == null || bioma.equals(TODOS) ? Set.of() : Set.of(bioma),
                    tfMunicipio.getText(), dpInicio.getValue(), dpFim.getValue());
        } catch (IllegalArgumentException e) {
            ctx.erro("Filtro inválido", e);
            return;
        }
        List<FocoIncendio> focos = b.filtrar(filtro);
        lblContagem.setText(Formatos.inteiro(focos.size()) + " focos no mapa");
        if (focos.isEmpty()) {
            ctx.aviso("Filtro vazio", "Nenhum foco atende ao filtro: " + filtro.descricao());
        }
        enviar("window.APS && APS.setFocos(" + json(b.biomas(), focos) + ");");
        if (!paginaPronta && engine.getLoadWorker().getState() == Worker.State.FAILED) {
            engine.reload();
        }
    }

    private static String json(List<String> biomas, List<FocoIncendio> focos) {
        StringBuilder sb = new StringBuilder(focos.size() * 70 + 100);
        sb.append("{\"biomas\":[");
        for (int i = 0; i < biomas.size(); i++) sb.append(i > 0 ? "," : "").append(Json.texto(biomas.get(i)));
        sb.append("],\"focos\":[");
        boolean primeiro = true;
        for (FocoIncendio f : focos) {
            if (!primeiro) sb.append(',');
            primeiro = false;
            sb.append('[').append(Json.numero(f.getLatitude(), 5)).append(',').append(Json.numero(f.getLongitude(), 5))
                    .append(',').append(Math.max(0, biomas.indexOf(f.getBioma())))
                    .append(',').append(Json.texto(f.getMunicipio()))
                    .append(',').append(Json.texto(f.getDataHora().format(Formatos.DATA_HORA))).append(']');
        }
        return sb.append("]}").toString();
    }

    private void enviarHotspots() {
        if (!paginaPronta) return;
        Preditor.ResultadoML r = ctx.mlProperty().get();
        if (r == null || !chkHotspots.isSelected()) {
            executarJs("window.APS && APS.setHotspots([]);");
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
        executarJs("window.APS && APS.setHotspots(" + sb.append(']') + ");");
    }

    private void enviar(String js) {
        if (paginaPronta) executarJs(js);
        else pendente = js;
    }

    private void executarJs(String js) {
        try {
            engine.executeScript(js);
        } catch (RuntimeException e) {
            LOG.warning("Falha ao executar script no mapa: " + e.getMessage());
            lblContagem.setText("Mapa indisponível (verifique a conexão com a internet)");
        }
    }
}
