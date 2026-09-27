package br.unip.aps.ui;

import br.unip.aps.ui.componentes.Chip;
import br.unip.aps.ui.componentes.Feedback;
import br.unip.aps.ui.componentes.Icones;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Tela Sobre. */
public class SobreController implements Pagina.Controlador {
    private final UiContexto ctx;

    @FXML private StackPane marca;
    @FXML private FlowPane chipsVersao, chipsTecnologias, chipsAreas;
    @FXML private VBox listaIntegrantes;
    @FXML private Label lblCurso, lblOrientador, lblDisciplina;
    @FXML private Hyperlink linkInpe;

    public SobreController(UiContexto ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        marca.getChildren().add(Icones.de(Icones.MARCA, 30));
        chipsVersao.getChildren().addAll(Chip.de("Versão 1.0", Icones.CUBO, Chip.Variante.NEUTRO),
                Chip.de("Java 21 · JavaFX 21", null, Chip.Variante.NEUTRO),
                Chip.de("SP · 2023–2024", Icones.MUNICIPIO, Chip.Variante.DESTAQUE));

        Properties g = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/grupo.properties")) {
            if (in != null) g.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            Feedback.alerta("grupo.properties não lido", e.getMessage());
        }
        lblCurso.setText(valor(g, "curso", "Ciência da Computação") + " · " + valor(g, "instituicao", "UNIP")
                + " · " + valor(g, "campus", "") + " · " + valor(g, "turma", ""));
        for (int i = 1; i <= 5; i++) {
            String v = g.getProperty("integrante." + i, "").strip();
            if (v.isEmpty()) continue;
            String[] p = v.split(";", 2);
            listaIntegrantes.getChildren().add(pessoa(p[0].strip(), p.length > 1 ? p[1].strip() : ""));
        }
        if (listaIntegrantes.getChildren().isEmpty()) {
            Label l = new Label("Preencha os integrantes em src/main/resources/grupo.properties.");
            l.getStyleClass().add("t-small");
            listaIntegrantes.getChildren().add(l);
        }
        lblOrientador.setText("Orientação: " + valor(g, "orientador", "—"));
        lblDisciplina.setText(valor(g, "disciplina", "Estrutura de Dados — APS"));

        for (String t : new String[]{"Java 21", "Maven", "JavaFX 21", "AtlantaFX", "Ikonli", "Inter (fonte)", "Leaflet",
                "Esri / OpenStreetMap", "Smile (ML)", "Apache POI", "OpenPDF", "JUnit 5", "JaCoCo", "GitHub Actions"}) {
            chipsTecnologias.getChildren().add(Chip.de(t));
        }
        for (String a : new String[]{"Estrutura de Dados", "Análise de Algoritmos", "Estatística", "Ciência de Dados / IA",
                "Geoprocessamento", "Meio Ambiente", "Engenharia de Software", "IHC"}) {
            chipsAreas.getChildren().add(Chip.de(a, null, Chip.Variante.DESTAQUE));
        }
        linkInpe.setOnAction(e -> abrir(ctx.sessao().config().urlInpe()));
    }

    private static String valor(Properties p, String chave, String padrao) {
        String v = p.getProperty(chave, padrao).strip();
        return v.isEmpty() ? padrao : v;
    }

    private static HBox pessoa(String nome, String ra) {
        boolean pendente = nome.startsWith("«");
        String iniciais = pendente ? "?" : iniciais(nome);
        Label avatar = new Label(iniciais);
        avatar.getStyleClass().add("pessoa-avatar");
        Label n = new Label(pendente ? "Integrante a preencher" : nome);
        n.getStyleClass().add("t-h3");
        Label r = new Label(ra.isEmpty() || ra.startsWith("«") ? "RA a preencher" : "RA " + ra);
        r.getStyleClass().add("t-caption");
        HBox h = new HBox(10, avatar, new VBox(1, n, r));
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private static String iniciais(String nome) {
        String[] p = nome.split("\\s+");
        return (p[0].substring(0, 1) + (p.length > 1 ? p[p.length - 1].substring(0, 1) : "")).toUpperCase();
    }

    private static void abrir(String url) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create(url));
        } catch (IOException | UnsupportedOperationException e) {
            Feedback.info("Abra no navegador", url);
        }
    }
}
