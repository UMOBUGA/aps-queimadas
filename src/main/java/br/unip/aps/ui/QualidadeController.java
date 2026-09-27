package br.unip.aps.ui;

import br.unip.aps.io.RelatorioCarga;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.util.Formatos;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Aba "Qualidade dos dados": evidencia da etapa de leitura/validacao/limpeza (encoding,
 * separador, colunas, rejeicoes e duplicatas).
 */
public class QualidadeController {

    private final UiContexto ctx;

    @FXML private TableView<RelatorioCarga.ResumoArquivo> tabelaArquivos;
    @FXML private TableView<Map.Entry<String, Long>> tabelaMotivos;
    @FXML private TableView<RelatorioCarga.Rejeicao> tabelaRejeicoes;
    @FXML private Label lblResumo, lblPlano;

    /** @param ctx contexto injetado */
    public QualidadeController(UiContexto ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        tabelaArquivos.getColumns().add(Tabelas.coluna("Arquivo", (RelatorioCarga.ResumoArquivo r) -> r.arquivo().getFileName().toString(), 210));
        tabelaArquivos.getColumns().add(Tabelas.coluna("Encoding", (RelatorioCarga.ResumoArquivo r) -> r.encoding().name(), 90));
        tabelaArquivos.getColumns().add(Tabelas.coluna("Separador", (RelatorioCarga.ResumoArquivo r) -> "'" + r.separador() + "'", 80));
        tabelaArquivos.getColumns().add(Tabelas.numero("Lidas", RelatorioCarga.ResumoArquivo::linhasLidas, Formatos::inteiro, 80));
        tabelaArquivos.getColumns().add(Tabelas.numero("Aceitas", RelatorioCarga.ResumoArquivo::aceitas, Formatos::inteiro, 80));
        tabelaArquivos.getColumns().add(Tabelas.numero("Rejeitadas", RelatorioCarga.ResumoArquivo::rejeitadas, Formatos::inteiro, 90));
        tabelaArquivos.getColumns().add(Tabelas.coluna("Colunas do cabeçalho", (RelatorioCarga.ResumoArquivo r) -> String.join(", ", r.colunas()), 520));

        tabelaMotivos.getColumns().add(Tabelas.coluna("Motivo", (Map.Entry<String, Long> e) -> e.getKey(), 420));
        tabelaMotivos.getColumns().add(Tabelas.numero("Quantidade", (Map.Entry<String, Long> e) -> e.getValue(), Formatos::inteiro, 110));
        tabelaMotivos.setPlaceholder(new Label("Nenhuma linha rejeitada e nenhum valor ausente — base íntegra."));

        tabelaRejeicoes.getColumns().add(Tabelas.coluna("Arquivo", (RelatorioCarga.Rejeicao r) -> r.arquivo().getFileName().toString(), 200));
        tabelaRejeicoes.getColumns().add(Tabelas.numero("Linha", RelatorioCarga.Rejeicao::linha, String::valueOf, 70));
        tabelaRejeicoes.getColumns().add(Tabelas.coluna("Motivo", RelatorioCarga.Rejeicao::motivo, 260));
        tabelaRejeicoes.getColumns().add(Tabelas.coluna("Conteúdo", RelatorioCarga.Rejeicao::conteudo, 600));
        tabelaRejeicoes.setPlaceholder(new Label("Nenhuma linha rejeitada."));

        lblPlano.setText("""
                1. Encoding: UTF-8 validado byte a byte (decodificação estrita); se inválido, ISO-8859-1. BOM removido e mojibake corrigido.
                2. Separador detectado no cabeçalho (vírgula no INPE; ponto e vírgula se reexportado pelo Excel, com vírgula decimal).
                3. Colunas mapeadas pelo nome (com sinônimos: data_pas / data_hora_gmt / datahora), independentemente da posição.
                4. Obrigatórias: lat, lon, data_pas, municipio, bioma. Linhas sem elas são rejeitadas com o motivo registrado.
                5. Valores -999 (sentinela do INPE), vazios ou negativos em FRP/precipitação/risco/dias sem chuva viram "ausente".
                6. Datas em vários formatos, ano plausível (1998 em diante); coordenadas dentro da caixa envolvente do Brasil.
                7. Municípios em MAIÚSCULAS com espaços normalizados; biomas em "Primeira Maiúscula"; acentos preservados.
                8. Arquivos de 2023 e 2024 unificados; foco_id repetido é descartado (deduplicação).""");

        ctx.baseProperty().addListener((o, a, b) -> atualizar(b));
    }

    private void atualizar(BaseDeFocos b) {
        if (b == null) {
            tabelaArquivos.getItems().clear();
            tabelaMotivos.getItems().clear();
            tabelaRejeicoes.getItems().clear();
            lblResumo.setText("");
            return;
        }
        RelatorioCarga r = b.getRelatorio();
        tabelaArquivos.setItems(FXCollections.observableArrayList(r.getArquivos()));
        List<Map.Entry<String, Long>> motivos = new ArrayList<>();
        r.getMotivos().forEach((k, v) -> motivos.add(Map.entry("Rejeitada: " + k, v)));
        r.getValoresAusentes().forEach((k, v) -> motivos.add(Map.entry("Ausente/-999 na coluna " + k, v)));
        tabelaMotivos.setItems(FXCollections.observableArrayList(motivos));
        tabelaRejeicoes.setItems(FXCollections.observableArrayList(r.getRejeicoes()));
        lblResumo.setText("Total: " + Formatos.inteiro(r.getTotalLidas()) + " linhas lidas, " + Formatos.inteiro(r.getTotalAceitas())
                + " aceitas, " + Formatos.inteiro(r.getTotalRejeitadas()) + " rejeitadas, " + Formatos.inteiro(r.getDuplicadosRemovidos())
                + " duplicadas removidas → " + Formatos.inteiro(b.tamanho()) + " focos na base unificada ("
                + b.dataInicial().format(Formatos.DATA) + " a " + b.dataFinal().format(Formatos.DATA) + ").");
    }
}
