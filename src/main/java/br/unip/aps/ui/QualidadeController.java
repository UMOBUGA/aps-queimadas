package br.unip.aps.ui;

import br.unip.aps.io.RelatorioCarga;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.ui.componentes.BarrasHorizontais;
import br.unip.aps.ui.componentes.ChartCard;
import br.unip.aps.ui.componentes.Chip;
import br.unip.aps.ui.componentes.Icones;
import br.unip.aps.ui.componentes.KpiCard;
import br.unip.aps.util.Formatos;
import br.unip.aps.util.Textos;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Tela "Qualidade dos dados". */
public class QualidadeController implements Pagina.Controlador {
    private final UiContexto ctx;

    @FXML private KpiCard kLidos, kValidos, kDescartados, kDuplicados, kCompletude;
    @FXML private ChartCard cCompletude, cProblemas, cArquivos;
    @FXML private TitledPane tpRejeicoes;
    @FXML private TableView<RelatorioCarga.Rejeicao> tabRejeicoes;

    private final BarrasHorizontais completude = new BarrasHorizontais();
    private final VBox problemas = new VBox();
    private final TableView<RelatorioCarga.ResumoArquivo> tabArquivos = new TableView<>();

    /** Coluna do CSV e como verificar se o foco a preencheu. */
    private record Coluna(String nome, boolean obrigatoria, Predicate<FocoIncendio> preenchido) { }

    private static final List<Coluna> COLUNAS = List.of(
            new Coluna("id_bdq", false, f -> f.getIdBdq() != 0),
            new Coluna("foco_id", false, f -> f.getFocoId() != null),
            new Coluna("lat", true, f -> true),
            new Coluna("lon", true, f -> true),
            new Coluna("data_pas", true, f -> f.getDataHora() != null),
            new Coluna("pais", false, f -> f.getPais() != null),
            new Coluna("estado", false, f -> f.getEstado() != null),
            new Coluna("municipio", true, f -> f.getMunicipio() != null),
            new Coluna("bioma", true, f -> f.getBioma() != null),
            new Coluna("satelite", false, f -> f.getSatelite() != null),
            new Coluna("frp", false, f -> f.getFrp() != null),
            new Coluna("precipitacao", false, f -> f.getPrecipitacao() != null),
            new Coluna("risco_fogo", false, f -> f.getRiscoFogo() != null),
            new Coluna("numero_dias_sem_chuva", false, f -> f.getNumeroDiasSemChuva() != null));

    public QualidadeController(UiContexto ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        completude.setMaximo(100);
        cCompletude.conteudo(completude);
        ScrollPane sp = new ScrollPane(problemas);
        sp.setFitToWidth(true);
        cProblemas.conteudo(sp);
        cArquivos.conteudo(tabArquivos);

        Tabelas.preparar(tabArquivos, "Nenhum arquivo carregado.");
        tabArquivos.getColumns().add(Tabelas.coluna("Arquivo", (RelatorioCarga.ResumoArquivo r) -> r.arquivo().getFileName().toString(), 200));
        tabArquivos.getColumns().add(Tabelas.coluna("Encoding", (RelatorioCarga.ResumoArquivo r) -> r.encoding().name(), 90));
        tabArquivos.getColumns().add(Tabelas.coluna("Sep.", (RelatorioCarga.ResumoArquivo r) -> "'" + r.separador() + "'", 50));
        tabArquivos.getColumns().add(Tabelas.numero("Lidas", RelatorioCarga.ResumoArquivo::linhasLidas, Formatos::inteiro, 80));
        tabArquivos.getColumns().add(Tabelas.numero("Aceitas", RelatorioCarga.ResumoArquivo::aceitas, Formatos::inteiro, 80));
        tabArquivos.getColumns().add(Tabelas.numero("Rejeitadas", RelatorioCarga.ResumoArquivo::rejeitadas, Formatos::inteiro, 90));
        tabArquivos.getColumns().add(Tabelas.coluna("Colunas do cabeçalho", (RelatorioCarga.ResumoArquivo r) -> String.join(", ", r.colunas()), 480));

        Tabelas.preparar(tabRejeicoes, "Nenhuma linha rejeitada — base íntegra.");
        tabRejeicoes.getColumns().add(Tabelas.coluna("Arquivo", (RelatorioCarga.Rejeicao r) -> r.arquivo().getFileName().toString(), 180));
        tabRejeicoes.getColumns().add(Tabelas.numero("Linha", RelatorioCarga.Rejeicao::linha, String::valueOf, 70));
        tabRejeicoes.getColumns().add(Tabelas.coluna("Motivo", RelatorioCarga.Rejeicao::motivo, 240));
        tabRejeicoes.getColumns().add(Tabelas.coluna("Conteúdo", RelatorioCarga.Rejeicao::conteudo, 560));

        ctx.baseProperty().addListener((o, a, b) -> atualizar(b));
        atualizar(ctx.baseProperty().get());
    }

    private void atualizar(BaseDeFocos b) {
        if (b == null) {
            for (ChartCard c : List.of(cCompletude, cProblemas, cArquivos)) c.estado(ChartCard.Estado.VAZIO);
            return;
        }
        RelatorioCarga r = b.getRelatorio();
        long lidas = r.getTotalLidas();
        kLidos.valor(Formatos.inteiro(lidas)).contexto(r.getArquivos().size() + " arquivo(s) CSV", KpiCard.Tendencia.NEUTRA);
        kValidos.valor(Formatos.inteiro(b.tamanho()))
                .contexto(Formatos.decimal(lidas == 0 ? 0 : 100.0 * b.tamanho() / lidas, 1) + "% dos registros", KpiCard.Tendencia.NEUTRA)
                .icone(KpiCard.EstiloIcone.SUCESSO);
        kDescartados.valor(Formatos.inteiro(r.getTotalRejeitadas()))
                .contexto(r.getTotalRejeitadas() == 0 ? "nenhuma linha inválida" : r.getMotivos().size() + " motivo(s)", KpiCard.Tendencia.NEUTRA)
                .icone(r.getTotalRejeitadas() == 0 ? KpiCard.EstiloIcone.NEUTRO : KpiCard.EstiloIcone.ALERTA);
        kDuplicados.valor(Formatos.inteiro(r.getDuplicadosRemovidos()))
                .contexto("foco_id repetido", KpiCard.Tendencia.NEUTRA);

        Set<String> presentes = new HashSet<>();
        for (RelatorioCarga.ResumoArquivo a : r.getArquivos()) for (String c : a.colunas()) presentes.add(Textos.semAcentos(c.strip()));
        List<BarrasHorizontais.Item> itens = new ArrayList<>();
        List<String[]> linhas = new ArrayList<>();
        double somaObrig = 0;
        int nObrig = 0;
        List<String> ausentes = new ArrayList<>();
        for (Coluna c : COLUNAS) {
            boolean existe = presentes.contains(c.nome()) || c.obrigatoria();
            long n = 0;
            if (existe) for (FocoIncendio f : b.getFocos()) if (c.preenchido().test(f)) n++;
            double pct = b.tamanho() == 0 ? 0 : 100.0 * n / b.tamanho();
            if (c.obrigatoria()) {
                somaObrig += pct;
                nObrig++;
            }
            if (!existe) ausentes.add(c.nome());
            String texto = existe ? Formatos.decimal(pct, 1) + "%" : "ausente";
            itens.add(new BarrasHorizontais.Item(c.nome() + (c.obrigatoria() ? " *" : ""), existe ? pct : 0, texto,
                    !existe ? "vazia" : pct >= 99.95 ? "sucesso" : "suave", false,
                    c.nome() + (c.obrigatoria() ? " (obrigatória)" : " (opcional)") + ": "
                            + (existe ? Formatos.inteiro(n) + " de " + Formatos.inteiro(b.tamanho()) + " preenchidos" : "coluna inexistente nos arquivos")));
            linhas.add(new String[]{c.nome(), c.obrigatoria() ? "obrigatória" : "opcional", texto});
        }
        completude.setItens(itens);
        cCompletude.setDados(new String[]{"Coluna", "Tipo", "Completude"}, () -> linhas);
        cCompletude.setExtra(Chip.de("* obrigatória", null, Chip.Variante.NEUTRO));
        cCompletude.estado(ChartCard.Estado.CONTEUDO);
        kCompletude.valor(Formatos.decimal(nObrig == 0 ? 0 : somaObrig / nObrig, 1) + "%")
                .contexto("obrigatórias · " + ausentes.size() + " opcionais ausentes", KpiCard.Tendencia.NEUTRA)
                .icone(KpiCard.EstiloIcone.DESTAQUE);

        montarProblemas(r, ausentes, b);
        tabArquivos.setItems(FXCollections.observableArrayList(r.getArquivos()));
        cArquivos.estado(ChartCard.Estado.CONTEUDO);
        tabRejeicoes.setItems(FXCollections.observableArrayList(r.getRejeicoes()));
        tpRejeicoes.setText("Linhas rejeitadas (" + Formatos.inteiro(r.getTotalRejeitadas()) + ")");
    }

    private void montarProblemas(RelatorioCarga r, List<String> ausentes, BaseDeFocos b) {
        problemas.getChildren().clear();
        if (r.getTotalRejeitadas() > 0) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Long> e : r.getMotivos().entrySet()) sb.append("• ").append(e.getKey()).append(": ").append(Formatos.inteiro(e.getValue())).append('\n');
            problema(Chip.Variante.PERIGO, "Erro", Formatos.inteiro(r.getTotalRejeitadas()) + " linhas rejeitadas", sb.toString().strip());
        } else {
            problema(Chip.Variante.SUCESSO, "OK", "Nenhuma linha rejeitada",
                    "Todas as linhas têm data válida, coordenadas no território brasileiro, município e bioma.");
        }
        if (r.getDuplicadosRemovidos() > 0) {
            problema(Chip.Variante.ALERTA, "Alerta", Formatos.inteiro(r.getDuplicadosRemovidos()) + " focos duplicados removidos",
                    "O mesmo foco_id aparecia em mais de um arquivo; apenas a primeira ocorrência foi mantida.");
        } else {
            problema(Chip.Variante.SUCESSO, "OK", "Sem duplicatas", "Nenhum foco_id repetido entre os arquivos unificados.");
        }
        for (Map.Entry<String, Long> e : r.getValoresAusentes().entrySet()) {
            problema(Chip.Variante.ALERTA, "Alerta", "Valores ausentes/−999 em " + e.getKey(),
                    Formatos.inteiro(e.getValue()) + " registros tratados como ausentes (sentinela do INPE).");
        }
        if (!ausentes.isEmpty()) {
            problema(Chip.Variante.INFO, "Info", "Colunas opcionais inexistentes no conjunto",
                    String.join(", ", ausentes) + ". Os arquivos EstadosBr_sat_ref não trazem dados meteorológicos nem FRP; "
                            + "os critérios e modelos que dependem deles ficam desabilitados.");
        }
        Set<String> enc = new HashSet<>();
        for (RelatorioCarga.ResumoArquivo a : r.getArquivos()) enc.add(a.encoding().name() + ", separador '" + a.separador() + "'");
        problema(Chip.Variante.INFO, "Info", "Formato detectado: " + String.join(" · ", enc),
                "Encoding validado byte a byte (UTF-8 estrito, com recurso a ISO-8859-1); BOM removido; colunas mapeadas pelo nome.");
        problema(Chip.Variante.INFO, "Info", "Datas em GMT (UTC)",
                "data_pas é mantida em GMT nas tabelas; o gráfico de horas converte para o horário de Brasília (UTC−3). Período: "
                        + b.dataInicial().format(Formatos.DATA) + " a " + b.dataFinal().format(Formatos.DATA) + ".");
    }

    private void problema(Chip.Variante v, String severidade, String titulo, String texto) {
        Label chip = Chip.de(severidade, v == Chip.Variante.PERIGO ? Icones.ERRO : v == Chip.Variante.ALERTA ? Icones.ALERTA
                : v == Chip.Variante.SUCESSO ? Icones.SUCESSO : Icones.INFO, v);
        chip.setMinWidth(76);
        Label t = new Label(titulo);
        t.getStyleClass().add("problema-titulo");
        Label x = new Label(texto);
        x.getStyleClass().add("problema-texto");
        x.setWrapText(true);
        VBox textos = new VBox(2, t, x);
        HBox.setHgrow(textos, Priority.ALWAYS);
        HBox linha = new HBox(12, chip, textos);
        linha.getStyleClass().add("problema");
        linha.setAlignment(Pos.TOP_LEFT);
        problemas.getChildren().add(linha);
    }
}
