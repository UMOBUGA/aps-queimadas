package br.unip.aps.busca;

import br.unip.aps.analysis.Contagem;
import br.unip.aps.analysis.Estatisticas;
import br.unip.aps.io.RepositorioDados;
import br.unip.aps.model.BaseDeFocos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Todos os metodos de consulta devem achar os mesmos focos; so o custo muda. */
@DisplayName("Consultas: sequencial x binaria x AVL x hash")
class ServicoConsultasTest {
    private static BaseDeFocos base;

    @BeforeAll
    static void carregar() throws Exception {
        base = new RepositorioDados(Path.of("nao-existe")).carregarTodos();
    }

    @Test
    void municipiosMesmosResultadosCustosDiferentes() {
        List<String> m = ServicoConsultas.sortearMunicipios(base.getFocos(), 200, 42);
        List<ServicoConsultas.Custo> r = new ServicoConsultas().porMunicipio(base.getFocos(), m);
        assertEquals(4, r.size());
        long esperado = r.get(0).resultados();
        for (ServicoConsultas.Custo c : r) assertEquals(esperado, c.resultados(), c.metodo().rotulo());
        assertTrue(r.get(1).comparacoesPorConsulta() < 40, "busca binaria: ~2 log2(n)");
        assertTrue(r.get(0).comparacoesPorConsulta() >= base.tamanho(), "busca sequencial examina todos");
        assertTrue(r.get(3).comparacoesPorConsulta() < 3, "hash: O(1) esperado");
    }

    @Test
    void intervalosMesmosResultadosEHashNaoSeAplica() {
        List<LocalDateTime[]> i = ServicoConsultas.sortearIntervalos(base.getFocos(), 100, 7, 42);
        List<ServicoConsultas.Custo> r = new ServicoConsultas().porIntervalo(base.getFocos(), i);
        assertEquals(r.get(0).resultados(), r.get(1).resultados());
        assertEquals(r.get(0).resultados(), r.get(2).resultados());
        assertFalse(r.get(3).suportado());
    }

    @Test
    void topKPorHeapIgualAoRankingPorOrdenacao() {
        ServicoConsultas.TopK t = new ServicoConsultas().topK(base.getFocos(), 10);
        assertEquals(t.porOrdenacao(), t.porHeap());
        List<Contagem> ref = new Estatisticas(base.getFocos()).topMunicipios(10);
        assertEquals(ref.get(0).total(), t.porHeap().get(0).total());
        assertTrue(t.custoHeap().comparacoes() < t.custoOrdenacao().comparacoes());
    }
}
