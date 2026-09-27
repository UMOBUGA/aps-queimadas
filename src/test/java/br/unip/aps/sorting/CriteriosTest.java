package br.unip.aps.sorting;

import br.unip.aps.Focos;
import br.unip.aps.model.FocoIncendio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Criterios de ordenacao")
class CriteriosTest {
    private static final LocalDateTime D = LocalDateTime.of(2024, 8, 20, 17, 0);

    @Test
    @DisplayName("municipios com acento seguem a ordem alfabetica pt-BR (Collator)")
    void colacaoPtBr() {
        List<FocoIncendio> l = new ArrayList<>(List.of(
                Focos.foco("ZACARIAS", "Cerrado", D, 1),
                Focos.foco("ÁGUAS DE LINDÓIA", "Cerrado", D, 2),
                Focos.foco("BAURU", "Cerrado", D, 3),
                Focos.foco("ÉLIAS FAUSTO", "Cerrado", D, 4),
                Focos.foco("AGUAÍ", "Cerrado", D, 5)));
        FocoIncendio[] a = l.toArray(new FocoIncendio[0]);
        AlgoritmoTipo.MERGE.criar().ordenar(a, Criterios.POR_MUNICIPIO);
        List<String> nomes = new ArrayList<>();
        for (FocoIncendio f : a) nomes.add(f.getMunicipio());
        assertEquals(List.of("AGUAÍ", "ÁGUAS DE LINDÓIA", "BAURU", "ÉLIAS FAUSTO", "ZACARIAS"), nomes);
    }

    @Test
    @DisplayName("String.compareTo colocaria 'ÁGUAS' depois de 'ZACARIAS' (motivo do Collator)")
    void compareToIngenuo() {
        assertTrue("ÁGUAS DE LINDÓIA".compareTo("ZACARIAS") > 0);
    }

    @Test
    @DisplayName("descricao e chave numerica de criterios")
    void descricaoEChave() {
        Criterios c = Criterios.composto(List.of(
                new Criterios.Nivel(CriterioOrdenacao.BIOMA, Ordem.CRESCENTE),
                new Criterios.Nivel(CriterioOrdenacao.DATA, Ordem.DECRESCENTE),
                new Criterios.Nivel(CriterioOrdenacao.BIOMA, Ordem.DECRESCENTE)));
        assertEquals(2, c.niveis().size(), "niveis repetidos sao ignorados");
        assertEquals("Bioma ↑ → Data/hora ↓", c.descricao());
        assertNull(c.chaveNumerica(), "multicriterio nao tem chave numerica");
        assertNotNull(Criterios.de(CriterioOrdenacao.DATA, Ordem.CRESCENTE).chaveNumerica());
        assertNull(Criterios.de(CriterioOrdenacao.MUNICIPIO, Ordem.CRESCENTE).chaveNumerica());
        assertThrows(IllegalArgumentException.class, () -> Criterios.composto(List.of()));
    }

    @Test
    @DisplayName("chave de double preserva a ordem, inclusive negativos")
    void chaveDouble() {
        double[] v = {-48.5, -48.4, -22.9, -0.0, 0.0, 1.5, 300};
        for (int i = 1; i < v.length; i++) {
            assertTrue(CriterioOrdenacao.chaveDouble(v[i - 1]) <= CriterioOrdenacao.chaveDouble(v[i]), v[i - 1] + " vs " + v[i]);
        }
    }

    @Test
    @DisplayName("campos opcionais nulos vao para o final em ambas as ordens")
    void nulosNoFinal() {
        FocoIncendio com = FocoIncendio.builder().municipio("A").bioma("Cerrado").dataHora(D).frp(10.0).build();
        FocoIncendio sem = FocoIncendio.builder().municipio("B").bioma("Cerrado").dataHora(D).build();
        assertTrue(CriterioOrdenacao.FRP.comparador(Ordem.CRESCENTE).compare(com, sem) < 0);
        assertTrue(CriterioOrdenacao.FRP.comparador(Ordem.DECRESCENTE).compare(com, sem) < 0);
    }
}
