# Status das fases e checklist de entrega

| Fase | Status | Onde |
|---|---|---|
| 1. Obtenção e entendimento dos dados | ✔ concluída | [DADOS.md](DADOS.md), `data/raw/` |
| 2. Arquitetura | ✔ concluída | [ARQUITETURA.md](ARQUITETURA.md) |
| 3. Algoritmos de ordenação | ✔ concluída (10 algoritmos) | `sorting/`, [ALGORITMOS.md](ALGORITMOS.md) |
| 4. Benchmark e análise de performance | ✔ concluída | `benchmark/`, [BENCHMARK.md](BENCHMARK.md), [resultados/](resultados/) |
| 5. Machine Learning | ✔ concluída (3 análises) | `ml/`, [ML.md](ML.md) |
| 6. Dashboard | ✔ concluído (6 abas) | `ui/`, `src/main/resources/br/unip/aps/ui/` |
| 7. Qualidade, testes e documentação | ✔ 199 testes, CI, design system (DESIGN.md), Javadoc, README, relatório de código | `src/test/`, `.github/workflows/` |
| 8. Dissertação (70% da nota) | ⏳ pendente | [GRUPO.md](GRUPO.md), [DESIGN.md](DESIGN.md) §8 |
| 9. Apresentação (30%) | ⏳ pendente | prints em `docs/prints/` |
| 10. Entrega | ⏳ pendente | checklist abaixo |

## Checklist final (Fase 10)

- [ ] Preencher [GRUPO.md](GRUPO.md): nomes, RAs, campus, turma, orientador.
- [ ] `./mvnw clean verify` com BUILD SUCCESS.
- [ ] Gerar `relatorios/codigo-fonte.pdf` (modo `relatorio-codigo`).
- [ ] **Trabalho parcial publicado no Microsoft Teams**, para acompanhamento do orientador.
- [ ] **Código publicado no Teams:** link do GitHub + `codigo-fonte.pdf` (e, se pedido, `.zip` sem `target/`).
- [ ] Dissertação no padrão ABNT/UNIP: Arial 12, espaçamento 1,5, margens esquerda e direita de 2,5 cm, A4.
- [ ] **Entrega final no site da UNIP. Obrigatória: sem ela, o grupo é reprovado.**
- [ ] Versão **impressa, encadernada em espiral com capa transparente**, acompanhada da **Ficha de APS**.
- [ ] Ensaio da apresentação, com o plano B: modo `cli` se o computador da sala não tiver internet ou JavaFX.
