package br.unip.aps.ui.componentes;

import br.unip.aps.analysis.Estatisticas;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/** Linha do tempo do mapa: escolhe um mes (ou um intervalo) na faixa termica e toca os meses em sequencia. */
public class LinhaDoTempo extends HBox {
    private static final Duration PASSO = Duration.millis(900);

    private final FaixaTermica faixa = new FaixaTermica();
    private final Button tocar = new Button();
    private final Button todos = new Button("Todos os meses");
    private final Label periodo = new Label("Todos os meses");
    private final Timeline relogio = new Timeline();
    private final List<YearMonth> meses = new ArrayList<>();
    private YearMonth de, ate;
    private BiConsumer<YearMonth, YearMonth> aoMudar;

    public LinhaDoTempo() {
        getStyleClass().add("linha-tempo");
        setId("linhaDoTempo");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(14);
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        tocar.setId("btnTocarMeses");
        tocar.getStyleClass().addAll("btn-primary", "linha-tempo-tocar");
        tocar.setOnAction(e -> {
            if (tocando()) pausar();
            else tocar();
        });
        Layout.naoEncolher(tocar);
        todos.setId("btnTodosMeses");
        todos.getStyleClass().add("btn-ghost");
        todos.setOnAction(e -> {
            pausar();
            mudar(null, null);
        });
        Layout.naoEncolher(todos);
        periodo.getStyleClass().add("linha-tempo-periodo");
        periodo.setMinWidth(Region.USE_PREF_SIZE);
        Label sobre = new Label("LINHA DO TEMPO");
        sobre.getStyleClass().add("t-overline");
        VBox textos = new VBox(0, sobre, periodo);
        textos.setMinWidth(118);

        faixa.semLegenda();
        faixa.setPrefWidth(430);
        faixa.setMinWidth(260);
        HBox.setHgrow(faixa, Priority.ALWAYS);
        faixa.setOnSelecionar((a, b) -> {
            pausar();
            mudar(a, b);
        });
        getChildren().addAll(tocar, textos, faixa, todos);

        relogio.getKeyFrames().add(new KeyFrame(PASSO, e -> avancar()));
        relogio.setCycleCount(Animation.INDEFINITE);
        relogio.statusProperty().addListener((o, a, s) -> atualizarBotao());
        atualizarBotao();
        atualizarRotulo();
    }

    /** Serie mensal da base (em ordem cronologica). */
    public void setDados(Map<YearMonth, Long> serie) {
        meses.clear();
        meses.addAll(serie.keySet());
        faixa.setDados(serie);
        faixa.setSelecao(de, ate);
    }

    /** Toca os meses em sequencia, a partir do mes seguinte ao atual (ou do primeiro). */
    public void tocar() {
        if (meses.isEmpty()) return;
        if (de == null || ate != null && !ate.equals(de) || de.equals(meses.get(meses.size() - 1))) mudar(meses.get(0), meses.get(0));
        relogio.playFromStart();
    }

    public void pausar() {
        relogio.stop();
    }

    public boolean tocando() {
        return relogio.getStatus() == Animation.Status.RUNNING;
    }

    /** Avanca um mes; no ultimo, para. */
    void avancar() {
        int i = de == null ? -1 : meses.indexOf(de);
        if (i + 1 >= meses.size()) {
            pausar();
            return;
        }
        YearMonth m = meses.get(i + 1);
        mudar(m, m);
        if (i + 2 >= meses.size()) pausar();
    }

    /** Define o periodo sem tocar (nulos = todos os meses). */
    public void setPeriodo(YearMonth inicio, YearMonth fim) {
        mudar(inicio, fim);
    }

    public YearMonth inicio() {
        return de;
    }

    public YearMonth fim() {
        return ate;
    }

    public void setOnMudar(BiConsumer<YearMonth, YearMonth> acao) {
        this.aoMudar = acao;
    }

    private void mudar(YearMonth inicio, YearMonth fim) {
        de = inicio;
        ate = inicio == null ? null : fim == null ? inicio : fim;
        faixa.setSelecao(de, ate);
        atualizarRotulo();
        if (aoMudar != null) aoMudar.accept(de, ate);
    }

    private void atualizarRotulo() {
        periodo.setText(rotulo(de, ate));
        todos.setDisable(de == null);
    }

    /** "ago/2024", "mar/2023 – ago/2024" ou "Todos os meses". */
    public static String rotulo(YearMonth de, YearMonth ate) {
        if (de == null) return "Todos os meses";
        String a = Estatisticas.MESES[de.getMonthValue() - 1] + "/" + de.getYear();
        if (ate == null || ate.equals(de)) return a;
        return a + " – " + Estatisticas.MESES[ate.getMonthValue() - 1] + "/" + ate.getYear();
    }

    private void atualizarBotao() {
        boolean t = tocando();
        tocar.setGraphic(Icones.de(t ? Icones.PAUSAR : Icones.EXECUTAR, 16));
        tocar.setText(t ? "Pausar" : "Tocar");
        tocar.setAccessibleText(t ? "Pausar a linha do tempo" : "Tocar os meses em sequência no mapa");
        tocar.setTooltip(new Tooltip(t ? "Pausar" : "Mostra os focos mês a mês, do primeiro ao último"));
    }
}
