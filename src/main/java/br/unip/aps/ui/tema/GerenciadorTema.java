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

/**
 * Gerencia o tema visual da aplicacao (claro/escuro), as fontes embarcadas e as preferencias de
 * interface do usuario, persistidas com {@link Preferences} entre execucoes.
 *
 * <p>Camadas: o AtlantaFX (Primer) e o <i>user-agent stylesheet</i> (base de todos os controles);
 * por cima, cada cena recebe as folhas do design system do projeto: {@code tokens.css},
 * {@code theme-light.css} ou {@code theme-dark.css}, {@code base.css}, {@code components.css} e
 * {@code charts.css}. Trocar de tema substitui apenas o UA stylesheet e o arquivo de tema.</p>
 */
public final class GerenciadorTema {

    /** Temas disponiveis. */
    public enum Tema {
        CLARO("theme-light.css"),
        ESCURO("theme-dark.css");

        private final String arquivo;

        Tema(String arquivo) {
            this.arquivo = arquivo;
        }

        /** @return o outro tema */
        public Tema oposto() {
            return this == CLARO ? ESCURO : CLARO;
        }
    }

    private static final Logger LOG = Logger.getLogger(GerenciadorTema.class.getName());
    private static final String BASE_CSS = "/br/unip/aps/ui/css/";
    private static final String[] FONTES = {"Inter-Regular.ttf", "Inter-Medium.ttf", "Inter-SemiBold.ttf", "Inter-Bold.ttf"};
    private static GerenciadorTema instancia;

    private final Preferences prefs;
    private final ObjectProperty<Tema> tema = new SimpleObjectProperty<>();
    private final BooleanProperty animacoes = new SimpleBooleanProperty();
    private final BooleanProperty sidebarRecolhida = new SimpleBooleanProperty();
    private final List<Scene> cenas = new ArrayList<>();

    private GerenciadorTema(Preferences prefs) {
        this.prefs = prefs;
        tema.set(lerTema());
        animacoes.set(prefs.getBoolean("animacoes", true));
        sidebarRecolhida.set(prefs.getBoolean("sidebarRecolhida", false));
        tema.addListener((o, a, n) -> {
            prefs.put("tema", n.name());
            aplicarGlobal();
        });
        animacoes.addListener((o, a, n) -> prefs.putBoolean("animacoes", n));
        sidebarRecolhida.addListener((o, a, n) -> prefs.putBoolean("sidebarRecolhida", n));
    }

    /** @return instancia unica (criada na primeira chamada, carregando fontes e preferencias) */
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
            return Tema.valueOf(prefs.get("tema", Tema.CLARO.name()));
        } catch (IllegalArgumentException e) {
            return Tema.CLARO;
        }
    }

    /** Carrega a fonte Inter (4 pesos) embarcada em {@code resources/fonts}. */
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

    /**
     * Registra uma cena para receber (e acompanhar) o tema atual.
     *
     * @param cena cena
     */
    public void registrar(Scene cena) {
        cena.getStylesheets().setAll(folhas());
        if (!cenas.contains(cena)) cenas.add(cena);
    }

    /**
     * Aplica as folhas do design system a um no raiz fora de cena registrada (ex.: DialogPane).
     *
     * @param raiz no raiz
     */
    public void aplicar(Parent raiz) {
        raiz.getStylesheets().setAll(folhas());
    }

    /** @return URLs das folhas do tema atual, na ordem de aplicacao */
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

    /** @return {@code true} se o tema atual for escuro */
    public boolean escuro() {
        return tema.get() == Tema.ESCURO;
    }

    public ObjectProperty<Tema> temaProperty() { return tema; }
    public BooleanProperty animacoesProperty() { return animacoes; }
    public BooleanProperty sidebarRecolhidaProperty() { return sidebarRecolhida; }

    /** Restaura as preferencias padrao (tema claro, animacoes ligadas, sidebar expandida). */
    public void restaurarPadroes() {
        tema.set(Tema.CLARO);
        animacoes.set(true);
        sidebarRecolhida.set(false);
    }
}
