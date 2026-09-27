package br.unip.aps.io;

import br.unip.aps.ApsException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Baixa e extrai os arquivos anuais de focos do INPE (funcao extra do sistema). */
public final class DownloaderInpe {
    private static final Logger LOG = Logger.getLogger(DownloaderInpe.class.getName());

    private final String urlBase;
    private final HttpClient http;

    public DownloaderInpe(String urlBase) {
        this.urlBase = urlBase.endsWith("/") ? urlBase : urlBase + "/";
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public String url(String uf, int ano) {
        String u = uf.strip().toUpperCase(Locale.ROOT);
        return urlBase + u + "/focos_br_" + u.toLowerCase(Locale.ROOT) + "_ref_" + ano + ".zip";
    }

    /** Baixa o ZIP do ano/UF e extrai o CSV para a pasta de destino. */
    public Path baixar(String uf, int ano, Path destino) throws ApsException {
        if (uf == null || !uf.strip().matches("[A-Za-z]{2}")) {
            throw new ApsException("UF invalida: '" + uf + "'. Use a sigla com 2 letras, ex.: SP.");
        }
        String url = url(uf, ano);
        LOG.info(() -> "Baixando " + url);
        try {
            Files.createDirectories(destino);
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMinutes(2))
                    .header("User-Agent", "APS-Queimadas-UNIP/1.0 (Java HttpClient)")
                    .GET().build();
            HttpResponse<InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() == 404) {
                resp.body().close();
                throw new ApsException("O INPE nao possui o arquivo " + url + " (verifique a UF e o ano).");
            }
            if (resp.statusCode() != 200) {
                resp.body().close();
                throw new ApsException("O servidor do INPE respondeu HTTP " + resp.statusCode() + " para " + url);
            }
            Path csv = null;
            try (ZipInputStream zip = new ZipInputStream(resp.body())) {
                ZipEntry e;
                while ((e = zip.getNextEntry()) != null) {
                    String nome = e.getName();
                    if (e.isDirectory() || !nome.toLowerCase(Locale.ROOT).endsWith(".csv")
                            || nome.contains("/") || nome.contains("\\") || nome.contains("..")) {
                        continue;
                    }
                    Path alvo = destino.resolve(nome).normalize();
                    if (!alvo.startsWith(destino.normalize())) continue;
                    Files.copy(zip, alvo, StandardCopyOption.REPLACE_EXISTING);
                    csv = alvo;
                }
            }
            if (csv == null) throw new ApsException("O arquivo baixado de " + url + " nao contem nenhum CSV.");
            final Path ok = csv;
            LOG.info(() -> "Extraido: " + ok.toAbsolutePath());
            return csv;
        } catch (IOException e) {
            throw new ApsException("Falha ao baixar " + url + ". Verifique sua conexao com a internet. (" + e.getMessage() + ")", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApsException("Download interrompido.", e);
        }
    }
}
