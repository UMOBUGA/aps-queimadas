# Fase 4 — Benchmark e análise de performance

## 1. Metodologia

| Aspecto | Decisão | Justificativa |
|---|---|---|
| Algoritmos | Os 10 implementados | Radix só no critério numérico (data) |
| Critérios | Data/hora, Bioma, Município | Os três exigidos pelo enunciado |
| Tamanhos (n) | 100, 1.000, 5.000, 10.000 e 10.378 (base inteira) | Tamanhos crescentes pedidos no enunciado |
| Cenários | Aleatório, já ordenado, inversamente ordenado | Caso médio, melhor e pior caso dos algoritmos simples |
| Amostragem | Sorteio com semente fixa (42) | Reprodutível |
| Mesma entrada | Para cada (critério, n, cenário) a entrada é preparada **uma vez**; cada execução recebe uma cópia idêntica | Todos os algoritmos competem em igualdade |
| Aquecimento do JIT | Aquecimento global (cada algoritmo 3× com n = 1.000) + **2 execuções descartadas** por caso | A JVM interpreta o bytecode no início e compila os métodos "quentes" (C1/C2) depois; sem aquecimento, mede-se o interpretador |
| Repetições | **5 execuções medidas** por caso: média, desvio-padrão amostral, mínimo e máximo | O mínimo é o estimador menos sensível a ruído (GC, sistema operacional) |
| Relógio | `System.nanoTime()` | Monotônico, com resolução de nanossegundos |
| Contagem de operações | `InstrumentedArray` + `OperationCounter` | Determinística: igual em todas as repetições |
| Verificação | Cada execução é conferida como ordenada | Garante que nenhum algoritmo "ganhou" errando |
| Complexidade empírica | Regressão log-log do custo × n → expoente **k** (custo ≈ c·nᵏ) | Compara a prática com a teoria: k ≈ 2 para O(n²), k ≈ 1,0–1,15 para O(n log n) na faixa medida |

**Como reproduzir:**

```bash
java -Xss8m -Xmx2g -jar target/aps-queimadas-1.0.0-all.jar resultados
```

Ou use a aba Benchmark do dashboard. Os arquivos gerados estão em [`resultados/`](resultados/).

**Cuidado com o Quick Sort.** Em entradas já ordenadas, usa pivô mediana de três e recursão na menor parte, então não há `StackOverflowError`. A discussão do pior caso O(n²) está em [ALGORITMOS.md](ALGORITMOS.md#4-decisões-de-projeto-que-valem-a-defesa-oral).

## 2. Resultados reais


### Execução

- **Tamanho:** 450 casos (420 executados; o Radix só roda no critério numérico).
- **Verificação:** 420 de 420 execuções conferidas como corretamente ordenadas.
- **Duração:** 19 min 31 s.
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
| Radix Sort (LSD) | 3,479 | 0,604 | 0 | 0 | 176.426 |
| Quick Sort 3-Way | 10,113 | 1,688 | 68.969 | 58.457 | 304.781 |
| Quick Sort | 11,674 | 0,707 | 157.626 | 50.442 | 384.078 |
| Merge Sort | 12,101 | 1,772 | 131.545 | 0 | 667.530 |
| Shell Sort | 18,402 | 1,122 | 152.332 | 0 | 342.307 |
| Tim Sort (simplificado) | 18,804 | 2,516 | 129.095 | 0 | 668.435 |
| Heap Sort | 21,321 | 2,197 | 245.261 | 129.172 | 1.007.210 |
| Insertion Sort | 1.884,527 | 33,553 | 26.198.395 | 0 | 52.407.158 |
| Selection Sort | 4.385,660 | 288,925 | 53.846.253 | 10.369 | 107.733.982 |
| Bubble Sort | 4.845,417 | 95,644 | 53.797.665 | 26.188.026 | 212.347.434 |

#### Data/hora · Já ordenada · n = 10.378

| Algoritmo | Tempo médio (ms) | Desvio (ms) | Comparações | Trocas | Acessos |
|---|---:|---:|---:|---:|---:|
| Bubble Sort | 0,712 | 0,101 | 10.377 | 0 | 20.754 |
| Insertion Sort | 0,806 | 0,293 | 10.377 | 0 | 20.754 |
| Merge Sort | 1,288 | 0,534 | 10.377 | 0 | 20.754 |
| Tim Sort (simplificado) | 1,905 | 0,312 | 33.715 | 0 | 44.092 |
| Radix Sort (LSD) | 2,468 | 0,233 | 0 | 0 | 176.426 |
| Shell Sort | 9,623 | 1,171 | 99.821 | 0 | 199.642 |
| Quick Sort | 11,375 | 0,583 | 233.591 | 34.394 | 395.895 |
| Heap Sort | 15,224 | 1,358 | 234.282 | 124.882 | 968.092 |
| Quick Sort 3-Way | 20,516 | 0,796 | 367.706 | 357.293 | 1.798.846 |
| Selection Sort | 3.428,551 | 101,363 | 53.846.253 | 0 | 107.692.506 |

#### Data/hora · Inversamente ordenada · n = 10.378

| Algoritmo | Tempo médio (ms) | Desvio (ms) | Comparações | Trocas | Acessos |
|---|---:|---:|---:|---:|---:|
| Radix Sort (LSD) | 2,615 | 0,632 | 0 | 0 | 176.426 |
| Merge Sort | 5,525 | 0,775 | 64.750 | 0 | 434.027 |
| Shell Sort | 9,045 | 0,672 | 116.330 | 0 | 257.549 |
| Tim Sort (simplificado) | 9,552 | 1,996 | 79.320 | 0 | 505.366 |
| Quick Sort | 10,005 | 0,878 | 203.856 | 39.161 | 384.556 |
| Quick Sort 3-Way | 11,553 | 0,892 | 233.173 | 222.761 | 1.126.181 |
| Heap Sort | 15,453 | 1,062 | 233.939 | 120.184 | 948.614 |
| Bubble Sort | 3.452,602 | 124,542 | 53.846.252 | 52.202.440 | 316.502.264 |
| Insertion Sort | 3.960,950 | 333,431 | 52.212.245 | 0 | 104.435.435 |
| Selection Sort | 4.028,921 | 193,938 | 53.846.253 | 5.286 | 107.713.650 |

#### Bioma · Aleatória · n = 10.378

| Algoritmo | Tempo médio (ms) | Desvio (ms) | Comparações | Trocas | Acessos |
|---|---:|---:|---:|---:|---:|
| Quick Sort 3-Way | 3,234 | 0,347 | 14.998 | 4.618 | 33.478 |
| Heap Sort | 8,728 | 0,581 | 135.750 | 65.946 | 535.284 |
| Merge Sort | 10,434 | 1,248 | 99.651 | 0 | 607.760 |
| Quick Sort | 10,982 | 1,507 | 146.668 | 64.791 | 429.864 |
| Tim Sort (simplificado) | 12,425 | 1,511 | 103.877 | 0 | 562.440 |
| Shell Sort | 16,378 | 3,107 | 104.306 | 0 | 214.683 |
| Insertion Sort | 868,959 | 29,330 | 13.116.062 | 0 | 26.236.739 |
| Bubble Sort | 5.582,077 | 466,309 | 43.189.072 | 13.105.685 | 138.800.884 |
| Selection Sort | 5.931,856 | 619,491 | 53.846.253 | 4.615 | 107.710.966 |

#### Bioma · Já ordenada · n = 10.378

| Algoritmo | Tempo médio (ms) | Desvio (ms) | Comparações | Trocas | Acessos |
|---|---:|---:|---:|---:|---:|
| Bubble Sort | 0,699 | 0,050 | 10.377 | 0 | 20.754 |
| Merge Sort | 0,926 | 0,074 | 10.377 | 0 | 20.754 |
| Insertion Sort | 1,136 | 0,389 | 10.377 | 0 | 20.754 |
| Quick Sort 3-Way | 1,530 | 0,421 | 14.998 | 4.618 | 33.478 |
| Tim Sort (simplificado) | 2,458 | 0,638 | 33.715 | 0 | 44.092 |
| Shell Sort | 9,460 | 0,589 | 99.821 | 0 | 199.642 |
| Heap Sort | 9,681 | 0,590 | 135.863 | 68.108 | 544.158 |
| Quick Sort | 13,836 | 1,535 | 186.094 | 62.332 | 460.554 |
| Selection Sort | 4.353,054 | 394,713 | 53.846.253 | 0 | 107.692.506 |

#### Bioma · Inversamente ordenada · n = 10.378

| Algoritmo | Tempo médio (ms) | Desvio (ms) | Comparações | Trocas | Acessos |
|---|---:|---:|---:|---:|---:|
| Quick Sort 3-Way | 4,731 | 0,276 | 14.998 | 4.619 | 33.482 |
| Merge Sort | 6,353 | 2,448 | 28.480 | 0 | 121.889 |
| Tim Sort (simplificado) | 11,521 | 3,121 | 50.426 | 0 | 167.745 |
| Heap Sort | 20,214 | 0,554 | 149.036 | 71.343 | 583.444 |
| Quick Sort | 27,235 | 1,185 | 145.242 | 66.227 | 434.258 |
| Shell Sort | 32,467 | 14,317 | 101.220 | 0 | 210.799 |
| Insertion Sort | 3.062,521 | 340,601 | 26.607.768 | 0 | 53.220.153 |
| Bubble Sort | 4.415,966 | 290,108 | 43.199.448 | 26.597.392 | 192.788.464 |
| Selection Sort | 5.014,551 | 1.222,759 | 53.846.253 | 4.616 | 107.710.970 |

#### Município · Aleatória · n = 10.378

| Algoritmo | Tempo médio (ms) | Desvio (ms) | Comparações | Trocas | Acessos |
|---|---:|---:|---:|---:|---:|
| Quick Sort 3-Way | 9,891 | 0,959 | 87.520 | 76.928 | 397.404 |
| Quick Sort | 14,006 | 0,365 | 160.270 | 46.687 | 371.494 |
| Merge Sort | 15,043 | 0,775 | 131.708 | 0 | 669.349 |
| Shell Sort | 21,620 | 1,262 | 169.516 | 0 | 384.919 |
| Tim Sort (simplificado) | 23,197 | 2,462 | 129.181 | 0 | 671.938 |
| Heap Sort | 27,516 | 2,310 | 245.030 | 129.053 | 1.006.272 |
| Insertion Sort | 1.464,213 | 657,794 | 26.637.238 | 0 | 53.284.832 |
| Selection Sort | 2.405,935 | 148,454 | 53.846.253 | 10.363 | 107.733.958 |
| Bubble Sort | 4.224,529 | 150,097 | 53.736.380 | 26.626.863 | 213.980.212 |

#### Município · Já ordenada · n = 10.378

| Algoritmo | Tempo médio (ms) | Desvio (ms) | Comparações | Trocas | Acessos |
|---|---:|---:|---:|---:|---:|
| Insertion Sort | 0,810 | 0,265 | 10.377 | 0 | 20.754 |
| Merge Sort | 0,818 | 0,292 | 10.377 | 0 | 20.754 |
| Tim Sort (simplificado) | 1,854 | 0,413 | 33.715 | 0 | 44.092 |
| Bubble Sort | 1,906 | 0,966 | 10.377 | 0 | 20.754 |
| Shell Sort | 6,325 | 0,754 | 99.821 | 0 | 199.642 |
| Quick Sort | 8,071 | 0,569 | 206.190 | 27.204 | 339.642 |
| Heap Sort | 12,482 | 0,587 | 238.715 | 127.307 | 986.658 |
| Quick Sort 3-Way | 27,701 | 1,370 | 530.510 | 520.026 | 2.612.786 |
| Selection Sort | 2.237,194 | 58,790 | 53.846.253 | 0 | 107.692.506 |

#### Município · Inversamente ordenada · n = 10.378

| Algoritmo | Tempo médio (ms) | Desvio (ms) | Comparações | Trocas | Acessos |
|---|---:|---:|---:|---:|---:|
| Merge Sort | 4,496 | 0,553 | 71.616 | 0 | 502.725 |
| Shell Sort | 6,913 | 0,424 | 121.776 | 0 | 273.505 |
| Tim Sort (simplificado) | 7,254 | 0,476 | 82.959 | 0 | 586.898 |
| Quick Sort | 7,464 | 0,473 | 198.478 | 32.415 | 352.766 |
| Heap Sort | 12,541 | 1,094 | 235.979 | 121.157 | 956.586 |
| Quick Sort 3-Way | 14,557 | 0,938 | 354.762 | 344.321 | 1.734.222 |
| Selection Sort | 2.802,712 | 261,913 | 53.846.253 | 7.340 | 107.721.866 |
| Insertion Sort | 2.923,853 | 603,472 | 53.637.659 | 0 | 107.286.267 |
| Bubble Sort | 3.046,934 | 185,995 | 53.845.977 | 53.627.861 | 322.203.398 |

### Expoente empírico k (custo ≈ c·n^k), critério Município

| Algoritmo | k comparações (aleatória) | k comparações (ordenada) | k comparações (inversa) | k tempo (aleatória) |
|---|---:|---:|---:|---:|
| Bubble Sort | 2,01 | 1,00 | 2,00 | 2,20 |
| Selection Sort | 2,00 | 2,00 | 2,00 | 1,93 |
| Insertion Sort | 1,97 | 1,00 | 2,00 | 1,87 |
| Shell Sort | 1,17 | 1,18 | 1,16 | 1,23 |
| Merge Sort | 1,16 | 1,00 | 1,10 | 1,13 |
| Quick Sort | 1,13 | 1,21 | 1,20 | 1,10 |
| Quick Sort 3-Way | 1,05 | 1,39 | 1,36 | 1,02 |
| Heap Sort | 1,18 | 1,16 | 1,19 | 1,15 |
| Tim Sort (simplificado) | 1,17 | 1,01 | 1,11 | 1,18 |

### Expoente empírico k, critério Data/hora

| Algoritmo | k comparações (aleatória) | k comparações (ordenada) | k comparações (inversa) | k tempo (aleatória) |
|---|---:|---:|---:|---:|
| Bubble Sort | 2,00 | 1,00 | 2,00 | 2,19 |
| Selection Sort | 2,00 | 2,00 | 2,00 | 2,10 |
| Insertion Sort | 1,99 | 1,00 | 2,00 | 2,05 |
| Shell Sort | 1,16 | 1,18 | 1,15 | 1,41 |
| Merge Sort | 1,16 | 1,00 | 1,08 | 1,16 |
| Quick Sort | 1,13 | 1,23 | 1,19 | 1,17 |
| Quick Sort 3-Way | 1,02 | 1,35 | 1,31 | 1,06 |
| Heap Sort | 1,18 | 1,16 | 1,19 | 1,28 |
| Tim Sort (simplificado) | 1,17 | 1,01 | 1,11 | 1,38 |
| Radix Sort (LSD) | 0 comp. | 0 comp. | 0 comp. | 0,53 |


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
   - É o mais rápido nesse critério em qualquer cenário: 3,2 ms contra 11,0 ms no aleatório.
   - Os iguais ao pivô não voltam para a recursão.

6. **Limitação do 3-Way.** Com chaves quase todas distintas e já ordenadas (município ou data, cenário "já ordenada"), ele faz mais trocas que o Quick Sort clássico: 520.026 contra 27.204 no município. A partição de três vias move elementos iguais ao pivô mesmo quando o vetor já está em ordem.

7. **Radix Sort: 0 comparações.**
   - É o mais rápido na data em todos os cenários: 2,6–3,5 ms.
   - O custo é idêntico em qualquer ordem inicial (176.426 acessos), porque é O(d·n) sem depender da disposição dos dados.
   - O expoente de tempo (k ≈ 0,5) fica abaixo de 1 porque, para n pequeno, dominam os custos fixos das 8 passadas × 256 posições de contagem.

8. **Estabilidade × desempenho.** Entre os estáveis, Merge e Tim Sort ficam na mesma ordem de grandeza do Quick Sort, com cerca de 130 mil comparações no aleatório. Para ordenação multicritério em passadas, o Merge é a escolha segura.

9. **Custo real × contagem.** O Heap Sort faz cerca de 2× mais comparações que o Merge (245 mil contra 132 mil no aleatório) e, na maioria dos casos, é o mais lento dos O(n log n), pela pouca localidade de cache nos saltos pai→filho do heap. A exceção é o bioma aleatório, em que as muitas chaves iguais encurtam a descida no heap. Os tempos dos O(n log n) são todos da ordem de 10–30 ms para 10 mil registros, enquanto os O(n²) levam de 0,9 a 6 s: **2 a 3 ordens de grandeza de diferença**.

10. **Variabilidade.** O desvio-padrão de alguns casos passa de 20% da média (ex.: Insertion no município aleatório, 1.464 ± 658 ms), por efeitos de JIT, GC e frequência variável da CPU do notebook. As contagens de operações, determinísticas, são a base mais confiável para comparar os algoritmos.


## Validação com JMH

O benchmark próprio foi conferido com o **JMH** (Java Microbenchmark Harness, da equipe do OpenJDK), num perfil Maven separado:

```bash
./mvnw -Pjmh -DskipTests compile exec:exec@jmh
```

**Configuração:** critério data/hora, cenário aleatório (semente 42), n = 10.378, 2 forks, cada um com 5 aquecimentos e 10 medições de 1 s. O JMH informa o intervalo de confiança de 99,9%. Os resultados estão em [`resultados/jmh.md`](resultados/jmh.md).

**O que a comparação mostrou (primeira rodada):**
- **Tempos absolutos:** o benchmark próprio mediu tempos **2,3 a 4 vezes maiores** que o JMH. Por exemplo, Merge Sort: 12,1 ms contra 4,1 ms.
  - **Causa:** o aquecimento de 2 execuções por caso não bastava para o compilador JIT (C2) otimizar os métodos.
  - **Não é a contagem:** a instrumentação de operações é a mesma nas duas medições.
- **Ordem relativa:** praticamente preservada, com correlação de Spearman de 0,89. As quatro primeiras posições coincidem (Radix, Quick 3-Way, Quick e Merge); só Shell, Tim e Heap trocam de lugar entre si.
- **Contagens de operações:** idênticas, porque não dependem da JVM.

**Correção aplicada:** o aquecimento passou a ser por **tempo mínimo** (cada algoritmo roda até completar 500 ms de aquecimento no caso), e a bateria foi regerada. A tabela final fica em [`resultados/jmh.md`](resultados/jmh.md).
