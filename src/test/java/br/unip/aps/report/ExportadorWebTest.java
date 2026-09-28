package br.unip.aps.report;

import br.unip.aps.Focos;
import br.unip.aps.io.RelatorioCarga;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.sorting.CenarioEntrada;
import br.unip.aps.sorting.CriterioOrdenacao;
import br.unip.aps.sorting.Criterios;
import br.unip.aps.sorting.Ordem;
import br.unip.aps.sorting.ResultadoOrdenacao;
import br.unip.aps.sorting.ServicoOrdenacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Dados da versão web")
class ExportadorWebTest {

    private static int contar(String texto, String regex) {
        Matcher m = Pattern.compile(regex).matcher(texto);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    @Test
    @DisplayName("grava focos, fichas, totais, algoritmos e as bibliotecas do mapa, com os números da base")
    void geraTudo(@TempDir Path pasta) throws Exception {
        BaseDeFocos base = new BaseDeFocos(Focos.aleatorios(300, 7), new RelatorioCarga(), List.of(Path.of("t.csv")));
        List<ResultadoOrdenacao<FocoIncendio>> comparativo = new ServicoOrdenacao(100_000).compararTodos(base.getFocos(),
                Criterios.de(CriterioOrdenacao.DATA, Ordem.CRESCENTE), 0, CenarioEntrada.ORIGINAL, true);

        ExportadorWeb.Resultado r = ExportadorWeb.gerar(base, List.of(comparativo), pasta);

        assertEquals(300, r.focos());
        assertEquals(10, r.municipios());
        String focos = Files.readString(pasta.resolve("dados/focos.json"), StandardCharsets.UTF_8);
        assertEquals(300, contar(focos, "\\[-?\\d+\\.\\d+,-?\\d+\\.\\d+,\\d+,\\d+,\\d{6},\""));
        String municipios = Files.readString(pasta.resolve("dados/municipios.json"), StandardCharsets.UTF_8);
        assertEquals(10, contar(municipios, "\"nome\":"));
        assertTrue(municipios.startsWith("[{\"nome\":"), "o primeiro é o que mais queimou");
        String resumo = Files.readString(pasta.resolve("dados/resumo.json"), StandardCharsets.UTF_8);
        assertTrue(resumo.contains("\"total\":300"));
        Matcher meses = Pattern.compile("\"meses\":\\[([^\\]]*)]").matcher(resumo);
        assertTrue(meses.find());
        assertEquals(24, meses.group(1).split(",").length, "dois anos, mês a mês");
        String alg = Files.readString(pasta.resolve("dados/algoritmos.json"), StandardCharsets.UTF_8);
        assertEquals(comparativo.size(), contar(alg, "\"algoritmo\":\"[^\"]+\",\"complexidade\""));
        assertTrue(alg.contains("\"verificado\":true"));
        assertTrue(alg.contains("\"ordens\":{\"ms\":["), "a ordem da lista vem pronta do Java (Merge Sort do projeto)");
        assertTrue(alg.contains("\"curvas\":{\"criterio\":\"Data/hora\""), "curvas do benchmark embarcado");
        String passos = Files.readString(pasta.resolve("dados/passos.json"), StandardCharsets.UTF_8);
        assertEquals(br.unip.aps.sorting.AlgoritmoTipo.values().length, contar(passos, "\"descricao\":"), "um roteiro de animação por algoritmo");
        String historia = Files.readString(pasta.resolve("dados/historia.json"), StandardCharsets.UTF_8);
        assertTrue(historia.contains("\"historico\":[{\"ano\":2019"), "série de SP embarcada");
        assertTrue(historia.contains("\"carga\":{\"lidas\":"));
        assertTrue(historia.contains("\"classes\":{\"metodo\":"));
        assertTrue(Files.notExists(pasta.resolve("dados/ml.json")), "sem ML, sem arquivo de ML");
        for (String f : List.of("vendor/leaflet.js", "vendor/leaflet.css", "vendor/mundo.js", "dados/malha.geojson")) {
            assertTrue(Files.size(pasta.resolve(f)) > 1000, f);
        }
    }

    @Test
    @DisplayName("com o ML, grava os grupos de focos e o previsto contra o real mês a mês")
    void geraMl(@TempDir Path pasta) throws Exception {
        BaseDeFocos base = new BaseDeFocos(sazonal(), new RelatorioCarga(), List.of(Path.of("t.csv")));
        br.unip.aps.ml.Preditor.ResultadoML ml = new br.unip.aps.ml.Preditor().executar(base.getFocos(),
                new br.unip.aps.ml.Preditor.Parametros(2023, 2024, 30, 2, 15, 10, 1L));

        ExportadorWeb.gerar(base, List.of(), ml, pasta);

        String json = Files.readString(pasta.resolve("dados/ml.json"), StandardCharsets.UTF_8);
        assertTrue(json.startsWith("{\"anoTreino\":2023,\"anoTeste\":2024"));
        assertEquals(ml.dbscan().hotspots().size(), contar(json, "\\[-?\\d+\\.\\d+,-?\\d+\\.\\d+,\\d+,"));
        assertEquals(12, contar(json, "\\[2024\\d{2},"), "um par previsto × real por mês de 2024");
        assertTrue(json.contains("\"erro\":{\"modelo\":"));
    }

    private static List<FocoIncendio> sazonal() {
        List<FocoIncendio> focos = new java.util.ArrayList<>();
        java.util.Random rnd = new java.util.Random(4);
        long id = 0;
        String[] muns = {"BAURU", "ITU", "BARRETOS", "SANTOS", "FRANCA", "MARILIA"};
        for (int ano = 2023; ano <= 2024; ano++) {
            for (int mes = 1; mes <= 12; mes++) {
                int intensidade = mes >= 7 && mes <= 9 ? 12 : 1;
                for (int m = 0; m < muns.length; m++) {
                    for (int k = 0; k < intensidade * (m < 3 ? 2 : 1); k++) {
                        focos.add(Focos.foco(muns[m], m % 2 == 0 ? "Cerrado" : "Mata Atlântica",
                                java.time.LocalDateTime.of(ano, mes, 1 + rnd.nextInt(28), 17, 0), id++));
                    }
                }
            }
        }
        return focos;
    }
}
