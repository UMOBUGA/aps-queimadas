package br.unip.aps.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/** Dados do grupo (grupo.properties): usados na tela Sobre, no modo apresentacao e na capa do relatorio. */
public final class Grupo {
    private static final Logger LOG = Logger.getLogger(Grupo.class.getName());

    private Grupo() { }

    /** Le o grupo.properties do classpath; devolve vazio se nao existir ou nao puder ser lido. */
    public static Properties carregar() {
        Properties p = new Properties();
        try (InputStream in = Grupo.class.getResourceAsStream("/grupo.properties")) {
            if (in == null) return p;
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                p.load(r);
            }
        } catch (IOException e) {
            LOG.warning("grupo.properties não pôde ser lido: " + e.getMessage());
        }
        return p;
    }

    /** Valor preenchido de uma chave, ou null se vazio ou ainda com o marcador «…». */
    public static String valor(Properties p, String chave) {
        String v = p.getProperty(chave);
        return v == null || v.isBlank() || v.contains("«") ? null : v.strip();
    }

    /** Nomes dos integrantes preenchidos (integrante.1 a integrante.5, formato "Nome;RA"). */
    public static List<String> integrantes(Properties p) {
        List<String> r = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            String v = valor(p, "integrante." + i);
            if (v != null) r.add(v.split(";")[0].strip());
        }
        return r;
    }
}
