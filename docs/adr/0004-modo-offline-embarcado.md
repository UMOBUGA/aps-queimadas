# 0004. Funcionar sem internet

**Situação:** aceita

## Contexto
A apresentação acontece num laboratório com rede incerta. O mapa dependia de tiles e scripts de CDN, e os dados precisavam ser baixados do INPE.

## Decisão
Leaflet e markercluster são servidos de `ui/web`; a malha municipal do IBGE (GeoJSON com área e centróide) e os CSVs de 2023 e 2024 vão dentro do JAR, assim como o resultado do benchmark e do estudo de ML. Se os tiles não carregam em 6 s, o mapa troca sozinho para a malha local.

## Consequências
- O JAR cresce (cerca de 70 MB com JavaFX), mas roda em qualquer máquina com Java 21.
- Os dados embarcados são uma cópia datada; o botão "Baixar do INPE" atualiza quando há rede.
