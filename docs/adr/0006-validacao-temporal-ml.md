# 0006. Validação temporal e baselines no ML

**Situação:** aceita

## Contexto
Focos de queimada têm forte sazonalidade e autocorrelação. Embaralhar os dados e validar com k-fold daria números otimistas, porque o modelo veria o futuro.

## Decisão
O modelo treina em anos anteriores e testa no ano seguinte (origem móvel), sem embaralhar. Toda métrica é comparada com baselines ingênuos: persistência (mês anterior), média histórica e classe majoritária. O estudo inclui ablação de grupos de variáveis, importância por permutação e intervalo conforme.

## Consequências
- Os ganhos reportados são menores, porém honestos; agosto de 2024 aparece como fora da distribuição.
- Os resultados ficam em `docs/resultados/ml-estudo.md`, gerados pelo modo `ml-estudo` com semente fixa.
