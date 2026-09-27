# Fase 1 — Obtenção e entendimento dos dados

## 1. Onde estão os dados

Diretório público do **Programa Queimadas (INPE)** com os focos detectados pelo **satélite de referência**, organizados por estado:

```
https://dataserver-coids.inpe.br/queimadas/queimadas/focos/csv/anual/EstadosBr_sat_ref/
└── SP/
    ├── focos_br_sp_ref_2003.zip
    ├── ...
    ├── focos_br_sp_ref_2023.zip   (≈71 KB compactado → 234 KB)
    └── focos_br_sp_ref_2024.zip   (≈342 KB compactado → 1,2 MB)
```

- **Padrão do nome:** `focos_br_<uf>_ref_<ano>.zip`, com a UF em minúsculas no arquivo e em maiúsculas na pasta.
- **Formato:** cada `.zip` contém um único `.csv` com o mesmo nome.
- **Como baixar:**
  - **Manual:** clique no `.zip` no navegador e extraia o CSV para `data/raw/`.
  - **Pelo sistema:** botão **"Baixar do INPE…"** no dashboard, ou `java -jar aps-queimadas-1.0.0-all.jar baixar --uf SP --anos 2023,2024`.
  - **No projeto:** os arquivos de SP (2023 e 2024) já estão versionados em `data/raw/`.

> **Por que o diretório `EstadosBr_sat_ref`?**
>
> O INPE usa um **satélite de referência** (AQUA, passagem da tarde) para compor séries históricas comparáveis. A quantidade de satélites ativos muda com os anos. Usar só o de referência elimina esse viés e evita contar várias vezes o mesmo fogo detectado por satélites diferentes.
>
> Essa escolha deve ser justificada na dissertação: é a leitura mais segura do enunciado, que aponta exatamente esse diretório.

## 2. Estrutura real do arquivo (conferida em 26/09/2026)

**Cabeçalho real**, com 9 colunas, codificação UTF-8, separador vírgula e quebra de linha LF:

```
id_bdq,foco_id,lat,lon,data_pas,pais,estado,municipio,bioma
 1615033254 ,6aeb30de-7e5c-346c-a954-88f4b6a980d3,  -20.939350 ,  -49.125500 ,2023-01-02 17:09:00,Brasil,SÃO PAULO,UCHOA,Mata Atlântica
```

| Coluna | Tipo | Observações |
|---|---|---|
| `id_bdq` | inteiro | ID no banco de queimadas; vem com espaços ao redor |
| `foco_id` | UUID | identificador único, usado para eliminar duplicatas |
| `lat`, `lon` | decimal | graus WGS84, com espaços de preenchimento |
| `data_pas` | data/hora | passagem do satélite, em **GMT/UTC** (`yyyy-MM-dd HH:mm:ss`) |
| `pais`, `estado` | texto | "Brasil", "SÃO PAULO" |
| `municipio` | texto | MAIÚSCULAS, com acentos |
| `bioma` | texto | "Cerrado" ou "Mata Atlântica" (em SP) |

**Colunas que o enunciado cita, mas que não existem neste conjunto:**

- `satelite`
- `numero_dias_sem_chuva`
- `precipitacao`
- `risco_fogo`
- `frp`

Elas aparecem em outros produtos do INPE (por exemplo, os arquivos diários e mensais com todos os satélites). O leitor do sistema as reconhece como **opcionais**: se existirem, habilitam critérios de ordenação extras e são limpas (`-999` → ausente); se não existirem, o sistema segue normalmente.

## 3. Perfil dos dados de SP (valores reais, extraídos pelo sistema)

| Indicador | Valor |
|---|---|
| Focos em 2023 | **1.666** |
| Focos em 2024 | **8.712** (+423%) |
| Total unificado | **10.378** (0 rejeitados, 0 duplicados) |
| Municípios distintos | **580** |
| Biomas | Mata Atlântica: 5.762 · Cerrado: 4.616 |
| Mês de pico | **agosto/2024: 3.612 focos** (crise de incêndios no interior paulista) |
| Horário das detecções | ~16h–17h GMT (13h–14h em Brasília), passagem vespertina do AQUA |

**Focos por mês:**

| Mês | jan | fev | mar | abr | mai | jun | jul | ago | set | out | nov | dez |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 2023 | 62 | 36 | 109 | 22 | 145 | 134 | 253 | 352 | 375 | 58 | 54 | 66 |
| 2024 | 75 | 93 | 138 | 63 | 398 | 532 | 499 | **3.612** | 2.522 | 714 | 25 | 41 |

## 4. Plano de limpeza (implementado em `io/CsvLoader.java`)

| # | Problema possível | Tratamento |
|---|---|---|
| 1 | **Encoding** misto (UTF-8 × Latin-1) | Decodificação UTF-8 **estrita** do arquivo inteiro; se falhar, usa ISO-8859-1. Remove o BOM e corrige *mojibake* (`SÃƒO` → `SÃO`). |
| 2 | **Separador** diferente (arquivo reexportado pelo Excel) | Detectado no cabeçalho (`,` `;` `\t` `\|`). Com `;`, aceita vírgula decimal. |
| 3 | **Colunas em outra ordem ou com outro nome** | Mapeamento por nome, sem acento e em minúsculas, com sinônimos (`data_pas` / `data_hora_gmt` / `datahora`). Coluna obrigatória ausente gera `DataValidationException` com a lista das colunas encontradas. |
| 4 | **Espaços** (`" 1615033254 "`) | `strip` em todos os campos e espaços internos colapsados. |
| 5 | **Valores ausentes/sentinela** (`-999`, vazio, negativo) | Nas colunas físicas opcionais viram `null` e são contados no relatório de qualidade. |
| 6 | **Datas inválidas** | Seis formatos aceitos; ano plausível (1998 até o ano atual + 1). Fora disso, a linha é rejeitada com o motivo. |
| 7 | **Coordenadas inválidas** | Fora da caixa envolvente do Brasil: linha rejeitada. |
| 8 | **Caixa/acentos em nomes** | Município em MAIÚSCULAS pt-BR (padrão IBGE); bioma em "Primeira Maiúscula"; acentos preservados. As buscas ignoram acentos e as ordenações usam `Collator` pt-BR. |
| 9 | **Unificação** dos anos | Todos os CSVs da pasta viram uma base única e imutável (`BaseDeFocos`); `foco_id` repetido é descartado. |
| 10 | **Linha corrompida** | Não interrompe a carga: é descartada e registrada (arquivo, número da linha, motivo e conteúdo). O relatório aparece na aba "Qualidade dos dados" e nos relatórios Excel. |

**Datas em GMT.** O INPE publica `data_pas` em GMT. O sistema mantém GMT nas tabelas (fiel à fonte) e converte para o horário de Brasília (UTC−3) apenas no gráfico "focos por hora local".
