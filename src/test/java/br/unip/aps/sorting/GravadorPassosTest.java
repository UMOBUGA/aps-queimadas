package br.unip.aps.sorting;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Passos gravados para a animação")
class GravadorPassosTest {
    private static final int[] VETOR = {9, 3, 7, 1, 8, 2, 10, 5, 4, 6, 3};

    @ParameterizedTest
    @EnumSource(AlgoritmoTipo.class)
    @DisplayName("reproduzir os passos ordena o vetor e as contagens batem com as do algoritmo")
    void reproduzirOrdena(AlgoritmoTipo tipo) {
        List<GravadorPassos.Passo> passos = GravadorPassos.gravar(tipo, VETOR);
        int[] v = VETOR.clone();
        long comparacoes = 0;
        for (GravadorPassos.Passo p : passos) {
            switch (p.tipo()) {
                case GravadorPassos.COMPARA -> comparacoes++;
                case GravadorPassos.TROCA -> {
                    int x = v[p.i()];
                    v[p.i()] = v[p.j()];
                    v[p.j()] = x;
                }
                default -> v[p.i()] = p.valor();
            }
        }
        int[] esperado = VETOR.clone();
        java.util.Arrays.sort(esperado);
        assertArrayEquals(esperado, v, tipo.nome());

        Integer[] caixa = new Integer[VETOR.length];
        for (int i = 0; i < caixa.length; i++) caixa[i] = VETOR[i];
        OperationCounter c = new OperationCounter();
        tipo.criar().ordenar(new InstrumentedArray<>(caixa, java.util.Comparator.naturalOrder(), Integer::longValue, c));
        assertEquals(c.snapshot().comparacoes(), comparacoes, "comparações gravadas × contadas no " + tipo.nome());
    }
}
