# Estruturas de dados e busca

Pacotes `br.unip.aps.estruturas` e `br.unip.aps.busca`. **Tudo foi implementado à mão**: heap, árvore, tabela hash, busca binária e vetor dinâmico.

O teste `ArquiteturaTest.estruturasImplementadasAMao` faz o build falhar se esses pacotes usarem qualquer destas classes: `HashMap`, `TreeMap`, `TreeSet`, `HashSet`, `PriorityQueue`, `Hashtable`, `ConcurrentHashMap`, `Arrays.binarySearch` ou `Collections.binarySearch`.

As operações são contadas pelo **mesmo `OperationCounter`** dos algoritmos de ordenação: comparações, leituras, escritas e trocas.

**Tela e reprodução:**
- **Tela:** "Estruturas & Busca" (Ctrl+3).
- **Números completos:**

```bash
java -jar target/aps-queimadas-1.0.0-all.jar estruturas --brasil 2019-2024
```

  Os resultados vão para [`resultados/estruturas.md`](resultados/estruturas.md).

## 1. Complexidades

| Estrutura / operação | Implementação | Tempo | Espaço extra |
|---|---|---|---|
| Busca sequencial | `Buscas.contarLinear` | Θ(n) por consulta | O(1) |
| Busca binária (`lowerBound`, `upperBound`) | `Buscas` | O(log n) por consulta, após ordenar em O(n log n) | O(1) |
| Árvore AVL: inserção, remoção, busca | `ArvoreAVL` | O(log n) no pior caso | O(n) |
| AVL: consulta por intervalo | `ArvoreAVL.intervalo` | O(log n + k), k = resultados | O(altura) de pilha |
| Tabela hash: inserir, buscar, remover | `TabelaHash` | O(1) esperado; O(n) no pior caso | O(n + m) |
| Heap binário: inserir, remover o topo | `HeapBinario` | O(log n) | O(n) |
| Top K com heap mínimo de tamanho K | `HeapBinario.topK` | O(n log K) | O(K) |
| External Merge Sort | `ExternalMergeSort` | O(n log M) nos runs + O(n log r) na intercalação, com r = ⌈n/M⌉ runs | O(M) em memória, O(n) em disco |
| Merge Sort paralelo (Fork/Join) | `MergeSortParalelo` | trabalho O(n log n), profundidade O(n) (intercalação sequencial) | O(n) |

## 2. Decisões de projeto

- **AVL em vez de Rubro-Negra.**
  - A AVL é mais estritamente balanceada: altura ≤ 1,44·log₂(n+2). Isso a torna melhor para consultas, que são o uso principal aqui: os índices são montados uma vez e consultados muitas vezes.
  - A Rubro-Negra rotaciona menos nas inserções, mas é mais alta, com altura ≤ 2·log₂(n+1).
  - Chaves repetidas (vários focos na mesma data/hora ou no mesmo município) ficam no mesmo nó, numa lista de valores.
  - Os **fatores de balanceamento** (altura da esquerda − altura da direita) disparam as 4 rotações: simples à direita, simples à esquerda, dupla esquerda-direita e dupla direita-esquerda.
- **Hash com encadeamento separado** (e não endereçamento aberto):
  - a remoção é simples, sem marcadores de "apagado";
  - o desempenho degrada de forma suave com carga alta;
  - cada chave guarda naturalmente uma lista de valores (multimapa).
- **Parâmetros da hash:**
  - **Função:** FNV-1a de 32 bits sobre o nome do município.
  - **Índice:** calculado por máscara, com capacidade em potência de 2 e mistura dos 16 bits altos.
  - **Crescimento:** carga máxima de 0,75, dobrando a capacidade (16 → 1.024 para os 580 municípios).
- **Heap mínimo para o Top K.** Os K maiores ficam num heap *mínimo* de tamanho K. Cada novo item só é comparado com o topo, e troca de lugar com ele se for maior: O(n log K) em vez do O(n log n) de ordenar tudo.
- **External Merge Sort:**
  - **Fase 1:** lê até M registros, ordena com o Merge Sort do projeto (estável) e grava um "run" em disco.
  - **Fase 2:** intercala os r runs com o `HeapBinario` (k-way merge), guardando em memória só uma linha de cada run.
  - **Chave de ordenação:** a data/hora GMT, cujo formato ISO já ordena corretamente como texto.
- **Merge Sort paralelo:**
  - As metades são ordenadas em paralelo com `fork`/`join`.
  - Abaixo de 8.192 elementos, a tarefa ordena sequencialmente, porque criar tarefas pequenas custa mais do que ganha.
  - Cada tarefa conta suas operações num contador próprio, e as contagens são somadas no `join`. O total de comparações é idêntico com 1, 2 ou 12 threads, e um teste garante isso.

## 3. Resultados reais

Máquina: 12 núcleos lógicos, Windows 11, Java 21.0.10. Todos os números vêm de [`resultados/estruturas.md`](resultados/estruturas.md) e de [`resultados/estruturas-externo.md`](resultados/estruturas-externo.md).

### Por que ordenar? Consultas por município (n = 10.378 focos de SP)

| 1.000 consultas | Preparo (comparações) | Comparações por consulta | Total |
|---|---:|---:|---:|
| Busca sequencial | 0 | 10.378 | 10.378.000 |
| Ordenar (Merge Sort) + busca binária | 123.132 | 26,8 | 149.965 |
| Índice AVL | 88.973 | 7,9 | 96.866 |
| Tabela hash | 10.798 | 1,0 | 11.798 |

- **Ordenação:** a ordenação se paga a partir de **11,9 consultas**. Com 1.000 consultas, a busca binária faz **69 vezes menos comparações** que a busca sequencial.
- **Hash:** é imbatível para igualdade.
- **Intervalos de datas:** a hash não responde intervalos, porque espalha as chaves e perde a ordem. Nesse caso, ordenar e usar busca binária é o melhor: 26,8 comparações por consulta, e o preparo se paga a partir de 3 consultas.

### Índices

| Índice AVL | Chaves distintas | Altura | Limite teórico | Rotações simples | Rotações duplas |
|---|---:|---:|---:|---:|---:|
| Data/hora | 573 | 10 | 12,9 | 435 | 67 |
| Município | 580 | 11 | 12,9 | 144 | 122 |

**Tabela hash de municípios:**

| Medida | Valor |
|---|---:|
| Chaves | 580 |
| Baldes | 1.024 |
| Fator de carga | 0,566 |
| Colisões na inserção | 234 |
| Maior cadeia | 4 |
| Baldes vazios | 579 |
| Baldes com 1 entrada | 333 |
| Baldes com 2 entradas | 90 |
| Baldes com 3 entradas | 21 |
| Baldes com 4 entradas | 1 |

### Top K municípios

| K | Heap (comparações) | Ordenar os 580 (comparações) |
|---:|---:|---:|
| 1 | 579 | 4.940 |
| 10 | 771 | 4.940 |
| 50 | 2.090 | 4.940 |

### Memória extra medida (critério data/hora, n = 10.378)

A memória foi medida com `ThreadMXBean.getThreadAllocatedBytes`, descontado o custo fixo da instrumentação (medido com n = 1):

| Algoritmo | Teórico | Medido |
|---|---|---:|
| Bubble, Selection, Insertion, Quick, Quick 3-Way, Heap | O(1) / O(log n) | 0 B |
| Shell | O(1) | 40 B |
| Merge | O(n) | 41.560 B (4 B por elemento = um vetor auxiliar de referências) |
| Tim (simplificado) | O(n) | 41.824 B |
| Radix (LSD) | O(n + b) | 216.024 B (20 B por elemento: vetor auxiliar e chaves `long`) |

### Merge Sort paralelo: 1.273.473 datas/horas dos focos do Brasil (2019–2024)

| Threads | Tempo médio | Speedup | Eficiência | Fração serial (Karp–Flatt) |
|---:|---:|---:|---:|---:|
| 1 | 472,6 ms | 1,00× | 100% | — |
| 2 | 330,2 ms | 1,43× | 72% | 0,397 |
| 4 | 256,6 ms | 1,84× | 46% | 0,391 |
| 8 | 212,4 ms | 2,23× | 28% | 0,371 |
| 12 | 194,8 ms | 2,43× | 20% | 0,359 |

**Lei de Amdahl:** S(p) = 1 / (f + (1 − f)/p).

- **Fração serial medida:** f ≈ 0,36 (métrica de Karp–Flatt).
- **Limite teórico:** o speedup máximo seria 1/f ≈ 2,8×, mesmo com infinitos núcleos.
- **Origem da parte serial:**
  - as intercalações dos níveis de cima da recursão, principalmente a última, que intercala os n elementos numa só thread;
  - a largura de banda da memória, porque o Merge Sort lê e escreve muito.
- **Variação entre execuções:** numa primeira execução na mesma máquina, com outros programas abertos, o speedup com 12 threads foi 3,01×. As **comparações não mudam** entre execuções: 15.313.877 com qualquer número de threads.

### External Merge Sort: Brasil 2019–2024 com memória limitada

| Medida | Heap livre (-Xmx3g) | Heap limitado (-Xmx64m) |
|---|---:|---:|
| Entrada | 6 CSVs, 163,6 MB | 6 CSVs, 163,6 MB |
| Registros | 1.273.473 | 1.273.473 |
| Registros por run (M) | 200.000 | 100.000 |
| Runs em disco | 7 | 13 |
| Comparações: fase 1 + fase 2 | 14.923.823 + 2.216.154 | 14.009.755 + 2.336.912 |
| Tempo: fase 1 + fase 2 | 3,9 s + 1,6 s | 4,2 s + 1,4 s |
| Pico de heap observado | 482 MB (a JVM adia a coleta de lixo) | **57 MB** |
| Saída conferida em ordem | sim | sim |

**Conclusão:**
- Com o heap da JVM limitado a **64 MB**, o sistema ordenou **163,6 MB** de CSV. Esse é o objetivo da ordenação externa: o custo em memória é O(M), não O(n).
- Com M menor, há mais runs (13 contra 7). A fase 1 fica mais barata, porque cada run é menor, e a intercalação fica um pouco mais cara, porque o heap é maior: log₂13 contra log₂7.

## 4. Testes

| Classe | O que garante |
|---|---|
| `EstruturasTest` | Busca binária igual à sequencial e com no máximo 2·⌈log₂(n+1)⌉ comparações. A AVL mantém as invariantes (ordem e \|fb\| ≤ 1) após 5.000 inserções e 2.000 remoções, com altura dentro do limite teórico. Rotações simples e duplas aparecem nos casos clássicos (1-2-3 e 3-1-2). Chaves repetidas e intervalo funcionam. A hash redimensiona, conta colisões, fica com carga ≤ 0,75 e reproduz os valores conhecidos do FNV-1a. O heap ordena e o Top K coincide com a ordenação. O Merge Sort paralelo dá resultado e contagem iguais com 1, 2 e 4 threads. O Merge Sort aloca O(n) e o Heap Sort, ~0. |
| `ExternalMergeSortTest` | Ordena os 10.378 focos com M = 1.000 (11 runs) e confere a ordem e o total de linhas. |
| `ServicoConsultasTest` | Os 4 métodos encontram os mesmos focos. A hash é O(1). A busca binária fica em ~2·log₂(n). A hash não se aplica a intervalos. O Top K por heap é igual ao ranking. |
| `ArquiteturaTest` | Nenhuma estrutura pronta nos pacotes `estruturas` e `busca`. |
