# Validação do benchmark com JMH

Critério data/hora, cenário aleatório (semente 42), n = 10.378. JMH 1.37: 2 forks × (5 aquecimentos + 10 medições de 1 s); o erro é o intervalo de confiança de 99,9% calculado pelo JMH. Benchmark próprio: aquecimento de 2 execuções e no mínimo 500 ms, depois 5 repetições; IC de 95% = média ± t(0,975; 4) · s/√5 com t = 2,776.

Java 21.0.10 · 12 núcleos lógicos · Windows 11

| Algoritmo | JMH (ms, IC 99,9%) | Benchmark próprio (ms, IC 95%) | Diferença das médias | Intervalos se sobrepõem |
|---|---:|---:|---:|---|
| Shell Sort | 6,155 ± 2,488 | 4,934 ± 0,842 | -19,8% | sim |
| Merge Sort | 3,131 ± 0,757 | 3,858 ± 0,834 | 23,2% | sim |
| Quick Sort | 2,057 ± 0,018 | 6,921 ± 2,910 | 236,4% | não |
| Quick Sort 3-Way | 1,379 ± 0,018 | 2,666 ± 1,004 | 93,3% | não |
| Quick Sort 2 pivôs | 1,558 ± 0,016 | 2,836 ± 0,817 | 82,0% | não |
| Intro Sort | 1,918 ± 0,025 | 3,135 ± 0,601 | 63,4% | não |
| Heap Sort | 3,984 ± 0,458 | 6,393 ± 3,233 | 60,5% | sim |
| Tim Sort (simplificado) | 2,818 ± 0,040 | 4,617 ± 0,781 | 63,8% | não |
| Radix Sort (LSD) | 0,649 ± 0,011 | 0,553 ± 0,055 | -14,8% | não |
| Arrays.sort do Java (referência) | 2,247 ± 0,022 | — | — | — |

## Comparações: nossos algoritmos × Arrays.sort do Java

Mesma entrada. O `Arrays.sort` de objetos usa o TimSort da biblioteca; a contagem vem de um comparador que soma cada chamada.

| Algoritmo | Comparações |
|---|---:|
| Arrays.sort do Java (TimSort) | 106.535 |
| Tim Sort (simplificado) | 129.095 |
| Merge Sort | 131.545 |
| Quick Sort | 157.626 |
| Quick Sort 3-Way | 68.969 |
| Quick Sort 2 pivôs | 123.114 |
| Intro Sort | 130.111 |
| Heap Sort | 245.261 |
