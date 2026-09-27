package br.unip.aps.ml;

import br.unip.aps.Focos;
import br.unip.aps.model.FocoIncendio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Machine Learning")
class PreditorTest {

    @Test
    @DisplayName("base mensal: agregacao por municipio/mes e variaveis sem vazamento do futuro")
    void baseMensal() {
        List<FocoIncendio> f = List.of(
                Focos.foco("ITU", "Cerrado", LocalDateTime.of(2023, 1, 5, 17, 0), 1),
                Focos.foco("ITU", "Cerrado", LocalDateTime.of(2023, 1, 9, 17, 0), 2),
                Focos.foco("ITU", "Mata Atlântica", LocalDateTime.of(2023, 3, 1, 17, 0), 3),
                Focos.foco("BAURU", "Cerrado", LocalDateTime.of(2024, 2, 1, 17, 0), 4));
        BaseMensal b = new BaseMensal(f);
        assertEquals(List.of("BAURU", "ITU"), b.municipios());
        assertEquals(24, b.meses());
        List<BaseMensal.Linha> mar = b.linhasEntre(YearMonth.of(2023, 3), YearMonth.of(2023, 3));
        BaseMensal.Linha itu = mar.get(1);
        assertEquals(1, itu.focos());
        assertEquals(0, itu.x()[PrevisaoFocos.indice("lag1")], "fevereiro sem focos");
        assertEquals(2, itu.x()[PrevisaoFocos.indice("lag2")], "janeiro com 2 focos");
        assertEquals(1.0, itu.x()[PrevisaoFocos.indice("media_hist")], 1e-9, "(2 + 0) / 2 meses anteriores");
        assertEquals(2.0 / 3, itu.x()[PrevisaoFocos.indice("frac_cerrado")], 1e-9);
        assertEquals(2 * 12 * 2, b.linhasDoAno(2023).size() + b.linhasDoAno(2024).size());
    }

    @Test
    @DisplayName("metricas de regressao e classificacao")
    void metricas() {
        Metricas.Regressao r = Metricas.regressao(new double[]{1, 2, 3}, new double[]{1, 2, 5});
        assertEquals(2.0 / 3, r.mae(), 1e-9);
        assertEquals(Math.sqrt(4.0 / 3), r.rmse(), 1e-9);
        assertEquals(1 - 4.0 / 2, r.r2(), 1e-9);
        Metricas.Classificacao c = Metricas.classificacao(new int[]{0, 0, 1, 2}, new int[]{0, 1, 1, 2}, 3);
        assertEquals(0.75, c.acuracia(), 1e-9);
        assertArrayEquals(new int[]{1, 1, 0}, c.matriz()[0]);
        assertEquals(0.5, c.precisao()[1], 1e-9);
        assertEquals(1.0, c.revocacao()[1], 1e-9);
    }

    @Test
    @DisplayName("niveis de atividade e pesos de balanceamento")
    void niveisEPesos() {
        assertEquals(NivelAtividade.BAIXO, NivelAtividade.de(0));
        assertEquals(NivelAtividade.MEDIO, NivelAtividade.de(4));
        assertEquals(NivelAtividade.ALTO, NivelAtividade.de(5));
        assertArrayEquals(new int[]{100, 13, 1}, ClassificadorNivel.pesos(new int[]{6098, 801, 61}));
        assertArrayEquals(new int[]{1, 1, 1}, ClassificadorNivel.pesos(new int[]{0, 0, 0}));
    }

    @Test
    @DisplayName("pipeline completo em dados sinteticos com sazonalidade e dois hotspots")
    void pipelineSintetico() {
        List<FocoIncendio> focos = new ArrayList<>();
        long id = 0;
        String[] muns = {"A", "B", "C", "D", "E", "F"};
        java.util.Random rnd = new java.util.Random(4);
        for (int ano = 2023; ano <= 2024; ano++) {
            for (int mes = 1; mes <= 12; mes++) {
                int intensidade = (mes >= 7 && mes <= 9) ? 12 : 1;
                for (int m = 0; m < muns.length; m++) {
                    for (int k = 0; k < intensidade * (m < 3 ? 2 : 1); k++) {
                        double lat = (m < 3 ? -21.0 : -23.0) + rnd.nextGaussian() * 0.03;
                        double lon = (m < 3 ? -48.0 : -46.5) + rnd.nextGaussian() * 0.03;
                        focos.add(FocoIncendio.builder().idBdq(id).focoId("s" + id++).municipio(muns[m])
                                .bioma(m % 2 == 0 ? "Cerrado" : "Mata Atlântica")
                                .dataHora(LocalDateTime.of(ano, mes, 1 + rnd.nextInt(28), 17, 0))
                                .latitude(lat).longitude(lon).build());
                    }
                }
            }
        }
        Preditor.ResultadoML r = new Preditor().executar(focos, new Preditor.Parametros(2023, 2024, 30, 2, 15, 10, 1L));
        assertEquals(6, r.municipios());
        assertTrue(r.regressao().modelo().mae() < r.regressao().persistencia().mae(),
                "com sazonalidade forte o RF deve superar a persistencia");
        assertEquals(12, r.regressao().realPorMes().size());
        assertEquals(12, r.regressao().previstoJanelaPorMes().size());
        assertEquals(2, r.dbscan().hotspots().size(), "dois aglomerados geograficos");
        assertEquals(2, r.kMeans().hotspots().size());
        assertEquals(1, r.kMeans().hotspots().get(0).id());
        assertTrue(r.cotovelo()[0] > r.cotovelo()[1], "WCSS cai de k=1 para k=2");
        assertFalse(r.classificacao().metricas().acuracia() < 0.5);
    }

    @Test
    @DisplayName("erros: anos ausentes e parametros invalidos")
    void erros() {
        List<FocoIncendio> f = br.unip.aps.Focos.aleatorios(50, 1);
        assertThrows(IllegalArgumentException.class,
                () -> new Preditor().executar(f, Preditor.Parametros.padrao(2019, 2020)));
        assertThrows(IllegalArgumentException.class, () -> new ClusterizacaoHotspots(1).kMeans(List.of(), 3));
        assertThrows(IllegalArgumentException.class, () -> new ClusterizacaoHotspots(1).dbscan(f, -1, 5));
    }

    @Test
    void haversine() {
        // Sao Paulo (Se) -> Campinas: ~84 km
        assertEquals(84, ClusterizacaoHotspots.distanciaKm(-23.5505, -46.6333, -22.9056, -47.0608), 3);
    }
}
