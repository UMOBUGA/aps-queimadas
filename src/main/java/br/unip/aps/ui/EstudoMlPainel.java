package br.unip.aps.ui;

import br.unip.aps.ml.EstudoPrevisao;
import br.unip.aps.ui.componentes.BarrasHorizontais;
import br.unip.aps.ui.componentes.ChartCard;
import br.unip.aps.ui.componentes.Graficos;
import br.unip.aps.util.Formatos;
import javafx.geometry.HPos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/** Monta os cards do estudo de previsao com historico longo (resultado pre-calculado embarcado no JAR). */
final class EstudoMlPainel {
    private static final Logger LOG = Logger.getLogger(EstudoMlPainel.class.getName());

    private EstudoMlPainel() {
    }

    /** Le o estudo gravado pelo modo ml-estudo; devolve null se o recurso nao existir ou for de outra versao. */
    static EstudoPrevisao.Resultado carregar() {
        try (InputStream in = EstudoMlPainel.class.getResourceAsStream("/resultados/ml-estudo.bin")) {
            if (in == null) return null;
            try (ObjectInputStream o = new ObjectInputStream(in)) {
                o.setObjectInputFilter(java.io.ObjectInputFilter.Config.createFilter(
                        "maxdepth=20;maxrefs=200000;br.unip.aps.**;java.base/*;!*"));
                return (EstudoPrevisao.Resultado) o.readObject();
            }
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            LOG.warning("Estudo de ML pré-calculado indisponível: " + e.getMessage());
            return null;
        }
    }

    static void janelas(ChartCard card, EstudoPrevisao.Resultado r) {
        GridPane g = new GridPane();
        g.getStyleClass().add("tabela-leve");
        g.setHgap(18);
        g.setVgap(8);
        String[] cab = {"Teste", "Treino", "Random Forest", "Persistência", "Média histórica", "Sazonal ingênuo", "R² do RF"};
        for (int c = 0; c < cab.length; c++) {
            Label l = new Label(cab[c]);
            l.getStyleClass().add("tabela-leve-cabecalho");
            g.add(l, c, 0);
            if (c >= 2) GridPane.setHalignment(l, HPos.RIGHT);
        }
        List<String[]> dados = new ArrayList<>();
        int linha = 1;
        for (EstudoPrevisao.Janela j : r.janelas()) {
            double melhorBase = Math.min(j.persistencia().mae(), Math.min(j.mediaHistorica().mae(), j.sazonal().mae()));
            String[] v = {String.valueOf(j.anoTeste()), j.treino(), d(j.modelo().mae()), d(j.persistencia().mae()), d(j.mediaHistorica().mae()),
                    d(j.sazonal().mae()), d(j.modelo().r2())};
            dados.add(v);
            for (int c = 0; c < v.length; c++) {
                Label l = new Label(v[c]);
                l.getStyleClass().add("tabela-leve-celula");
                if (c == 2 && j.modelo().mae() <= melhorBase) l.getStyleClass().add("destaque");
                g.add(l, c, linha);
                if (c >= 2) GridPane.setHalignment(l, HPos.RIGHT);
            }
            linha++;
        }
        EstudoPrevisao.Intervalo i = r.intervalo();
        Label nota = new Label("MAE em focos por município e mês; em negrito, quando o Random Forest vence todos os baselines. Intervalo conforme de 90%: ŷ ± "
                + d(i.meiaLargura()) + " focos, com cobertura de " + Formatos.decimal(100 * i.coberturaCalibracao(), 1) + "% em "
                + i.anoCalibracao() + " e " + Formatos.decimal(100 * i.coberturaTeste(), 1) + "% em " + i.anoTeste()
                + ": o ano atípico quebra a hipótese de que o futuro se parece com o passado.");
        nota.getStyleClass().add("t-small");
        nota.setWrapText(true);
        nota.setMaxWidth(Double.MAX_VALUE);
        nota.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        card.conteudo(new VBox(14, g, nota));
        card.setDados(new String[]{"Ano de teste", "Treino", "RF MAE", "Persistência MAE", "Média histórica MAE", "Sazonal MAE", "RF R²"}, () -> dados);
        card.estado(ChartCard.Estado.CONTEUDO);
    }

    static void ablacao(ChartCard card, EstudoPrevisao.Resultado r) {
        BarrasHorizontais b = new BarrasHorizontais();
        List<BarrasHorizontais.Item> itens = new ArrayList<>();
        List<String[]> dados = new ArrayList<>();
        double max = 0;
        for (EstudoPrevisao.Linha l : r.ablacao()) max = Math.max(max, l.mae());
        EstudoPrevisao.Janela ultima = r.janelas().get(r.janelas().size() - 1);
        for (EstudoPrevisao.Linha l : List.of(ultima.persistencia(), ultima.mediaHistorica(), ultima.sazonal())) max = Math.max(max, l.mae());
        double melhor = Double.MAX_VALUE;
        for (EstudoPrevisao.Linha l : r.ablacao()) melhor = Math.min(melhor, l.mae());
        for (EstudoPrevisao.Linha l : r.ablacao()) {
            boolean explicativo = l.nome().startsWith("Explicativo");
            itens.add(new BarrasHorizontais.Item(l.nome(), l.mae(), d(l.mae()), l.mae() == melhor ? "lider" : explicativo ? "neutra" : "suave",
                    l.mae() == melhor, l.nome() + ": MAE " + d(l.mae()) + " · RMSE " + d(l.rmse()) + " · R² " + d(l.r2())));
            dados.add(new String[]{l.nome(), d(l.mae()), d(l.rmse()), d(l.r2())});
        }
        for (EstudoPrevisao.Linha l : List.of(ultima.persistencia(), ultima.mediaHistorica(), ultima.sazonal())) {
            itens.add(new BarrasHorizontais.Item("Baseline: " + l.nome(), l.mae(), d(l.mae()), "neutra", false,
                    l.nome() + ": MAE " + d(l.mae())));
            dados.add(new String[]{"Baseline: " + l.nome(), d(l.mae()), d(l.rmse()), d(l.r2())});
        }
        b.setMaximo(max);
        b.setItens(itens);
        card.conteudo(b);
        card.setDados(new String[]{"Variáveis", "MAE", "RMSE", "R²"}, () -> dados);
        card.estado(ChartCard.Estado.CONTEUDO);
    }

    static void caso(ChartCard card, EstudoPrevisao.Resultado r) {
        EstudoPrevisao.Caso c = r.caso();
        LineChart<String, Number> g = new LineChart<>(new CategoryAxis(), new NumberAxis());
        g.setAnimated(false);
        g.setLegendVisible(false);
        g.setCreateSymbols(true);
        XYChart.Series<String, Number> atual = new XYChart.Series<>(), media = new XYChart.Series<>();
        List<String[]> dados = new ArrayList<>();
        for (int d = 0; d < c.diario().size(); d++) {
            String dia = String.valueOf(d + 1);
            atual.getData().add(new XYChart.Data<>(dia, c.diario().get(d)[1]));
            media.getData().add(new XYChart.Data<>(dia, c.mediaDiariaAnteriores()[d]));
            dados.add(new String[]{dia, String.valueOf(c.diario().get(d)[1]), Formatos.decimal(c.mediaDiariaAnteriores()[d], 1)});
        }
        g.getData().add(media);
        g.getData().add(atual);
        Graficos.classe(atual, "serie-ano-recente");
        Graficos.classe(media, "serie-ano-anterior");
        Graficos.tooltips(atual, x -> x.getXValue() + "/" + c.mes().getMonthValue() + "/" + c.mes().getYear() + ": "
                + Formatos.inteiro(x.getYValue().longValue()) + " focos");
        Graficos.tooltips(media, x -> "dia " + x.getXValue() + ", média de " + r.anoInicio() + "–" + (c.mes().getYear() - 1) + ": "
                + Formatos.decimal(x.getYValue().doubleValue(), 1) + " focos");
        ((NumberAxis) g.getYAxis()).setLabel("focos por dia");
        card.limparLegenda();
        card.adicionarLegenda(String.valueOf(c.mes().getYear()), "ano-recente");
        card.adicionarLegenda("média " + r.anoInicio() + "–" + (c.mes().getYear() - 1), "ano-anterior");
        Label fatos = new Label(Formatos.inteiro(c.real()) + " focos reais contra " + Formatos.inteiro(Math.round(c.previsto()))
                + " previstos. O mês foi " + Formatos.decimal(c.real() / (double) Math.max(1, c.maiorMesTreino()), 2)
                + "× o maior mês do treino (" + Formatos.inteiro(c.maiorMesTreino()) + " em " + c.maiorMesTreinoData() + "). "
                + "Os focos tiveram em média " + Formatos.decimal(c.diasSemChuva(), 1) + " dias sem chuva, contra "
                + Formatos.decimal(c.diasSemChuvaAnteriores(), 1) + " nos agostos anteriores. Árvores de decisão só preveem valores "
                + "dentro da faixa vista no treino, então um evento fora da distribuição não pode ser antecipado.");
        fatos.getStyleClass().add("t-small");
        fatos.setWrapText(true);
        fatos.setMaxWidth(Double.MAX_VALUE);
        fatos.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        card.conteudo(new VBox(10, g, fatos));
        card.setDados(new String[]{"Dia", "Focos " + c.mes().getYear(), "Média dos anos anteriores"}, () -> dados);
        card.estado(ChartCard.Estado.CONTEUDO);
    }

    static void permutacao(ChartCard card, EstudoPrevisao.Resultado r) {
        BarrasHorizontais b = new BarrasHorizontais();
        List<BarrasHorizontais.Item> itens = new ArrayList<>();
        List<String[]> dados = new ArrayList<>();
        int n = 0;
        for (EstudoPrevisao.Importancia i : r.importancia()) {
            dados.add(new String[]{i.variavel(), Formatos.decimal(i.aumentoMae(), 4)});
            if (n++ < 10 && i.aumentoMae() > 0) {
                itens.add(new BarrasHorizontais.Item(i.variavel(), i.aumentoMae(), "+" + Formatos.decimal(i.aumentoMae(), 3),
                        n == 1 ? "lider" : "suave", n == 1, i.variavel() + ": o MAE aumenta " + Formatos.decimal(i.aumentoMae(), 4)
                        + " focos ao embaralhar esta variável"));
            }
        }
        b.setItens(itens);
        card.conteudo(b);
        card.setDados(new String[]{"Variável", "Aumento do MAE"}, () -> dados);
        card.estado(ChartCard.Estado.CONTEUDO);
    }

    private static String d(double v) {
        return Formatos.decimal(v, 3);
    }
}
