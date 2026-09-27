# 0009. Gráficos vetoriais nos relatórios

**Situação:** aceita

## Contexto
Os relatórios em PDF usavam capturas de tela dos gráficos. Pelo console não havia gráfico algum, e as capturas ficavam borradas na impressão.

## Decisão
O PDF desenha os gráficos principais (temporada mensal, Top 10, curvas do benchmark, real × previsto) direto no documento, em vetor, com rótulos diretos que não se sobrepõem. As capturas do painel continuam como anexo, em escala 2×. O sumário é montado em duas passadas para mostrar as páginas reais de cada seção.

## Consequências
- Relatórios nítidos em qualquer zoom e iguais pelo console ou pela interface.
- O Excel usa gráficos nativos (XDDF), editáveis pelo usuário.
