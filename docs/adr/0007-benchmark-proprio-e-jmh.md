# 0007. Benchmark próprio para contar, JMH para validar o tempo

**Situação:** aceita

## Contexto
O benchmark próprio mede tempo e operações sobre o vetor instrumentado, o que é necessário para a análise. Mas medir tempo em Java sem cuidado (aquecimento do JIT, eliminação de código morto) distorce os números.

## Decisão
O benchmark próprio continua sendo a fonte das contagens e do expoente empírico, com aquecimento por tempo antes de cada medição. Um perfil Maven `jmh` mede os mesmos algoritmos com o JMH, e a comparação fica em `docs/resultados/jmh.md`.

## Consequências
- A ordem dos algoritmos coincide entre as duas medições; as diferenças absolutas de tempo ficam documentadas em `docs/BENCHMARK.md`.
- O JMH não entra no build padrão para não alongar o CI.
