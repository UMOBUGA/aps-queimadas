package br.unip.aps.geo;

import br.unip.aps.model.FocoIncendio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static br.unip.aps.Focos.foco;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Ficha do município")
class PerfilMunicipioTest {
    private static final String MATA = "Mata Atlântica";

    private static List<FocoIncendio> base() {
        List<FocoIncendio> r = new ArrayList<>();
        long id = 1;
        for (int i = 0; i < 3; i++) r.add(foco("CAMPINAS", MATA, LocalDateTime.of(2023, 8, 1 + i, 15, 0), id++));
        for (int i = 0; i < 5; i++) r.add(foco("CAMPINAS", MATA, LocalDateTime.of(2024, 8, 1 + i, 15, 0), id++));
        r.add(foco("CAMPINAS", "Cerrado", LocalDateTime.of(2024, 9, 2, 15, 0), id++));
        for (int i = 0; i < 4; i++) r.add(foco("SUMARÉ", MATA, LocalDateTime.of(2024, 8, 10 + i, 15, 0), id++));
        r.add(foco("VALINHOS", MATA, LocalDateTime.of(2023, 7, 1, 15, 0), id++));
        for (int i = 0; i < 12; i++) r.add(foco("PRESIDENTE PRUDENTE", "Cerrado", LocalDateTime.of(2024, 9, 1 + i, 15, 0), id++));
        return r;
    }

    @Test
    @DisplayName("conta focos por ano e por mês, acha o pico e o bioma principal")
    void totaisPorAnoEMes() {
        PerfilMunicipio p = PerfilMunicipio.de(base(), "campínas");
        assertEquals("Campinas", p.nome());
        assertEquals("CAMPINAS", p.nomeInpe());
        assertEquals(9, p.total());
        assertEquals(2, p.porAno().size());
        assertEquals(3, p.porAno().get(0).total());
        assertEquals(6, p.porAno().get(1).total());
        assertEquals(24, p.porMes().size());
        assertEquals(5, p.porMes().get(12 + 7).focos());
        assertEquals(new PerfilMunicipio.Mes(2024, 8, 5), p.pico());
        assertEquals(MATA, p.biomaPrincipal());
        assertTrue(p.densidade() > 0);
    }

    @Test
    @DisplayName("posição no ranking considera só quem tem mais focos")
    void ranking() {
        PerfilMunicipio p = PerfilMunicipio.de(base(), "CAMPINAS");
        assertEquals(2, p.posicao());
        assertEquals(4, p.municipiosComFocos());
        assertEquals(1, PerfilMunicipio.de(base(), "PRESIDENTE PRUDENTE").posicao());
    }

    @Test
    @DisplayName("vizinhos com focos vêm da malha do IBGE, do maior para o menor")
    void vizinhos() {
        PerfilMunicipio p = PerfilMunicipio.de(base(), "CAMPINAS");
        assertTrue(p.vizinhos() >= 5);
        assertEquals(2, p.vizinhosComFocos().size());
        assertEquals("Sumaré", p.vizinhosComFocos().get(0).chave());
        assertEquals(4, p.vizinhosComFocos().get(0).total());
        assertEquals("Valinhos", p.vizinhosComFocos().get(1).chave());
    }

    @Test
    @DisplayName("todas as fichas de uma vez batem com as fichas avulsas e vêm do maior para o menor")
    void todos() {
        List<PerfilMunicipio> todos = PerfilMunicipio.todos(base());
        assertEquals(4, todos.size());
        assertEquals("Presidente Prudente", todos.get(0).nome());
        assertEquals("Valinhos", todos.get(3).nome());
        PerfilMunicipio campinas = todos.get(1);
        assertEquals(PerfilMunicipio.de(base(), "CAMPINAS"), campinas);
        assertEquals(100.0, campinas.variacao(), 1e-9);
        assertTrue(Double.isNaN(todos.get(0).variacao()), "sem focos no primeiro ano, a variação não existe");
    }

    @Test
    @DisplayName("município da malha sem focos tem ficha zerada; nome desconhecido é recusado")
    void semFocosEDesconhecido() {
        PerfilMunicipio p = PerfilMunicipio.de(base(), "Jundiaí");
        assertEquals(0, p.total());
        assertEquals(0, p.posicao());
        assertNull(p.pico());
        assertEquals("", p.biomaPrincipal());
        assertThrows(IllegalArgumentException.class, () -> PerfilMunicipio.de(base(), "Cidade Que Não Existe"));
    }
}
