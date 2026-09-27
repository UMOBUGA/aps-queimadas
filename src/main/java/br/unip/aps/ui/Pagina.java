package br.unip.aps.ui;

import br.unip.aps.ui.componentes.Icones;

/** Telas do dashboard (navegacao pela sidebar). */
public enum Pagina {
    VISAO_GERAL("Visão geral", Icones.VISAO_GERAL, "visao-geral.fxml", true, false,
            "Indicadores e distribuição dos focos de queimadas"),
    ORDENACAO("Ordenação", Icones.ORDENACAO, "ordenacao.fxml", true, false,
            "Ordene os focos e veja as operações de cada algoritmo"),
    ESTRUTURAS("Estruturas & Busca", Icones.ESTRUTURAS, "estruturas.fxml", false, false,
            "Busca binária, AVL, hash, heap, ordenação externa e paralela"),
    BENCHMARK("Benchmark", Icones.BENCHMARK, "benchmark.fxml", false, false,
            "Custo dos algoritmos em função do tamanho da entrada"),
    MAPA("Mapa", Icones.MAPA, "mapa.fxml", true, false,
            "Distribuição geográfica dos focos"),
    ML("Machine Learning", Icones.ML, "ml.fxml", false, false,
            "Previsão, classificação e hotspots"),
    QUALIDADE("Qualidade dos dados", Icones.QUALIDADE, "qualidade.fxml", false, false,
            "Leitura, validação e limpeza dos CSVs do INPE"),
    CONFIGURACOES("Configurações", Icones.CONFIGURACOES, "configuracoes.fxml", false, true,
            "Tema, animações e preferências"),
    SOBRE("Sobre", Icones.SOBRE, "sobre.fxml", false, true,
            "Equipe, disciplina, tecnologias e fonte dos dados");

    private final String titulo;
    private final String icone;
    private final String fxml;
    private final boolean usaFiltro;
    private final boolean rodape;
    private final String descricao;

    Pagina(String titulo, String icone, String fxml, boolean usaFiltro, boolean rodape, String descricao) {
        this.titulo = titulo;
        this.icone = icone;
        this.fxml = fxml;
        this.usaFiltro = usaFiltro;
        this.rodape = rodape;
        this.descricao = descricao;
    }

    public String titulo() { return titulo; }
    public String icone() { return icone; }
    public String fxml() { return fxml; }
    public boolean usaFiltro() { return usaFiltro; }
    public boolean rodape() { return rodape; }
    public String descricao() { return descricao; }

    /** Contrato opcional dos controllers de pagina. */
    public interface Controlador {
        /** Chamado sempre que a tela passa a ser exibida. */
        default void aoExibir() { }

        /** Chamado quando a tela deixa de ser exibida. */
        default void aoOcultar() { }

        /** Prepara um estado demonstrativo (usado pela captura automatica de telas). */
        default void demonstrar(Runnable concluido) {
            concluido.run();
        }
    }
}
