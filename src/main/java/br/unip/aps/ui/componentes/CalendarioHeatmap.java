package br.unip.aps.ui.componentes;

import br.unip.aps.util.Formatos;
import br.unip.aps.util.Textos;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Map;

/** Calendario de focos por dia (heatmap), no estilo "contribution graph". */
public class CalendarioHeatmap extends VBox {
    static final int[] LIMITES = {1, 3, 10, 30, 100};
    private static final double CELULA = 11;
    private static final double GAP = 2.5;

    /** Cria o componente vazio. */
    public CalendarioHeatmap() {
        setSpacing(Espaco.M);
    }

    static int faixa(long n) {
        int f = 0;
        for (int i = 0; i < LIMITES.length; i++) if (n >= LIMITES[i]) f = i + 1;
        return f;
    }

    public void setDados(Map<LocalDate, Long> porDia, List<Integer> anos) {
        getChildren().clear();
        for (int ano : anos) getChildren().add(ano(ano, porDia));
        getChildren().add(legenda());
    }

    private HBox ano(int ano, Map<LocalDate, Long> porDia) {
        GridPane g = new GridPane();
        g.setHgap(GAP);
        g.setVgap(GAP);
        LocalDate d = LocalDate.of(ano, 1, 1);
        int offset = d.getDayOfWeek().getValue() % 7;
        long total = 0;
        int mesAnterior = 0;
        while (d.getYear() == ano) {
            int idx = d.getDayOfYear() - 1 + offset;
            int col = idx / 7, lin = idx % 7;
            long n = porDia.getOrDefault(d, 0L);
            total += n;
            Region c = new Region();
            c.getStyleClass().addAll("calendario-celula", "heat-" + faixa(n));
            c.setMinSize(CELULA, CELULA);
            c.setPrefSize(CELULA, CELULA);
            c.setMaxSize(CELULA, CELULA);
            Graficos.tooltip(c, d.format(Formatos.DATA) + " (" + d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Textos.PT_BR)
                    + "): " + Formatos.inteiro(n) + (n == 1 ? " foco" : " focos"));
            g.add(c, col + 1, lin + 1);
            if (d.getDayOfMonth() == 1 && d.getMonthValue() != mesAnterior) {
                Label m = new Label(br.unip.aps.analysis.Estatisticas.MESES[d.getMonthValue() - 1]);
                m.getStyleClass().add("calendario-mes");
                g.add(m, col + 1, 0, 4, 1);
                mesAnterior = d.getMonthValue();
            }
            d = d.plusDays(1);
        }
        for (DayOfWeek dw : new DayOfWeek[]{DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY}) {
            Label l = new Label(dw.getDisplayName(TextStyle.SHORT, Textos.PT_BR));
            l.getStyleClass().add("calendario-mes");
            g.add(l, 0, (dw.getValue() % 7) + 1);
        }
        Label titulo = new Label(String.valueOf(ano));
        titulo.getStyleClass().add("calendario-ano");
        Label sub = new Label(Formatos.inteiro(total) + " focos");
        sub.getStyleClass().add("t-caption");
        VBox cab = new VBox(2, titulo, sub);
        cab.setMinWidth(64);
        HBox linha = new HBox(Espaco.M, cab, g);
        linha.setAlignment(Pos.TOP_LEFT);
        return linha;
    }

    private HBox legenda() {
        HBox h = new HBox(4);
        h.setAlignment(Pos.CENTER_LEFT);
        Label menos = new Label("menos");
        menos.getStyleClass().add("t-caption");
        h.getChildren().add(menos);
        String[] faixas = {"0", "1–2", "3–9", "10–29", "30–99", "100+"};
        for (int i = 0; i <= 5; i++) {
            Region c = new Region();
            c.getStyleClass().addAll("calendario-celula", "heat-" + i);
            c.setMinSize(CELULA, CELULA);
            c.setMaxSize(CELULA, CELULA);
            Graficos.tooltip(c, faixas[i] + " focos no dia");
            h.getChildren().add(c);
        }
        Label mais = new Label("mais   ·   faixas: 0 · 1–2 · 3–9 · 10–29 · 30–99 · 100+ focos/dia");
        mais.getStyleClass().add("t-caption");
        h.getChildren().add(mais);
        return h;
    }
}
