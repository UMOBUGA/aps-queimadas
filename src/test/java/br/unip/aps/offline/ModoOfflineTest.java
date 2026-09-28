package br.unip.aps.offline;

import br.unip.aps.geo.MalhaMunicipal;
import br.unip.aps.io.RepositorioDados;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.model.FocoIncendio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O sistema precisa abrir sem internet e sem a pasta data/raw (apresentacao em sala). */
@DisplayName("Modo offline")
class ModoOfflineTest {

    @Test
    void semPastaDeDadosUsaABaseEmbarcada(@TempDir Path vazio) throws Exception {
        RepositorioDados repo = new RepositorioDados(vazio.resolve("nao-existe"));
        BaseDeFocos base = repo.carregarTodos();
        assertTrue(repo.usouEmbarcados());
        assertEquals(10_378, base.tamanho());
        assertEquals(java.util.List.of(2023, 2024), base.anos());
    }

    @Test
    void mapaNaoDependeDeCdn() throws IOException {
        String html = ler("/br/unip/aps/ui/mapa.html");
        Matcher m = Pattern.compile("<(?:script|link)[^>]+(?:src|href)=\"([^\"]+)\"").matcher(html);
        int recursos = 0;
        while (m.find()) {
            String url = m.group(1);
            assertFalse(url.startsWith("http"), "recurso remoto no mapa: " + url);
            assertNotNull(getClass().getResource("/br/unip/aps/ui/" + url), "recurso ausente no JAR: " + url);
            recursos++;
        }
        assertEquals(5, recursos, "leaflet.css, MarkerCluster.css, leaflet.js, leaflet.markercluster.js e mundo.js");
        assertTrue(ler("/br/unip/aps/ui/web/mundo.js").contains("\"n\":\"Brasil\""), "o mapa-múndi offline precisa incluir o Brasil");
        assertTrue(html.contains("entrarOffline"), "o mapa precisa trocar para a malha do IBGE quando os tiles falham");
    }

    @Test
    void malhaDoIbgeCobreTodosOsMunicipiosDaBase(@TempDir Path vazio) throws Exception {
        MalhaMunicipal malha = MalhaMunicipal.sp();
        assertEquals(645, malha.municipios().size());
        BaseDeFocos base = new RepositorioDados(vazio).carregarTodos();
        for (FocoIncendio f : base.getFocos()) {
            assertNotNull(malha.buscar(f.getMunicipio()), "municipio sem par na malha do IBGE: " + f.getMunicipio());
        }
        String geo = malha.geojson();
        int features = geo.split("\"type\":\"Feature\"", -1).length - 1;
        assertEquals(645, features);
        assertTrue(malha.buscar("SÃO PAULO").areaKm2() > 1500);
    }

    @Test
    void benchmarkPreCalculadoEstaEmbarcado() throws IOException {
        String csv = ler("/resultados/benchmark.csv");
        assertTrue(csv.lines().count() > 400, "a bateria completa tem 420 medicoes");
    }

    private String ler(String recurso) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(recurso)) {
            assertNotNull(in, recurso);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
