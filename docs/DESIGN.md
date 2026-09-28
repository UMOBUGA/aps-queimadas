# Design da interface — "Boletim de Fogo"

Este documento registra a direção visual do dashboard, o design system e as decisões de UX. A seção 8 é um **texto pronto para adaptar** na seção "Projeto do programa" da dissertação.

Os prints ficam em [`prints/`](prints/). Para regenerá-los:

```bash
java -jar target/aps-queimadas-2.0.0-all.jar capturas
```

---

## 1. Conceito

Cada tela é uma página de uma **reportagem especial de dados** sobre a temporada de fogo em São Paulo, no estilo das editorias de infografia dos jornais: manchetes com números grandes, fios tipográficos no lugar de cards e uma única escala de cor para a intensidade do fogo.

| Decisão | Motivo |
|---|---|
| **Tema escuro como padrão** (claro disponível com Ctrl+T) | O uso principal é a apresentação à banca num projetor. As cores de fogo ganham luminosidade sobre o carvão e o contraste se mantém numa sala com pouca luz. |
| **Manchete de dados** no lugar da grade de 8 cards | Uma frase com o número principal ("Em 2024, São Paulo registrou **8.712** focos de incêndio, **5,2 vezes** o total de 2023") comunica mais rápido do que oito indicadores do mesmo peso. |
| **Faixa térmica** no topo de todas as telas | É a assinatura visual: um retângulo por mês da base, colorido pela quantidade de focos. Mostra a temporada inteira de relance e funciona como filtro. |
| **Fios em vez de cards** | Um fio grosso sobre o título separa as seções, como numa página de jornal. O conteúdo fica sobre o próprio fundo e ganha espaço. |
| **Mapa em tela cheia** | O mapa é o protagonista da tela Mapa. Os controles e o total de focos flutuam por cima dele. |

## 2. Cor

### 2.1 Interface

| Token | Escuro (padrão) | Claro | Uso / contraste |
|---|---|---|---|
| `-aps-bg-app` | `#0D0A09` carvão | `#F3F2F0` | Fundo |
| `-aps-surface` | `#15100E` | `#FFFFFF` | Painéis e tabelas |
| `-aps-text-1` | `#F6EEE7` | `#15100D` | Texto principal (16,5:1 / 18,9:1) |
| `-aps-text-2` | `#CDBDB1` | `#4A413B` | Secundário (10,4:1 / 9,9:1) |
| `-aps-text-3` | `#A08D80` | `#6E635B` | Terciário (6,0:1 / 5,8:1) |
| `-aps-accent` | `#FF6B1A` brasa | `#D9480F` | Destaque, ano recente, ação primária |
| Botão primário | `#FF6B1A` + texto `#1A0B02` (6,7:1) | `#C2410C` + texto branco (5,2:1) | |
| `-aps-rail-*` | `#0A0807` | igual | Menu lateral: escuro nos dois temas, como a "lombada" da publicação |

Todas as combinações de texto passam no **WCAG AA** (≥ 4,5:1), com contraste calculado pela fórmula de luminância relativa da W3C.

### 2.2 Dados

| Entidade | Escuro | Claro |
|---|---|---|
| Ano mais recente (2024) | `#FF6B1A` brasa | `#D9480F` |
| Ano anterior (2023) | `#7FA3D1` aço frio | `#3D6DB5` |
| Mata Atlântica | `#2E9E63` | `#1F6E45` |
| Cerrado | `#D6A03A` | `#C48E22` |
| **Intensidade (escala "inferno")** | `#420A68` → `#932667` → `#DD513A` → `#FCA50A` → `#FCFFA4` | invertida: `#FDDCA0` → … → `#420A68` |

- **2024 quente e 2023 frio:** o contraste de temperatura entre as cores reforça a leitura "o fogo aumentou".
- **Escala inferno:** é perceptualmente uniforme e segura para daltônicos, e lembra a imagem de uma câmera térmica. Ela é usada na faixa térmica, no calendário, no mapa de calor, na matriz de confusão e no gráfico de horas.
- **A escala é sequencial**, do escuro ao incandescente: nunca um arco-íris.
- **Cor segue a entidade, não a posição.** `Graficos.classe` reaplica a classe CSS da série quando o JavaFX a reescreve, então 2024 é brasa mesmo quando o filtro deixa só esse ano.

## 3. Tipografia

| Papel | Fonte | Tamanhos |
|---|---|---|
| Display: títulos de tela, números, manchete | **Big Shoulders Display** (Black, ExtraBold), licença OFL | 168 (manchete) · 54–60 (destaques e placar) · 46 (título da tela) · 25–34 (títulos de seção) |
| Texto, rótulos, tabelas, controles | **Inter** (Regular, Medium, SemiBold), licença OFL | 22 (linha da manchete) · 17 (texto de apoio) · 13,5 (corpo) · 12,5 (rótulos) · 11 (legendas) |

- **Big Shoulders** nasceu da sinalização urbana de Chicago. Condensada e pesada, faz números de cinco dígitos caberem em colunas estreitas, como num placar.
- **Inter** cuida de tudo o que precisa ser lido com precisão.
- **Arquivos:** as duas famílias são embarcadas em `src/main/resources/fonts` e carregadas pelo `GerenciadorTema`.

## 4. Estrutura das telas

```
┌──────────────┬─────────────────────────────────────────────────────────────┐
│ 🔥 QUEIMADAS │ VISÃO GERAL                               ⌂ ↻ ☁ ☀ [Exportar▾]│
│ Focos · INPE │ São Paulo · 2023–2024 · 10.378 focos · 2 arquivos           │
│              │ ▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇▇  ← faixa térmica (24 meses)       │
│ Análise      │ [Bioma▾][Ano▾][Município…][Início]→[Fim]  ✕       10.378 focos│
│ ▸ Visão geral├─────────────────────────────────────────────────────────────┤
│   Ordenação  │ Em 2024, São Paulo registrou                                │
│   Benchmark  │ 8.712 focos de incêndio                                     │
│   Mapa       │ 5,2 vezes o total de 2023 (1.666). O pico foi em agosto…    │
│   ML         │ 580 │ ago/24 │ Andradina │ Mata Atlântica │ 14h               │
│   Qualidade  │ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━│
│              │ A temporada, mês a mês                             3.612    │
│ Configurações│ ━━━━━━━━━━━━━━━━━━━━━━━━  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━│
│ Sobre        │ Onde queima (Top 10)       Em que bioma (donut)             │
└──────────────┴─────────────────────────────────────────────────────────────┘
```

- **Faixa térmica clicável:**
  - clicar em um mês filtra o período;
  - Shift+clique estende a seleção;
  - clicar de novo limpa;
  - fora das telas com filtro, o clique leva à Visão geral.
- **Manchete:** o texto se reescreve conforme o filtro. Com Cerrado + 2024, por exemplo, vira "Em 2024, o bioma Cerrado em São Paulo registrou 3.955 focos". Todos os números vêm dos dados; nada é fixo no código.
- **Placar da Ordenação:** comparações, trocas, acessos e tempo em números de 46 px. É a exigência central do enunciado ("informar o número de operações"), então merece o maior destaque da tela.
- **Mapa:**
  - ocupa toda a área de conteúdo, com mapas-base Esri claro/escuro que acompanham o tema;
  - o total de focos e os modos (Pontos, Agrupado, Calor) flutuam sobre o mapa;
  - o zoom fica à direita.

## 5. Componentes (`br.unip.aps.ui.componentes`)

| Componente | Função |
|---|---|
| `FaixaTermica` | Assinatura visual: 24 meses na escala inferno, legenda "menos → mais focos", seleção de período por clique e entrada animada da esquerda para a direita |
| `ChartCard` | Seção com fio superior, título em display, subtítulo, anotação à direita (ex.: "3.612 focos no pico"), legenda e menu (Exportar PNG / Ver dados em tabela) |
| `KpiCard` | Número em display sobre fio fino; na Ordenação, recebe a classe `placar` |
| `DonutChart`, `BarrasHorizontais`, `CalendarioHeatmap`, `MatrizCalor` | Gráficos próprios; o líder do Top 10 recebe a cor mais quente da escala |
| `SortVisualizer` | Animação da ordenação sobre fundo térmico; as cores indicam comparando, trocando e ordenada |
| `FilterBar`, `Chip`, `Toast`, `Dialogos`, `EmptyState`, `LoadingOverlay` | Filtros e feedback. As notificações (toasts) são escuras nos dois temas. |
| `PaletaComandos` | Busca global (Ctrl+K): telas, ações, visões salvas, algoritmos e municípios, com busca aproximada por subsequência |
| `Movimento` | Entrada escalonada dos cartões e contagem animada dos números (KPIs e manchete) |
| `Logo` | Marca vetorial: chama em gradiente térmico com três barras de gráfico (menu, splash, ícone e Sobre) |
| `ArvoreVisual` | Animação da árvore AVL com as rotações destacadas |

**Estados dos controles:** hover, pressionado (escala 0,97), foco visível (anel brasa), desabilitado e selecionado (fundo em tinta cheia).

**Tabelas:** fios horizontais em vez de listras zebradas, números alinhados à direita e cabeçalho com fio grosso.

## 6. Movimento

Segui a regra de animar pouco e com propósito (Emil Kowalski): um único momento marcante e nada em ações repetidas.

| Onde | O quê | Duração |
|---|---|---|
| Primeira abertura da Visão geral | O número da manchete conta de 0 até o valor real (curva de desaceleração forte) | 900 ms, uma vez por sessão |
| Faixa térmica | Os meses surgem da esquerda para a direita | 220 ms por mês, 18 ms de defasagem |
| Troca de tela | Esmaecimento | 200 ms |
| Primeira visita a cada tela | Cartões sobem 12 px e aparecem em sequência | 300 ms, 45 ms de defasagem |
| Mudança de um KPI | O número conta do valor antigo ao novo | 450 ms |
| Botões e itens do menu | Encolhem 3% ao pressionar | Imediato |

Todas as animações podem ser desligadas em Configurações. Nada anima em atalhos de teclado.

## 7. Acessibilidade e atalhos

- **Contraste:** nível AA nos dois temas (tabela 2.1). A identidade dos dados nunca depende só da cor: há legenda, rótulo direto ou tooltip.
- **Alternativa ao gráfico:** todo gráfico tem "Ver dados em tabela".
- **Teclado:**

| Atalho | Ação |
|---|---|
| Ctrl+K | Paleta de comandos |
| Ctrl+1…7 | Telas |
| F5 | Modo apresentação (setas navegam, Esc sai) |
| Ctrl+O | Abrir CSV |
| Ctrl+R | Recarregar |
| Ctrl+E | Exportar |
| Ctrl+T | Tema |
| Ctrl+B | Menu |
| F1 | Sobre |

- **Faixa térmica pelo teclado:** setas movem o cursor entre os meses, Enter seleciona e o leitor de tela anuncia mês e contagem.
- **Cores para daltonismo** (Configurações): troca verde, amarelo e vermelho pela paleta Okabe-Ito (azul, laranja, verde-azulado, rosa), na interface e no mapa. As cores foram validadas simulando protanopia, deuteranopia e tritanopia.
- **Alto contraste:** fundo puro, textos secundários com a força do principal e bordas visíveis.
- **Tamanho do texto:** Normal, Grande (115%) ou Maior (130%). O programa gera cópias das folhas de estilo com todas as fontes ampliadas, então nenhuma tela fica de fora.
- **Teste automático de cores (`CoresAcessiveisTest`):** em cada combinação de tema, modo daltônico e alto contraste, confere o contraste WCAG AA do texto (≥ 4,5:1), o contraste das cores de dados com o fundo (≥ 3:1) e a separação perceptual (ΔE em OKLab) entre anos, biomas, estados e séries nas três simulações de daltonismo. O teste encontrou um problema real: no tema claro, o verde da Mata Atlântica e o dourado do Cerrado eram quase iguais para quem tem protanopia; o dourado foi escurecido para `#A67C00`.
- **Preferências:** tema, animações, menu recolhido, densidade compacta, acessibilidade, visões salvas e o tour já visto são persistidos com `java.util.prefs.Preferences`.

## 7.1 Experiências de produto

- **Paleta de comandos (Ctrl+K):** encontra qualquer tela, ação, algoritmo ou município digitando parte do nome; o trecho exato pontua mais que a subsequência e o início de palavra ganha bônus.
- **Modo apresentação (F5):** sete telas em tela cheia com texto maior e um roteiro por slide (lido de `grupo.properties`, chaves `apresentacao.*`), terminando num slide de conclusão com a equipe.
- **Tour guiado:** cinco passos na primeira abertura (menu, faixa térmica, filtros, exportação e atalhos); pode ser pulado e não volta a aparecer.
- **Visões salvas:** a combinação atual de filtros vira uma visão com nome, reaplicada pelo menu "Visões" ou pela paleta.
- **Comparar dois períodos:** na Visão geral, escolhem-se dois meses (padrão agosto de 2023 × agosto de 2024) e a tela mostra focos, municípios, dias e líderes lado a lado, com a variação.
- **Pequenos múltiplos por bioma:** um gráfico por bioma com o mesmo eixo vertical, para comparar picos sem distorção.
- **Escala linear ou log** no gráfico mensal: em log os meses calmos e o pico ficam legíveis juntos.
- **Densidade compacta** (Configurações): reduz espaçamentos para telas pequenas.
- **Passo a passo com pseudocódigo:** na visualização da ordenação, o pseudocódigo do algoritmo fica ao lado das barras e a linha da operação atual (comparação, troca ou escrita) é destacada; botões avançam e voltam um passo, e uma frase descreve a operação ("Compara a[3] = 17 com a[4] = 9").
- **Seis temporadas e São Paulo no Brasil:** a Visão geral mostra a série mensal de 2019 a 2024 e o ranking dos estados no ano escolhido (ordenado pelo Merge Sort do projeto), com a participação de SP no total nacional.
- **Mapa-base à escolha:** um painel com miniaturas reais de sete estilos (Neutro, Ruas, Relevo, Terreno, Satélite, Satélite com nomes e Atlas, todos da Esri e cobrindo o mundo inteiro). A escolha é salva; em fundos coloridos os pontos ganham contorno branco e os hotspots viram amarelo-claro sobre imagem de satélite. Abre em 180 ms com desaceleração forte, fecha com Esc ou clique fora e funciona pelo teclado.
- **Menu lateral no tema claro:** mesma cor de fundo da página, com texto escuro; no tema escuro continua carvão.
- **Estruturas aplicadas:** autocompletar com Trie, focos num raio com árvore k-d (com botão que desenha o círculo no mapa) e vizinhança por grafo com busca em largura.

---

## 8. Texto para a dissertação — "Projeto do programa: interface"

> Adapte à formatação ABNT/UNIP.

**8.1 Conceito.** A interface foi projetada como uma reportagem especial de dados sobre a temporada de fogo em São Paulo. A escolha se apoia no público principal: a apresentação à banca, em projetor, exige leitura rápida e à distância. Por isso cada tela abre com a informação mais importante em grande escala. Na Visão geral, é uma frase-manchete gerada dos próprios dados: "Em 2024, São Paulo registrou 8.712 focos de incêndio, 5,2 vezes o total de 2023".

**8.2 Hierarquia e redução de escolhas.** A primeira versão exibia oito indicadores de mesmo peso. A Lei de Hick (HICK, 1952) indica que o tempo de decisão cresce com o número de alternativas equivalentes. A versão final hierarquiza a informação em três camadas:

- um número principal;
- cinco destaques em linha;
- as seções de análise, separadas por fios tipográficos.

Cada tela tem uma única ação primária ("Exportar" ou "Executar").

**8.3 Cor com função.** Três escalas, cada uma com um papel:

- **Categórica** para entidades (anos, biomas, algoritmos).
- **Sequencial "inferno"** para intensidade. É perceptualmente uniforme e distinguível por daltônicos, e se parece com a imagem de uma câmera térmica.
- **Semântica** para estados (sucesso, alerta, erro).

O ano recente recebe a cor mais quente e o anterior, uma cor fria. Isso reforça a leitura do aumento de focos sem depender de texto.

**8.4 Assinatura visual: a faixa térmica.** No topo de todas as telas, uma faixa mostra os 24 meses da base, um retângulo por mês, coloridos pela quantidade de focos. Ela resume a temporada inteira (o pico de agosto a outubro de 2024 salta aos olhos) e serve de controle: clicar em um mês filtra o período em todas as análises. É um exemplo de visualização que também é navegação.

**8.5 Correções guiadas por dados.**

- **Pizza → donut.** O gráfico de pizza foi substituído por um donut com legenda numérica, porque comprimentos e rótulos diretos são percebidos com mais precisão do que ângulos (CLEVELAND; McGILL, 1984).
- **Benchmark em pequenos múltiplos.** O benchmark foi dividido em dois gráficos (O(n²) e O(n log n)), cada um com a curva teórica tracejada.
- **Horário das detecções.** O gráfico de horas mostra que as detecções se concentram entre 13h e 15h, porque o satélite de referência passa sobre São Paulo no início da tarde. A interface explica essa característica do sensor em vez de escondê-la.

**8.6 Acessibilidade.**

- Todas as combinações de texto atendem ao nível AA das WCAG 2.1 (W3C, 2018).
- A informação nunca depende apenas da cor.
- Todo gráfico oferece a alternativa "ver dados em tabela".
- Há navegação por teclado com foco visível, e as animações podem ser desligadas.

**8.7 Arquitetura da interface.**

- **MVC:** as telas são arquivos FXML (visão) com *controllers* dedicados.
- **Componentes:** são classes Java reutilizáveis.
- **Estilos:** ficam em folhas CSS organizadas em camadas (tokens → tema → base → componentes → gráficos). Trocar o tema é substituir uma única folha.
- **Tarefas longas:** rodam em *threads* separadas (`javafx.concurrent.Task`), com progresso e cancelamento.

**8.8 Experiência de uso.** Para a apresentação e para o uso diário, a interface ganhou recursos de produtos profissionais:

- **Paleta de comandos (Ctrl+K):** qualquer tela, ação, algoritmo ou município é encontrado digitando parte do nome, com busca aproximada.
- **Modo apresentação (F5):** as telas viram slides em tela cheia com texto maior, na ordem do roteiro da banca.
- **Tour guiado:** cinco passos na primeira abertura explicam menu, faixa térmica, filtros, exportação e atalhos.
- **Visões salvas e comparação de períodos:** combinações de filtros ganham nome, e dois meses podem ser comparados lado a lado com a variação.
- **Pequenos múltiplos por bioma** com o mesmo eixo vertical (TUFTE, 2001) e alternância entre escala linear e logarítmica.
- **Movimento com propósito:** só a primeira visita a cada tela anima a entrada dos cartões e a contagem dos números; nada anima em ações repetidas.

**8.9 Relatórios.** O PDF segue a mesma identidade: capa com o número principal e a faixa térmica, sumário com as páginas reais, resumo executivo escrito a partir dos dados e gráficos desenhados em vetor (nítidos na impressão). O Excel traz cabeçalho congelado, filtros, formatação condicional, gráficos nativos editáveis e uma aba "Sobre os dados".

**Referências sugeridas:**

- CLEVELAND, W. S.; McGILL, R. Graphical perception: theory, experimentation, and application to the development of graphical methods. *Journal of the American Statistical Association*, v. 79, n. 387, p. 531–554, 1984.
- HICK, W. E. On the rate of gain of information. *Quarterly Journal of Experimental Psychology*, v. 4, n. 1, p. 11–26, 1952.
- SMITH, N.; VAN DER WALT, S. *A better default colormap for Matplotlib* (viridis, inferno). SciPy Conference, 2015.
- W3C. *Web Content Accessibility Guidelines (WCAG) 2.1*. 2018. Disponível em: https://www.w3.org/TR/WCAG21/.
- TUFTE, E. R. *The Visual Display of Quantitative Information*. 2. ed. Cheshire: Graphics Press, 2001.

## 9. Prints recomendados para a dissertação

| Arquivo (`docs/prints/`) | Legenda sugerida |
|---|---|
| `completa/10-visao-geral-escuro.png` | Visão geral: manchete de dados, destaques, temporada mês a mês, municípios, biomas, pequenos múltiplos, comparação de períodos, calendário e horário |
| `01-visao-geral-claro.png` | A mesma tela no tema claro |
| `19-visao-geral-filtro-cerrado-2024.png` | A manchete se reescreve com o filtro (Cerrado, 2024) |
| `11-ordenacao-escuro.png` | Ordenação multicritério com o placar de operações |
| `20-ordenacao-comparativo.png` · `21-ordenacao-visualizacao.png` | Comparativo dos algoritmos e visualização animada |
| `completa/12-estruturas-escuro.png` | Estruturas e busca: árvore AVL animada e custo das consultas |
| `completa/13-benchmark-escuro.png` | Benchmark em escala log-log |
| `14-mapa-escuro.png` · `22-mapa-calor.png` · `23-mapa-agrupado.png` | Mapa em tela cheia: pontos por bioma, densidade e agrupamento com hotspots |
| `24-mapa-municipios.png` | Mapa coroplético por densidade de focos (quebras de Jenks) |
| `completa/15-ml-escuro.png` | Machine Learning: validação em janelas, ablação, permutação e estudo de agosto/2024 |
| `completa/16-qualidade-escuro.png` | Qualidade dos dados |
