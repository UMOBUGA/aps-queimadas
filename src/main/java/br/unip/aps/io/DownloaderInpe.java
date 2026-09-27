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
        if (urlBase == null || !urlBase.startsWith("https://")) {
            throw new IllegalArgumentException("O endereço do INPE deve usar https (aps.inpe.url): " + urlBase);
        }
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
        return baixarUrl(url(uf, ano), destino);
    }

    /** URL do arquivo anual do Brasil inteiro (satelite de referencia). */
    public String urlBrasil(int ano) {
        return urlBase.replace("EstadosBr_sat_ref/", "Brasil_sat_ref/") + "focos_br_ref_" + ano + ".zip";
    }

    /** Baixa o CSV anual do Brasil inteiro (usado pelo External Merge Sort). */
    public Path baixarBrasil(int ano, Path destino) throws ApsException {
        return baixarUrl(urlBrasil(ano), destino);
    }

    /** URL do anual do Brasil com todos os satelites (traz dias sem chuva, precipitacao, risco de fogo e FRP). */
    public String urlTodosSatelites(int ano) {
        return urlBase.replace("EstadosBr_sat_ref/", "Brasil_todos_sats/") + "focos_br_todos-sats_" + ano + ".zip";
    }

    /** Baixa o anual de todos os satelites guardando apenas as linhas do estado informado (streaming, sem o Brasil em disco). */
    @SuppressWarnings({"PMD.CloseResource", "PMD.AvoidBranchingStatementAsLastInLoop"})
    public Path baixarTodosSatelites(int ano, String estado, String uf, Path destino) throws ApsException {
        String url = urlTodosSatelites(ano);
        String alvo = "," + estado.toUpperCase(Locale.ROOT) + ",";
        LOG.info(() -> "Baixando e filtrando " + url);
        try {
            Files.createDirectories(destino);
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(20))
                    .header("User-Agent", "APS-Queimadas-UNIP/1.0 (Java HttpClient)").GET().build();
            HttpResponse<InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() != 200) {
                resp.body().close();
                throw new ApsException("O servidor do INPE respondeu HTTP " + resp.statusCode() + " para " + url);
            }
            Path saida = destino.resolve("focos_" + uf.toLowerCase(Locale.ROOT) + "_todos-sats_" + ano + ".csv");
            try (ZipInputStream zip = new ZipInputStream(resp.body())) {
                ZipEntry e;
                while ((e = zip.getNextEntry()) != null) {
                    if (e.isDirectory() || !e.getName().toLowerCase(Locale.ROOT).endsWith(".csv")) continue;
                    java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(zip, java.nio.charset.StandardCharsets.UTF_8));
                    try (java.io.BufferedWriter out = Files.newBufferedWriter(saida, java.nio.charset.StandardCharsets.UTF_8)) {
                        String cab = in.readLine();
                        if (cab == null) continue;
                        out.write(cab);
                        out.newLine();
                        String linha;
                        while ((linha = in.readLine()) != null) {
                            if (linha.toUpperCase(Locale.ROOT).contains(alvo)) {
                                out.write(linha);
                                out.newLine();
                            }
                        }
                    }
                    break;
                }
            }
            return saida;
        } catch (IOException e) {
            throw new ApsException("Falha ao baixar " + url + " (" + e.getMessage() + ")", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApsException("Download interrompido.", e);
        }
    }

    private Path baixarUrl(String url, Path destino) throws ApsException {
        LOG.info(() -> "Baixando " + url);
        try {
            Files.createDirectories(destino);
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMinutes(10))
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
