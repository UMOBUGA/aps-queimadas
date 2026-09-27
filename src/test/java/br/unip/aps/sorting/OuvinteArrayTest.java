package br.unip.aps.sorting;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Comparator;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * O ouvinte do {@link InstrumentedArray} (usado pela visualizacao animada) nao pode alterar a
 * ordenacao nem a contagem, e a reproducao dos eventos gravados deve reconstruir o vetor ordenado.
 */
@DisplayName("Ouvinte de operacoes (visualizacao animada)")
class OuvinteArrayTest {

    @ParameterizedTest(name = "{0}")
    @EnumSource(AlgoritmoTipo.class)
    void eventosReconstroemAOrdenacaoSemAlterarAContagem(AlgoritmoTipo tipo) {
        Integer[] original = new Random(3).ints(40, 0, 25).boxed().toArray(Integer[]::new);

        Integer[] semOuvinte = original.clone();
        OperationMetrics m1 = tipo.criar().ordenar(semOuvinte, Comparator.naturalOrder(), Integer::longValue);

        Integer[] comOuvinte = original.clone();
        Integer[] replay = original.clone();
        long[] comparacoesVistas = {0};
        OperationCounter contador = new OperationCounter();
        InstrumentedArray<Integer> a = new InstrumentedArray<>(comOuvinte, Comparator.naturalOrder(), Integer::longValue, contador,
                new InstrumentedArray.Ouvinte() {
                    @Override
                    public void comparacao(int i, int j) {
                        comparacoesVistas[0]++;
                    }

                    @Override
                    public void troca(int i, int j) {
                        Integer t = replay[i];
                        replay[i] = replay[j];
                        replay[j] = t;
                    }

                    @Override
                    public void atribuicao(int i, Object valor) {
                        replay[i] = (Integer) valor;
                    }
                });
        tipo.criar().ordenar(a);

        assertArrayEquals(semOuvinte, comOuvinte, "o ouvinte nao altera o resultado");
        assertArrayEquals(semOuvinte, replay, "reproduzir os eventos reconstroi o vetor ordenado");
        assertEquals(m1.comparacoes(), contador.getComparacoes(), "o ouvinte nao altera a contagem");
        assertEquals(m1.trocas(), contador.getTrocas());
        assertEquals(m1.atribuicoes(), contador.getAtribuicoes());
        assertEquals(true, comparacoesVistas[0] <= contador.getComparacoes());
    }
}
