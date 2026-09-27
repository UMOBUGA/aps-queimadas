# Fase 5 — Machine Learning e análise preditiva

Biblioteca: **Smile 4.4** (Java puro; as bibliotecas nativas foram excluídas no `pom.xml`). A implementação completa está no pacote `br.unip.aps.ml`.

**Execução:**

- aba **Machine Learning** do dashboard;
- modo `ml`;
- opção 6 do menu `cli`.

## 1. Da ordenação ao aprendizado

O enunciado descreve a ordenação como a **etapa inicial** que prepara os dados para o ML. No sistema, isso é literal:

1. **`BaseMensal`** ordena os 10.378 focos por **município → data** com o **Merge Sort** do projeto. Ele é estável e O(n log n); na base real, foram **123.337 comparações** em cerca de 10 ms.
2. Com a base ordenada, a agregação em séries **município × mês** é **uma única varredura linear** (*control break*):
   - todos os focos de um município ficam contíguos e em ordem cronológica;
   - basta detectar a troca de município e de mês;
   - não é preciso nenhuma estrutura auxiliar de agrupamento.
3. **Resultado:** 580 municípios × 24 meses = **13.920 observações**.

**Variáveis de cada observação (município m, mês t)**, sem vazamento: nada vem de t ou do futuro.

| Variável | Significado |
|---|---|
| `mes_sin`, `mes_cos` | Sazonalidade codificada no círculo (dezembro fica perto de janeiro) |
| `lat`, `lon` | Centroide dos focos do município |
| `frac_cerrado` | Fração dos focos do município no Cerrado |
| `lag1`, `lag2`, `lag3` | Focos do município em t−1, t−2, t−3 |
| `media_hist` | Média mensal do município em todos os meses anteriores a t |
| `estado_lag1` | Focos no estado inteiro em t−1 (intensidade da estação) |

## 2. Modelos e protocolo de validação

**Validação temporal.** Séries temporais nunca são embaralhadas: isso vazaria o futuro para o treino.

- **Treino fixo:** treina em **2023** e testa em **2024**.
- **Janela expansível** (*walk-forward*): para cada mês de 2024, treina com **todo o histórico anterior** a ele e prevê só aquele mês. Simula o uso real, com retreino mensal.

| Tarefa | Modelo | Métricas | Referência (baseline) |
|---|---|---|---|
| **Previsão** da quantidade de focos por município/mês | Random Forest de regressão (200 árvores; semente fixa por árvore) | MAE, RMSE, R² | Persistência (ŷ = mês anterior) e média histórica |
| **Classificação** do nível de atividade: baixo (0), médio (1–4), alto (5+ focos no mês) | Random Forest de classificação (Gini) com **balanceamento de classes** | Acurácia, precisão, revocação, F1 por classe, **F1 macro**, matriz de confusão | Classe majoritária (sempre "baixo") |
| **Hotspots** geográficos | **DBSCAN** (eps = 10 km, minPts = 30) e **K-Means** (k = 8, K-Means++), com método do cotovelo | Nº de grupos, ruído, raio, município principal | — |

**Adaptação ao conjunto de dados.** O enunciado sugere classificar o *risco de fogo* a partir de precipitação, dias sem chuva e FRP. Os CSVs `EstadosBr_sat_ref` **não trazem essas colunas**. Por isso o alvo do classificador passou a ser o **nível de atividade de fogo** esperado, uma medida operacional de risco.

Se o grupo usar outro produto do INPE que tenha essas colunas, o `CsvLoader` já as lê. Elas podem ser adicionadas às variáveis da `BaseMensal`.

**Coordenadas projetadas em km.** Antes da clusterização, as coordenadas são convertidas para km (projeção equirretangular local). Em graus, 1° de longitude encolhe com a latitude, e as distâncias ficariam distorcidas.

## 3. Resultados reais (SP, treino 2023 → teste 2024)

Arquivo completo: [`resultados/resultados.xlsx`](resultados/resultados.xlsx), aba "Machine Learning".

### Previsão de focos por município/mês (6.960 observações de teste)

| Modelo | MAE (focos) | RMSE | R² |
|---|---|---|---|
| **Random Forest — treino fixo 2023** | **1,313** | 4,919 | −0,001 |
| Random Forest — janela expansível | 1,671 | **4,851** | **0,027** |
| Baseline: persistência (mês anterior) | 1,860 | 6,165 | −0,572 |
| Baseline: média histórica do município | 1,418 | 4,942 | −0,010 |

**Interpretação (para a conclusão da dissertação):**

- **O Random Forest supera os dois baselines no MAE.** O erro médio cai 29% em relação à persistência.
- **R² ≈ 0 mostra a limitação central.** 2024 foi um ano **fora da distribuição** de 2023: +423% de focos e 3.612 só em agosto. Modelos de árvore não extrapolam valores maiores do que os vistos no treino, então o treino fixo subestima a crise.
- **A janela expansível adapta-se melhor.** Ela incorpora os meses já observados de 2024 e melhora o RMSE e o R², ou seja, erra menos nos picos. Em troca, piora o MAE nos meses calmos.
- **Previsão de extremos exige variáveis meteorológicas.** Precipitação, umidade, vento e dias sem chuva são justamente os campos ausentes deste conjunto. É a limitação e o trabalho futuro mais importantes.

### Classificação do nível de atividade

| Modelo | Acurácia | F1 macro |
|---|---|---|
| Baseline: sempre "baixo" | 0,757 | 0,287 |
| **Random Forest balanceado** | 0,607 | **0,450** |

**Interpretação:**

- **O desbalanceamento das classes** é forte: no treino de 2023 são 6.098 "baixo", 801 "médio" e 61 "alto".
- **Sem balanceamento, a floresta repete o baseline.** Primeira versão testada: acurácia 0,757 e F1 macro 0,29, porque previa quase sempre "baixo".
- **O ajuste:** o `classWeight` do Smile representa a **proporção** de cada classe na amostragem estratificada, e não um peso inverso. Com proporções n_c / n_min, cada árvore passa a ver as três classes em igual número.
- **O efeito:** o F1 macro sobe de 0,287 para 0,450 e o modelo passa a detectar os meses críticos. A acurácia cai porque deixa de acertar "de graça" a classe dominante. Esse **trade-off entre acurácia e F1 macro** é um ponto clássico a discutir na dissertação.

### Hotspots (DBSCAN, eps = 10 km, minPts = 30)

- **51 aglomerados**, com **3.451 focos isolados** (ruído).
- A tabela completa (centroide, raio, município principal, bioma, focos por ano) está no Excel e na aba ML.
- **No mapa**, marque "Hotspots do ML" para sobrepor os grupos aos focos.
- **Calibração dos parâmetros** (valores medidos):
  - eps = 15 km funde quase todo o interior em um único grupo (8.843 focos);
  - eps = 5 km com minPts = 50 deixa 94% dos focos como ruído;
  - eps = 10 km com minPts = 30 equilibra os dois extremos.

## 4. Estudo com seis anos de histórico (2019–2024)

A primeira versão treinava só com 2023 e testava em 2024. Esta evolução amplia o histórico e testa o que realmente ajuda a prever.

**Números completos:** [`resultados/ml-estudo.md`](resultados/ml-estudo.md). Para reproduzir:

```bash
java -jar aps-queimadas.jar historico --anos 2019-2024 --meteorologia
java -jar aps-queimadas.jar ml-estudo
```

**Dados:**
- **Focos de referência:** 26.643 focos do satélite de referência em SP, em 624 municípios com pelo menos um foco.
- **Meteorologia:** 563.459 detecções de todos os satélites. São os únicos arquivos do INPE que trazem `numero_dias_sem_chuva`, `precipitacao`, `risco_fogo` e `frp`, baixados já filtrados para SP.
- **Vizinhança:** calculada pela malha do IBGE, com vértices de fronteira compartilhados. São 644 municípios com fronteira detectada, 1 por proximidade (os 5 centroides mais próximos) e média de 5,7 vizinhos.

**Variáveis novas:**

| Grupo | Variáveis |
|---|---|
| Ano anterior | `lag12` (mesmo mês do ano anterior) e `media_12m` (média móvel dos 12 meses anteriores) |
| Vizinhos | `viz_lag1` e `viz_lag12` (soma dos focos dos municípios vizinhos) |
| Meteorologia do mês anterior | risco de fogo, dias sem chuva, precipitação e FRP médios no município (ou no estado, se não houve detecção), focos de todos os satélites e risco médio do estado |

**Sem vazamento:** tudo é do mês anterior ou antes, exceto na linha "explicativa" da ablação, marcada como tal.

### Validação em janelas (rolling origin)

Para cada ano de teste, o modelo treina com todos os anos anteriores (desde 2020, para que o `lag12` exista):

| Teste | Treino | RF MAE | RF R² | Persistência | Média histórica | Sazonal ingênuo |
|---:|---|---:|---:|---:|---:|---:|
| 2021 | 2020 | 1,201 | 0,068 | 1,055 | 1,059 | 1,076 |
| 2022 | 2020–2021 | 0,562 | −0,769 | 0,344 | 0,633 | 0,780 |
| 2023 | 2020–2022 | **0,341** | 0,124 | 0,358 | 0,563 | 0,344 |
| 2024 | 2020–2023 | 1,355 | 0,133 | 1,729 | 1,352 | **1,196** |

**Leitura honesta:** o Random Forest vence todos os baselines só em 2023. Em anos atípicos, ele perde para regras simples:
- **2021 e 2022:** vence a persistência.
- **2024:** vence o "sazonal ingênuo" (mesmo mês do ano anterior).

Com uma série de só 4–5 anos por município e contagens muito esparsas (a maioria dos municípios tem 0 focos na maior parte dos meses), a vantagem de um modelo complexo é pequena e instável. Validar em várias janelas, e não num único corte, é justamente o que revela isso.

### Ablação (treino 2020–2023, teste 2024)

| Variáveis (acumuladas) | MAE | RMSE | R² |
|---|---:|---:|---:|
| Base da versão anterior | 1,327 | 4,340 | 0,166 |
| + ano anterior | 1,320 | 4,380 | 0,150 |
| + vizinhos | **1,300** | 4,431 | 0,130 |
| + meteorologia do mês anterior | 1,355 | 4,424 | 0,133 |
| Explicativo: + meteorologia do próprio mês (não é previsão) | 1,603 | 4,386 | 0,148 |

- **Contribuições pequenas:** cada grupo melhora pouco. Os vizinhos dão o menor MAE, e a meteorologia do mês anterior piora ligeiramente.
- **O próprio mês também não ajuda:** mesmo com a meteorologia do próprio mês, que não estaria disponível numa previsão real, o MAE não melhora.
- **Causa:** a limitação não é falta de variáveis. É o modelo, que não extrapola (ver o estudo de caso).

### Intervalo de previsão conforme (90%)

**Método:** calibrado nos resíduos absolutos de 2023, com modelo treinado até 2022, usando o quantil de ordem ⌈(n+1)·0,9⌉ ordenado com o Merge Sort do projeto. O resultado é ŷ ± 0,832 foco por município e mês.

**Cobertura:**
- **Calibração (2023):** 90,0%.
- **Teste (2024):** 66,4%.

O método conforme garante a cobertura quando o futuro se comporta como o passado (hipótese de permutabilidade). O ano atípico de 2024 quebra essa hipótese, e a queda de 90% para 66% mede o tamanho dessa quebra.

### Importância por permutação (teste de 2024)

**Método:** mede o aumento do MAE ao embaralhar cada variável (3 permutações).

**Mais importantes:** `mes_cos` (+0,047), `media_hist` (+0,029), `todos_sats_lag1` (+0,017), `mes_sin` (+0,017) e `lag1` (+0,009).

**Importância negativa:** as médias meteorológicas `frp_lag1`, `risco_lag1` e `estado_risco_lag1` dão valores negativos, ou seja, atrapalham no teste. Isso confirma a ablação: a sazonalidade e o histórico do município carregam quase todo o sinal.

### Estudo de caso: agosto de 2024

| Medida | Valor |
|---|---:|
| Focos reais | 3.612 |
| Previstos pelo modelo completo | 1.083 |
| Maior mês do treino (2020–2023) | 2.277 (agosto de 2021) |
| Agosto de 2024 ÷ maior mês do treino | 1,59× |
| Dias sem chuva médios nos focos (todos os satélites) | 31,4 (agostos anteriores: 14,8) |
| Risco de fogo médio | 0,98 (agostos anteriores: 0,92) |
| Pitangueiras: real × previsto | 100 × 3,3 (agosto de 2023: 0 focos) |

**Por que o modelo não previu:**
1. **Fora do treino:** o mês esteve 59% acima de qualquer mês do período de treino. Uma Random Forest de regressão prevê a média das folhas das árvores, que nunca ultrapassa os valores vistos no treino, então não extrapola.
2. **Sem sinal no histórico:** os municípios com mais focos em agosto de 2024 tinham tido 0 ou 1 foco em agosto de 2023. O histórico deles não indicava o evento.
3. **Sinal fora do alcance:** o sinal que existe nos dados (quase o dobro de dias sem chuva) é do próprio mês. Não se conhece com antecedência.
4. **Caminho para prever extremos:** previsões meteorológicas de fato, como estiagem prevista e anomalia de umidade, e modelos capazes de extrapolar, como regressão de Poisson ou binomial negativa com tendência.

## 5. Deep Learning aplicado a imagens de satélite (fundamentação teórica)

O sistema não implementa Deep Learning, mas a dissertação deve explicar como ele seria aplicado:

- **Dados.** Imagens multiespectrais (Landsat-8/9, Sentinel-2, CBERS-4A/WFI) recortadas em *patches*. Os rótulos vêm de mapeamentos de referência: PRODES/DETER (INPE) para desmatamento e MapBiomas Fogo para cicatrizes de queimada.
- **Modelos.** **Redes neurais convolucionais (CNNs)** aprendem filtros hierárquicos (bordas → texturas → padrões de uso do solo), conforme Goodfellow, Bengio e Courville (2016). Para mapear *onde* está o desmatamento, usam-se arquiteturas de **segmentação semântica** como a **U-Net** (Ronneberger et al., 2015), que classificam cada pixel.
- **Atributos espectrais.** As bandas NIR e SWIR e índices como **NDVI** e **NBR** (*Normalized Burn Ratio*) realçam vegetação queimada. A diferença do NBR antes e depois do fogo (dNBR) mede a severidade.
- **Integração com este sistema.** Os focos do INPE, ordenados por data e município e agregados por este sistema, podem servir de **rótulos fracos** (onde e quando procurar cicatrizes) e de **variáveis temporais** para modelos híbridos **CNN + LSTM**, que combinam imagem e série histórica para prever a evolução das queimadas.
