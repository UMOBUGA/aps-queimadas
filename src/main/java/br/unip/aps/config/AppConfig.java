package br.unip.aps.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/** Configuracao da aplicacao, lida em camadas (a ultima vence). */
public final class AppConfig {
    private static final Logger LOG = Logger.getLogger(AppConfig.class.getName());

    private final Properties props = new Properties();

    private AppConfig() { }

    public static AppConfig carregar() {
        AppConfig c = new AppConfig();
        try (InputStream in = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                try (Reader r = new java.io.InputStreamReader(in, StandardCharsets.UTF_8)) {
                    c.props.load(r);
                }
            }
        } catch (IOException e) {
            LOG.warning("Nao foi possivel ler application.properties: " + e.getMessage());
        }
        Path local = Path.of("aps.properties");
        if (Files.isRegularFile(local)) {
            try (Reader r = Files.newBufferedReader(local, StandardCharsets.UTF_8)) {
                c.props.load(r);
                LOG.info("Configuracao local carregada de " + local.toAbsolutePath());
            } catch (IOException e) {
                LOG.warning("Nao foi possivel ler aps.properties: " + e.getMessage());
            }
        }
        for (String k : System.getProperties().stringPropertyNames()) {
            if (k.startsWith("aps.")) c.props.setProperty(k, System.getProperty(k));
        }
        return c;
    }

    public String texto(String chave, String padrao) {
        String v = props.getProperty(chave);
        return v == null || v.isBlank() ? padrao : v.strip();
    }

    public int inteiro(String chave, int padrao) {
        try {
            return Integer.parseInt(texto(chave, String.valueOf(padrao)).replace("_", ""));
        } catch (NumberFormatException e) {
            LOG.warning("Valor invalido para " + chave + "; usando " + padrao);
            return padrao;
        }
    }

    public double decimal(String chave, double padrao) {
        try {
            return Double.parseDouble(texto(chave, String.valueOf(padrao)).replace(',', '.'));
        } catch (NumberFormatException e) {
            LOG.warning("Valor invalido para " + chave + "; usando " + padrao);
            return padrao;
        }
    }

    public List<Integer> inteiros(String chave, List<Integer> padrao) {
        String v = props.getProperty(chave);
        if (v == null || v.isBlank()) return padrao;
        List<Integer> r = new ArrayList<>();
        try {
            for (String p : v.split(",")) if (!p.isBlank()) r.add(Integer.parseInt(p.strip().replace("_", "")));
            return r;
        } catch (NumberFormatException e) {
            LOG.warning("Lista invalida para " + chave + "; usando " + padrao);
            return padrao;
        }
    }

    public Path diretorioDados() {
        return Path.of(texto("aps.dados.diretorio", "data/raw"));
    }

    public Path diretorioRelatorios() {
        return Path.of(texto("aps.relatorios.diretorio", "relatorios"));
    }

    public String uf() {
        return texto("aps.uf", "SP");
    }

    public List<Integer> anos() {
        return inteiros("aps.anos", List.of(2023, 2024));
    }

    public int limiteQuadratico() {
        return inteiro("aps.ordenacao.limiteQuadratico", 20_000);
    }

    public String urlInpe() {
        return texto("aps.inpe.url", "https://dataserver-coids.inpe.br/queimadas/queimadas/focos/csv/anual/EstadosBr_sat_ref/");
    }
}
