package br.unip.aps.ui.componentes;

import br.unip.aps.sorting.AlgoritmoTipo;

import java.util.List;

/** Pseudocodigo de cada algoritmo e a linha que corresponde a cada tipo de operacao (comparacao, troca, escrita). */
public record Pseudocodigo(List<String> linhas, int comparacao, int troca, int escrita) {

    /** Pseudocodigo do algoritmo, em portugues, no nivel de detalhe das operacoes contadas. */
    public static Pseudocodigo de(AlgoritmoTipo tipo) {
        return switch (tipo) {
            case BUBBLE -> new Pseudocodigo(List.of(
                    "para i de 0 até n−2:",
                    "    trocou ← falso",
                    "    para j de 0 até n−2−i:",
                    "        se a[j] > a[j+1]:",
                    "            trocar a[j] e a[j+1]",
                    "            trocou ← verdadeiro",
                    "    se não trocou: parar"), 3, 4, 4);
            case SELECTION -> new Pseudocodigo(List.of(
                    "para i de 0 até n−2:",
                    "    min ← i",
                    "    para j de i+1 até n−1:",
                    "        se a[j] < a[min]: min ← j",
                    "    trocar a[i] e a[min]"), 3, 4, 4);
            case INSERTION -> new Pseudocodigo(List.of(
                    "para i de 1 até n−1:",
                    "    x ← a[i];  j ← i − 1",
                    "    enquanto j ≥ 0 e a[j] > x:",
                    "        a[j+1] ← a[j];  j ← j − 1",
                    "    a[j+1] ← x"), 2, 3, 3);
            case SHELL -> new Pseudocodigo(List.of(
                    "para cada salto h de Ciura (maior → menor):",
                    "    para i de h até n−1:",
                    "        x ← a[i];  j ← i",
                    "        enquanto j ≥ h e a[j−h] > x:",
                    "            a[j] ← a[j−h];  j ← j − h",
                    "        a[j] ← x"), 3, 4, 4);
            case MERGE -> new Pseudocodigo(List.of(
                    "ordenar(a, lo, hi):",
                    "    se lo ≥ hi: retornar",
                    "    meio ← (lo + hi) / 2",
                    "    ordenar(a, lo, meio);  ordenar(a, meio+1, hi)",
                    "    copiar a[lo..hi] para aux",
                    "    intercalar: se aux[i] ≤ aux[j]",
                    "        a[k] ← o menor dos dois;  k ← k + 1"), 5, 6, 6);
            case QUICK -> new Pseudocodigo(List.of(
                    "ordenar(a, lo, hi):",
                    "    pivô ← mediana de a[lo], a[meio], a[hi]",
                    "    i ← lo;  j ← hi + 1",
                    "    avançar i enquanto a[i] < pivô; recuar j enquanto pivô < a[j]",
                    "    se i < j: trocar a[i] e a[j]; repetir",
                    "    colocar o pivô em a[j]",
                    "    ordenar a parte menor; repetir na maior"), 1, 4, 4);
            case QUICK_3WAY -> new Pseudocodigo(List.of(
                    "ordenar(a, lo, hi):",
                    "    v ← a[lo];  lt ← lo;  gt ← hi;  i ← lo + 1",
                    "    enquanto i ≤ gt:",
                    "        se a[i] < v: trocar a[lt] e a[i]; lt++; i++",
                    "        senão se a[i] > v: trocar a[i] e a[gt]; gt−−",
                    "        senão: i++",
                    "    ordenar(a, lo, lt−1);  ordenar(a, gt+1, hi)"), 2, 3, 3);
            case QUICK_2PIVOS -> new Pseudocodigo(List.of(
                    "ordenar(a, lo, hi):",
                    "    se o trecho é pequeno: Insertion Sort; retornar",
                    "    p ← a[lo];  q ← a[hi]  (pivôs nos terços, p ≤ q)",
                    "    para cada a[k] entre lt e gt:",
                    "        se a[k] < p: levar para a esquerda",
                    "        senão se a[k] > q: levar para a direita",
                    "    colocar p e q nas posições finais",
                    "    ordenar as três partes"), 3, 4, 4);
            case INTRO -> new Pseudocodigo(List.of(
                    "introsort(a, lo, hi, profundidade):",
                    "    enquanto o trecho tem mais de 16 elementos:",
                    "        se profundidade = 0: Heap Sort no trecho; retornar",
                    "        p ← particionar (mediana de três, Hoare)",
                    "        introsort na parte menor; continuar na maior",
                    "Insertion Sort final nos trechos pequenos"), 3, 3, 3);
            case HEAP -> new Pseudocodigo(List.of(
                    "para k de n/2−1 até 0: afundar(k)      (monta o max-heap)",
                    "enquanto n > 1:",
                    "    trocar a[0] e a[n−1];  n ← n − 1",
                    "    afundar(0):",
                    "        j ← maior filho de k",
                    "        se a[k] < a[j]: trocar a[k] e a[j]; k ← j"), 4, 5, 5);
            case TIM -> new Pseudocodigo(List.of(
                    "para cada bloco de 32 elementos:",
                    "    busca binária da posição de a[i] no bloco",
                    "    deslocar os maiores e inserir a[i]",
                    "tamanho ← 32",
                    "enquanto tamanho < n:",
                    "    intercalar blocos vizinhos de 'tamanho'",
                    "    tamanho ← 2 · tamanho"), 1, 2, 2);
            case RADIX -> new Pseudocodigo(List.of(
                    "para cada byte da chave (menos → mais significativo):",
                    "    contar quantos elementos têm cada valor do byte",
                    "    somar as contagens (posição inicial de cada valor)",
                    "    distribuir em aux, na ordem, de forma estável",
                    "    copiar aux de volta para a"), -1, 4, 4);
            case COUNTING -> new Pseudocodigo(List.of(
                    "ler as chaves; achar a menor e a maior",
                    "contar quantas vezes cada chave aparece",
                    "somar as contagens (posição inicial de cada chave)",
                    "distribuir em aux, na ordem, de forma estável",
                    "copiar aux de volta para a"), -1, 4, 4);
        };
    }
}
