# 0008. Design system por tokens CSS

**Situação:** aceita

## Contexto
Com oito telas, dois temas e dezenas de gráficos, cores definidas no código Java divergiam entre telas e quebravam o tema escuro.

## Decisão
Todas as cores vêm de tokens `-aps-*` em `ui/css/tokens.css`, com valores por tema. O código Java só aplica classes CSS (por exemplo, `Graficos.classe(serie, "serie-ano-recente")`). Componentes reutilizáveis (KpiCard, ChartCard, FaixaTermica, PaletaComandos) ficam em `ui.componentes`. A escala térmica "inferno" codifica magnitude; biomas e anos têm cor fixa por entidade.

## Consequências
- Trocar de tema não exige código.
- `setStyle` com cor no Java é considerado defeito de revisão.
- A documentação visual fica em `docs/DESIGN.md`.
