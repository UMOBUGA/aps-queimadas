# Resultados reais: estudo de previsão com histórico 2019–2024

- Focos do satélite de referência em SP: 26.643 (624 municípios com pelo menos um foco).
- Detecções de todos os satélites (variáveis meteorológicas): 563.459.
- Vizinhança pela malha do IBGE: 644 municípios com fronteira detectada, 1 por proximidade (5 mais próximos); média de 5,7 vizinhos.
- Preparo: ordenação município → data com Merge Sort (357.869 comparações) e agregação em uma passada.
- Random Forest com 200 árvores, semente fixa. Duração total: 1 min 3 s.

## 1. Validação em janelas (rolling origin): treino com todos os anos anteriores

| Ano de teste | Treino | Random Forest MAE | RMSE | R² | Persistência MAE | Média histórica MAE | Sazonal ingênuo MAE |
|---:|---|---:|---:|---:|---:|---:|---:|
| 2021 | 2020–2020 | 1,201 | 2,888 | 0,068 | 1,055 | 1,059 | 1,076 |
| 2022 | 2020–2021 | 0,562 | 1,097 | -0,769 | 0,344 | 0,633 | 0,780 |
| 2023 | 2020–2022 | 0,341 | 0,756 | 0,124 | 0,358 | 0,563 | 0,344 |
| 2024 | 2020–2023 | 1,355 | 4,424 | 0,133 | 1,729 | 1,352 | 1,196 |

## 2. Ablação (treino 2020–2023, teste 2024): grupos de variáveis acumulados

| Variáveis | MAE | RMSE | R² |
|---|---:|---:|---:|
| Base (versão anterior) | 1,327 | 4,340 | 0,166 |
| + ano anterior | 1,320 | 4,380 | 0,150 |
| + municípios vizinhos | 1,300 | 4,431 | 0,130 |
| + meteorologia (mês anterior) | 1,355 | 4,424 | 0,133 |
| Explicativo: clima do mês | 1,603 | 4,386 | 0,148 |

## 3. Intervalo de previsão conforme (90%)

Calibrado nos resíduos de 2023 (modelo treinado até 2022): ŷ ± 0,832 focos por município e mês.

| Cobertura desejada | Cobertura na calibração (2023) | Cobertura no teste (2024) |
|---:|---:|---:|
| 90,0% | 90,0% | 66,4% |

## 4. Importância por permutação (aumento do MAE ao embaralhar a variável, média de 3 permutações)

| Variável | Aumento do MAE |
|---|---:|
| mes_cos | 0,047 |
| media_hist | 0,029 |
| todos_sats_lag1 | 0,017 |
| mes_sin | 0,017 |
| lag1 | 0,009 |
| media_12m | 0,006 |
| viz_lag12 | 0,005 |
| lag12 | 0,005 |
| lat | 0,003 |
| lag2 | 0,003 |
| lag3 | 0,002 |
| frac_cerrado | 0,002 |
| viz_lag1 | 0,002 |
| lon | 0,002 |
| precipitacao_lag1 | 0,001 |
| estado_lag1 | 0,001 |
| dias_sem_chuva_lag1 | 0,001 |
| frp_lag1 | -0,002 |
| risco_lag1 | -0,004 |
| estado_risco_lag1 | -0,014 |

## 5. Estudo de caso: 2024-08

| Medida | Valor |
|---|---:|
| Focos reais no mês (SP) | 3.612 |
| Focos previstos (soma dos municípios) | 1.083 |
| Maior mês do período de treino | 2.277 (2021-08) |
| Razão entre o mês e o maior mês de treino | 1,586× |
| Maior valor município-mês no treino | 101 |
| Maior valor município-mês no caso | 100 (PITANGUEIRAS) |
| Dias sem chuva (média dos focos, todos os satélites) | 31,386 (anos anteriores: 14,813) |
| Risco de fogo (média) | 0,980 (anos anteriores: 0,916) |

| Município | Real | Previsto | Mesmo mês do ano anterior |
|---|---:|---:|---:|
| PITANGUEIRAS | 100 | 3,3 | 0 |
| ALTINOPOLIS | 97 | 3,4 | 0 |
| IBITINGA | 77 | 4,6 | 1 |
| SERTAOZINHO | 72 | 3,4 | 0 |
| OLIMPIA | 63 | 4,0 | 1 |
| PATROCINIO PAULISTA | 57 | 3,4 | 0 |
| CAJURU | 55 | 3,1 | 0 |
| JABOTICABAL | 54 | 3,5 | 1 |

## 6. Focos no estado por mês em 2024: real × previsto (modelo completo)

| Mês | Real | Previsto |
|---|---:|---:|
| 2024-01 | 75 | 75 |
| 2024-02 | 93 | 82 |
| 2024-03 | 138 | 86 |
| 2024-04 | 63 | 109 |
| 2024-05 | 398 | 142 |
| 2024-06 | 532 | 382 |
| 2024-07 | 499 | 1.096 |
| 2024-08 | 3.612 | 1.083 |
| 2024-09 | 2.522 | 1.664 |
| 2024-10 | 714 | 938 |
| 2024-11 | 25 | 618 |
| 2024-12 | 41 | 67 |
