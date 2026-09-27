package br.unip.aps;

import br.unip.aps.model.FocoIncendio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Fabrica de focos sinteticos para os testes.
 */
public final class Focos {

    private static final String[] MUNICIPIOS = {"SÃO CARLOS", "ÁGUAS DE LINDÓIA", "BAURU", "ANDRADINA", "ÉLIAS FAUSTO",
            "ZACARIAS", "ITU", "ARAÇATUBA", "SANTO ANTÔNIO DO ARACANGUÁ", "AGUAÍ"};
    private static final String[] BIOMAS = {"Cerrado", "Mata Atlântica"};

    private Focos() { }

    /**
     * @param municipio municipio
     * @param bioma     bioma
     * @param data      data/hora
     * @param id        id
     * @return foco
     */
    public static FocoIncendio foco(String municipio, String bioma, LocalDateTime data, long id) {
        return FocoIncendio.builder().idBdq(id).focoId("f-" + id).municipio(municipio).bioma(bioma).dataHora(data)
                .latitude(-22 - (id % 100) / 100.0).longitude(-48 + (id % 37) / 50.0).estado("SÃO PAULO").build();
    }

    /**
     * @param n       quantidade
     * @param semente semente
     * @return focos aleatorios em 2023-2024, com muitas repeticoes de municipio/bioma
     */
    public static List<FocoIncendio> aleatorios(int n, long semente) {
        Random r = new Random(semente);
        List<FocoIncendio> l = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            LocalDateTime d = LocalDateTime.of(2023 + r.nextInt(2), 1 + r.nextInt(12), 1 + r.nextInt(28), r.nextInt(24), r.nextInt(60));
            l.add(foco(MUNICIPIOS[r.nextInt(MUNICIPIOS.length)], BIOMAS[r.nextInt(BIOMAS.length)], d, i));
        }
        return l;
    }
}
