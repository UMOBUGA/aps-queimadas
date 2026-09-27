# Resultados reais: estruturas de dados e busca

Gerado em 27/09/2026 13:54 · Java 21.0.10 · 12 núcleos lógicos · Windows 11

Base: 10.378 focos de SP (2023–2024). Consultas sorteadas com semente fixa 42.

## 1. Consultas por município (igualdade)

| Consultas | Método | Preparo (comparações) | Comparações por consulta | Total de comparações | Tempo total |
|---:|---|---:|---:|---:|---:|
| 1 | Busca sequencial | 0 | 10.378,0 | 10.378 | 3,766 ms |
| 1 | Ordenar + busca binária | 123.132 | 26,0 | 123.158 | 12,055 ms |
| 1 | Índice AVL | 81.088 | 8,0 | 81.096 | 8,070 ms |
| 1 | Tabela hash | 9.799 | 1,0 | 9.800 | 5,112 ms |
| 10 | Busca sequencial | 0 | 10.378,0 | 103.780 | 14,996 ms |
| 10 | Ordenar + busca binária | 123.132 | 27,0 | 123.402 | 22,201 ms |
| 10 | Índice AVL | 81.159 | 7,9 | 81.238 | 3,985 ms |
| 10 | Tabela hash | 9.808 | 1,0 | 9.818 | 2,184 ms |
| 100 | Busca sequencial | 0 | 10.378,0 | 1.037.800 | 15,723 ms |
| 100 | Ordenar + busca binária | 123.132 | 26,8 | 125.815 | 4,378 ms |
| 100 | Índice AVL | 81.873 | 7,9 | 82.666 | 1,947 ms |
| 100 | Tabela hash | 9.898 | 1,0 | 9.998 | 1,455 ms |
| 1.000 | Busca sequencial | 0 | 10.378,0 | 10.378.000 | 96,563 ms |
| 1.000 | Ordenar + busca binária | 123.132 | 26,8 | 149.965 | 5,619 ms |
| 1.000 | Índice AVL | 88.973 | 7,9 | 96.866 | 4,982 ms |
| 1.000 | Tabela hash | 10.798 | 1,0 | 11.798 | 2,197 ms |

Ponto de equilíbrio (a partir de quantas consultas o preparo compensa, em comparações):

- **Ordenar + busca binária:** 11,9 consultas
- **Índice AVL:** 8,6 consultas (altura 11 · 144 rotações simples e 122 duplas)
- **Tabela hash:** 1,0 consultas (580 chaves · carga 0,57 · 234 colisões · maior cadeia 4)

## 2. Consultas por intervalo de datas (janelas de 7 dias)

| Consultas | Método | Preparo (comparações) | Comparações por consulta | Total de comparações | Tempo total |
|---:|---|---:|---:|---:|---:|
| 1 | Busca sequencial | 0 | 18.570,0 | 18.570 | 4,369 ms |
| 1 | Ordenar + busca binária | 46.792 | 27,0 | 46.819 | 8,738 ms |
| 1 | Índice AVL | 95.702 | 34,0 | 95.736 | 5,317 ms |
| 1 | Tabela hash | — | não se aplica | — | — |
| 10 | Busca sequencial | 0 | 16.297,6 | 162.976 | 11,333 ms |
| 10 | Ordenar + busca binária | 46.792 | 27,1 | 47.063 | 3,469 ms |
| 10 | Índice AVL | 95.702 | 34,8 | 96.050 | 3,946 ms |
| 10 | Tabela hash | — | não se aplica | — | — |
| 100 | Busca sequencial | 0 | 15.950,4 | 1.595.042 | 16,568 ms |
| 100 | Ordenar + busca binária | 46.792 | 26,8 | 49.474 | 1,007 ms |
| 100 | Índice AVL | 95.702 | 37,9 | 99.488 | 1,992 ms |
| 100 | Tabela hash | — | não se aplica | — | — |
| 1.000 | Busca sequencial | 0 | 15.751,0 | 15.750.978 | 146,299 ms |
| 1.000 | Ordenar + busca binária | 46.792 | 26,8 | 73.605 | 1,664 ms |
| 1.000 | Índice AVL | 95.702 | 36,5 | 132.228 | 6,997 ms |
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
| Heap Sort | O(1) | 0 | 0 |
| Tim Sort (simplificado) | O(n) | 41.824 | 4 |
| Radix Sort (LSD) | O(n + b) | 216.024 | 20 |

Medição: `ThreadMXBean.getThreadAllocatedBytes` antes e depois de cada ordenação (inclui objetos temporários, não a pilha de recursão).

## 6. Merge Sort paralelo (Fork/Join): speedup e Lei de Amdahl

Entrada: 1.273.473 datas/horas dos focos do Brasil; limiar sequencial de 8.192 elementos; 2 aquecimentos e 5 repetições por configuração.

| Threads | Tempo médio | Speedup | Eficiência | Fração serial (Karp–Flatt) | Comparações | Ordenado |
|---:|---:|---:|---:|---:|---:|---|
| 1 | 472,6 ms | 1,00× | 100% | — | 15.313.877 | sim |
| 2 | 330,2 ms | 1,43× | 72% | 0,397 | 15.313.877 | sim |
| 4 | 256,6 ms | 1,84× | 46% | 0,391 | 15.313.877 | sim |
| 8 | 212,4 ms | 2,23× | 28% | 0,371 | 15.313.877 | sim |
| 12 | 194,8 ms | 2,43× | 20% | 0,359 | 15.313.877 | sim |

## 7. External Merge Sort: focos do Brasil com memória limitada

| Medida | Valor |
|---|---:|
| Arquivos de entrada | 6 (163,6 MB) |
| Registros ordenados | 1.273.473 |
| Limite de memória | 200.000 registros por run |
| Runs gravados em disco | 7 |
| Comparações na fase 1 (Merge Sort de cada run) | 14.923.823 |
| Comparações na fase 2 (intercalação 7-way com heap) | 2.216.154 |
| Tempo da fase 1 | 4,036 s |
| Tempo da fase 2 | 1,289 s |
| Pico de heap da JVM observado | 222 MB |
| Saída verificada em ordem | sim |
