package br.unip.aps.ui;

import br.unip.aps.analysis.Estatisticas;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.ui.componentes.Icones;
import br.unip.aps.ui.componentes.Movimento;
import br.unip.aps.ui.tema.GerenciadorTema;
import br.unip.aps.util.Formatos;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Modo apresentacao (F5): roteiro guiado em tela cheia, avanco pelas setas e integrante responsavel por etapa. */
final class ModoApresentacao {
    private record Etapa(String id, String titulo, String roteiro, Pagina pagina) { }

    private static final List<Etapa> ETAPAS = List.of(
            new Etapa("dados", "Os dados", "Focos do satélite de referência do INPE em SP, 2023–2024: o que aconteceu na temporada", Pagina.VISAO_GERAL),
            new Etapa("ordenacao", "Ordenação", "Dez algoritmos implementados à mão, com a contagem de cada operação", Pagina.ORDENACAO),
            new Etapa("estruturas", "Estruturas e busca", "Por que ordenar: busca binária, árvore AVL, hash e heap sobre os mesmos focos", Pagina.ESTRUTURAS),
            new Etapa("benchmark", "Benchmark", "Custo × tamanho da entrada: na escala log-log, a inclinação é o expoente da complexidade", Pagina.BENCHMARK),
            new Etapa("mapa", "Mapa", "Onde queima: pontos, densidade por município e os agrupamentos encontrados pelo ML", Pagina.MAPA),
            new Etapa("ml", "Machine Learning", "Previsão por município e mês, validação honesta e por que agosto de 2024 escapou ao modelo", Pagina.ML),
            new Etapa("conclusao", "Conclusão", "", null));

    private final UiContexto ctx;
    private final StackPane raiz;
    private final HBox barra = new HBox(16);
    private final HBox pontos = new HBox(6);
    private final Label titulo = new Label();
    private final Label roteiro = new Label();
    private final Label integrante = new Label();
    private final VBox conclusao = new VBox(18);
    private final List<String> nomes = new ArrayList<>();
    private final Properties grupo = new Properties();
    private final EventHandler<KeyEvent> teclas = this::tecla;
    private int indice;
    private boolean ativo;
    private boolean menuEstavaRecolhido;

    ModoApresentacao(UiContexto ctx, StackPane raiz) {
        this.ctx = ctx;
        this.raiz = raiz;
        lerGrupo();
        titulo.getStyleClass().add("apresentacao-titulo");
        roteiro.getStyleClass().add("apresentacao-roteiro");
        integrante.getStyleClass().add("apresentacao-integrante");
        VBox textos = new VBox(2, titulo, roteiro);
        HBox.setHgrow(textos, Priority.ALWAYS);
        textos.setMinWidth(0);
        roteiro.setWrapText(false);
        Label dica = new Label("← →  navegar   ·   Esc  sair");
        dica.getStyleClass().add("apresentacao-dica");
        barra.getChildren().addAll(pontos, textos, integrante, dica);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().add("apresentacao-barra");
        barra.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(barra, Pos.BOTTOM_CENTER);
        StackPane.setMargin(barra, new Insets(0, 24, 20, 24));
        barra.setPickOnBounds(false);
        conclusao.getStyleClass().add("apresentacao-conclusao");
        conclusao.setAlignment(Pos.CENTER_LEFT);
    }

    private void lerGrupo() {
        grupo.putAll(br.unip.aps.config.Grupo.carregar());
        nomes.addAll(br.unip.aps.config.Grupo.integrantes(grupo));
    }

    boolean ativo() {
        return ativo;
    }

    void alternar() {
        if (ativo) sair();
        else entrar();
    }

    private void entrar() {
        if (ctx.baseProperty().get() == null) return;
        ativo = true;
        indice = 0;
        menuEstavaRecolhido = GerenciadorTema.get().sidebarRecolhidaProperty().get();
        GerenciadorTema.get().sidebarRecolhidaProperty().set(true);
        Scene s = raiz.getScene();
        s.addEventFilter(KeyEvent.KEY_PRESSED, teclas);
        s.getRoot().getStyleClass().add("modo-apresentacao");
        ctx.stage().setFullScreenExitHint("");
        ctx.stage().setFullScreen(true);
        raiz.getChildren().add(barra);
        mostrar();
    }

    private void sair() {
        ativo = false;
        Scene s = raiz.getScene();
        s.removeEventFilter(KeyEvent.KEY_PRESSED, teclas);
        s.getRoot().getStyleClass().remove("modo-apresentacao");
        raiz.getChildren().removeAll(barra, conclusao);
        ctx.stage().setFullScreen(false);
        GerenciadorTema.get().sidebarRecolhidaProperty().set(menuEstavaRecolhido);
    }

    private void tecla(KeyEvent e) {
        switch (e.getCode()) {
            case RIGHT, PAGE_DOWN, SPACE -> avancar(1);
            case LEFT, PAGE_UP -> avancar(-1);
            case ESCAPE, F5 -> sair();
            default -> {
                return;
            }
        }
        e.consume();
    }

    private void avancar(int d) {
        int novo = Math.max(0, Math.min(ETAPAS.size() - 1, indice + d));
        if (novo == indice) return;
        indice = novo;
        mostrar();
    }

    private void mostrar() {
        Etapa e = ETAPAS.get(indice);
        raiz.getChildren().remove(conclusao);
        if (e.pagina() != null) {
            ctx.navegar(e.pagina());
        } else {
            montarConclusao();
            raiz.getChildren().add(raiz.getChildren().indexOf(barra), conclusao);
            Movimento.entradaEscalonada(conclusao);
        }
        titulo.setText((indice + 1) + "/" + ETAPAS.size() + " · " + e.titulo());
        roteiro.setText(e.roteiro());
        String quem = grupo.getProperty("apresentacao." + e.id(), "").strip();
        if (quem.isEmpty() && !nomes.isEmpty()) quem = nomes.get(indice % nomes.size());
        integrante.setText(quem.isEmpty() ? "" : "Apresenta: " + quem);
        integrante.setGraphic(quem.isEmpty() ? null : Icones.de(Icones.PESSOA, 16));
        pontos.getChildren().clear();
        for (int i = 0; i < ETAPAS.size(); i++) {
            Region p = new Region();
            p.getStyleClass().add("apresentacao-ponto");
            if (i == indice) p.getStyleClass().add("atual");
            else if (i < indice) p.getStyleClass().add("feito");
            pontos.getChildren().add(p);
        }
    }

    private void montarConclusao() {
        conclusao.getChildren().clear();
        BaseDeFocos b = ctx.baseProperty().get();
        Label t = new Label("O que os dados e os algoritmos mostraram");
        t.getStyleClass().add("apresentacao-conclusao-titulo");
        conclusao.getChildren().add(t);
        List<String> pontosChave = new ArrayList<>();
        if (b != null) {
            Estatisticas est = new Estatisticas(b.getFocos());
            List<Integer> anos = new ArrayList<>(est.porAno().keySet());
            if (anos.size() >= 2) {
                long v1 = est.porAno().get(anos.get(anos.size() - 2)), v2 = est.porAno().get(anos.get(anos.size() - 1));
                pontosChave.add(Formatos.inteiro(v2) + " focos em " + anos.get(anos.size() - 1) + ", " + Formatos.decimal((double) v2 / v1, 1)
                        + " vezes " + anos.get(anos.size() - 2) + ": uma temporada fora do padrão.");
            }
        }
        pontosChave.add("Algoritmos O(n log n) fazem milhares de vezes menos comparações que os O(n²) na base inteira; o Radix não compara.");
        pontosChave.add("Ordenar compensa: a busca binária responde uma consulta com cerca de 27 comparações, contra 10.378 da busca sequencial.");
        pontosChave.add("O modelo de ML supera baselines em anos típicos, mas não extrapola: agosto de 2024 ficou acima de tudo o que ele já tinha visto.");
        for (String p : pontosChave) {
            Label l = new Label(p);
            l.getStyleClass().add("apresentacao-conclusao-item");
            l.setWrapText(true);
            conclusao.getChildren().add(l);
        }
    }
}
