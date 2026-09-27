package br.unip.aps.ui.componentes;

import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Catalogo de icones (Ikonli · Material Design Icons 2). Centralizar os codigos garante que a
 * mesma acao tenha sempre o mesmo icone em toda a interface.
 */
public final class Icones {

    public static final String MARCA = "mdi2f-fire";
    public static final String VISAO_GERAL = "mdi2v-view-dashboard-outline";
    public static final String ORDENACAO = "mdi2s-sort-ascending";
    public static final String BENCHMARK = "mdi2s-speedometer";
    public static final String MAPA = "mdi2m-map-outline";
    public static final String ML = "mdi2b-brain";
    public static final String QUALIDADE = "mdi2s-shield-check-outline";
    public static final String CONFIGURACOES = "mdi2c-cog-outline";
    public static final String SOBRE = "mdi2i-information-outline";

    public static final String ABRIR = "mdi2f-folder-open-outline";
    public static final String RECARREGAR = "mdi2r-refresh";
    public static final String BAIXAR = "mdi2c-cloud-download-outline";
    public static final String EXPORTAR = "mdi2e-export-variant";
    public static final String EXCEL = "mdi2m-microsoft-excel";
    public static final String PDF = "mdi2f-file-pdf-box";
    public static final String CSV = "mdi2f-file-delimited-outline";
    public static final String CODIGO = "mdi2c-code-braces";
    public static final String TEMA_ESCURO = "mdi2w-weather-night";
    public static final String TEMA_CLARO = "mdi2w-white-balance-sunny";
    public static final String RECOLHER = "mdi2c-chevron-double-left";
    public static final String EXPANDIR = "mdi2c-chevron-double-right";

    public static final String CALENDARIO = "mdi2c-calendar-outline";
    public static final String CALENDARIO_MES = "mdi2c-calendar-month-outline";
    public static final String SUBINDO = "mdi2t-trending-up";
    public static final String DESCENDO = "mdi2t-trending-down";
    public static final String MUNICIPIO = "mdi2m-map-marker-outline";
    public static final String LIDER = "mdi2t-trophy-outline";
    public static final String BIOMA = "mdi2t-tree-outline";
    public static final String SERIE = "mdi2c-chart-timeline-variant";

    public static final String FILTRO = "mdi2f-filter-variant";
    public static final String FECHAR = "mdi2c-close";
    public static final String BUSCA = "mdi2m-magnify";
    public static final String MAIS_ACOES = "mdi2d-dots-horizontal";
    public static final String IMAGEM = "mdi2i-image-outline";
    public static final String TABELA = "mdi2t-table";
    public static final String SUCESSO = "mdi2c-check-circle-outline";
    public static final String ERRO = "mdi2a-alert-circle-outline";
    public static final String ALERTA = "mdi2a-alert-outline";
    public static final String INFO = "mdi2i-information-outline";
    public static final String PERGUNTA = "mdi2h-help-circle-outline";

    public static final String ARRASTAR = "mdi2d-drag-vertical";
    public static final String ADICIONAR = "mdi2p-plus";
    public static final String REMOVER = "mdi2d-delete-outline";
    public static final String EXECUTAR = "mdi2p-play";
    public static final String PAUSAR = "mdi2p-pause";
    public static final String REINICIAR = "mdi2s-skip-previous";
    public static final String COMPARAR = "mdi2c-compare-horizontal";
    public static final String ANIMAR = "mdi2a-animation-play-outline";
    public static final String COMPARACOES = "mdi2c-counter";
    public static final String TROCAS = "mdi2s-swap-horizontal";
    public static final String TEMPO = "mdi2t-timer-outline";
    public static final String CRESCENTE = "mdi2a-arrow-up";
    public static final String DECRESCENTE = "mdi2a-arrow-down";
    public static final String VERIFICADO = "mdi2c-check-decagram-outline";
    public static final String ACESSOS = "mdi2d-database-outline";
    public static final String VENCEDOR = "mdi2c-crown-outline";
    public static final String CAMADAS = "mdi2l-layers-outline";
    public static final String ALVO = "mdi2t-target";
    public static final String GRADE = "mdi2g-grid";
    public static final String SEM_DADOS = "mdi2d-database-off-outline";
    public static final String VAZIO_FILTRO = "mdi2f-file-search-outline";
    public static final String PARAR = "mdi2s-stop-circle-outline";
    public static final String GRUPO = "mdi2a-account-group-outline";
    public static final String ESCOLA = "mdi2s-school-outline";
    public static final String SATELITE = "mdi2s-satellite-variant";
    public static final String IDEIA = "mdi2l-lightbulb-on-outline";
    public static final String SIGMA = "mdi2s-sigma";
    public static final String DONUT = "mdi2c-chart-donut";
    public static final String RELOGIO = "mdi2c-clock-outline";
    public static final String PALETA = "mdi2p-palette-outline";
    public static final String TEMA = "mdi2t-theme-light-dark";
    public static final String TECLADO = "mdi2k-keyboard-outline";
    public static final String CUBO = "mdi2c-cube-outline";
    public static final String PERCENTUAL = "mdi2p-percent-outline";
    public static final String DISPERSAO = "mdi2c-chart-scatter-plot";
    public static final String EXPERIMENTO = "mdi2f-flask-outline";
    public static final String MENOS = "mdi2m-minus";
    public static final String PONTOS = "mdi2d-dots-grid";

    private Icones() { }

    /**
     * @param codigo  codigo Ikonli (ex.: {@link #MARCA})
     * @param tamanho tamanho em px
     * @return icone pronto para usar como {@code graphic}
     */
    public static FontIcon de(String codigo, int tamanho) {
        FontIcon i = new FontIcon(codigo);
        i.setIconSize(tamanho);
        return i;
    }

    /**
     * @param codigo codigo Ikonli
     * @return icone de 18 px
     */
    public static FontIcon de(String codigo) {
        return de(codigo, 18);
    }
}
