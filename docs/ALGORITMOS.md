# Fase 3 — Algoritmos de ordenação

Todos os algoritmos foram implementados à mão em `src/main/java/br/unip/aps/sorting/algorithms/`. Nenhum código de produção usa `Collections.sort`, `Arrays.sort`, `List.sort`, `Stream.sorted`, `TreeMap`/`TreeSet` ou `PriorityQueue`. O teste `ArquiteturaTest` varre o código-fonte e **falha o build** se alguém usar um deles.

Até a ordenação por clique no cabeçalho das tabelas do JavaFX foi desativada, porque internamente ela usa `Collections.sort`.

## 1. Quadro comparativo

| Algoritmo | Melhor | Médio | Pior | Espaço extra | Estável | In-place | Ideia central |
|---|---|---|---|---|---|---|---|
| Bubble Sort | O(n) | O(n²) | O(n²) | O(1) | ✔ | ✔ | Troca vizinhos fora de ordem; para cedo se não houver trocas |
| Selection Sort | O(n²) | O(n²) | O(n²) | O(1) | ✘ | ✔ | Seleciona o mínimo; no máximo n−1 trocas |
| Insertion Sort | O(n) | O(n²) | O(n²) | O(1) | ✔ | ✔ | Insere no prefixo ordenado; custo ∝ inversões |
| Shell Sort (Ciura) | O(n log n) | ~O(n^1,25) | O(n^1,5) | O(1) | ✘ | ✔ | Insertion com saltos decrescentes |
| Merge Sort | O(n)* | O(n log n) | O(n log n) | O(n) | ✔ | ✘ | Divide ao meio e intercala |
| Quick Sort | O(n log n) | O(n log n) | O(n²) | O(log n) | ✘ | ✔ | Particiona em torno do pivô (mediana de três) |
| Quick Sort 3-Way | O(n) | O(n log n) | O(n²) | O(log n) | ✘ | ✔ | Partição <, =, > (Dijkstra) |
| Quick Sort 2 pivôs | O(n log n) | O(n log n) | O(n²) | O(log n) | ✘ | ✔ | Dois pivôs, três partes (Yaroslavskiy); base do `Arrays.sort` do Java para números |
| Intro Sort | O(n log n) | O(n log n) | O(n log n) | O(log n) | ✘ | ✔ | Quick Sort que vira Heap Sort passando de 2·log₂(n) de profundidade (Musser) |
| Heap Sort | O(n log n) | O(n log n) | O(n log n) | O(1) | ✘ | ✔ | Max-heap e extração repetida do máximo |
| Tim Sort (simplificado) | O(n) | O(n log n) | O(n log n) | O(n) | ✔ | ✘ | Insertion binário em blocos de 32 + merge |
| Radix Sort LSD | O(d·n) | O(d·n) | O(d·n) | O(n+b) | ✔ | ✘ | Counting sort por byte da chave, **0 comparações** |
| Counting Sort | O(n+k) | O(n+k) | O(n+k) | O(n+k) | ✔ | ✘ | Conta cada chave num vetor do tamanho do intervalo k, **0 comparações**; só para intervalos pequenos |

\* Com a otimização "pula a intercalação se as metades já estão em ordem".

## 2. Critérios de ordenação

- **Exigidos pelo enunciado:** **Data/hora**, **Bioma**, **Município**.
- **Extras:** Hora local (0 a 23, em UTC−3), Latitude, Longitude e ID. Satélite, FRP, Risco de fogo, Dias sem chuva e Precipitação só ficam habilitados se o CSV tiver essas colunas.
- **Direção:** crescente ou decrescente. A inversão é feita com `reversed()` apenas sobre o valor, para que os ausentes (`null`) fiquem sempre no final.
- **Multicritério:** até 3 níveis com `thenComparing`. Exemplo: *Bioma ↑ → Município ↑ → Data ↓* agrupa por bioma; dentro de cada bioma, ordena os municípios alfabeticamente; dentro de cada município, mostra o foco mais recente primeiro.
- **Texto com acentos:** usa `java.text.Collator` pt-BR.
  - Com `String.compareTo`, "ÁGUAS DE LINDÓIA" ficaria **depois** de "ZACARIAS", porque 'Á' = U+00C1 > 'Z' = U+005A.
  - Para não pagar o custo do `Collator` a cada comparação, a **`CollationKey`** de município e bioma é calculada uma única vez, na criação do registro. Comparar duas chaves é uma comparação de bytes.
- **Radix:** cada critério numérico fornece também uma chave `long` equivalente.
  - Data: segundos desde 1970.
  - Hora local: 0 a 23. É o critério em que o Counting Sort brilha: k = 24.
  - Latitude e longitude: bits do `double` ajustados para manter a ordem de negativos.
  - Ordem decrescente: `~chave`.

## 3. Modelo de custo (`OperationCounter`)

Baseado em Sedgewick & Wayne (*Algorithms*, 4ª ed., §2.1):

| Métrica | O que conta |
|---|---|
| **Comparações** | Cada chamada ao `Comparator` |
| **Trocas** | Cada `swap` de dois elementos |
| **Atribuições** (substituições) | Cada escrita de elemento em array (principal ou auxiliar). Uma troca = 2 escritas |
| **Acessos ao array** | Leituras + escritas |
| **Tempo** | `System.nanoTime()` (relógio monotônico) |

Todas as contagens são `long`. O Bubble Sort com 100 mil elementos faz cerca de 5×10⁹ comparações, o que estouraria um `int`.

A contagem é centralizada no **`InstrumentedArray`** (padrão *Decorator*): os algoritmos escrevem `a.less(i, j)`, `a.swap(i, j)`, `a.get(i)` e `a.set(i, v)` e nunca acessam o vetor diretamente.

`OperationCounterTest` confere os valores teóricos, por exemplo:

- Bubble no vetor ordenado: exatamente n−1 comparações e 0 trocas; invertido: n(n−1)/2 de cada.
- Selection: sempre n(n−1)/2 comparações.
- Insertion no vetor ordenado: n−1 comparações e 0 escritas.

## 4. Decisões de projeto que valem a defesa oral

1. **Ordenação sobre uma cópia.** O `ServicoOrdenacao` copia a lista para um array antes de ordenar. A `BaseDeFocos` é imutável (`List.copyOf`), e o teste `originalPreservado` garante que ela não muda.
2. **Merge Sort estável.** Na intercalação, em empate copia-se primeiro o elemento da metade esquerda (`if (direita < esquerda)`, estritamente menor). A estabilidade de **todos** os algoritmos é conferida pelo teste `estabilidade`: os estáveis precisam preservar a ordem dos empates, e os instáveis precisam demonstrar que não preservam.
3. **Quick Sort sem `StackOverflowError`:**
   - **Pivô mediana de três:** o vetor já ordenado deixa de ser o pior caso.
   - **Partição de Hoare** (os ponteiros param em chaves iguais ao pivô): com muitas repetições a partição continua balanceada. É essencial aqui, porque o critério *bioma* tem **apenas 2 valores** em SP; a partição de Lomuto degeneraria para O(n²).
   - **Recursão só na parte menor** e laço na maior: pilha O(log n) garantida.
   - O teste `quickSortEntradaOrdenadaGrande` ordena 200 mil elementos já ordenados com menos de 10⁷ comparações.
4. **Quick Sort 3-Way** como extra: para bioma, os iguais ao pivô não voltam para a recursão, e o custo cai para cerca de 2 passadas lineares.
5. **Radix Sort** mostra que o limite inferior Ω(n log n) vale só para ordenações **por comparação**: ele faz **zero comparações**. Em troca, só funciona com chave numérica única; o sistema avisa se for escolhido com critério de texto.
6. **Custo dos O(n²):**
   - Com n acima de `aps.ordenacao.limiteQuadratico` (20.000 por padrão), o sistema avisa e estima as comparações (n²/2) antes de executar.
   - A amostra tem tamanho configurável (primeiros n ou sorteio com semente fixa).
   - Toda execução pode ser **cancelada** (botão Cancelar): o contador verifica a interrupção da thread.
7. **Counting Sort só onde cabe.** Ele precisa de um vetor de contagem do tamanho do intervalo de chaves. Na data em segundos (cerca de 63 milhões de valores em dois anos) seriam 250 MB; por isso o algoritmo declara o intervalo que aceita (até 4 milhões de valores), o sistema o esconde nos critérios em que não serve e explica o motivo se for escolhido. Na hora local, com 24 valores, ele ordena os 10.378 focos com zero comparações.
8. **Intro Sort e Quick Sort com dois pivôs** fecham o quadro com as escolhas das bibliotecas reais: o `std::sort` do C++ usa Intro Sort (garantia de O(n log n) no pior caso) e o `Arrays.sort` do Java para tipos primitivos usa dois pivôs. O perfil JMH compara os nossos com o `Arrays.sort` de objetos do Java, só como referência (a regra do enunciado vale para o programa, não para o benchmark separado).
9. **Verificação independente.** Depois de cada ordenação, o sistema confere se o resultado está ordenado e exibe "✔ ordenado" ou "FALHOU".

## 5. Estabilidade: por que importa aqui

Em uma ordenação **estável**, focos com a mesma chave (por exemplo, o mesmo município) mantêm a ordem anterior.

**Exemplo:** ordenar primeiro por data e depois, com um algoritmo estável, por município. Resultado: municípios em ordem alfabética e, dentro de cada município, focos em ordem cronológica. É o equivalente a um critério composto, obtido em duas passadas.

- **Algoritmos instáveis** (Selection, Shell, Quick, Heap) podem embaralhar os empates.
- **Nesta base** isso é visível: há até 146 focos no mesmo município e 5.762 no mesmo bioma.

## Como adicionar um algoritmo

1. **Implementar** `sorting/algorithms/<Nome>Sort.java` com a interface `SortAlgorithm`: `nome()`, `complexidade()`, `descricao()` e `ordenar(InstrumentedArray<T> a)`. Os dados são acessados só por `a.get`, `a.set`, `a.swap`, `a.less`, `a.compare` e `a.auxiliar(n)`; algoritmos sem comparação (radix, counting) retornam `exigeChaveNumerica() = true` e usam `a.key(i)`.
2. **Registrar** a constante em `sorting/AlgoritmoTipo.java` (ex.: `COMB(CombSort::new)`). Menu, dashboard, benchmark e relatórios passam a enxergá-lo sozinhos.
3. **Testar:** o `SortAlgorithmsTest` é parametrizado por `AlgoritmoTipo`, então o novo algoritmo já entra nos casos de vazio, repetidos, nulos, estabilidade e multicritério. Acrescente em `OperationCounterTest` a contagem exata de um caso conhecido e ajuste o total em `fabricaPorNome`.
4. **Documentar** na tabela acima e, se entrar na dissertação, rodar de novo o modo `resultados`.
