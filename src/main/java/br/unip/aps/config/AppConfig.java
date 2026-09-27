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

/**
 * Configuracao da aplicacao, lida em camadas (a ultima vence):
 * <ol>
 *   <li>{@code application.properties} embutido no JAR (valores padrao);</li>
 *   <li>{@code aps.properties} no diretorio de trabalho (opcional, para o usuario ajustar sem recompilar);</li>
 *   <li>propriedades de sistema {@code -Daps.chave=valor} (linha de comando).</li>
 * </ol>
 */
public final class AppConfig {

    private static final Logger LOG = Logger.getLogger(AppConfig.class.getName());

    private final Properties props = new Properties();

    private AppConfig() { }

    /** @return configuracao carregada das tres camadas */
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

    /**
     * @param chave  chave
     * @param padrao valor padrao
     * @return valor textual
     */
    public String texto(String chave, String padrao) {
        String v = props.getProperty(chave);
        return v == null || v.isBlank() ? padrao : v.strip();
    }

    /**
     * @param chave  chave
     * @param padrao valor padrao (tambem usado se o valor for invalido)
     * @return valor inteiro
     */
    public int inteiro(String chave, int padrao) {
        try {
            return Integer.parseInt(texto(chave, String.valueOf(padrao)).replace("_", ""));
        } catch (NumberFormatException e) {
            LOG.warning("Valor invalido para " + chave + "; usando " + padrao);
            return padrao;
        }
    }

    /**
     * @param chave  chave
     * @param padrao valor padrao
     * @return valor decimal
     */
    public double decimal(String chave, double padrao) {
        try {
            return Double.parseDouble(texto(chave, String.valueOf(padrao)).replace(',', '.'));
        } catch (NumberFormatException e) {
            LOG.warning("Valor invalido para " + chave + "; usando " + padrao);
            return padrao;
        }
    }

    /**
     * @param chave  chave
     * @param padrao lista padrao
     * @return lista de inteiros separados por virgula
     */
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

    /** @return diretorio dos CSVs do INPE */
    public Path diretorioDados() {
        return Path.of(texto("aps.dados.diretorio", "data/raw"));
    }

    /** @return diretorio de saida dos relatorios */
    public Path diretorioRelatorios() {
        return Path.of(texto("aps.relatorios.diretorio", "relatorios"));
    }

    /** @return sigla da UF analisada (ex.: SP) */
    public String uf() {
        return texto("aps.uf", "SP");
    }

    /** @return anos analisados */
    public List<Integer> anos() {
        return inteiros("aps.anos", List.of(2023, 2024));
    }

    /** @return n acima do qual algoritmos O(n²) geram aviso */
    public int limiteQuadratico() {
        return inteiro("aps.ordenacao.limiteQuadratico", 20_000);
    }

    /** @return URL base do diretorio de dados do INPE */
    public String urlInpe() {
        return texto("aps.inpe.url", "https://dataserver-coids.inpe.br/queimadas/queimadas/focos/csv/anual/EstadosBr_sat_ref/");
    }
}
