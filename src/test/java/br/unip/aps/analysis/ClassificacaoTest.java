package br.unip.aps.analysis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Classes do mapa coropletico. */
@DisplayName("Classificação: quantis e Jenks")
class ClassificacaoTest {
    private static final double[] GRUPOS = {22, 1, 11, 2, 20, 3, 10, 21, 12};

    @Test
    void jenksEncontraOsGruposNaturais() {
        assertArrayEquals(new double[]{3, 12, 22}, Classificacao.limites(GRUPOS, 3, Classificacao.Metodo.JENKS));
    }

    @Test
    void quantisDividemEmPartesIguais() {
        assertArrayEquals(new double[]{3, 12, 22}, Classificacao.limites(GRUPOS, 3, Classificacao.Metodo.QUANTIS));
        double[] l = Classificacao.limites(new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, 5, Classificacao.Metodo.QUANTIS);
        assertArrayEquals(new double[]{2, 4, 6, 8, 10}, l);
    }

    @Test
    void jenksSeparaOutlier() {
        double[] l = Classificacao.limites(new double[]{1, 1, 2, 2, 3, 3, 4, 100}, 2, Classificacao.Metodo.JENKS);
        assertEquals(4, l[0]);
        assertEquals(100, l[1]);
        assertEquals(1, Classificacao.classe(100, l));
        assertEquals(0, Classificacao.classe(2, l));
    }

    @Test
    void poucosValoresDistintosReduzemAsClasses() {
        assertEquals(2, Classificacao.limites(new double[]{5, 5, 7, 7}, 5, Classificacao.Metodo.JENKS).length);
    }
}
