package br.unip.aps.ui.tema;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/** Tema, fontes e preferencias da interface. */
public final class GerenciadorTema {
    /** Temas disponiveis. */
    public enum Tema {
        CLARO("theme-light.css"),
        ESCURO("theme-dark.css");

        private final String arquivo;

        Tema(String arquivo) {
            this.arquivo = arquivo;
        }

        public Tema oposto() {
            return this == CLARO ? ESCURO : CLARO;
        }
    }

    private static final Logger LOG = Logger.getLogger(GerenciadorTema.class.getName());
    private static final String BASE_CSS = "/br/unip/aps/ui/css/";
    private static final String[] FONTES = {"Inter-Regular.ttf", "Inter-Medium.ttf", "Inter-SemiBold.ttf", "Inter-Bold.ttf",
            "BigShouldersDisplay-SemiBold.ttf", "BigShouldersDisplay-Bold.ttf", "BigShouldersDisplay-ExtraBold.ttf", "BigShouldersDisplay-Black.ttf"};
    private static GerenciadorTema instancia;

    private final Preferences prefs;
    private final ObjectProperty<Tema> tema = new SimpleObjectProperty<>();
    private final BooleanProperty animacoes = new SimpleBooleanProperty();
    private final BooleanProperty sidebarRecolhida = new SimpleBooleanProperty();
    private final BooleanProperty compacto = new SimpleBooleanProperty();
    private final List<Scene> cenas = new ArrayList<>();

    private GerenciadorTema(Preferences prefs) {
        this.prefs = prefs;
        tema.set(lerTema());
        animacoes.set(prefs.getBoolean("animacoes", true));
        sidebarRecolhida.set(prefs.getBoolean("sidebarRecolhida", false));
        compacto.set(prefs.getBoolean("compacto", false));
        compacto.addListener((o, a, n) -> {
            prefs.putBoolean("compacto", n);
            for (Scene s : cenas) aplicarDensidade(s);
        });
        tema.addListener((o, a, n) -> {
            prefs.put("tema", n.name());
            aplicarGlobal();
        });
        animacoes.addListener((o, a, n) -> prefs.putBoolean("animacoes", n));
        sidebarRecolhida.addListener((o, a, n) -> prefs.putBoolean("sidebarRecolhida", n));
    }

    public static synchronized GerenciadorTema get() {
        if (instancia == null) {
            carregarFontes();
            Preferences p;
            try {
                p = Preferences.userNodeForPackage(GerenciadorTema.class);
            } catch (SecurityException e) {
                p = Preferences.userRoot().node("aps-queimadas-temp");
            }
            instancia = new GerenciadorTema(p);
            instancia.aplicarGlobal();
        }
        return instancia;
    }

    private Tema lerTema() {
        try {
            return Tema.valueOf(prefs.get("tema", Tema.ESCURO.name()));
        } catch (IllegalArgumentException e) {
            return Tema.ESCURO;
        }
    }

    static void carregarFontes() {
        for (String f : FONTES) {
            try (InputStream in = GerenciadorTema.class.getResourceAsStream("/fonts/" + f)) {
                if (in == null || Font.loadFont(in, 13) == null) {
                    LOG.warning("Fonte nao carregada: " + f + " (usando fonte do sistema)");
                }
            } catch (java.io.IOException e) {
                LOG.warning("Falha ao carregar a fonte " + f + ": " + e.getMessage());
            }
        }
    }

    private void aplicarGlobal() {
        Application.setUserAgentStylesheet(tema.get() == Tema.ESCURO
                ? new PrimerDark().getUserAgentStylesheet()
                : new PrimerLight().getUserAgentStylesheet());
        cenas.removeIf(s -> s.getWindow() == null && s.getRoot() == null);
        for (Scene s : cenas) s.getStylesheets().setAll(folhas());
    }

    /** Registra uma cena para receber (e acompanhar) o tema atual. */
    public void registrar(Scene cena) {
        cena.getStylesheets().setAll(folhas());
        aplicarDensidade(cena);
        if (!cenas.contains(cena)) cenas.add(cena);
    }

    /** Aplica as folhas de estilo a um no fora de uma cena registrada. */
    public void aplicar(Parent raiz) {
        raiz.getStylesheets().setAll(folhas());
    }

    public List<String> folhas() {
        List<String> r = new ArrayList<>();
        for (String f : List.of("tokens.css", tema.get().arquivo, "base.css", "components.css", "charts.css")) {
            URL u = GerenciadorTema.class.getResource(BASE_CSS + f);
            if (u != null) r.add(u.toExternalForm());
        }
        return r;
    }

    /** Alterna entre claro e escuro. */
    public void alternar() {
        tema.set(tema.get().oposto());
    }

    public boolean escuro() {
        return tema.get() == Tema.ESCURO;
    }

    private void aplicarDensidade(Scene cena) {
        if (cena.getRoot() == null) return;
        cena.getRoot().getStyleClass().remove("compacto");
        if (compacto.get()) cena.getRoot().getStyleClass().add("compacto");
    }

    public BooleanProperty compactoProperty() { return compacto; }

    public ObjectProperty<Tema> temaProperty() { return tema; }
    public BooleanProperty animacoesProperty() { return animacoes; }
    public BooleanProperty sidebarRecolhidaProperty() { return sidebarRecolhida; }

    /** Restaura as preferencias padrao (tema escuro, animacoes ligadas, sidebar expandida). */
    public void restaurarPadroes() {
        tema.set(Tema.ESCURO);
        animacoes.set(true);
        sidebarRecolhida.set(false);
        compacto.set(false);
    }
}
