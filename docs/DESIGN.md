# Design da interface — design system, decisões de UX e texto para a dissertação

Este documento registra o redesign do dashboard (Fases 1 a 8 do plano de UI). A seção 8 é um **texto pronto para adaptar** na seção "Projeto do programa" da dissertação.

Os prints ficam em [`prints/`](prints/). Para regenerá-los:

```bash
java -jar target/aps-queimadas-1.0.0-all.jar capturas
```

---

## 1. Diagnóstico da versão anterior → solução

| # | Problema | Solução adotada |
|---|---|---|
| 1 | Visual padrão "Modena" (botões, abas, combos e scrollbars de sistema antigo) | Tema base **AtlantaFX (Primer)** sobrescrito pelo design system do projeto: tokens próprios, scrollbars finas, controles com raio de 6–7 px |
| 2 | Barra de ações sem hierarquia (botões vermelhos e cinzas misturados, sem ícones) | **Uma ação primária** ("Exportar", com menu: Excel, PDF, Relatório de código). Ações secundárias viram **botões de ícone com tooltip** (Abrir CSV, Recarregar, Baixar do INPE, Tema) |
| 3 | Cabeçalho com gradiente pesado e badge com informação densa | Top bar neutra com breadcrumb + título da tela e **chips discretos** de contexto (UF, período, nº de focos, arquivos, com lista no tooltip) |
| 4 | **Bug da pizza**: rótulos sobrepostos no canto e legenda duplicada | Pizza substituída por **donut** próprio (`DonutChart`): total no centro, legenda lateral com valor e %, 2 px de folga entre segmentos e tooltip |
| 5 | Barras "invisíveis" (brancas) e gráfico de horas com uma única barra branca | **Causa 1 (CSS):** a regra `-fx-background-color: #b3261e, white` empilhava uma camada branca sobre a barra. **Causa 2 (dados):** o agrupamento estava correto. O satélite de referência (AQUA) passa sobre SP no início da tarde, e **100% das detecções caem entre 13h e 15h (UTC−3)**: 16h GMT = 577, 17h = 8.896, 18h = 905. O gráfico agora mostra as **24 horas** com essa explicação no subtítulo. O Top 10 virou barras horizontais sólidas com o valor no fim de cada barra |
| 6 | KPIs desalinhados, sem ícones nem contexto | `KpiCard` padronizado: **8 cards** de mesma largura e altura em grade responsiva (4/3/2 colunas), rótulo em caixa-alta, valor grande, ícone em badge, linha de contexto com tendência e sparkline |
| 7 | Paleta sem sistema | Paleta **com significado fixo** (ano, bioma, algoritmo, calor), validada para daltonismo e contraste, e usada igual em gráficos, mapa e relatórios |
| 8 | Tipografia sem hierarquia, espaço desperdiçado | Fonte **Inter** embarcada, escala tipográfica de 6 níveis, escala de espaçamento 4/8/12/16/24/32 e grade com alinhamento rigoroso |

## 2. Conceito

**Painel de monitoramento ambiental** sóbrio e confiável (referências: Linear, Vercel, Grafana, NASA FIRMS).

- **Base neutra:** a cor não enfeita, informa.
- **Destaque brasa** (`#E8590C`) aparece pouco e só onde importa:
  - a ação primária ("Exportar");
  - o ano mais recente;
  - o item de menu ativo;
  - a escala de intensidade.

## 3. Design tokens

Arquivos em `src/main/resources/br/unip/aps/ui/css/`:

| Arquivo | Conteúdo |
|---|---|
| `tokens.css` | Paleta bruta (slate, ink, brasa, semânticas) e documentação das escalas |
| `theme-light.css` / `theme-dark.css` | Tokens semânticos `-aps-*` e sobrescrita dos tokens do AtlantaFX |
| `base.css` | Tipografia e controles base |
| `components.css` | Componentes |
| `charts.css` | Gráficos |

**Regra:** componentes só referenciam tokens semânticos, e o código Java não tem cores. Trocar o tema é trocar um único arquivo.

### 3.1 Interface

| Token | Claro | Escuro | Uso / contraste |
|---|---|---|---|
| `-aps-bg-app` | `#F5F6F8` | `#0B0F14` | Fundo da aplicação |
| `-aps-surface` | `#FFFFFF` | `#151B23` | Cards |
| `-aps-border` | `#E4E7EC` | `#273140` | Bordas sutis |
| `-aps-text-1` | `#101828` | `#F2F4F7` | Texto principal (17,8:1 / 15,7:1) |
| `-aps-text-2` | `#475467` | `#B4BDC9` | Secundário (7,7:1 / 9,1:1) |
| `-aps-text-3` | `#667085` | `#8C97A6` | Terciário (5,0:1 / 5,9:1) |
| `-aps-accent` | `#E8590C` | `#F97316` | Destaque gráfico |
| `-aps-accent-strong` | `#C2410C` | `#F97316` | Botão primário (texto 5,18:1 / 6,86:1) |
| sucesso / alerta / erro / info | `#067647` `#B54708` `#B42318` `#175CD3` | `#47CD89` `#FDB022` `#F97066` `#53B1FD` | Sempre ≥ 5:1 sobre o fundo suave correspondente |

Todas as combinações de texto passam no **WCAG AA** (4,5:1). O cálculo foi feito com a fórmula de luminância relativa da W3C.

### 3.2 Dados (significado fixo em todo o sistema)

| Entidade | Claro | Escuro |
|---|---|---|
| Ano mais recente (2024) | `#E8590C` brasa | `#EC6325` |
| Anos anteriores (2023) | `#3D6DB5` azul-acinzentado | `#6A95D6` |
| Mata Atlântica | `#1F6E45` verde floresta | `#23804F` |
| Cerrado | `#C48E22` ocre | `#BC862A` |
| Amazônia · Caatinga · Pantanal · Pampa (reservados) | `#2A78D6` `#E87BA4` `#4A3AA7` `#1BAF7A` | `#3987E5` `#D55181` `#9085E9` `#199E70` |
| Calor (sequencial, 1 matiz) | `#F0975C` → `#E8590C` → `#C4470A` → `#963608` → `#662405` | Invertida (mais focos = mais brilho) |

**Validação.** As paletas foram conferidas com um validador de paletas categóricas. O script verifica:

- faixa de luminosidade e croma mínimo;
- separação entre cores (ΔE em OKLab) sob **protanopia, deuteranopia e tritanopia**;
- contraste com a superfície.

**Resultados:**

- **Anos:** ΔE 22,8 (CVD).
- **Mata Atlântica × Cerrado:** separação garantida por luminosidade (verde escuro × ocre claro).
- **Contraste do Cerrado sobre branco (2,9:1):** fica abaixo de 3:1. Isso é compensado por rótulos diretos e pela tabela "Ver dados" em todos os gráficos.

**Cor segue a entidade, nunca a posição.** O JavaFX reescreve as classes CSS das séries quando outra série é adicionada. O utilitário `Graficos.classe` reaplica a classe da entidade sempre que isso acontece. Resultado: 2024 é sempre brasa, mesmo quando o filtro deixa só esse ano.

### 3.3 Tipografia, espaçamento, raios e sombras

- **Fonte:** Inter (Rasmus Andersson, licença OFL), 4 pesos embarcados em `resources/fonts`.
  - O JavaFX registra Medium e SemiBold como famílias próprias ("Inter Medium", "Inter SemiBold").
  - O JavaFX não suporta `font-feature-settings`, então não há números tabulares. Os números nas tabelas são alinhados à direita.
- **Escala tipográfica:** display 28 · h1 20 · h2 15 · h3 13,5 · corpo 13,5 · pequeno 12,5 · legenda 11 (px).
- **Espaçamento:** 4 · 8 · 12 · 16 · 24 · 32 (classe `ui.componentes.Espaco`).
- **Raios:** 6–7 px (controles) · 10 (chips, tabelas) · 14 (cards e diálogos).
- **Sombras:** 2 níveis (cards; popups e toasts), mais fortes no tema escuro.

## 4. Estrutura e navegação

```
┌────────────┬───────────────────────────────────────────────────────────────┐
│ ▣ APS      │ APS Queimadas › Análise                    [SP][2023–2024]    │
│ Queimadas  │ Visão geral            [10.378 focos][2 arq.] ⌂ ↻ ☁ ☾ [Exportar▾]│
│            ├───────────────────────────────────────────────────────────────┤
│ ANÁLISE    │ ≡ Filtros [Bioma▾][Ano▾][🔍 Município…][Início▾]→[Fim▾] ✕ 10.378│
│ ▸ Visão ger│   (Bioma: Cerrado ✕) (Ano: 2024 ✕)                            │
│   Ordenação├───────────────────────────────────────────────────────────────┤
│   Benchmark│ [KPI][KPI][KPI][KPI]                                          │
│   Mapa     │ [KPI][KPI][KPI][KPI]                                          │
│   ML       │ [Focos por mês ────────][Focos por bioma (donut)]              │
│   Qualidade│ [Top 10 municípios ─────][Focos por hora (24h)]                │
│            │ [Calendário de focos por dia ──────────────────────────]       │
│ ⚙ Config.  │                                                               │
│ ⓘ Sobre    │                                                     [toast]   │
│ « Recolher │                                                               │
└────────────┴───────────────────────────────────────────────────────────────┘
```

- **Sidebar recolhível** (Ctrl+B): ícone + rótulo; recolhida, mostra só o ícone com tooltip. O item ativo tem fundo brasa suave.
- **Barra de filtros global**, fixa, visível só nas telas que a usam (Visão geral, Ordenação, Mapa):
  - bioma e ano com seleção múltipla;
  - município com autocompletar (a busca ignora acentos; escolher na lista filtra o município exato);
  - período por mês;
  - chips removíveis dos filtros ativos.
- **Estados globais:**
  - splash durante a carga;
  - estado vazio quando não há CSV (ações: Abrir CSV / Baixar do INPE);
  - barra de progresso fina sob a top bar;
  - overlay com Cancelar em tarefas de mais de 300 ms;
  - toasts de sucesso e erro;
  - diálogos estilizados, em vez do `Alert` padrão.
- **Responsividade:** `GradeResponsiva` recalcula as colunas pela largura (KPIs 4/3/2; gráficos 2/1). Janela mínima de 1100×700. Botões e chips nunca encolhem até virar "…" (`Layout.naoEncolher`).

## 5. Componentes (`br.unip.aps.ui.componentes`)

| Componente | Função |
|---|---|
| `KpiCard` | Rótulo, valor, ícone em badge, contexto com tendência (cor por regra: aumento de focos = alerta), sparkline |
| `ChartCard` | Título, subtítulo, legenda em linha, chip de destaque, menu (Exportar PNG / Ver dados em tabela), estados conteúdo, skeleton e vazio |
| `DonutChart`, `BarrasHorizontais`, `CalendarioHeatmap`, `MatrizCalor`, `Sparkline` | Gráficos próprios, com tooltips em todos os elementos |
| `SidebarItem`, `FilterBar`, `Chip`, `EmptyState`, `Toast`, `Dialogos`, `LoadingOverlay` | Navegação, filtros e feedback |
| `SortVisualizer` | Animação da ordenação (32 barras) gravada via `InstrumentedArray.Ouvinte`, com as mesmas contagens do sistema |
| `Graficos`, `Icones`, `Espaco`, `Layout` | Utilitários do design system |

**Botões:** primário, secundário, fantasma, perigo e ícone. Todos têm estados hover, pressionado, desabilitado e **foco visível** (`:focus-visible`).

**Tabelas:**

- cabeçalho fixo, linhas zebradas;
- números à direita em pt-BR;
- badges coloridos de bioma;
- rolagem virtualizada.

A ordenação por clique no cabeçalho foi desativada de propósito: o `TableView` usa `Collections.sort` internamente, e toda ordem exibida precisa vir dos algoritmos do projeto.

## 6. Gráficos

| Gráfico | Decisão |
|---|---|
| Focos por mês | Área com gradiente leve, pontos só no hover, **pico anotado** ("Pico: ago/2024 — 3.612"), legenda em linha |
| Biomas | Donut com total no centro e legenda com valor e % |
| Top 10 | Barras horizontais sólidas com valor no fim; líder em negrito e chip "Líder" |
| Hora local | 24 barras (0–23); o subtítulo explica o horário de passagem do satélite |
| **Novo:** calendário por dia | Heatmap estilo "contribution graph", com faixas fixas 0 · 1–2 · 3–9 · 10–29 · 30–99 · 100+ |
| Benchmark | **Pequenos múltiplos**: O(n²) e O(n log n) em gráficos separados (cada um com até 7 séries e cor fixa por algoritmo); escala linear/log; curvas teóricas tracejadas |
| ML | Real × previsto (real em cinza-grafite); matriz de confusão como heatmap normalizado por linha; importância em barras |
| Mapa | Mapas-base Esri Light/Dark Gray (acompanham o tema); modos Pontos, Agrupado (cluster) e **Calor** (grade de densidade de ~9 km com contagem exata no tooltip); cor por bioma ou ano; hotspots do DBSCAN |

Todos os gráficos têm:

- grade horizontal tracejada e sem grade vertical;
- tooltip com valor exato e percentual;
- "Ver dados em tabela", a alternativa acessível ao gráfico;
- exportação em PNG.

As animações (200–320 ms) podem ser desligadas em Configurações.

## 7. Acessibilidade e atalhos

- **Contraste AA** nos dois temas (tabela 3.1). A identidade dos dados nunca depende só da cor: há legenda, rótulo direto ou tooltip.
- **Teclado:**

| Atalho | Ação |
|---|---|
| Ctrl+1…6 | Telas |
| Ctrl+O | Abrir CSV |
| Ctrl+R | Recarregar |
| Ctrl+E | Exportar |
| Ctrl+T | Tema |
| Ctrl+B | Menu |
| F1 | Sobre |

  Tab percorre os controles com foco visível.
- **Preferências** (tema, animações, menu) persistidas com `java.util.prefs.Preferences`.
- **Formatação pt-BR** em todos os números e datas.
- **Relatórios PDF/Excel** com a mesma identidade: fonte Inter embarcada no PDF, cabeçalho brasa, zebra e rodapé paginado (classe `report.Identidade`).

---

## 8. Texto para a dissertação — "Projeto do programa: interface e experiência do usuário"

> Adapte à formatação ABNT/UNIP. As afirmações abaixo descrevem o que foi implementado; os números vêm dos prints e das medições reais.

**8.1 Objetivos de design.** A interface foi concebida como um painel de monitoramento ambiental. O objetivo é que o usuário responda rapidamente três perguntas:

- quantos focos houve e como variaram;
- onde e quando ocorreram;
- qual algoritmo de ordenação é mais eficiente para cada critério.

O projeto partiu de um diagnóstico da primeira versão, que usava os componentes padrão do JavaFX: hierarquia confusa, cores sem significado e defeitos de renderização em gráficos. Esse diagnóstico orientou um redesign baseado em um *design system*: um conjunto de tokens (cores, tipografia, espaçamentos) e componentes reutilizáveis que garantem consistência entre telas, temas e relatórios.

**8.2 Hierarquia visual e Lei de Hick.** Cada tela tem uma única ação primária, destacada pela cor brasa; as demais são secundárias ou ícones com tooltip. A Lei de Hick (HICK, 1952) indica que o tempo de decisão cresce com o número de alternativas equivalentes. Reduzir as escolhas visíveis e agrupar as exportações em um único menu diminui a carga cognitiva. A navegação foi reorganizada em uma barra lateral com seis telas de análise e duas de sistema, separadas por seção.

**8.3 Princípios da Gestalt.**

- **Proximidade e região comum:** os cards agrupam título, legenda, gráfico e ações relacionados.
- **Similaridade:** a mesma cor representa sempre a mesma entidade em todas as telas (o ano mais recente é sempre brasa; a Mata Atlântica é sempre verde).
- **Continuidade:** a grade responsiva alinha rigorosamente as bordas dos cards.
- **Figura-fundo:** a base neutra faz os dados sobressaírem.

**8.4 Cor com propósito.** As cores de dados foram escolhidas por função:

- **Categórica** para entidades (anos, biomas, algoritmos).
- **Sequencial de um único matiz** para intensidade (calor), evitando o arco-íris, que distorce a percepção de ordem.
- **Validada objetivamente:** a paleta foi conferida quanto à distinção sob os três tipos de daltonismo (protanopia, deuteranopia e tritanopia) e quanto ao contraste mínimo.

**8.5 Correção de problemas de visualização.** Três exemplos de decisões guiadas por dados:

- **Pizza → donut.** O gráfico de pizza foi substituído por um donut com legenda numérica, porque ângulos são percebidos com menos precisão do que comprimentos e rótulos diretos (CLEVELAND; McGILL, 1984).
- **Benchmark em pequenos múltiplos.** Dez algoritmos em um único gráfico exigiriam dez cores indistinguíveis. A divisão em O(n²) e O(n log n) mantém no máximo sete séries por gráfico e permite comparar cada curva com a curva teórica tracejada.
- **Horário das detecções.** O gráfico de horas parecia ter um erro de agrupamento. A análise dos dados mostrou que todas as detecções ocorrem entre 13h e 15h (horário de Brasília), porque o satélite de referência passa sobre São Paulo no início da tarde. O gráfico passou a exibir as 24 horas com essa explicação: um caso em que a interface precisa comunicar uma característica do sensor, não esconder um "defeito".

**8.6 Acessibilidade.**

- Todas as combinações de texto atendem ao nível AA das WCAG 2.1 (W3C, 2018): contraste ≥ 4,5:1, com valores calculados na tabela de tokens.
- A informação nunca depende apenas da cor: legendas, rótulos e tooltips acompanham as marcas.
- Todo gráfico oferece uma alternativa textual ("Ver dados em tabela").
- Há navegação completa por teclado com foco visível, atalhos e tema escuro para ambientes com pouca luz.

**8.7 Responsividade e desempenho percebido.**

- **Layout:** a grade reorganiza os indicadores conforme a largura da janela.
- **Operações demoradas:** ordenação, benchmark, aprendizado de máquina e exportação rodam em *threads* separadas (`javafx.concurrent.Task`), com barra de progresso, sobreposição cancelável e notificações ao concluir. A interface nunca congela.
- **Feedback imediato:** estados de carregamento (*skeleton*) e estados vazios explicativos orientam o usuário.

**8.8 Arquitetura da interface.**

- **MVC:** as telas são arquivos FXML (visão) com *controllers* dedicados, que recebem um contexto compartilhado por injeção de dependência.
- **Componentes:** implementados como classes Java reutilizáveis e documentadas.
- **Estilos:** separados do código em seis folhas CSS organizadas em camadas (tokens → tema → base → componentes → gráficos). A troca de tema em tempo real consiste apenas em substituir a folha de tema e o tema base do AtlantaFX.

**Referências sugeridas:**

- CLEVELAND, W. S.; McGILL, R. Graphical perception: theory, experimentation, and application to the development of graphical methods. *Journal of the American Statistical Association*, v. 79, n. 387, p. 531–554, 1984.
- HICK, W. E. On the rate of gain of information. *Quarterly Journal of Experimental Psychology*, v. 4, n. 1, p. 11–26, 1952.
- W3C. *Web Content Accessibility Guidelines (WCAG) 2.1*. 2018. Disponível em: https://www.w3.org/TR/WCAG21/.
- TUFTE, E. R. *The Visual Display of Quantitative Information*. 2. ed. Cheshire: Graphics Press, 2001.
- NIELSEN, J. *Usability Engineering*. San Francisco: Morgan Kaufmann, 1993.

## 9. Prints recomendados para a dissertação

| Arquivo (`docs/prints/`) | Legenda sugerida | Seção |
|---|---|---|
| `completa/01-visao-geral-claro.png` | Visão geral: indicadores, focos por mês, biomas, municípios, horário e calendário | Projeto do programa — Dashboard |
| `09-visao-geral-escuro.png` | Tema escuro da visão geral | Design system |
| `17-visao-geral-filtro-cerrado-2024.png` | Filtro global com chips ativos (Cerrado, 2024) | Interação |
| `02-ordenacao-claro.png` | Ordenação multicritério (Bioma → Município → Data) com contagem de operações | Algoritmos |
| `18-ordenacao-comparativo.png` | Comparativo dos algoritmos sobre a mesma entrada | Algoritmos / Resultados |
| `19-ordenacao-visualizacao.png` | Visualização animada do Quick Sort | Apresentação |
| `completa/03-benchmark-claro.png` | Benchmark: custo × n com curvas teóricas, vencedores e expoente empírico | Resultados |
| `04-mapa-claro.png`, `20-mapa-calor.png`, `21-mapa-agrupado.png` | Distribuição geográfica: pontos por bioma, densidade e agrupamento com hotspots | Geoprocessamento |
| `completa/05-ml-claro.png` | Machine Learning: métricas, previsão, matriz de confusão, importância e hotspots | ML |
| `completa/06-qualidade-claro.png` | Qualidade dos dados: completude e problemas por severidade | Metodologia (limpeza) |
| `07-configuracoes-claro.png`, `08-sobre-claro.png` | Preferências, atalhos e créditos | Apêndice |
