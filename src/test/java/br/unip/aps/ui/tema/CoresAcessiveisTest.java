package br.unip.aps.ui.tema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Confere as cores dos temas: contraste WCAG do texto e separacao das cores de dados para os tres tipos de daltonismo. */
@DisplayName("Cores acessiveis (WCAG e daltonismo)")
class CoresAcessiveisTest {
    private static final Pattern TOKEN = Pattern.compile("(-aps-[a-z0-9-]+):\\s*(#[0-9A-Fa-f]{6})\\s*;");
    private static final double[][] PROTAN = {{0.152286, 1.052583, -0.204868}, {0.114503, 0.786281, 0.099216}, {-0.003882, -0.048116, 1.051998}};
    private static final double[][] DEUTAN = {{0.367322, 0.860646, -0.227968}, {0.280085, 0.672501, 0.047413}, {-0.011820, 0.042940, 0.968881}};
    private static final double[][] TRITAN = {{1.255528, -0.076749, -0.178779}, {-0.078411, 0.930809, 0.147602}, {0.004733, 0.691367, 0.303900}};

    private static Map<String, String> tokens(String tema, boolean daltonico, boolean contraste) throws IOException {
        Map<String, String> t = new HashMap<>();
        ler("theme-" + (tema.equals("escuro") ? "dark" : "light") + ".css", t);
        if (daltonico) ler("acessibilidade/daltonico-" + tema + ".css", t);
        if (contraste) ler("acessibilidade/contraste-" + tema + ".css", t);
        return t;
    }

    private static void ler(String arquivo, Map<String, String> t) throws IOException {
        try (InputStream in = CoresAcessiveisTest.class.getResourceAsStream("/br/unip/aps/ui/css/" + arquivo)) {
            Matcher m = TOKEN.matcher(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            while (m.find()) t.put(m.group(1), m.group(2));
        }
    }

    private static double[] linear(String hex) {
        double[] c = new double[3];
        for (int i = 0; i < 3; i++) {
            double v = Integer.parseInt(hex.substring(1 + 2 * i, 3 + 2 * i), 16) / 255.0;
            c[i] = v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
        }
        return c;
    }

    private static double luminancia(String hex) {
        double[] c = linear(hex);
        return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
    }

    static double contraste(String a, String b) {
        double la = luminancia(a), lb = luminancia(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double[] simular(double[] c, double[][] m) {
        if (m == null) return c;
        double[] r = new double[3];
        for (int i = 0; i < 3; i++) r[i] = Math.min(1, Math.max(0, m[i][0] * c[0] + m[i][1] * c[1] + m[i][2] * c[2]));
        return r;
    }

    private static double[] oklab(double[] c) {
        double l = Math.cbrt(0.4122214708 * c[0] + 0.5363325363 * c[1] + 0.0514459929 * c[2]);
        double m = Math.cbrt(0.2119034982 * c[0] + 0.6806995451 * c[1] + 0.1073969566 * c[2]);
        double s = Math.cbrt(0.0883024619 * c[0] + 0.2817188376 * c[1] + 0.6299787005 * c[2]);
        return new double[]{0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
                1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
                0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s};
    }

    /** Diferenca perceptual em OKLab (x100) depois de simular o tipo de visao. */
    static double deltaE(String a, String b, double[][] visao) {
        double[] x = oklab(simular(linear(a), visao)), y = oklab(simular(linear(b), visao));
        return 100 * Math.sqrt(Math.pow(x[0] - y[0], 2) + Math.pow(x[1] - y[1], 2) + Math.pow(x[2] - y[2], 2));
    }

    private static void separaveis(Map<String, String> t, String a, String b, String contexto) {
        String ca = t.get(a), cb = t.get(b);
        assertTrue(ca != null && cb != null, contexto + ": token ausente " + a + " ou " + b);
        assertTrue(deltaE(ca, cb, null) >= 15, contexto + ": " + a + " e " + b + " parecidos até na visão normal");
        for (double[][] v : new double[][][]{PROTAN, DEUTAN, TRITAN}) {
            double d = deltaE(ca, cb, v);
            assertTrue(d >= 8, contexto + ": " + a + " × " + b + " com ΔE " + String.format("%.1f", d) + " em visão com daltonismo");
        }
    }

    @ParameterizedTest(name = "tema {0}, daltônico={1}, alto contraste={2}")
    @CsvSource({"escuro,false,false", "claro,false,false", "escuro,true,false", "claro,true,false",
            "escuro,false,true", "claro,false,true", "escuro,true,true", "claro,true,true"})
    @DisplayName("texto com contraste AA e cores de dados distinguíveis")
    void coresDoTema(String tema, boolean daltonico, boolean contraste) throws IOException {
        Map<String, String> t = tokens(tema, daltonico, contraste);
        String contexto = tema + (daltonico ? " + daltônico" : "") + (contraste ? " + contraste" : "");
        for (String fundo : new String[]{"-aps-bg-app", "-aps-surface"}) {
            for (String texto : new String[]{"-aps-text-1", "-aps-text-2", "-aps-text-3"}) {
                double c = contraste(t.get(texto), t.get(fundo));
                assertTrue(c >= 4.5, contexto + ": " + texto + " sobre " + fundo + " tem contraste " + String.format("%.2f", c));
            }
            for (String dado : new String[]{"-aps-ano-recente", "-aps-ano-anterior", "-aps-bioma-mata", "-aps-bioma-cerrado"}) {
                double c = contraste(t.get(dado), t.get(fundo));
                assertTrue(c >= 3, contexto + ": " + dado + " sobre " + fundo + " tem contraste " + String.format("%.2f", c));
            }
        }
        separaveis(t, "-aps-ano-recente", "-aps-ano-anterior", contexto);
        separaveis(t, "-aps-bioma-mata", "-aps-bioma-cerrado", contexto);
        if (daltonico) {
            separaveis(t, "-aps-success", "-aps-danger", contexto);
            String[] series = {"-aps-serie-1", "-aps-serie-2", "-aps-serie-3", "-aps-serie-4", "-aps-serie-5", "-aps-serie-6"};
            for (int i = 1; i < series.length; i++) separaveis(t, series[i - 1], series[i], contexto);
        }
    }
}
