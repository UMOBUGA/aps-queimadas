# 0002. Contar operações por um vetor instrumentado

**Situação:** aceita

## Contexto
Comparar algoritmos só pelo tempo é frágil (JIT, coletor de lixo, máquina). O enunciado valoriza a análise de complexidade, que depende de contar comparações, trocas e acessos com exatidão.

## Decisão
Todo algoritmo recebe um `InstrumentedArray<T>` e só acessa os dados por `get`, `set`, `swap`, `less` e `compare`. O vetor soma as operações num `OperationCounter` e avisa ouvintes (padrão Observer), que alimentam a animação da ordenação. Os algoritmos implementam `SortAlgorithm` (Strategy) e são criados pelo `AlgoritmoTipo` (Factory).

## Consequências
- A contagem é uniforme: um algoritmo não consegue "esquecer" de contar.
- A mesma execução serve para métricas, animação e verificação.
- Há um custo de indireção; por isso o tempo é validado à parte com JMH (ADR 0007).
