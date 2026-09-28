# Manual do usuário

O APS Queimadas mostra os focos de incêndio que o INPE detectou em São Paulo em 2023 e 2024 e compara algoritmos de ordenação e estruturas de dados sobre esses dados. Este manual explica cada tela e como tirar proveito dela.

## Instalar e abrir

- **Windows:** baixe o arquivo `APS-Queimadas-2.2.0-windows-portatil.zip` na página de versões do projeto, descompacte e abra `APS Queimadas.exe`. Não é preciso instalar o Java nem ter permissão de administrador.
- **Linux:** instale o pacote `.deb` com um duplo clique ou com `sudo apt install ./aps-queimadas_2.2.0_amd64.deb`.
- **macOS:** abra o `.dmg` e arraste o aplicativo para a pasta Aplicativos.
- **Qualquer sistema com Java 21:** `java -jar aps-queimadas-2.2.0-all.jar`.

O programa funciona sem internet: os dados do INPE, a malha dos municípios do IBGE e o mapa vêm junto. Com internet, o mapa usa um fundo mais detalhado.

## Primeiros passos

Na primeira abertura, um tour de cinco passos apresenta o menu, a faixa térmica, os filtros, a exportação e os atalhos. Ele pode ser pulado e não volta a aparecer.

![Visão geral](prints/10-visao-geral-escuro.png)

## A faixa térmica e os filtros

A faixa no topo de todas as telas tem um retângulo por mês; quanto mais quente a cor, mais focos. Clique num mês para filtrar todas as telas por ele; clique de novo para limpar.

- **Filtros:** bioma, ano, município (digite o começo do nome e aperte Enter) e período.
- **Visões salvas:** no botão Visões, guarde a combinação atual de filtros com um nome e reabra quando quiser.
- **Limpar:** o botão Limpar volta a mostrar a base inteira.

## Visão geral

Abre com a frase-manchete do período e os destaques: municípios afetados, mês de pico, município líder, bioma e horário das detecções. Abaixo vêm a temporada mês a mês (com escala linear ou logarítmica), os municípios com mais focos, os biomas, um gráfico por bioma, a comparação de dois meses, as seis temporadas de 2019 a 2024, a posição de São Paulo entre os estados, o calendário dia a dia e o horário das detecções.

Todo gráfico tem o menu de três pontos com **Exportar PNG** e **Ver dados em tabela**.

## Ordenação

Escolha o critério (por exemplo, bioma, depois município, depois data), o algoritmo, o tamanho da amostra e o cenário (ordem do arquivo, aleatória, já ordenada, inversa ou quase ordenada) e clique em **Ordenar e exibir**. O placar mostra comparações, trocas, atribuições, acessos e tempo, e a tabela mostra os dados ordenados.

- **Comparar todos os algoritmos:** roda todos os algoritmos compatíveis com o critério sobre a mesma entrada (Radix e Counting só aceitam critérios numéricos).
- **Visualização animada:** anima a ordenação de 32 valores. Os botões de passo avançam e voltam uma operação por vez, e o pseudocódigo ao lado destaca a linha que está sendo executada.
- Algoritmos O(n²) com amostras grandes pedem confirmação antes de rodar, porque podem levar segundos.

![Ordenação](prints/11-ordenacao-escuro.png)

## Estruturas e busca

Compara quatro formas de responder a mesma pergunta (busca sequencial, ordenar e fazer busca binária, árvore AVL e tabela hash) contando as comparações de cada uma. Também mostra:

- a árvore AVL sendo montada, com as rotações;
- o autocompletar com uma **Trie**;
- os focos num raio com uma **árvore k-d**; o botão **Ver no mapa** desenha o círculo no Mapa;
- os municípios vizinhos num **grafo** e a pergunta "o fogo pula para o vizinho?";
- Top K com heap, memória de cada algoritmo, ordenação externa e Merge Sort paralelo.

## Benchmark

Mede os algoritmos com tamanhos crescentes e três cenários. Os gráficos usam escala log-log: a inclinação da reta é o expoente do custo. Sem uma execução local, a tela mostra o resultado gravado junto com o programa.

## Mapa

Mostra os focos como pontos, agrupados, em mapa de calor ou por município (coroplético por densidade, com quebras de Jenks ou quantis). O botão **Hotspots do ML** sobrepõe os agrupamentos encontrados pelo DBSCAN.

O botão **Mapa-base** abre miniaturas dos estilos de fundo: Neutro (acompanha o tema), Ruas e cidades, Relevo, Terreno, Satélite, Satélite com nomes e Atlas. A escolha fica salva. Nos fundos coloridos, os focos ganham contorno branco para continuar visíveis. Sem internet, só o Neutro fica disponível e o programa desenha os países (Natural Earth) e os municípios de SP (IBGE).

![Mapa-base](prints/25-mapa-base-seletor.png)

### Linha do tempo

A barra no canto inferior esquerdo mostra os focos de cada mês numa faixa de calor. **Tocar** passa pelos meses em sequência, do primeiro ao último, e o mapa, a contagem e a legenda acompanham. Clique num mês para vê-lo sozinho; Shift+clique escolhe um intervalo. **Todos os meses** volta ao período inteiro. O pico de agosto de 2024 aparece logo na faixa.

![Agosto de 2024](prints/28-mapa-agosto-2024.png)

### Ficha do município

Clique num foco e depois em **Ficha do município**, ou clique num município no modo Municípios. A ficha mostra o total de focos, a posição no ranking do estado, os anos lado a lado, o bioma principal, a densidade, os focos por mês e os vizinhos que também tiveram focos. **Ver no mapa** enquadra o município. Pela paleta (Ctrl+K), digite "ficha de" e o nome da cidade.

![Ficha do município](prints/29-mapa-ficha-municipio.png)

### Exportar o mapa

O botão **PNG** salva o mapa como imagem com o dobro dos pixels da tela, sem os controles, pronta para o relatório ou a dissertação.

### Comparar municípios

No fim da ficha, digite outro município em **Comparar** e tecle Enter. O painel mostra os dois lado a lado, com os focos mês a mês na mesma escala e uma frase de resumo ("Andradina teve 1,9 vezes os focos de Pereira Barreto. Os dois fazem fronteira."). No mapa, o primeiro fica em laranja e o segundo em azul.

![Comparação](prints/30-mapa-comparacao.png)

### Gravar a linha do tempo em GIF

O botão **GIF**, na linha do tempo, passa pelos 24 meses, fotografa o mapa em cada um e salva um GIF animado. Leva uns 15 segundos; o botão mostra o andamento.

## Versão web (celular)

Em <https://gustavoblopes79.github.io/aps-queimadas/app/> há uma versão para o celular: mapa com a linha do tempo, ranking dos municípios com busca, ficha, comparação e o custo real dos 13 algoritmos. No menu do navegador, "Instalar app" ou "Adicionar à tela inicial" a deixa como um aplicativo, que abre até sem internet.

Os números da versão web não são recalculados no celular: o programa em Java gera tudo com `java -jar aps-queimadas-2.2.0-all.jar web`, que ordena os focos com os 13 algoritmos, conta as operações e grava os resultados em `site/app/dados`.

## Machine Learning

Prevê focos por município e mês com Random Forest e compara com previsões ingênuas. A tela mostra a validação em janelas de tempo, o efeito de cada grupo de variáveis e o estudo de agosto de 2024.

## Relatórios

O botão **Exportar** (Ctrl+E) gera um PDF com capa, sumário, resumo executivo, gráficos e notas de método, ou uma planilha Excel com gráficos e uma aba explicando os dados.

## Paleta de comandos e atalhos

Aperte **Ctrl+K** e digite parte do que procura: uma tela, uma ação, um algoritmo ou um município. Enter executa.

- **Ctrl+1 a Ctrl+7:** telas principais.
- **F5:** modo apresentação (setas navegam, Esc sai).
- **Ctrl+O:** abrir CSV. **Ctrl+R:** recarregar. **Ctrl+E:** exportar.
- **Ctrl+T:** tema claro ou escuro. **Ctrl+B:** recolher o menu. **F1:** sobre.

## Acessibilidade

Em **Configurações > Acessibilidade**:

- **Cores para daltonismo:** troca verde, amarelo e vermelho por azul e laranja, distinguíveis em protanopia, deuteranopia e tritanopia. Vale também para o mapa.
- **Alto contraste:** fundo puro e textos secundários com a mesma força do principal.
- **Tamanho do texto:** Normal, Grande (115%) ou Maior (130%).

Todo o programa pode ser usado pelo teclado: Tab percorre os controles com foco visível.

## Modo apresentação

F5 abre as telas em tela cheia, com texto maior e o roteiro de cada etapa. Os nomes de quem apresenta cada parte vêm do arquivo `grupo.properties`.

## Linha de comando

`java -jar aps-queimadas-2.2.0-all.jar ajuda` lista os modos. Os mais usados são `cli` (menu no terminal), `ordenar`, `comparar`, `benchmark`, `resultados` (gera os números da dissertação) e `estruturas`.

## Problemas comuns

- **O mapa aparece sem o fundo detalhado:** o programa está sem internet e usou a malha local. Os dados continuam corretos.
- **"Arquivo aberto em outro programa" ao exportar:** feche o Excel ou o leitor de PDF e tente de novo.
- **A ordenação O(n²) demora:** reduza a amostra ou use um algoritmo O(n log n). O botão Cancelar interrompe a tarefa.
- **Linhas rejeitadas na carga:** veja o motivo na tela Qualidade dos dados.
