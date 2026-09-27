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

## 4. Deep Learning aplicado a imagens de satélite (fundamentação teórica)

O sistema não implementa Deep Learning, mas a dissertação deve explicar como ele seria aplicado:

- **Dados.** Imagens multiespectrais (Landsat-8/9, Sentinel-2, CBERS-4A/WFI) recortadas em *patches*. Os rótulos vêm de mapeamentos de referência: PRODES/DETER (INPE) para desmatamento e MapBiomas Fogo para cicatrizes de queimada.
- **Modelos.** **Redes neurais convolucionais (CNNs)** aprendem filtros hierárquicos (bordas → texturas → padrões de uso do solo), conforme Goodfellow, Bengio e Courville (2016). Para mapear *onde* está o desmatamento, usam-se arquiteturas de **segmentação semântica** como a **U-Net** (Ronneberger et al., 2015), que classificam cada pixel.
- **Atributos espectrais.** As bandas NIR e SWIR e índices como **NDVI** e **NBR** (*Normalized Burn Ratio*) realçam vegetação queimada. A diferença do NBR antes e depois do fogo (dNBR) mede a severidade.
- **Integração com este sistema.** Os focos do INPE, ordenados por data e município e agregados por este sistema, podem servir de **rótulos fracos** (onde e quando procurar cicatrizes) e de **variáveis temporais** para modelos híbridos **CNN + LSTM**, que combinam imagem e série histórica para prever a evolução das queimadas.
