# Changelog

Todas as mudanças relevantes do projeto. Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/); versões seguem [SemVer](https://semver.org/lang/pt-BR/).

## [2.0.0] - 2026-09-27

### Adicionado
- **Modo offline:** Leaflet e a malha municipal do IBGE embarcados, CSVs do INPE e resultados do benchmark dentro do JAR; o mapa troca sozinho para a malha local quando os tiles não carregam.
- **Estruturas de dados feitas à mão:** busca binária (lower/upper bound), árvore AVL com rotações animadas, tabela hash (FNV-1a, encadeamento), heap binário e Top-K, External Merge Sort com intercalação k-way e Merge Sort paralelo (Fork/Join). Nova tela **Estruturas & Busca** e experimentos com 6 anos de focos do Brasil.
- **ML mais forte:** histórico 2019–2024, vizinhos pela malha do IBGE, meteorologia do INPE, validação em janelas (rolling origin), ablação, intervalo conforme e importância por permutação; estudo de caso de agosto/2024.
- **Rigor analítico:** mapa coroplético por densidade (focos/1.000 km²) com quebras naturais de Jenks e quantis; aviso do viés do sensor; validação do benchmark com JMH.
- **Experiência:** paleta de comandos (Ctrl+K), modo apresentação (F5), tour guiado, visões salvas, comparação de dois períodos, pequenos múltiplos por bioma, escala log, densidade compacta, movimento com propósito e logo vetorial.
- **Relatórios corporativos:** PDF com capa, sumário com páginas reais, resumo executivo gerado dos dados, gráficos vetoriais e notas metodológicas; Excel com cabeçalho congelado, filtros, formatação condicional, gráficos nativos e aba "Sobre os dados".
- **Qualidade:** Checkstyle, PMD, SpotBugs e cobertura mínima (JaCoCo) no `verify`; teste de mutação (PIT) no perfil `mutacao`; ADRs em `docs/adr`.
- **Distribuição:** workflow de release que gera instalador `.deb` (Linux), `.dmg` (macOS) e versão portátil para Windows com runtime próprio (jpackage + jlink), além do JAR executável.

### Alterado
- Visual editorial "Boletim de Fogo": tipografia Big Shoulders + Inter, escala térmica inferno, faixa térmica de 24 meses e seções com fios.
- Benchmark em escala log-log e leitura do resultado embarcado quando não há execução local.

### Corrigido
- Legendas e títulos sobrepostos no mapa, rótulos truncados no ML, capturas de tela com fundo transparente e contraste de cores no tema claro.

## [1.0.0] - 2026-09-26

### Adicionado
- Dez algoritmos de ordenação implementados à mão, com contagem de comparações, trocas, atribuições e acessos por um vetor instrumentado.
- Leitura dos CSVs do INPE com validação linha a linha e relatório de carga.
- Benchmark com aquecimento do JIT, expoente empírico (regressão log-log) e exportação CSV, Excel e PDF.
- Random Forest, classificação do nível de atividade, K-Means e DBSCAN.
- Dashboard JavaFX, menu de console e relatório com as linhas de código.

[2.0.0]: https://github.com/gustavoblopes79/aps-queimadas/releases/tag/v2.0.0
[1.0.0]: https://github.com/gustavoblopes79/aps-queimadas/commits/a154b66
