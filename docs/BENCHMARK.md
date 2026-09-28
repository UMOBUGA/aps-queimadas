# Fase 4 — Benchmark e análise de performance

## 1. Metodologia

| Aspecto | Decisão | Justificativa |
|---|---|---|
| Algoritmos | Os 13 implementados | Radix só no critério numérico (data); o Counting Sort exige intervalo de chaves pequeno e por isso não entra nos três critérios da bateria (data em segundos, bioma e município) |
| Critérios | Data/hora, Bioma, Município | Os três exigidos pelo enunciado |
| Tamanhos (n) | 100, 1.000, 5.000, 10.000 e 10.378 (base inteira) | Tamanhos crescentes pedidos no enunciado |
| Cenários | Aleatório, já ordenado, inversamente ordenado | Caso médio, melhor e pior caso dos algoritmos simples |
| Amostragem | Sorteio com semente fixa (42) | Reprodutível |
| Mesma entrada | Para cada (critério, n, cenário) a entrada é preparada **uma vez**; cada execução recebe uma cópia idêntica | Todos os algoritmos competem em igualdade |
| Aquecimento do JIT | Aquecimento global (cada algoritmo 3× com n = 1.000) + por caso **2 execuções e no mínimo 500 ms** descartados | A JVM interpreta o bytecode no início e compila os métodos "quentes" (C1/C2) depois; sem aquecimento, mede-se o interpretador |
| Repetições | **5 execuções medidas** por caso: média, desvio-padrão amostral, mínimo e máximo | O mínimo é o estimador menos sensível a ruído (GC, sistema operacional) |
| Relógio | `System.nanoTime()` | Monotônico, com resolução de nanossegundos |
| Contagem de operações | `InstrumentedArray` + `OperationCounter` | Determinística: igual em todas as repetições |
| Verificação | Cada execução é conferida como ordenada | Garante que nenhum algoritmo "ganhou" errando |
| Complexidade empírica | Regressão log-log do custo × n → expoente **k** (custo ≈ c·nᵏ) | Compara a prática com a teoria: k ≈ 2 para O(n²), k ≈ 1,0–1,15 para O(n log n) na faixa medida |

**Como reproduzir:**

```bash
java -Xss8m -Xmx2g -jar target/aps-queimadas-2.0.0-all.jar resultados
```

Ou use a aba Benchmark do dashboard. Os arquivos gerados estão em [`resultados/`](resultados/).

**Cuidado com o Quick Sort.** Em entradas já ordenadas, usa pivô mediana de três e recursão na menor parte, então não há `StackOverflowError`. A discussão do pior caso O(n²) está em [ALGORITMOS.md](ALGORITMOS.md#4-decisões-de-projeto-que-valem-a-defesa-oral).

## 2. Resultados reais


### Execução

- **Tamanho:** 585 casos (13 algoritmos × 3 critérios × 5 tamanhos × 3 cenários); 510 executados, porque o Radix só roda no critério numérico e o Counting Sort não cabe em nenhum dos três critérios da bateria.
- **Verificação:** 510 de 510 execuções conferidas como corretamente ordenadas.
- **Duração:** 8 min 51 s.
- **Máquina:** ver [resultados/AMBIENTE.md](resultados/AMBIENTE.md).
- **Arquivos completos:**
  - [`resultados/benchmark.csv`](resultados/benchmark.csv) — todos os tamanhos, com mínimo, máximo e referências teóricas;
  - [`resultados/resultados.xlsx`](resultados/resultados.xlsx) — abas Benchmark e Complexidade empírica;
  - [`resultados/resultados.pdf`](resultados/resultados.pdf).

> **Lição de metodologia.** A primeira execução completa foi **descartada**. Enquanto ela rodava, outros processos pesados foram executados na máquina (download de dependências Maven e push para o GitHub), e os tempos do critério Data/hora ficaram 4 a 5 vezes maiores do que em uma execução isolada. As **contagens de operações ficaram idênticas** nas duas execuções: elas não dependem da máquina.
>
> Esse é o motivo pelo qual o sistema reporta as contagens ao lado do tempo, e pelo qual benchmarks devem rodar com a máquina ociosa.

### Tabelas por critério e cenário (n = 10.378, base inteira)

Os tempos são a média de 5 repetições após o aquecimento. As tabelas estão ordenadas do mais rápido para o mais lento.

#### Data/hora · Aleatória · n = 10.378

| Algoritmo | Tempo médio (ms) | Desvio (ms) | Comparações | Trocas | Acessos |
|---|---:|---:|---:|---:|---:|
| Radix Sort (LSD) | 0,553 | 0,044 | 0 | 0 | 176.426 |
| Quick Sort 3-Way | 2,666 | 0,809 | 68.969 | 58.457 | 304.781 |
| Quick Sort 2 pivôs | 2,836 | 0,658 | 123.114 | 25.093 | 346.600 |
| Intro Sort | 3,135 | 0,484 | 130.111 | 38.988 | 416.174 |
| Merge Sort | 3,858 | 0,672 | 131.545 | 0 | 667.530 |
| Tim Sort (simplificado) | 4,617 | 0,629 | 129.095 | 0 | 668.435 |
| Shell Sort | 4,934 | 0,678 | 152.332 | 0 | 342.307 |
| Heap Sort | 6,393 | 2,605 | 245.261 | 129.172 | 1.007.210 |
| Quick Sort | 6,921 | 2,344 | 157.626 | 50.442 | 384.078 |
| Insertion Sort | 442,398 | 12,237 | 26.198.395 | 0 | 52.407.158 |
| Selection Sort | 1.270,942 | 220,352 | 53.846.253 | 10.369 | 107.733.982 |
| Bubble Sort | 1.546,612 | 111,861 | 53.797.665 | 26.188.026 | 212.347.434 |

#### Data/hora · Já ordenada · n = 10.378

| Algoritmo | Tempo médio (ms) | Desvio (ms) | Comparações | Trocas | Acessos |
|---|---:|---:|---:|---:|---:|
| Bubble Sort | 0,158 | 0,005 | 10.377 | 0 | 20.754 |
| Insertion Sort | 0,203 | 0,060 | 10.377 | 0 | 20.754 |
| Merge Sort | 0,206 | 0,012 | 10.377 | 0 | 20.754 |
| Tim Sort (simplificado) | 0,470 | 0,047 | 33.715 | 0 | 44.092 |
| Radix Sort (LSD) | 0,518 | 0,027 | 0 | 0 | 176.426 |
| Shell Sort | 1,699 | 0,081 | 99.821 | 0 | 199.642 |
| Intro Sort | 2,754 | 0,202 | 204.987 | 21.283 | 495.106 |
| Quick Sort | 3,052 | 0,067 | 233.591 | 34.394 | 395.895 |
| Quick Sort 2 pivôs | 3,778 | 0,183 | 280.976 | 15.872 | 625.440 |
| Heap Sort | 3,841 | 0,058 | 234.282 | 124.882 | 968.092 |
| Quick Sort 3-Way | 5,478 | 0,127 | 367.706 | 357.293 | 1.798.846 |
| Selection Sort | 854,434 | 188,133 | 53.846.253 | 0 | 107.692.506 |

#### Data/hora · Inversamente ordenada · n = 10.378

| Algoritmo | Tempo médio (ms) | Desvio (ms) | Comparações | Trocas | Acessos |
|---|---:|---:|---:|---:|---:|
| Radix Sort (LSD) | 0,565 | 0,136 | 0 | 0 | 176.426 |
| Merge Sort | 1,344 | 0,074 | 64.750 | 0 | 434.027 |
| Quick Sort 2 pivôs | 1,462 | 0,237 | 101.960 | 19.996 | 283.904 |
| Tim Sort (simplificado) | 2,041 | 0,135 | 79.320 | 0 | 505.366 |
| Shell Sort | 2,062 | 0,081 | 116.330 | 0 | 257.549 |
| Intro Sort | 2,254 | 0,146 | 174.289 | 25.035 | 448.718 |
| Quick Sort | 2,658 | 0,089 | 203.856 | 39.161 | 384.556 |
| Quick Sort 3-Way | 3,359 | 0,092 | 233.173 | 222.761 | 1.126.181 |
| Heap Sort | 3,878 | 0,187 | 233.939 | 120.184 | 948.614 |
| Insertion Sort | 753,179 | 12,212 | 52.212.245 | 0 | 104.435.435 |
| Selection Sort | 767,042 | 2,726 | 53.846.253 | 5.286 | 107.713.650 |
| Bubble Sort | 907,655 | 10,691 | 53.846.252 | 52.202.440 | 316.502.264 |

#### Bioma · Aleatória · n = 10.378

| Algoritmo | Tempo médio (ms) | Desvio (ms) | Comparações | Trocas | Acessos |
|---|---:|---:|---:|---:|---:|
| Quick Sort 3-Way | 0,401 | 0,049 | 14.998 | 4.618 | 33.478 |
| Quick Sort 2 pivôs | 0,547 | 0,037 | 25.368 | 4.624 | 69.232 |
| Heap Sort | 2,207 | 0,112 | 135.750 | 65.946 | 535.284 |
| Merge Sort | 2,294 | 0,113 | 99.651 | 0 | 607.760 |
| Quick Sort | 2,524 | 0,133 | 146.668 | 64.791 | 429.864 |
| Intro Sort | 2,677 | 0,182 | 116.485 | 48.711 | 427.814 |
| Tim Sort (simplificado) | 2,808 | 0,150 | 103.877 | 0 | 562.440 |
| Shell Sort | 5,457 | 3,188 | 104.306 | 0 | 214.683 |
| Insertion Sort | 184,930 | 1,870 | 13.116.062 | 0 | 26.236.739 |
| Bubble Sort | 1.077,591 | 27,856 | 43.189.072 | 13.105.685 | 138.800.884 |
| Selection Sort | 1.083,233 | 12,313 | 53.846.253 | 4.615 | 107.710.966 |

#### Bioma · Já ordenada · n = 10.378

| Algoritmo | Tempo médio (ms) | Desvio (ms) | Comparações | Trocas | Acessos |
|---|---:|---:|---:|---:|---:|
| Bubble Sort | 0,166 | 0,002 | 10.377 | 0 | 20.754 |
| Insertion Sort | 0,167 | 0,002 | 10.377 | 0 | 20.754 |
| Merge Sort | 0,219 | 0,042 | 10.377 | 0 | 20.754 |
| Quick Sort 3-Way | 0,281 | 0,030 | 14.998 | 4.618 | 33.478 |
| Tim Sort (simplificado) | 0,498 | 0,013 | 33.715 | 0 | 44.092 |
| Shell Sort | 1,965 | 0,100 | 99.821 | 0 | 199.642 |
| Heap Sort | 2,194 | 0,081 | 135.863 | 68.108 | 544.158 |
| Intro Sort | 2,647 | 0,237 | 157.350 | 47.592 | 505.068 |
| Quick Sort | 2,808 | 0,147 | 186.094 | 62.332 | 460.554 |
| Quick Sort 2 pivôs | 673,754 | 8,813 | 47.949.538 | 15.034 | 95.959.212 |
| Selection Sort | 790,709 | 16,869 | 53.846.253 | 0 | 107.692.506 |

#### Bioma · Inversamente ordenada · n = 10.378

| Algoritmo | Tempo médio (ms) | Desvio (ms) | Comparações | Trocas | Acessos |
|---|---:|---:|---:|---:|---:|
| Quick Sort 3-Way | 0,251 | 0,007 | 14.998 | 4.619 | 33.482 |
| Merge Sort | 0,604 | 0,025 | 28.480 | 0 | 121.889 |
| Tim Sort (simplificado) | 0,945 | 0,057 | 50.426 | 0 | 167.745 |
| Shell Sort | 1,980 | 0,090 | 101.220 | 0 | 210.799 |
| Intro Sort | 2,032 | 0,127 | 118.799 | 51.974 | 445.494 |
| Heap Sort | 2,157 | 0,082 | 149.036 | 71.343 | 583.444 |
| Quick Sort | 2,262 | 0,192 | 145.242 | 66.227 | 434.258 |
| Insertion Sort | 392,551 | 9,082 | 26.607.768 | 0 | 53.220.153 |
| Quick Sort 2 pivôs | 675,839 | 10,387 | 47.949.538 | 18.504 | 95.973.092 |
| Bubble Sort | 776,412 | 16,044 | 43.199.448 | 26.597.392 | 192.788.464 |
| Selection Sort | 860,772 | 29,797 | 53.846.253 | 4.616 | 107.710.970 |

#### Município · Aleatória · n = 10.378

| Algoritmo | Tempo médio (ms) | Desvio (ms) | Comparações | Trocas | Acessos |
|---|---:|---:|---:|---:|---:|
| Quick Sort 3-Way | 2,284 | 0,162 | 87.520 | 76.928 | 397.404 |
| Quick Sort 2 pivôs | 3,192 | 0,142 | 143.962 | 35.637 | 430.472 |
| Intro Sort | 3,227 | 0,070 | 134.722 | 37.050 | 417.644 |
| Quick Sort | 3,637 | 0,137 | 160.270 | 46.687 | 371.494 |
| Merge Sort | 3,811 | 0,193 | 131.708 | 0 | 669.349 |
| Tim Sort (simplificado) | 4,483 | 0,052 | 129.181 | 0 | 671.938 |
| Heap Sort | 5,310 | 0,126 | 245.030 | 129.053 | 1.006.272 |
| Shell Sort | 5,395 | 0,087 | 169.516 | 0 | 384.919 |
| Insertion Sort | 428,073 | 16,759 | 26.637.238 | 0 | 53.284.832 |
| Selection Sort | 855,350 | 7,268 | 53.846.253 | 10.363 | 107.733.958 |
| Bubble Sort | 1.453,345 | 4,534 | 53.736.380 | 26.626.863 | 213.980.212 |

#### Município · Já ordenada · n = 10.378

| Algoritmo | Tempo médio (ms) | Desvio (ms) | Comparações | Trocas | Acessos |
|---|---:|---:|---:|---:|---:|
| Insertion Sort | 0,225 | 0,010 | 10.377 | 0 | 20.754 |
| Bubble Sort | 0,245 | 0,041 | 10.377 | 0 | 20.754 |
| Merge Sort | 0,306 | 0,039 | 10.377 | 0 | 20.754 |
| Tim Sort (simplificado) | 0,618 | 0,082 | 33.715 | 0 | 44.092 |
| Shell Sort | 1,978 | 0,109 | 99.821 | 0 | 199.642 |
| Intro Sort | 2,566 | 0,218 | 177.637 | 14.328 | 412.586 |
| Quick Sort 2 pivôs | 2,696 | 0,136 | 181.946 | 20.392 | 445.460 |
| Quick Sort | 2,951 | 0,120 | 206.190 | 27.204 | 339.642 |
| Heap Sort | 3,847 | 0,087 | 238.715 | 127.307 | 986.658 |
| Quick Sort 3-Way | 9,262 | 0,432 | 530.510 | 520.026 | 2.612.786 |
| Selection Sort | 778,143 | 27,859 | 53.846.253 | 0 | 107.692.506 |

#### Município · Inversamente ordenada · n = 10.378

| Algoritmo | Tempo médio (ms) | Desvio (ms) | Comparações | Trocas | Acessos |
|---|---:|---:|---:|---:|---:|
| Merge Sort | 1,769 | 0,084 | 71.616 | 0 | 502.725 |
| Tim Sort (simplificado) | 2,473 | 0,106 | 82.959 | 0 | 586.898 |
| Intro Sort | 2,476 | 0,158 | 169.915 | 19.515 | 417.890 |
| Shell Sort | 2,657 | 0,129 | 121.776 | 0 | 273.505 |
| Quick Sort 2 pivôs | 2,811 | 0,117 | 187.891 | 25.430 | 477.502 |
| Quick Sort | 2,861 | 0,126 | 198.478 | 32.415 | 352.766 |
| Heap Sort | 4,499 | 0,528 | 235.979 | 121.157 | 956.586 |
| Quick Sort 3-Way | 5,564 | 0,196 | 354.762 | 344.321 | 1.734.222 |
| Insertion Sort | 853,470 | 10,859 | 53.637.659 | 0 | 107.286.267 |
| Selection Sort | 937,471 | 14,036 | 53.846.253 | 7.340 | 107.721.866 |
| Bubble Sort | 1.053,249 | 23,311 | 53.845.977 | 53.627.861 | 322.203.398 |

### Expoente empírico k (custo ≈ c·n^k), critério Município

| Algoritmo | k comparações (aleatória) | k comparações (ordenada) | k comparações (inversa) | k tempo (aleatória) |
|---|---:|---:|---:|---:|
| Bubble Sort | 2,01 | 1,00 | 2,00 | 2,16 |
| Selection Sort | 2,00 | 2,00 | 2,00 | 2,12 |
| Insertion Sort | 1,97 | 1,00 | 2,00 | 2,05 |
| Shell Sort | 1,17 | 1,18 | 1,16 | 1,39 |
| Merge Sort | 1,16 | 1,00 | 1,10 | 1,29 |
| Quick Sort | 1,13 | 1,21 | 1,20 | 1,29 |
| Quick Sort 3-Way | 1,05 | 1,39 | 1,36 | 1,20 |
| Quick Sort 2 pivôs | 1,13 | 1,34 | 1,29 | 1,28 |
| Intro Sort | 1,13 | 1,30 | 1,28 | 1,30 |
| Heap Sort | 1,18 | 1,16 | 1,19 | 1,32 |
| Tim Sort (simplificado) | 1,17 | 1,01 | 1,11 | 1,34 |

### Expoente empírico k, critério Data/hora

| Algoritmo | k comparações (aleatória) | k comparações (ordenada) | k comparações (inversa) | k tempo (aleatória) |
|---|---:|---:|---:|---:|
| Bubble Sort | 2,00 | 1,00 | 2,00 | 2,11 |
| Selection Sort | 2,00 | 2,00 | 2,00 | 2,11 |
| Insertion Sort | 1,99 | 1,00 | 2,00 | 2,05 |
| Shell Sort | 1,16 | 1,18 | 1,15 | 1,33 |
| Merge Sort | 1,16 | 1,00 | 1,08 | 1,27 |
| Quick Sort | 1,13 | 1,23 | 1,19 | 1,27 |
| Quick Sort 3-Way | 1,02 | 1,35 | 1,31 | 1,11 |
| Quick Sort 2 pivôs | 1,13 | 1,43 | 1,16 | 1,22 |
| Intro Sort | 1,14 | 1,29 | 1,27 | 1,22 |
| Heap Sort | 1,18 | 1,16 | 1,19 | 1,26 |
| Tim Sort (simplificado) | 1,17 | 1,01 | 1,11 | 1,27 |
| Radix Sort (LSD) | 0 comp. | 0 comp. | 0 comp. | 1,03 |

## 3. Análise dos resultados

1. **Teoria confirmada pelo expoente empírico.**
   - **O(n²):** Bubble, Selection e Insertion têm k ≈ **2,00** nas comparações. Selection é exatamente n(n−1)/2 = 53.846.253 em **todos** os cenários, porque a busca do mínimo não se beneficia de ordem prévia.
   - **O(n log n):** os algoritmos dessa classe ficam em k ≈ **1,1–1,2**. Na faixa 100–10.378, n·log₂n cresce como n^1,1–1,2, então é o valor esperado.

2. **Melhor caso visível.**
   - Com a entrada já ordenada, Bubble (com parada antecipada), Insertion e Merge (com o teste "metades já em ordem") fazem **exatamente n−1 = 10.377 comparações**, com k = 1,00: comportamento linear.
   - O Selection continua com 53,8 milhões de comparações.

3. **Pior caso visível.**
   - Na entrada invertida, o Insertion faz cerca de n²/2 comparações e o Bubble, cerca de n²/2 trocas. Exemplo (município): 53.627.861 trocas e 322 milhões de acessos ao array.
   - O Insertion **desloca** em vez de trocar: 0 trocas e metade dos acessos do Bubble.

4. **Selection: poucas trocas, muitas comparações.** Faz no máximo n−1 trocas (10.369 no aleatório), contra 26 milhões do Bubble. Seria a escolha certa se escrever fosse muito mais caro do que comparar.

5. **Chaves repetidas (bioma, só 2 valores): Quick Sort 3-Way.**
   - Faz só **14.998 comparações**, cerca de 1,45·n, contra 146.668 do Quick Sort clássico.
   - No aleatório leva 0,40 ms, contra 2,52 ms do Quick Sort clássico. O Quick Sort com dois pivôs vem logo atrás (0,55 ms): os elementos iguais aos pivôs também saem da recursão.
   - Os iguais ao pivô não voltam para a recursão.

6. **Limitação do 3-Way.** Com chaves quase todas distintas e já ordenadas (município ou data, cenário "já ordenada"), ele faz mais trocas que o Quick Sort clássico: 520.026 contra 27.204 no município. A partição de três vias move elementos iguais ao pivô mesmo quando o vetor já está em ordem.

7. **Radix Sort: 0 comparações.**
   - É o mais rápido na data em todos os cenários: 0,52–0,57 ms.
   - O custo é idêntico em qualquer ordem inicial (176.426 acessos), porque é O(d·n) sem depender da disposição dos dados.
   - O expoente de tempo ficou em k = 1,10, perto do linear esperado. Na primeira rodada, com aquecimento curto, ele aparecia como 0,53: o tempo de n pequeno estava inflado pelo código ainda não compilado pelo JIT.

8. **Estabilidade × desempenho.** Entre os estáveis, Merge e Tim Sort ficam na mesma ordem de grandeza do Quick Sort, com cerca de 130 mil comparações no aleatório. Para ordenação multicritério em passadas, o Merge é a escolha segura.

9. **Custo real × contagem.** O Heap Sort faz cerca de 2× mais comparações que o Merge (245 mil contra 132 mil no município aleatório) e leva mais tempo (5,3 ms contra 3,8 ms), pela pouca localidade de cache nos saltos pai→filho do heap. Entre os O(n log n) no aleatório, os mais lentos são o Shell Sort (bioma e município) e o Quick Sort clássico (data). No cenário aleatório, os O(n log n) levam de 0,4 a 7 ms para 10 mil registros, enquanto os O(n²) levam de 0,18 a 1,5 s: **2 a 3 ordens de grandeza de diferença**.

10. **Variabilidade.** O desvio-padrão de alguns casos passa de 20% da média (ex.: Selection na data já ordenada, 854 ± 188 ms), por efeitos de GC e da frequência variável da CPU do notebook. As contagens de operações, determinísticas, são a base mais confiável para comparar os algoritmos.

11. **O aquecimento importa.** Com aquecimento de 2 execuções (primeira rodada), os tempos dos O(n log n) eram 2 a 4 vezes maiores que os do JMH. Com no mínimo 500 ms de aquecimento por caso, o Merge Sort na data aleatória caiu de 12,1 ms para 3,9 ms, e os expoentes de tempo passaram a ficar um pouco acima dos de comparações (1,1–1,3), provavelmente por efeitos de cache quando os dados deixam de caber nos níveis mais rápidos de memória.

12. **Os novos algoritmos.** Na entrada já ordenada por município, Intro Sort (177.637 comparações) e Quick Sort com dois pivôs (181.946) comparam menos que o Quick Sort clássico (206.190), e fazem muito menos trocas que o 3-Way (520.026). No aleatório, os dois ficam entre os mais rápidos em todos os critérios. O Counting Sort não aparece nesta bateria; na Hora local (24 chaves) ele ordena a base com zero comparações e exatamente 2n escritas.


## Validação com JMH

O benchmark próprio foi conferido com o **JMH** (Java Microbenchmark Harness, da equipe do OpenJDK), num perfil Maven separado:

```bash
./mvnw -Pjmh -DskipTests compile exec:exec@jmh
```

**Configuração:** critério data/hora, cenário aleatório (semente 42), n = 10.378, 2 forks, cada um com 5 aquecimentos e 10 medições de 1 s. O JMH informa o intervalo de confiança de 99,9%. Os resultados estão em [`resultados/jmh.md`](resultados/jmh.md).

**Primeira rodada (aquecimento de 2 execuções):** o benchmark próprio mediu tempos **2,3 a 4 vezes maiores** que o JMH (Merge Sort: 12,1 ms contra 4,1 ms). A ordem dos algoritmos coincidia (Spearman 0,89) e as contagens de operações eram idênticas, porque não dependem da JVM. A causa era o aquecimento curto: o compilador JIT (C2) ainda não tinha otimizado os métodos.

**Correção aplicada:** o aquecimento passou a ser por **tempo mínimo** (2 execuções e no mínimo 500 ms por caso), e toda a bateria foi refeita.

**Segunda rodada (resultado final, em [`resultados/jmh.md`](resultados/jmh.md)):**
- A diferença das médias caiu para a faixa de −20% a +94% na maioria dos algoritmos; em 3 dos 9 (Shell, Merge e Heap) os intervalos de confiança se sobrepõem. O Quick Sort clássico continua 3,4 vezes mais lento no benchmark próprio (6,9 ms contra 2,1 ms), com desvio alto nas 5 repetições.
- A ordem dos algoritmos se mantém: correlação de Spearman de 0,80 entre os dois rankings. Nos dois, o Radix é o mais rápido e o Quick Sort 3-Way lidera entre os que comparam.
- **Referência do Java:** o `Arrays.sort` de objetos (TimSort da biblioteca) levou 2,25 ms e fez 106.535 comparações na mesma entrada. O nosso Tim Sort simplificado fez 129.095 e o Merge Sort, 131.545; os Quick Sort 3-Way (68.969) e com dois pivôs (123.114) compararam menos, mas nenhum deles é estável. Os tempos dos nossos algoritmos incluem a contagem de operações do vetor instrumentado, e mesmo assim os Quick Sort 3-Way (1,38 ms) e com dois pivôs (1,56 ms) ficaram abaixo do `Arrays.sort` sob o JMH.
- **Conclusão:** para comparar algoritmos, as contagens de operações são exatas e reprodutíveis; os tempos do benchmark próprio servem para a ordem e para o expoente, e o JMH dá o valor absoluto confiável.
