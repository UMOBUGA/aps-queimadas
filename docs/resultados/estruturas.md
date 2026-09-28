# Resultados reais: estruturas de dados e busca

Gerado em 27/09/2026 22:24 · Java 21.0.10 · 12 núcleos lógicos · Windows 11

Base: 10.378 focos de SP (2023–2024). Consultas sorteadas com semente fixa 42.

## 1. Consultas por município (igualdade)

| Consultas | Método | Preparo (comparações) | Comparações por consulta | Total de comparações | Tempo total |
|---:|---|---:|---:|---:|---:|
| 1 | Busca sequencial | 0 | 10.378,0 | 10.378 | 2,971 ms |
| 1 | Ordenar + busca binária | 123.132 | 26,0 | 123.158 | 16,787 ms |
| 1 | Índice AVL | 81.088 | 8,0 | 81.096 | 10,838 ms |
| 1 | Tabela hash | 9.799 | 1,0 | 9.800 | 6,762 ms |
| 10 | Busca sequencial | 0 | 10.378,0 | 103.780 | 9,580 ms |
| 10 | Ordenar + busca binária | 123.132 | 27,0 | 123.402 | 27,515 ms |
| 10 | Índice AVL | 81.159 | 7,9 | 81.238 | 5,540 ms |
| 10 | Tabela hash | 9.808 | 1,0 | 9.818 | 3,506 ms |
| 100 | Busca sequencial | 0 | 10.378,0 | 1.037.800 | 35,739 ms |
| 100 | Ordenar + busca binária | 123.132 | 26,8 | 125.815 | 5,127 ms |
| 100 | Índice AVL | 81.873 | 7,9 | 82.666 | 2,619 ms |
| 100 | Tabela hash | 9.898 | 1,0 | 9.998 | 1,653 ms |
| 1.000 | Busca sequencial | 0 | 10.378,0 | 10.378.000 | 132,571 ms |
| 1.000 | Ordenar + busca binária | 123.132 | 26,8 | 149.965 | 5,522 ms |
| 1.000 | Índice AVL | 88.973 | 7,9 | 96.866 | 5,861 ms |
| 1.000 | Tabela hash | 10.798 | 1,0 | 11.798 | 3,246 ms |

Ponto de equilíbrio (a partir de quantas consultas o preparo compensa, em comparações):

- **Ordenar + busca binária:** 11,9 consultas
- **Índice AVL:** 8,6 consultas (altura 11 · 144 rotações simples e 122 duplas)
- **Tabela hash:** 1,0 consultas (580 chaves · carga 0,57 · 234 colisões · maior cadeia 4)

## 2. Consultas por intervalo de datas (janelas de 7 dias)

| Consultas | Método | Preparo (comparações) | Comparações por consulta | Total de comparações | Tempo total |
|---:|---|---:|---:|---:|---:|
| 1 | Busca sequencial | 0 | 18.570,0 | 18.570 | 4,727 ms |
| 1 | Ordenar + busca binária | 46.792 | 27,0 | 46.819 | 14,407 ms |
| 1 | Índice AVL | 95.702 | 34,0 | 95.736 | 6,937 ms |
| 1 | Tabela hash | — | não se aplica | — | — |
| 10 | Busca sequencial | 0 | 16.297,6 | 162.976 | 11,956 ms |
| 10 | Ordenar + busca binária | 46.792 | 27,1 | 47.063 | 3,076 ms |
| 10 | Índice AVL | 95.702 | 34,8 | 96.050 | 3,743 ms |
| 10 | Tabela hash | — | não se aplica | — | — |
| 100 | Busca sequencial | 0 | 15.950,4 | 1.595.042 | 22,474 ms |
| 100 | Ordenar + busca binária | 46.792 | 26,8 | 49.474 | 2,766 ms |
| 100 | Índice AVL | 95.702 | 37,9 | 99.488 | 2,644 ms |
| 100 | Tabela hash | — | não se aplica | — | — |
| 1.000 | Busca sequencial | 0 | 15.751,0 | 15.750.978 | 170,915 ms |
| 1.000 | Ordenar + busca binária | 46.792 | 26,8 | 73.605 | 1,944 ms |
| 1.000 | Índice AVL | 95.702 | 36,5 | 132.228 | 4,272 ms |
| 1.000 | Tabela hash | — | não se aplica | — | — |

Ponto de equilíbrio (a partir de quantas consultas o preparo compensa, em comparações):

- **Ordenar + busca binária:** 3,0 consultas
- **Índice AVL:** 6,1 consultas (altura 10 · 573 datas distintas)

## 3. Índices construídos sobre a base

| Índice AVL | Chaves distintas | Altura | Limite AVL 1,44·log₂(n+2) | log₂(n) | Rotações simples | Rotações duplas | Comparações na construção |
|---|---:|---:|---:|---:|---:|---:|---:|
| Data/hora | 573 | 10 | 12,9 | 9,2 | 435 | 67 | 95.702 |
| Município | 580 | 11 | 12,9 | 9,2 | 144 | 122 | 81.080 |

| Tabela hash (município, FNV-1a, encadeamento) | Valor |
|---|---:|
| Chaves (municípios) | 580 |
| Capacidade final (baldes) | 1024 |
| Fator de carga | 0,566 |
| Redimensionamentos (16 → 1024) | 6 |
| Colisões na inserção | 234 |
| Maior cadeia | 4 |
| Baldes por comprimento de cadeia | 0: 579 · 1: 333 · 2: 90 · 3: 21 · 4: 1 |

## 4. Top K municípios: heap × ordenar tudo

| K | Candidatos | Comparações (heap de K) | Comparações (Merge Sort de todos) | Mesmo resultado |
|---:|---:|---:|---:|---|
| 1 | 580 | 579 | 4.940 | sim |
| 10 | 580 | 771 | 4.940 | sim |
| 50 | 580 | 2.090 | 4.940 | sim |

Contagem por município com a tabela hash (uma passada): 9.798 comparações para 10.378 focos. Líder: ANDRADINA (146 focos).

## 5. Memória extra por algoritmo (critério data/hora, n = 10.378)

| Algoritmo | Espaço extra teórico | Bytes alocados (medidos) | Bytes por elemento |
|---|---|---:|---:|
| Bubble Sort | O(1) | 0 | 0 |
| Selection Sort | O(1) | 0 | 0 |
| Insertion Sort | O(1) | 0 | 0 |
| Shell Sort | O(1) | 40 | 0 |
| Merge Sort | O(n) | 41.560 | 4 |
| Quick Sort | O(log n) | 0 | 0 |
| Quick Sort 3-Way | O(log n) | 0 | 0 |
| Quick Sort 2 pivôs | O(log n) | 0 | 0 |
| Intro Sort | O(log n) | 0 | 0 |
| Heap Sort | O(1) | 0 | 0 |
| Tim Sort (simplificado) | O(n) | 41.824 | 4 |
| Radix Sort (LSD) | O(n + b) | 216.024 | 20 |

Medição: `ThreadMXBean.getThreadAllocatedBytes` antes e depois de cada ordenação (inclui objetos temporários, não a pilha de recursão).

## 6. Merge Sort paralelo (Fork/Join): speedup e Lei de Amdahl

Entrada: 1.273.473 datas/horas dos focos do Brasil; limiar sequencial de 8.192 elementos; 2 aquecimentos e 5 repetições por configuração.

| Threads | Tempo médio | Speedup | Eficiência | Fração serial (Karp–Flatt) | Comparações | Ordenado |
|---:|---:|---:|---:|---:|---:|---|
| 1 | 306,5 ms | 1,00× | 100% | — | 15.313.877 | sim |
| 2 | 188,4 ms | 1,63× | 81% | 0,229 | 15.313.877 | sim |
| 4 | 140,1 ms | 2,19× | 55% | 0,276 | 15.313.877 | sim |
| 8 | 129,5 ms | 2,37× | 30% | 0,340 | 15.313.877 | sim |
| 12 | 109,8 ms | 2,79× | 23% | 0,300 | 15.313.877 | sim |

## 7. Trie, árvore k-d e grafo de municípios

### Autocompletar (Trie × testar o prefixo em todos os nomes)

| Prefixo | Nomes encontrados | Nós visitados na Trie | Caracteres comparados testando todos |
|---|---:|---:|---:|
| S | 74 | 1 | 580 |
| SAO | 23 | 3 | 715 |
| SAO J | 10 | 5 | 761 |
| SANTA | 18 | 5 | 768 |
| RIB | 7 | 3 | 618 |

### Focos num raio a partir do centro de ANDRADINA (árvore k-d × calcular todas as distâncias)

| Raio | Focos no raio | Distâncias calculadas (árvore) | Distâncias calculadas (todas) | Economia |
|---:|---:|---:|---:|---:|
| 10 km | 56 | 126 | 10.378 | 98,8% |
| 25 km | 236 | 323 | 10.378 | 96,9% |
| 50 km | 459 | 619 | 10.378 | 94,0% |
| 100 km | 938 | 1.238 | 10.378 | 88,1% |

### O fogo pula para o vizinho? (grafo de fronteiras do IBGE + busca em largura)

Grafo com 645 municípios e 1.837 fronteiras. Caso = município sem focos num mês; verifica-se se ele teve focos no mês seguinte, separando quem tinha um vizinho com focos no mês do caso.

| Situação no mês | Casos | Com focos no mês seguinte | Taxa |
|---|---:|---:|---:|
| Algum vizinho com focos | 5.951 | 1.076 | 18,1% |
| Nenhum vizinho com focos | 6.366 | 523 | 8,2% |

Razão entre as taxas: **2,20×**. É uma associação, não prova de causa: municípios vizinhos também dividem clima, relevo e uso do solo. Focos sem município na malha: 0.

## 8. External Merge Sort: focos do Brasil com memória limitada

| Medida | Valor |
|---|---:|
| Arquivos de entrada | 6 (163,6 MB) |
| Registros ordenados | 1.273.473 |
| Limite de memória | 200.000 registros por run |
| Runs gravados em disco | 7 |
| Comparações na fase 1 (Merge Sort de cada run) | 14.923.823 |
| Comparações na fase 2 (intercalação 7-way com heap) | 2.216.154 |
| Tempo da fase 1 | 2,249 s |
| Tempo da fase 2 | 1,050 s |
| Pico de heap da JVM observado | 364 MB |
| Saída verificada em ordem | sim |
