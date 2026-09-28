# Qualidade de engenharia

Tudo abaixo roda no `./mvnw verify` (local e CI). Se qualquer item falhar, o build falha.

| Verificação | Ferramenta | Regra |
|---|---|---|
| Testes | JUnit 5 | todos passando (unidade, arquitetura, relatórios e interface) |
| Interação com a interface | TestFX + Monocle | teclado e mouse simulados numa tela virtual, em JVM separada: Ctrl+K, atalhos, F5/Esc, filtro por município e controles de acessibilidade. Rodam também no CI, que não tem tela |
| Cores | `CoresAcessiveisTest` | contraste WCAG AA do texto e separação das cores de dados em protanopia, deuteranopia e tritanopia, em cada combinação de tema, modo daltônico e alto contraste |
| Arquitetura | `ArquiteturaTest` | sem ordenação pronta em `src/main`; sem `HashMap`/`TreeMap` como índice em `estruturas` e `busca` |
| Estilo | Checkstyle 10 (`config/checkstyle.xml`) | sem tabs, sem imports inúteis, sem comentários de linha, sem `Collections.sort`/`Arrays.sort`/`TreeMap`/`TreeSet`/`PriorityQueue` em produção |
| Análise estática | PMD 7 (`config/pmd.xml`) | regras de defeito real: recurso não fechado, variável/método não usado, comparação de strings com `==`, `null` mal verificado |
| Bugs | SpotBugs (esforço máximo, `config/spotbugs-exclude.xml`) | nenhum achado de prioridade média ou alta; cada exclusão tem justificativa no arquivo |
| Cobertura | JaCoCo | ≥ 65% das linhas no total e ≥ 85% nos pacotes de algoritmos, estruturas, busca, análise, benchmark, ML, modelo e geo |

## Teste de mutação (PIT)

O PIT altera o código (troca `<` por `<=`, remove chamadas, inverte condições) e verifica se algum teste percebe. Um mutante "morto" significa que os testes pegaram o defeito.

```bash
./mvnw -Pmutacao test-compile org.pitest:pitest-maven:mutationCoverage
```

Execução de 27/09/2026 (PIT 1.17.4, 4 threads, 13 algoritmos e as estruturas novas):

| Pacote | Mutantes | Mortos | Pontuação | Força (só código coberto) |
|---|---:|---:|---:|---:|
| `sorting.algorithms` | 489 | 400 | 81,8% | 84,0% |
| `estruturas` | 578 | 381 | 65,9% | 74,1% |
| `busca` | 191 | 98 | 51,3% | 58,7% |
| **Total** | **1.258** | **879** | **69,9%** | **76,0%** |

O perfil falha abaixo de 65%.

**O que o PIT ensinou.** Com os três algoritmos novos, a pontuação dos algoritmos caiu para 75,1%. O motivo: o Intro Sort termina com um Insertion Sort que conserta qualquer erro das fases anteriores, e o recurso ao Heap Sort nunca era exercitado. Os testes só conferiam se o resultado estava ordenado, então mutações que estragavam o Quick Sort ou o Heap Sort internos passavam despercebidas (o algoritmo ficava lento, mas correto). O `HibridosTest` passou a conferir também o custo (comparações e trocas em torno de n·log₂n) e força o caminho do Heap Sort; a pontuação voltou a 81,8%.

Os sobreviventes restantes se concentram na medição de speedup do Merge Sort paralelo (21), no mapeamento de caracteres da Trie (15: acentos e símbolos raros nos testes), em condições de limite do Quick Sort 3-Way (15), na análise de propagação e nas camadas do grafo (`ServicoGeografico`, 23) e no custo das consultas (`ServicoConsultas`, 12). São os próximos alvos de teste.

## Revisão de segurança

| Superfície | Risco | Tratamento |
|---|---|---|
| CSV exportado | texto começando com `=`, `+`, `@` vira fórmula ao abrir no Excel (CSV injection) | `ReportExporter.semFormula` prefixa com apóstrofo; números negativos ficam intactos (teste `csvSemFormula`) |
| Estudo de ML embarcado | desserialização Java de `ml-estudo.bin` | `ObjectInputFilter` aceita só classes do projeto e do `java.base`, com limite de profundidade e referências |
| Download do INPE | arquivo ZIP remoto | endereço obrigatoriamente HTTPS (validado no `DownloaderInpe`); nomes de saída fixos, sem caminho vindo do ZIP (sem *zip slip*); tempo limite de conexão e de leitura |
| Visões salvas | preferência corrompida | leitura protegida: mostra aviso em vez de derrubar a tela |
| Mapa (WebView) | conteúdo remoto | só tiles de mapa; nenhuma ponte JavaScript → Java exposta |
| Segredos | chaves no repositório | nenhum segredo usado; o release usa o `GITHUB_TOKEN` do próprio Actions |

## Simplificações

- Leitura do `grupo.properties` unificada em `config.Grupo` (antes duplicada em três telas e no relatório).
- Caixa de título de nomes de municípios unificada em `Textos.nomeProprio`.
- `ModoApresentacao` deixou de guardar uma referência sem uso ao controlador principal.
