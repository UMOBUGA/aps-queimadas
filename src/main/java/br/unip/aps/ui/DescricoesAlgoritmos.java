package br.unip.aps.ui;

import br.unip.aps.sorting.AlgoritmoTipo;

/** Textos de interface (pt-BR com acentuacao) que explicam cada algoritmo na tela de Ordenacao. */
final class DescricoesAlgoritmos {
    private DescricoesAlgoritmos() { }

    static String de(AlgoritmoTipo t) {
        return switch (t) {
            case BUBBLE -> "Compara pares vizinhos e troca os que estão fora de ordem; o maior elemento “flutua” para o fim a cada "
                    + "passada. Para cedo se uma passada não fizer trocas.";
            case SELECTION -> "Seleciona o menor elemento da parte não ordenada e o coloca na próxima posição: sempre n(n−1)/2 "
                    + "comparações, mas no máximo n−1 trocas.";
            case INSERTION -> "Insere cada elemento na posição correta do prefixo já ordenado, deslocando os maiores. O custo é "
                    + "proporcional ao número de inversões: ótimo para dados quase ordenados.";
            case SHELL -> "Insertion Sort com saltos decrescentes (sequência de Ciura): move elementos distantes cedo e termina "
                    + "com h = 1 sobre um vetor quase ordenado.";
            case MERGE -> "Divide o vetor ao meio, ordena cada metade recursivamente e intercala as metades: O(n log n) garantido "
                    + "e estável, ao custo de O(n) de memória auxiliar.";
            case QUICK -> "Particiona em torno de um pivô (mediana de três, partição de Hoare) e ordena as partes; em média o mais "
                    + "rápido na prática, mas O(n²) no pior caso e não estável.";
            case QUICK_3WAY -> "Quick Sort com partição em três faixas (<, =, >) de Dijkstra: os iguais ao pivô não são "
                    + "reprocessados — ideal para critérios com muitas repetições (bioma, município).";
            case HEAP -> "Monta um max-heap e extrai repetidamente o maior elemento para o fim do vetor: O(n log n) garantido "
                    + "sem memória extra, porém não estável.";
            case TIM -> "Híbrido: ordena blocos de 32 com Insertion Sort binário e intercala os blocos de baixo para cima; "
                    + "estável e muito eficiente em dados parcialmente ordenados.";
            case RADIX -> "Distribui os elementos pelos bytes da chave numérica (Counting Sort estável por dígito): zero "
                    + "comparações e tempo linear.";
        };
    }
}
