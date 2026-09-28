# Perguntas prováveis da banca e onde está a resposta

Roteiro de estudo para a apresentação: a pergunta, a resposta curta e o arquivo que a comprova. Os números vêm de [resultados/](resultados/) e da [tabela final](#tabela-final-de-números).

## Ordenação

| Pergunta | Resposta curta | Onde mostrar |
|---|---|---|
| Vocês usaram alguma ordenação pronta? | Não. O build falha se `Collections.sort`, `Arrays.sort`, `List.sort`, `sorted()`, `TreeMap`, `TreeSet` ou `PriorityQueue` aparecerem em produção. Até o sistema, quando precisa ordenar, chama o nosso Merge Sort. | `src/test/java/br/unip/aps/ArquiteturaTest.java` (`semOrdenacaoPronta`), `config/checkstyle.xml`, `sorting/Ordenacoes.java`, [ADR 0003](adr/0003-sem-ordenacao-pronta.md) |
| Como vocês contam as operações? | Os algoritmos só acessam os dados por um vetor instrumentado; `get`, `set`, `swap`, `less` e `compare` somam no contador. Não há como esquecer de contar. | `sorting/InstrumentedArray.java`, `sorting/OperationCounter.java`, [ADR 0002](adr/0002-contagem-por-vetor-instrumentado.md) |
| A contagem está certa? | Testes conferem as fórmulas teóricas exatas: Bubble com n−1 comparações no melhor caso e n(n−1)/2 no pior, Selection sempre n(n−1)/2. | `src/test/java/br/unip/aps/sorting/OperationCounterTest.java` |
| O dado original é alterado? | Não. A ordenação trabalha sobre uma cópia; a `BaseDeFocos` é imutável. | `sorting/ServicoOrdenacao.java`, `model/BaseDeFocos.java` |
| Como sabem que o resultado está ordenado? | Toda ordenação é verificada de forma independente, e os testes comparam cada algoritmo com `Collections.sort` (permitido só nos testes). | `ServicoOrdenacao.ordenar` (`Ordenacoes.estaOrdenada`), `SortAlgorithmsTest` |
| O que é estabilidade e quais são estáveis? | Elementos iguais mantêm a ordem relativa. Bubble, Insertion, Merge, Tim e Radix são estáveis; o teste exige que cada algoritmo se comporte como declara. | `SortAlgorithmsTest.estabilidade`, [ALGORITMOS.md](ALGORITMOS.md) |
| Por que o Quick Sort não degrada com dados ordenados? | Pivô pela mediana de três e partição de Hoare; há teste com 200 mil elementos já ordenados. | `sorting/algorithms/QuickSort.java`, `SortAlgorithmsTest` |
| Como ordenam por município com acento? | Comparação pelo `Collator` pt-BR (chave de colação pré-calculada), então "Álvares" fica junto de "Alvares". | `sorting/CriterioOrdenacao.java`, `util/Textos.java` |
| O Radix compara? | Não: faz 0 comparações e usa a chave numérica (data, latitude). Por isso não aparece em critérios de texto. | `sorting/algorithms/RadixSort.java` |
| Como ordenam por vários critérios? | Comparador composto (bioma → município → data), aplicado pelo mesmo algoritmo. | `sorting/Criterios.java` |
| Por que o Counting Sort não aparece na data? | Ele cria um vetor do tamanho do intervalo de chaves: a data em segundos tem cerca de 63 milhões de valores (250 MB). Na Hora local (24 valores) ele ordena os focos com zero comparações. O sistema só oferece o algoritmo onde ele cabe. | `sorting/algorithms/CountingSort.java` (`aceitaFaixa`) |
| Qual algoritmo o Java usa? E o C++? | `Arrays.sort` de objetos usa TimSort; para números, Quick Sort com dois pivôs. O `std::sort` do C++ usa Intro Sort. Implementamos os dois e comparamos com o Java no JMH. | `sorting/algorithms/DualPivotQuickSort.java`, `IntroSort.java`, [resultados/jmh.md](resultados/jmh.md) |
| Dá para ver o algoritmo linha a linha? | Sim: a visualização mostra o pseudocódigo e destaca a linha de cada comparação, troca ou escrita, com botões para avançar e voltar um passo. | `ui/componentes/SortVisualizer.java`, `Pseudocodigo.java` |

## Benchmark e complexidade

| Pergunta | Resposta curta | Onde mostrar |
|---|---|---|
| Como mediram o tempo na JVM? | Aquecimento do JIT (2 execuções e no mínimo 500 ms por caso), 5 repetições, média e desvio, entradas com semente fixa. | `benchmark/BenchmarkRunner.java`, [BENCHMARK.md](BENCHMARK.md) |
| Como provam que é O(n log n) ou O(n²)? | Regressão linear de log(custo) sobre log(n): a inclinação é o expoente k. Comparações dão k ≈ 2 nos quadráticos e ≈ 1,1 nos n log n. | `benchmark/AnaliseComplexidade.java`, aba Benchmark (escala log-log) |
| Os tempos são confiáveis? | Foram conferidos com o JMH, o padrão da indústria para microbenchmarks em Java. | `src/jmh/java/.../OrdenacaoJmh.java`, [resultados/jmh.md](resultados/jmh.md), [ADR 0007](adr/0007-benchmark-proprio-e-jmh.md) |
| Por que o Insertion ganha em dados quase ordenados? | Cada elemento anda pouco: o custo vira O(n + inversões). O cenário "ordenado" do benchmark mostra isso. | [resultados/benchmark.csv](resultados/benchmark.csv), cenário Ordenada |

## Estruturas de dados e busca

| Pergunta | Resposta curta | Onde mostrar |
|---|---|---|
| Quando vale a pena ordenar para buscar? | Depende do número de consultas: com 1 consulta a busca sequencial ganha; o ponto de equilíbrio da busca binária é por volta de 12 consultas. | [resultados/estruturas.md](resultados/estruturas.md), tela Estruturas & Busca |
| Por que AVL e não uma árvore binária simples? | A AVL se rebalanceia com rotações e garante altura O(log n); o teste confere o limite de 1,44·log₂(n+2). | `estruturas/ArvoreAVL.java` (`valida`), `EstruturasTest` |
| Como a tabela hash trata colisões? | Encadeamento, hash FNV-1a e redimensionamento com fator de carga 0,75. | `estruturas/TabelaHash.java` |
| E se os dados não couberem na memória? | External Merge Sort: ordena blocos, grava em disco e intercala com um heap (k-way). Testado com -Xmx64m. | `estruturas/ExternalMergeSort.java`, [resultados/estruturas-externo.md](resultados/estruturas-externo.md) |
| Para que serve uma Trie? | Buscar por prefixo examinando só as letras digitadas, sem comparar com todos os nomes. É ela que sugere municípios na busca. | `estruturas/Trie.java`, `ui/componentes/FilterBar.java` |
| Como acham os focos num raio sem calcular todas as distâncias? | Árvore k-d: corta o estado pela mediana, alternando latitude e longitude, e descarta regiões inteiras longe do centro. | `estruturas/ArvoreKD.java`, [resultados/estruturas.md §7](resultados/estruturas.md) |
| O fogo se espalha para o vizinho? | Montamos o grafo das fronteiras do IBGE: municípios com um vizinho em chamas tiveram focos no mês seguinte com frequência bem maior do que os sem vizinho em chamas. É associação, não causa. | `estruturas/Grafo.java`, `busca/ServicoGeografico.java` (`propagacao`) |
| Paralelizar ajuda? | Merge Sort com Fork/Join; o speedup e a fração serial (Karp–Flatt) estão medidos. | `estruturas/MergeSortParalelo.java`, [ESTRUTURAS.md](ESTRUTURAS.md) |

## Dados

| Pergunta | Resposta curta | Onde mostrar |
|---|---|---|
| De onde vêm os dados? | Programa Queimadas do INPE, satélite de referência, SP 2023 e 2024: 10.378 focos após limpeza. | `data/raw/`, [DADOS.md](DADOS.md) |
| Por que 2024 teve 5,2 vezes mais focos? | Agosto de 2024 concentrou 3.612 focos (41,5% do ano), 1,6 vez o maior mês do histórico de treino (agosto de 2021); os focos daquele mês tinham em média 31,4 dias sem chuva, contra 14,8 nos anos anteriores. O sensor é o mesmo nos dois anos, então a comparação é válida. | Visão geral, [resultados/ml-estudo.md §5](resultados/ml-estudo.md) |
| Esses números são o total de queimadas? | Não: é a contagem de um sensor. Serve para comparar tendências, não para medir área queimada. | nota da Visão geral, notas metodológicas do PDF |
| O que acontece com linhas inválidas? | São rejeitadas com o motivo e registradas, sem interromper a carga. | `io/CsvLoader.java`, `io/RelatorioCarga.java`, tela Qualidade dos dados |

## Machine Learning

| Pergunta | Resposta curta | Onde mostrar |
|---|---|---|
| O modelo funciona? | Com honestidade: em 2024 a Random Forest errou 1,36 foco por município e mês, contra 1,20 da previsão sazonal ingênua. Agosto de 2024 fugiu de todo o histórico. | [resultados/ml-estudo.md](resultados/ml-estudo.md), [ML.md](ML.md) |
| Por que não usaram k-fold? | Série temporal: embaralhar deixaria o modelo ver o futuro. Usamos validação em janelas no tempo. | `ml/EstudoPrevisao.java`, [ADR 0006](adr/0006-validacao-temporal-ml.md) |
| Onde entra a ordenação no ML? | A base é ordenada por município → data com o nosso Merge Sort e agregada numa única passada. | `ml/BaseMensal.java`, `ml/Preditor.java` |
| O que o DBSCAN encontrou? | 51 grupos de focos densos (raio de 10 km, mínimo 30 focos), com 3.451 focos isolados. | tela ML, mapa com "Hotspots do ML" |

## Engenharia

| Pergunta | Resposta curta | Onde mostrar |
|---|---|---|
| Como garantem a qualidade? | Testes, Checkstyle, PMD, SpotBugs e cobertura mínima rodam a cada commit; teste de mutação no perfil `mutacao`. | [QUALIDADE.md](QUALIDADE.md), `.github/workflows/ci.yml` |
| Quais padrões de projeto usaram? | Strategy, Factory, Decorator, Observer, Builder, MVC, Facade, Adapter, Command e Memento. | [ARQUITETURA.md §6](ARQUITETURA.md#6-padrões-de-projeto-para-a-dissertação) |
| O programa é acessível? | Tem modo daltônico, alto contraste, texto ampliado e navegação por teclado; um teste automático simula três tipos de daltonismo e confere o contraste das cores em todas as combinações. | `ui/tema/GerenciadorTema.java`, `CoresAcessiveisTest.java` |
| Testaram a interface de verdade? | Sim: o TestFX aperta teclas e clica numa tela virtual (Ctrl+K, atalhos, F5, filtro, acessibilidade), inclusive no CI. | `ui/InteracaoTest.java` |
| Roda sem internet? | Sim: dados, malha do IBGE, Leaflet e resultados vêm dentro do programa. | [ADR 0004](adr/0004-modo-offline-embarcado.md) |
| Como instalo? | Pacotes com Java embutido para Windows, Linux e macOS na página de releases. | `.github/workflows/release.yml` |

## Tabela final de números

Todos os valores são de execuções reais registradas em `docs/resultados` (máquina e versão do Java no cabeçalho de cada arquivo).

| Tema | Número | Fonte |
|---|---:|---|
| Focos de SP na base (2023 + 2024, após limpeza) | 10.378 | `data/raw`, [DADOS.md](DADOS.md) |
| Focos em 2024 × 2023 | 8.712 × 1.666 (5,2 vezes) | Visão geral |
| Pico do período | agosto de 2024: 3.612 focos (41,5% do ano) | Visão geral |
| Algoritmos de ordenação implementados à mão | 13 | [ALGORITMOS.md](ALGORITMOS.md) |
| Ordenar a base inteira (bioma → município → data) | Merge Sort: 122.682 comparações · Bubble Sort: 53.786.078 | [resultados/saida-console.txt](resultados/saida-console.txt) |
| Expoente empírico das comparações (cenário aleatório) | O(n²): k ≈ 2,0 · O(n log n): k = 1,02 a 1,18 | [BENCHMARK.md](BENCHMARK.md) |
| Medições do benchmark verificadas como ordenadas | 510 de 510 | [resultados/benchmark.csv](resultados/benchmark.csv) |
| Benchmark próprio × JMH | ordem dos algoritmos com Spearman 0,80; `Arrays.sort` do Java: 106.535 comparações | [resultados/jmh.md](resultados/jmh.md) |
| Encontrar um município (1.000 consultas) | sequencial: 10.378 comparações por consulta · hash: 1,0 | [resultados/estruturas.md](resultados/estruturas.md) |
| Focos a até 25 km de Andradina | árvore k-d: 323 distâncias calculadas contra 10.378 (96,9% a menos) | [resultados/estruturas.md](resultados/estruturas.md) |
| O fogo pula para o vizinho? | 18,1% com vizinho em chamas × 8,2% sem (2,2 vezes) | [resultados/estruturas.md](resultados/estruturas.md) |
| Previsão de focos por município e mês em 2024 (erro médio) | Random Forest 1,355 × previsão sazonal ingênua 1,196 | [resultados/ml-estudo.md](resultados/ml-estudo.md) |
| Testes automatizados | 307 (incluindo 5 de interação com a interface) | [QUALIDADE.md](QUALIDADE.md) |
| Cobertura de linhas | 73,3% | JaCoCo |
| Mutantes mortos (PIT) | 69,9% no total · 81,8% nos algoritmos | [QUALIDADE.md](QUALIDADE.md) |
