package br.unip.aps.analysis;

import br.unip.aps.Focos;
import br.unip.aps.model.FocoIncendio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Estatisticas e filtros")
class EstatisticasTest {

    private final List<FocoIncendio> focos = List.of(
            Focos.foco("ITU", "Cerrado", LocalDateTime.of(2023, 8, 1, 17, 0), 1),
            Focos.foco("ITU", "Cerrado", LocalDateTime.of(2024, 8, 2, 17, 0), 2),
            Focos.foco("ITU", "Mata Atlântica", LocalDateTime.of(2024, 8, 3, 17, 0), 3),
            Focos.foco("BAURU", "Cerrado", LocalDateTime.of(2024, 9, 3, 16, 0), 4),
            Focos.foco("SÃO CARLOS", "Cerrado", LocalDateTime.of(2024, 9, 4, 16, 0), 5));

    @Test
    void agregacoes() {
        Estatisticas e = new Estatisticas(focos);
        assertEquals(5, e.total());
        assertEquals(List.of(2023, 2024), List.copyOf(e.porAno().keySet()));
        assertEquals(2, e.porMes(2024)[7]);
        assertEquals(new Contagem("ITU", 3), e.topMunicipios(1).get(0));
        assertEquals("Cerrado", e.porBioma().get(0).chave());
        assertEquals(3, e.municipiosAfetados());
        assertEquals(14, e.serieMensal().size(), "ago/2023 a set/2024, incluindo meses sem focos");
        assertEquals(3, e.porHoraLocal()[14], "17h GMT = 14h em Brasilia");
        assertEquals(2, e.porHoraLocal()[13], "16h GMT = 13h em Brasilia");
    }

    @Test
    void comparativoAnual() {
        List<Estatisticas.LinhaComparativo> c = new Estatisticas(focos).comparativo(2023, 2024);
        assertEquals(13, c.size());
        Estatisticas.LinhaComparativo ago = c.get(7);
        assertEquals(1, ago.anoA());
        assertEquals(2, ago.anoB());
        assertEquals(100.0, ago.variacao(), 1e-9);
        assertTrue(Double.isNaN(c.get(8).variacao()), "divisao por zero vira NaN");
        assertEquals(0, c.get(12).mes());
    }

    @Test
    void filtros() {
        FiltroFocos f = new FiltroFocos(Set.of(2024), Set.of("Cerrado"), "sao car", null, null);
        assertEquals(1, focos.stream().filter(f).count(), "busca sem acento encontra SÃO CARLOS");
        FiltroFocos periodo = new FiltroFocos(null, null, null, LocalDate.of(2024, 8, 2), LocalDate.of(2024, 8, 31));
        assertEquals(2, focos.stream().filter(periodo).count());
        assertTrue(FiltroFocos.TODOS.test(focos.get(0)));
        assertFalse(new FiltroFocos(Set.of(1999), null, null, null, null).test(focos.get(0)));
        assertThrows(IllegalArgumentException.class,
                () -> new FiltroFocos(null, null, null, LocalDate.of(2024, 2, 1), LocalDate.of(2024, 1, 1)));
    }
}
