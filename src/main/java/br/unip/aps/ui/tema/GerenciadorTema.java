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
    private final BooleanProperty daltonico = new SimpleBooleanProperty();
    private final BooleanProperty altoContraste = new SimpleBooleanProperty();
    private final javafx.beans.property.IntegerProperty escalaTexto = new javafx.beans.property.SimpleIntegerProperty(100);
    private final java.util.Map<String, String> folhasAmpliadas = new java.util.HashMap<>();
    private final List<Scene> cenas = new ArrayList<>();

    private GerenciadorTema(Preferences prefs) {
        this.prefs = prefs;
        tema.set(lerTema());
        animacoes.set(prefs.getBoolean("animacoes", true));
        sidebarRecolhida.set(prefs.getBoolean("sidebarRecolhida", false));
        compacto.set(prefs.getBoolean("compacto", false));
        daltonico.set(prefs.getBoolean("daltonico", false));
        altoContraste.set(prefs.getBoolean("altoContraste", false));
        escalaTexto.set(normalizarEscala(prefs.getInt("escalaTexto", 100)));
        daltonico.addListener((o, a, n) -> {
            prefs.putBoolean("daltonico", n);
            aplicarGlobal();
        });
        altoContraste.addListener((o, a, n) -> {
            prefs.putBoolean("altoContraste", n);
            aplicarGlobal();
        });
        escalaTexto.addListener((o, a, n) -> {
            prefs.putInt("escalaTexto", n.intValue());
            aplicarGlobal();
        });
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
        String sufixo = tema.get() == Tema.ESCURO ? "escuro" : "claro";
        List<String> arquivos = new ArrayList<>(List.of("tokens.css", tema.get().arquivo, "base.css", "components.css", "charts.css"));
        if (daltonico.get()) arquivos.add("acessibilidade/daltonico-" + sufixo + ".css");
        if (altoContraste.get()) arquivos.add("acessibilidade/contraste-" + sufixo + ".css");
        for (String f : arquivos) {
            URL u = GerenciadorTema.class.getResource(BASE_CSS + f);
            if (u == null) continue;
            r.add(escalaTexto.get() != 100 && TEM_TEXTO.contains(f) ? ampliada(f, u) : u.toExternalForm());
        }
        return r;
    }

    private static final List<String> TEM_TEXTO = List.of("base.css", "components.css", "charts.css");
    private static final java.util.regex.Pattern TAMANHO = java.util.regex.Pattern.compile("(-fx-font-size:\\s*)([0-9.]+)px");

    private String ampliada(String arquivo, URL original) {
        String chave = arquivo + "@" + escalaTexto.get();
        String pronta = folhasAmpliadas.get(chave);
        if (pronta != null) return pronta;
        try (InputStream in = original.openStream()) {
            String css = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            double fator = escalaTexto.get() / 100.0;
            java.util.regex.Matcher m = TAMANHO.matcher(css);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                double px = Double.parseDouble(m.group(2)) * fator;
                m.appendReplacement(sb, m.group(1) + String.format(java.util.Locale.ROOT, "%.1f", px) + "px");
            }
            m.appendTail(sb);
            java.nio.file.Path tmp = java.nio.file.Files.createTempFile("aps-" + arquivo.replace(".css", "") + "-" + escalaTexto.get() + "-", ".css");
            tmp.toFile().deleteOnExit();
            java.nio.file.Files.writeString(tmp, sb, java.nio.charset.StandardCharsets.UTF_8);
            String url = tmp.toUri().toString();
            folhasAmpliadas.put(chave, url);
            return url;
        } catch (java.io.IOException e) {
            LOG.warning("Não foi possível ampliar " + arquivo + ": " + e.getMessage());
            return original.toExternalForm();
        }
    }

    private static int normalizarEscala(int v) {
        return v >= 125 ? 130 : v >= 110 ? 115 : 100;
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
    public BooleanProperty daltonicoProperty() { return daltonico; }
    public BooleanProperty altoContrasteProperty() { return altoContraste; }
    /** Tamanho do texto em porcentagem: 100, 115 ou 130. */
    public javafx.beans.property.IntegerProperty escalaTextoProperty() { return escalaTexto; }

    public ObjectProperty<Tema> temaProperty() { return tema; }
    public BooleanProperty animacoesProperty() { return animacoes; }
    public BooleanProperty sidebarRecolhidaProperty() { return sidebarRecolhida; }

    /** Restaura as preferencias padrao (tema escuro, animacoes ligadas, sidebar expandida). */
    public void restaurarPadroes() {
        tema.set(Tema.ESCURO);
        animacoes.set(true);
        sidebarRecolhida.set(false);
        compacto.set(false);
        daltonico.set(false);
        altoContraste.set(false);
        escalaTexto.set(100);
    }
}
