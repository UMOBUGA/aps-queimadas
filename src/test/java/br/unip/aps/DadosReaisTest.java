package br.unip.aps;

import br.unip.aps.io.RepositorioDados;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.sorting.AlgoritmoTipo;
import br.unip.aps.sorting.CenarioEntrada;
import br.unip.aps.sorting.CriterioOrdenacao;
import br.unip.aps.sorting.Criterios;
import br.unip.aps.sorting.Ordem;
import br.unip.aps.sorting.ResultadoOrdenacao;
import br.unip.aps.sorting.ServicoOrdenacao;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Testes de integracao com os CSVs reais do INPE (SP 2023-2024) em data/raw. */
@DisplayName("Integracao com os dados reais do INPE")
class DadosReaisTest {
    private static BaseDeFocos base;

    @BeforeAll
    static void carregar() throws Exception {
        Path dir = Path.of("data/raw");
        assumeTrue(Files.isRegularFile(dir.resolve("focos_br_sp_ref_2023.csv"))
                && Files.isRegularFile(dir.resolve("focos_br_sp_ref_2024.csv")), "CSVs do INPE ausentes");
        base = new RepositorioDados(dir).carregarTodos();
    }

    @Test
    void base() {
        assertEquals(10_378, base.tamanho());
        assertEquals(List.of(2023, 2024), base.anos());
        assertEquals(List.of("Cerrado", "Mata Atlântica"), base.biomas());
        assertEquals(0, base.getRelatorio().getTotalRejeitadas());
        assertEquals(List.of("SÃO PAULO"), base.estados());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(AlgoritmoTipo.class)
    @DisplayName("todo algoritmo ordena a base real pelos criterios exigidos (data, bioma, municipio)")
    void criteriosExigidos(AlgoritmoTipo tipo) throws Exception {
        ServicoOrdenacao s = new ServicoOrdenacao(Integer.MAX_VALUE);
        int n = tipo.quadratico() ? 3_000 : 0;
        List<CriterioOrdenacao> crits = new ArrayList<>(List.of(CriterioOrdenacao.DATA));
        if (!tipo.exigeChaveNumerica()) crits.addAll(List.of(CriterioOrdenacao.BIOMA, CriterioOrdenacao.MUNICIPIO));
        for (CriterioOrdenacao c : crits) {
            ResultadoOrdenacao<FocoIncendio> r = s.ordenar(base.getFocos(), new ServicoOrdenacao.Solicitacao(tipo,
                    Criterios.de(c, Ordem.CRESCENTE), n, true, CenarioEntrada.ALEATORIO, 42L));
            assertTrue(r.verificado(), tipo + " / " + c);
            assertTrue(tipo.exigeChaveNumerica() || r.metricas().comparacoes() > 0);
        }
    }

    @Test
    @DisplayName("os dados originais nao sao alterados pela ordenacao")
    void originalPreservado() throws Exception {
        List<FocoIncendio> antes = new ArrayList<>(base.getFocos());
        new ServicoOrdenacao(20_000).ordenar(base.getFocos(), ServicoOrdenacao.Solicitacao.de(AlgoritmoTipo.HEAP,
                Criterios.de(CriterioOrdenacao.MUNICIPIO, Ordem.DECRESCENTE)));
        assertEquals(antes, base.getFocos());
    }
}
