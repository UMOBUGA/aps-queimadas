# Como contribuir

## Preparar o ambiente

- JDK 21 (Temurin recomendado). O Maven vem junto (`mvnw` / `mvnw.cmd`).
- `./mvnw clean verify` compila, roda os testes, confere estilo (Checkstyle), análise estática (PMD e SpotBugs) e a cobertura mínima (JaCoCo). Se passar aqui, passa no CI.

## Regras do enunciado (o build falha se forem quebradas)

1. **Nada de ordenação pronta em `src/main`:** `Collections.sort`, `Arrays.sort`, `List.sort`, `stream().sorted()`, `TreeMap`, `TreeSet`, `PriorityQueue`. Para ordenar, use `sorting.Ordenacoes.ordenar(lista, comparador)`. O `ArquiteturaTest` e o Checkstyle verificam.
2. **Algoritmos só acessam os dados pelo `InstrumentedArray`** (`get`, `set`, `swap`, `less`, `compare`), senão a contagem de operações fica errada.
3. **Estruturas em `estruturas` e `busca` são feitas à mão:** sem `HashMap`, `TreeMap` e afins como índice.
4. **Números na documentação vêm de execuções reais** (`docs/resultados`). Nunca digite uma métrica.

## Estilo

- Código, Javadoc e mensagens em **português**; mensagens de erro amigáveis, pensadas para o usuário final.
- Sem comentários de linha (`//`): nomes claros e Javadoc de uma linha nas classes e métodos públicos.
- Interface: cores só pelos tokens `-aps-*` do CSS; componentes reutilizáveis em `ui.componentes`; tarefas longas via `UiContexto.executar(...)`.

## Fluxo

1. Crie um ramo a partir de `main`.
2. Commits pequenos e descritivos (o que mudou e por quê).
3. Novo comportamento vem com teste. Novo algoritmo: siga a seção "Como adicionar um algoritmo" em `docs/ALGORITMOS.md`.
4. Abra o pull request; o CI precisa estar verde.

## Teste de mutação

```bash
./mvnw -Pmutacao test-compile org.pitest:pitest-maven:mutationCoverage
```

O relatório fica em `target/pit-reports/index.html`.

## Decisões de arquitetura

Mudanças estruturais ganham um ADR em `docs/adr` (contexto, decisão, consequências).
