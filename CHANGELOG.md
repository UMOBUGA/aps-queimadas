# Changelog

Todas as mudanças relevantes do projeto. Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/); versões seguem [SemVer](https://semver.org/lang/pt-BR/).

## [2.2.0] - 2026-09-28

### Adicionado
- **Linha do tempo no mapa:** faixa térmica dos meses com botão Tocar, que mostra os focos mês a mês; clique escolhe um mês e Shift+clique, um intervalo. A contagem, a legenda e o mapa por município acompanham o período.
- **Ficha do município:** total, posição no ranking do estado, comparação entre os anos, bioma principal, densidade, focos por mês e vizinhos com focos (malha do IBGE). Abre pelo balão do foco, pelo clique no município ou pela paleta ("ficha de…").
- **Mapa-múndi offline:** países do Natural Earth (domínio público) embarcados; sem internet, o mundo aparece sob a malha dos municípios.
- **Exportar o mapa em PNG** com o dobro da resolução da tela e sem os controles.

### Alterado
- GitHub Actions atualizadas para as versões com Node 24.

## [2.1.0] - 2026-09-28

### Adicionado
- **Mapa-base à escolha** com miniaturas: Neutro, Ruas e cidades, Relevo, Terreno, Satélite, Satélite com nomes e Atlas (Esri), com escolha salva e contorno branco nos focos em fundos coloridos. Os estilos extras são carregados da internet; sem conexão, o mapa usa o Neutro com a malha dos municípios do IBGE.

### Alterado
- Menu lateral no tema claro com a mesma cor da página e texto escuro.

## [2.0.0] - 2026-09-27

### Adicionado
- **Modo offline:** Leaflet e a malha municipal do IBGE embarcados, CSVs do INPE e resultados do benchmark dentro do JAR; o mapa troca sozinho para a malha local quando os tiles não carregam.
- **Estruturas de dados feitas à mão:** busca binária (lower/upper bound), árvore AVL com rotações animadas, tabela hash (FNV-1a, encadeamento), heap binário e Top-K, External Merge Sort com intercalação k-way e Merge Sort paralelo (Fork/Join). Nova tela **Estruturas & Busca** e experimentos com 6 anos de focos do Brasil.
- **ML mais forte:** histórico 2019–2024, vizinhos pela malha do IBGE, meteorologia do INPE, validação em janelas (rolling origin), ablação, intervalo conforme e importância por permutação; estudo de caso de agosto/2024.
- **Rigor analítico:** mapa coroplético por densidade (focos/1.000 km²) com quebras naturais de Jenks e quantis; aviso do viés do sensor; validação do benchmark com JMH.
- **Experiência:** paleta de comandos (Ctrl+K), modo apresentação (F5), tour guiado, visões salvas, comparação de dois períodos, pequenos múltiplos por bioma, escala log, densidade compacta, movimento com propósito e logo vetorial.
- **Relatórios corporativos:** PDF com capa, sumário com páginas reais, resumo executivo gerado dos dados, gráficos vetoriais e notas metodológicas; Excel com cabeçalho congelado, filtros, formatação condicional, gráficos nativos e aba "Sobre os dados".
- **Qualidade:** Checkstyle, PMD, SpotBugs e cobertura mínima (JaCoCo) no `verify`; teste de mutação (PIT) no perfil `mutacao`; ADRs em `docs/adr`.
- **Mais algoritmos:** Intro Sort, Quick Sort com dois pivôs e Counting Sort (com o novo critério Hora local), totalizando 13.
- **Passo a passo com pseudocódigo** na visualização da ordenação, com avanço e retorno de um passo.
- **Estruturas aplicadas:** Trie (sugestões da busca de municípios e tela Estruturas), árvore k-d (focos num raio, com círculo no mapa) e grafo de municípios vizinhos com busca em largura (análise de propagação).
- **Referência do Java no JMH:** `Arrays.sort` comparado aos nossos algoritmos em tempo e em número de comparações.
- **Histórico e Brasil:** série de 2019 a 2024 e ranking dos estados na Visão geral (modo `agregados`).
- **Acessibilidade:** modo daltônico (Okabe-Ito), alto contraste, tamanho do texto e teste automático de cores.
- **Testes de interação** com TestFX numa tela virtual (Monocle).
- **Manual do usuário** em Markdown e PDF (modo `manual`) e GIF de demonstração.
- **Distribuição:** workflow de release que gera instalador `.deb` (Linux), `.dmg` (macOS) e versão portátil para Windows com runtime próprio (jpackage + jlink), além do JAR executável.

### Alterado
- Visual editorial "Boletim de Fogo": tipografia Big Shoulders + Inter, escala térmica inferno, faixa térmica de 24 meses e seções com fios.
- Benchmark em escala log-log e leitura do resultado embarcado quando não há execução local.

### Corrigido
- Legendas e títulos sobrepostos no mapa, rótulos truncados no ML, capturas de tela com fundo transparente e contraste de cores no tema claro.
- Dourado do Cerrado no tema claro, que se confundia com o verde da Mata Atlântica para pessoas com protanopia.
- Aquecimento do benchmark por tempo mínimo (500 ms): os tempos ficaram muito mais próximos dos medidos pelo JMH.

## [1.0.0] - 2026-09-26

### Adicionado
- Dez algoritmos de ordenação implementados à mão, com contagem de comparações, trocas, atribuições e acessos por um vetor instrumentado.
- Leitura dos CSVs do INPE com validação linha a linha e relatório de carga.
- Benchmark com aquecimento do JIT, expoente empírico (regressão log-log) e exportação CSV, Excel e PDF.
- Random Forest, classificação do nível de atividade, K-Means e DBSCAN.
- Dashboard JavaFX, menu de console e relatório com as linhas de código.

[2.2.0]: https://github.com/gustavoblopes79/aps-queimadas/releases/tag/v2.2.0
[2.1.0]: https://github.com/gustavoblopes79/aps-queimadas/releases/tag/v2.1.0
[2.0.0]: https://github.com/gustavoblopes79/aps-queimadas/releases/tag/v2.0.0
[1.0.0]: https://github.com/gustavoblopes79/aps-queimadas/commits/a154b66
