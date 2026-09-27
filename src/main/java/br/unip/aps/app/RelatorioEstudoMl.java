package br.unip.aps.app;

import br.unip.aps.ml.EstudoPrevisao;
import br.unip.aps.util.Formatos;

import java.time.YearMonth;
import java.util.Map;

/** Converte o estudo de previsao em Markdown para docs/resultados. */
public final class RelatorioEstudoMl {
    private RelatorioEstudoMl() {
    }

    public static String markdown(EstudoPrevisao.Resultado r) {
        StringBuilder md = new StringBuilder();
        md.append("# Resultados reais: estudo de previsão com histórico ").append(r.anoInicio()).append("–").append(r.anoFim()).append("\n\n");
        md.append("- Focos do satélite de referência em SP: ").append(Formatos.inteiro(r.focos())).append(" (").append(r.municipios())
                .append(" municípios com pelo menos um foco).\n");
        md.append("- Detecções de todos os satélites (variáveis meteorológicas): ").append(Formatos.inteiro(r.deteccoesTodosSatelites())).append(".\n");
        md.append("- Vizinhança pela malha do IBGE: ").append(r.vizinhosPorFronteira()).append(" municípios com fronteira detectada, ")
                .append(r.vizinhosPorProximidade()).append(" por proximidade (5 mais próximos); média de ")
                .append(Formatos.decimal(r.mediaVizinhos(), 1)).append(" vizinhos.\n");
        md.append("- Preparo: ordenação município → data com Merge Sort (").append(Formatos.inteiro(r.ordenacaoPreparo().comparacoes()))
                .append(" comparações) e agregação em uma passada.\n");
        md.append("- Random Forest com ").append(r.arvores()).append(" árvores, semente fixa. Duração total: ")
                .append(Formatos.duracao(r.duracaoMs() * 1_000_000)).append(".\n\n");

        md.append("## 1. Validação em janelas (rolling origin): treino com todos os anos anteriores\n\n");
        md.append("| Ano de teste | Treino | Random Forest MAE | RMSE | R² | Persistência MAE | Média histórica MAE | Sazonal ingênuo MAE |\n");
        md.append("|---:|---|---:|---:|---:|---:|---:|---:|\n");
        for (EstudoPrevisao.Janela j : r.janelas()) {
            md.append("| ").append(j.anoTeste()).append(" | ").append(j.treino()).append(" | ").append(d(j.modelo().mae())).append(" | ")
                    .append(d(j.modelo().rmse())).append(" | ").append(d(j.modelo().r2())).append(" | ").append(d(j.persistencia().mae()))
                    .append(" | ").append(d(j.mediaHistorica().mae())).append(" | ").append(d(j.sazonal().mae())).append(" |\n");
        }
        md.append("\n## 2. Ablação (treino ").append(r.anoInicio() + 1).append("–").append(r.anoFim() - 1).append(", teste ")
                .append(r.anoFim()).append("): grupos de variáveis acumulados\n\n");
        md.append("| Variáveis | MAE | RMSE | R² |\n|---|---:|---:|---:|\n");
        for (EstudoPrevisao.Linha l : r.ablacao()) {
            md.append("| ").append(l.nome()).append(" | ").append(d(l.mae())).append(" | ").append(d(l.rmse())).append(" | ").append(d(l.r2())).append(" |\n");
        }
        EstudoPrevisao.Intervalo i = r.intervalo();
        md.append("\n## 3. Intervalo de previsão conforme (90%)\n\n");
        md.append("Calibrado nos resíduos de ").append(i.anoCalibracao()).append(" (modelo treinado até ").append(i.anoCalibracao() - 1)
                .append("): ŷ ± ").append(d(i.meiaLargura())).append(" focos por município e mês.\n\n");
        md.append("| Cobertura desejada | Cobertura na calibração (").append(i.anoCalibracao()).append(") | Cobertura no teste (")
                .append(i.anoTeste()).append(") |\n|---:|---:|---:|\n");
        md.append("| ").append(p(i.coberturaEsperada())).append(" | ").append(p(i.coberturaCalibracao())).append(" | ")
                .append(p(i.coberturaTeste())).append(" |\n");
        md.append("\n## 4. Importância por permutação (aumento do MAE ao embaralhar a variável, média de 3 permutações)\n\n");
        md.append("| Variável | Aumento do MAE |\n|---|---:|\n");
        for (EstudoPrevisao.Importancia im : r.importancia()) md.append("| ").append(im.variavel()).append(" | ").append(d(im.aumentoMae())).append(" |\n");
        EstudoPrevisao.Caso c = r.caso();
        md.append("\n## 5. Estudo de caso: ").append(c.mes()).append("\n\n");
        md.append("| Medida | Valor |\n|---|---:|\n");
        md.append("| Focos reais no mês (SP) | ").append(Formatos.inteiro(c.real())).append(" |\n");
        md.append("| Focos previstos (soma dos municípios) | ").append(Formatos.inteiro(Math.round(c.previsto()))).append(" |\n");
        md.append("| Maior mês do período de treino | ").append(Formatos.inteiro(c.maiorMesTreino())).append(" (").append(c.maiorMesTreinoData()).append(") |\n");
        md.append("| Razão entre o mês e o maior mês de treino | ").append(d(c.real() / (double) Math.max(1, c.maiorMesTreino()))).append("× |\n");
        md.append("| Maior valor município-mês no treino | ").append(c.maiorMunicipioMesTreino()).append(" |\n");
        md.append("| Maior valor município-mês no caso | ").append(c.maiorMunicipioMesCaso()).append(" (").append(c.municipioCaso()).append(") |\n");
        md.append("| Dias sem chuva (média dos focos, todos os satélites) | ").append(d(c.diasSemChuva())).append(" (anos anteriores: ")
                .append(d(c.diasSemChuvaAnteriores())).append(") |\n");
        md.append("| Risco de fogo (média) | ").append(d(c.risco())).append(" (anos anteriores: ").append(d(c.riscoAnteriores())).append(") |\n");
        md.append("\n| Município | Real | Previsto | Mesmo mês do ano anterior |\n|---|---:|---:|---:|\n");
        for (String[] m : c.municipios()) {
            md.append("| ").append(m[0]).append(" | ").append(m[1]).append(" | ").append(m[2].replace('.', ',')).append(" | ").append(m[3]).append(" |\n");
        }
        md.append("\n## 6. Focos no estado por mês em ").append(r.anoFim()).append(": real × previsto (modelo completo)\n\n");
        md.append("| Mês | Real | Previsto |\n|---|---:|---:|\n");
        for (Map.Entry<YearMonth, double[]> e : r.serie().entrySet()) {
            md.append("| ").append(e.getKey()).append(" | ").append(Formatos.inteiro(Math.round(e.getValue()[0]))).append(" | ")
                    .append(Formatos.inteiro(Math.round(e.getValue()[1]))).append(" |\n");
        }
        return md.toString();
    }

    private static String d(double v) {
        return Formatos.decimal(v, 3);
    }

    private static String p(double v) {
        return Formatos.decimal(100 * v, 1) + "%";
    }
}
