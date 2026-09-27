# 0003. Nenhuma ordenação pronta em produção

**Situação:** aceita

## Contexto
Usar `Collections.sort` ou uma `TreeMap` em qualquer ponto do sistema esvaziaria o objetivo do trabalho, e seria fácil fazer isso sem perceber (por exemplo, para montar um ranking na interface).

## Decisão
O código de produção não usa `Collections.sort`, `Arrays.sort`, `List.sort`, `stream().sorted()`, `TreeMap`, `TreeSet` nem `PriorityQueue`. Quando o sistema precisa ordenar internamente, chama `Ordenacoes.ordenar`, que usa o Merge Sort do projeto. O `ArquiteturaTest` varre `src/main` e o Checkstyle repete a verificação: o build falha se a regra for quebrada. Nos testes, `Collections.sort` é permitido como referência.

## Consequências
- A regra é verificável pela banca e pelo CI, não só uma promessa.
- Rankings da interface (Top 10, heap Top-K) exercitam as estruturas do próprio projeto.
