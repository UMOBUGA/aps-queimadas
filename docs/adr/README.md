# Decisões de arquitetura (ADRs)

Cada registro explica o contexto, a decisão e as consequências de uma escolha estrutural. Formato de Michael Nygard.

| # | Decisão | Situação |
|---|---|---|
| [0001](0001-java-21-javafx.md) | Java 21 + JavaFX como plataforma única (desktop, console e relatórios) | Aceita |
| [0002](0002-contagem-por-vetor-instrumentado.md) | Contar operações por um vetor instrumentado, não por contadores espalhados | Aceita |
| [0003](0003-sem-ordenacao-pronta.md) | Nenhuma ordenação pronta em produção, garantida por teste de arquitetura | Aceita |
| [0004](0004-modo-offline-embarcado.md) | Dados, malha do IBGE e Leaflet embarcados para funcionar sem internet | Aceita |
| [0005](0005-estruturas-a-mao.md) | Estruturas de dados próprias (AVL, hash, heap) instrumentadas | Aceita |
| [0006](0006-validacao-temporal-ml.md) | Validação temporal e baselines ingênuos no ML | Aceita |
| [0007](0007-benchmark-proprio-e-jmh.md) | Benchmark próprio para a contagem, JMH para validar o tempo | Aceita |
| [0008](0008-design-system-por-tokens.md) | Design system por tokens CSS e componentes reutilizáveis | Aceita |
| [0009](0009-relatorios-vetoriais.md) | Gráficos dos relatórios desenhados em vetor no próprio PDF | Aceita |
