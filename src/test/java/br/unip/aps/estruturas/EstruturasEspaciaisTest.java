package br.unip.aps.estruturas;

import br.unip.aps.Focos;
import br.unip.aps.busca.ServicoGeografico;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.sorting.OperationCounter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Trie, arvore k-d e grafo")
class EstruturasEspaciaisTest {

    @Test
    @DisplayName("Trie: prefixo sem acento, ordem alfabetica, contagem e chave exata")
    void trie() {
        Trie<Integer> t = new Trie<>(new OperationCounter());
        String[] nomes = {"São Paulo", "SÃO CARLOS", "Santos", "São José dos Campos", "Sorocaba", "Campinas"};
        for (int i = 0; i < nomes.length; i++) t.inserir(nomes[i], i);
        t.inserir("Santos", 99);
        assertEquals(6, t.chaves());
        assertEquals(List.of("SÃO CARLOS", "São José dos Campos", "São Paulo"), t.comPrefixo("sao", 10));
        assertEquals(4, t.contarPrefixo("SA") - 1);
        assertEquals(List.of(2, 99), t.buscar("SANTOS"));
        assertTrue(t.buscar("San").isEmpty());
        assertEquals(List.of("Santos"), t.comPrefixo("s", 1));
        assertTrue(t.comPrefixo("xyz", 5).isEmpty());
    }

    @Test
    @DisplayName("Arvore k-d devolve exatamente os mesmos pontos que a forca bruta, calculando menos distancias")
    void arvoreKdIgualForcaBruta() {
        Random r = new Random(5);
        List<double[]> pontos = new ArrayList<>();
        for (int i = 0; i < 5_000; i++) pontos.add(new double[]{-25 + r.nextDouble() * 5, -53 + r.nextDouble() * 9});
        ArvoreKD<double[]> kd = new ArvoreKD<>(pontos, p -> p[0], p -> p[1], new OperationCounter());
        assertTrue(kd.altura() <= 14, "altura " + kd.altura());
        long calculos = 0;
        for (int q = 0; q < 300; q++) {
            double lat = -25 + r.nextDouble() * 5, lon = -53 + r.nextDouble() * 9, raio = 5 + r.nextDouble() * 60;
            List<double[]> achados = new ArrayList<>();
            calculos += kd.noRaio(lat, lon, raio, achados::add);
            int esperado = 0;
            for (double[] p : pontos) if (ArvoreKD.distanciaKm(lat, lon, p[0], p[1]) <= raio) esperado++;
            assertEquals(esperado, achados.size(), "consulta " + q);
        }
        assertTrue(calculos < 300L * pontos.size() / 5, "a arvore deveria podar a maior parte: " + calculos);
    }

    @Test
    @DisplayName("Grafo: busca em largura calcula a distancia em arestas; laco e aresta repetida sao ignorados")
    void grafoBfs() {
        Grafo g = new Grafo(new OperationCounter());
        g.ligar("A", "B");
        g.ligar("B", "C");
        g.ligar("C", "D");
        g.ligar("A", "C");
        g.ligar("A", "B");
        g.ligar("E", "E");
        assertEquals(4, g.arestas());
        int[] d = g.distancias(g.indiceDe("A"));
        assertEquals(0, d[g.indiceDe("A")]);
        assertEquals(1, d[g.indiceDe("C")]);
        assertEquals(2, d[g.indiceDe("D")]);
        assertEquals(-1, d[g.indiceDe("E")]);
        assertEquals(List.of("B", "C"), g.vizinhosDe("A"));
    }

    @Test
    @DisplayName("Servico geografico: malha de SP vira grafo conexo e as consultas batem com a varredura")
    void servicoGeografico() {
        ServicoGeografico s = new ServicoGeografico();
        Grafo g = s.grafo();
        assertTrue(g.vertices() >= 640, "vertices " + g.vertices());
        int[] d = g.distancias(g.indiceDe(br.unip.aps.geo.MalhaMunicipal.chave("SAO PAULO")));
        int alcancaveis = 0;
        for (int x : d) if (x >= 0) alcancaveis++;
        assertEquals(g.vertices(), alcancaveis, "o grafo dos municipios de SP deve ser conexo");

        List<FocoIncendio> focos = Focos.aleatorios(2_000, 8);
        ServicoGeografico.Raio raio = s.raio(focos, focos.get(0).getMunicipio(), 50);
        assertTrue(raio.distanciasArvore() <= raio.distanciasLinear());
        ServicoGeografico.Prefixo p = s.prefixo(focos, focos.get(0).getMunicipio().substring(0, 3), 50);
        assertTrue(p.nomes().contains(focos.get(0).getMunicipio()));
        assertTrue(p.passosTrie() < p.comparacoesLinear());
        List<ServicoGeografico.Camada> c = s.camadas(focos, focos.get(0).getMunicipio(), 3);
        assertEquals(1, c.get(0).municipios());
        ServicoGeografico.Propagacao pr = s.propagacao(focos);
        assertTrue(pr.casosComVizinho() + pr.casosSemVizinho() > 0);
    }
}
