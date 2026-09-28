# Ambiente de execução dos resultados

Os arquivos desta pasta foram gerados em **27/09/2026** com:

```
java -Xss8m -Xmx2g -cp ... br.unip.aps.Main resultados
```

| Item | Valor |
|---|---|
| CPU | Intel Core i5-1334U (13ª geração, 10 núcleos / 12 threads, notebook) |
| RAM | 16 GB |
| Sistema | Windows 11 Home |
| JVM | OpenJDK 21.0.10 (JetBrains Runtime), HotSpot 64-bit |
| Heap | `-Xmx2g`; pilha `-Xss8m` |
| Dados | `focos_br_sp_ref_2023.csv` + `focos_br_sp_ref_2024.csv` (10.378 focos) |
| Configuração | `application.properties`: tamanhos 100/1.000/5.000/10.000/10.378; aquecimento de 2 execuções e no mínimo 500 ms + 5 repetições; semente 42 |

**Observação.** Notebooks variam a frequência da CPU (turbo e economia de energia). Por isso os tempos trazem o desvio-padrão, e as **contagens de operações** — determinísticas e independentes da máquina — são a medida principal de comparação.

Em outra máquina, os tempos absolutos mudam, mas a ordem relativa dos algoritmos e os expoentes empíricos se mantêm.
