package br.unip.aps.ui.componentes;

import br.unip.aps.ui.FiltroGlobal;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Paleta de comandos (busca aproximada) e visoes salvas da barra de filtros. */
class ExperienciaTest {

    @Test
    void trechoExatoVenceSubsequencia() {
        int exato = PaletaComandos.pontuar("mapa", "mapa de focos");
        int sub = PaletaComandos.pontuar("mapa", "modo apresentação de ml");
        assertTrue(exato > sub, "o trecho exato deve pontuar mais");
    }

    @Test
    void subsequenciaCasaForaDeOrdemNao() {
        assertTrue(PaletaComandos.pontuar("ordn", "ordenação") > 0);
        assertEquals(-1, PaletaComandos.pontuar("zq", "ordenação"));
        assertEquals(-1, PaletaComandos.pontuar("aom", "mao"));
    }

    @Test
    void inicioDePalavraGanhaBonus() {
        assertTrue(PaletaComandos.pontuar("sj", "sao jose") > PaletaComandos.pontuar("sj", "casaja"));
    }

    @Test
    void visaoSalvaVoltaIgual() {
        FiltroGlobal f = new FiltroGlobal(Set.of(2023, 2024), Set.of("Cerrado"), "RIBEIRAO PRETO", true,
                YearMonth.of(2024, 8), YearMonth.of(2024, 9));
        assertEquals(f, FilterBar.desserializar(FilterBar.serializar(f)));
    }

    @Test
    void visaoVaziaEVisaoAntigaSaoAceitas() {
        assertEquals(FiltroGlobal.VAZIO, FilterBar.desserializar(FilterBar.serializar(FiltroGlobal.VAZIO)));
        FiltroGlobal curta = FilterBar.desserializar("2024");
        assertEquals(Set.of(2024), curta.anos());
        assertEquals(List.of(), List.copyOf(curta.biomas()));
    }
}
