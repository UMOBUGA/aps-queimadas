# External Merge Sort com heap limitado

JVM com heap máximo de 64 MB (-Xmx) · Java 21.0.10


## 7. External Merge Sort: focos do Brasil com memória limitada

| Medida | Valor |
|---|---:|
| Arquivos de entrada | 6 (163,6 MB) |
| Registros ordenados | 1.273.473 |
| Limite de memória | 100.000 registros por run |
| Runs gravados em disco | 13 |
| Comparações na fase 1 (Merge Sort de cada run) | 14.009.755 |
| Comparações na fase 2 (intercalação 13-way com heap) | 2.336.912 |
| Tempo da fase 1 | 4,185 s |
| Tempo da fase 2 | 1,399 s |
| Pico de heap da JVM observado | 57 MB |
| Saída verificada em ordem | sim |
