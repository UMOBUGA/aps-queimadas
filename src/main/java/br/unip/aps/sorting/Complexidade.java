package br.unip.aps.sorting;

/**
 * Ficha tecnica de um algoritmo de ordenacao (exibida no dashboard e nos relatorios).
 *
 * @param melhorCaso   complexidade de tempo no melhor caso
 * @param casoMedio    complexidade de tempo no caso medio
 * @param piorCaso     complexidade de tempo no pior caso
 * @param espaco       memoria auxiliar
 * @param estavel      se preserva a ordem relativa de elementos com chaves iguais
 * @param inPlace      se ordena usando O(1) (ou O(log n) de pilha) de memoria extra
 * @param quadratico   se o caso medio e O(n^2) (usado para avisos de desempenho)
 * @param baseadoEmComparacao {@code false} para algoritmos que nao comparam (Radix)
 */
public record Complexidade(String melhorCaso, String casoMedio, String piorCaso, String espaco,
                           boolean estavel, boolean inPlace, boolean quadratico,
                           boolean baseadoEmComparacao) {
}
