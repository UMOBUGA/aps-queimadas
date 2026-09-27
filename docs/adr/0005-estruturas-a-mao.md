# 0005. Estruturas de dados próprias e instrumentadas

**Situação:** aceita

## Contexto
Além da ordenação, a banca pergunta por busca e indexação. Usar `HashMap` ou `TreeMap` esconderia exatamente o que a disciplina estuda.

## Decisão
Nos pacotes `estruturas` e `busca`, as estruturas são feitas à mão e contam operações: busca binária (lower/upper bound), árvore AVL com rotações observáveis, tabela hash com FNV-1a e encadeamento, heap binário com Top-K, External Merge Sort e Merge Sort paralelo. O `ArquiteturaTest` proíbe as coleções equivalentes da biblioteca nesses pacotes.

## Consequências
- É possível comparar busca linear, binária, AVL e hash pelo número de comparações, não só pelo tempo.
- Mais código para manter e testar; a cobertura mínima desses pacotes é 85%.
