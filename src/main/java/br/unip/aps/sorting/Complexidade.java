package br.unip.aps.sorting;

/** Ficha tecnica de um algoritmo de ordenacao (exibida no dashboard e nos relatorios). */
public record Complexidade(String melhorCaso, String casoMedio, String piorCaso, String espaco,
                           boolean estavel, boolean inPlace, boolean quadratico,
                           boolean baseadoEmComparacao) {
}
