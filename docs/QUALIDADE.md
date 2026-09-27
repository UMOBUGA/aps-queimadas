# Qualidade de engenharia

Tudo abaixo roda no `./mvnw verify` (local e CI). Se qualquer item falhar, o build falha.

| Verificação | Ferramenta | Regra |
|---|---|---|
| Testes | JUnit 5 | todos passando (unidade, arquitetura, relatórios, interface em modo headless) |
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

Execução de 27/09/2026 (PIT 1.17.4, 4 threads, 5 min 34 s):

| Pacote | Mutantes | Mortos | Pontuação | Força (só código coberto) |
|---|---:|---:|---:|---:|
| `sorting.algorithms` | 301 | 248 | 82,4% | 85,5% |
| `estruturas` | 426 | 272 | 63,8% | 73,7% |
| `busca` | 106 | 56 | 52,8% | 58,3% |
| **Total** | **833** | **576** | **69,1%** | **76,3%** |

O perfil falha abaixo de 65%. Os sobreviventes se concentram na medição de speedup e na intercalação do Merge Sort paralelo (21 + 9), em condições de limite do Quick Sort 3-Way (15), no cálculo de custo e tempo das consultas (`ServicoConsultas`, 12 + 9) e nos avisos de progresso do External Merge Sort (11). São os próximos alvos de teste.

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
