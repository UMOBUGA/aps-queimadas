package br.unip.aps.analysis;

/**
 * Par (categoria, quantidade) usado em rankings e graficos.
 *
 * @param chave categoria (municipio, bioma, mes...)
 * @param total quantidade de focos
 */
public record Contagem(String chave, long total) {
}
