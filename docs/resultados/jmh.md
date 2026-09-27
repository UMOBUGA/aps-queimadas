# Validação do benchmark com JMH

Critério data/hora, cenário aleatório (semente 42), n = 10.378. JMH 1.37: 2 forks × (5 aquecimentos + 10 medições de 1 s); o erro é o intervalo de confiança de 99,9% calculado pelo JMH. Benchmark próprio: 2 aquecimentos + 5 repetições; IC de 95% = média ± t(0,975; 4) · s/√5 com t = 2,776.

Java 21.0.10 · 12 núcleos lógicos · Windows 11

| Algoritmo | JMH (ms, IC 99,9%) | Benchmark próprio (ms, IC 95%) | Diferença das médias | Intervalos se sobrepõem |
|---|---:|---:|---:|---|
| Shell Sort | 7,853 ± 1,409 | 18,402 ± 1,393 | 134,3% | não |
| Merge Sort | 4,115 ± 0,359 | 12,101 ± 2,200 | 194,1% | não |
| Quick Sort | 3,453 ± 0,391 | 11,674 ± 0,878 | 238,0% | não |
| Quick Sort 3-Way | 2,508 ± 0,300 | 10,113 ± 2,095 | 303,2% | não |
| Heap Sort | 7,265 ± 0,992 | 21,321 ± 2,728 | 193,5% | não |
| Tim Sort (simplificado) | 5,197 ± 1,095 | 18,804 ± 3,123 | 261,8% | não |
| Radix Sort (LSD) | 1,151 ± 0,137 | 3,479 ± 0,750 | 202,3% | não |
