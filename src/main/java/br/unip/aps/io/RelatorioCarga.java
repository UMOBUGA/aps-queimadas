package br.unip.aps.io;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Relatorio de qualidade da carga de dados (data quality report). */
public final class RelatorioCarga {
    public static final int MAX_DETALHES = 500;

    /** Detalhe de uma linha rejeitada. */
    public record Rejeicao(Path arquivo, long linha, String motivo, String conteudo) { }

    /** Resumo por arquivo lido. */
    public record ResumoArquivo(Path arquivo, Charset encoding, char separador,
                                List<String> colunas, long linhasLidas, long aceitas, long rejeitadas) { }

    private final List<ResumoArquivo> arquivos = new ArrayList<>();
    private final List<Rejeicao> rejeicoes = new ArrayList<>();
    private final Map<String, Long> motivos = new LinkedHashMap<>();
    private final Map<String, Long> valoresAusentes = new LinkedHashMap<>();
    private long totalRejeitadas;
    private long duplicadosRemovidos;

    void adicionarArquivo(ResumoArquivo r) {
        arquivos.add(r);
    }

    void rejeitar(Path arquivo, long linha, String motivo, String conteudo) {
        totalRejeitadas++;
        motivos.merge(motivo, 1L, Long::sum);
        if (rejeicoes.size() < MAX_DETALHES) {
            rejeicoes.add(new Rejeicao(arquivo, linha, motivo, conteudo));
        }
    }

    void valorAusente(String coluna) {
        valoresAusentes.merge(coluna, 1L, Long::sum);
    }

    void duplicadoRemovido() {
        duplicadosRemovidos++;
    }

    public List<ResumoArquivo> getArquivos() { return Collections.unmodifiableList(arquivos); }

    public List<Rejeicao> getRejeicoes() { return Collections.unmodifiableList(rejeicoes); }

    public Map<String, Long> getMotivos() { return Collections.unmodifiableMap(motivos); }

    public Map<String, Long> getValoresAusentes() { return Collections.unmodifiableMap(valoresAusentes); }

    public long getTotalRejeitadas() { return totalRejeitadas; }

    public long getDuplicadosRemovidos() { return duplicadosRemovidos; }

    public long getTotalLidas() {
        long t = 0;
        for (ResumoArquivo a : arquivos) t += a.linhasLidas();
        return t;
    }

    public long getTotalAceitas() {
        long t = 0;
        for (ResumoArquivo a : arquivos) t += a.aceitas();
        return t;
    }

    public String resumoTexto() {
        StringBuilder sb = new StringBuilder();
        for (ResumoArquivo a : arquivos) {
            sb.append(String.format("  %-28s %s, sep='%s'  lidas=%d aceitas=%d rejeitadas=%d%n",
                    a.arquivo().getFileName(), a.encoding().name(),
                    a.separador() == '\t' ? "\\t" : String.valueOf(a.separador()),
                    a.linhasLidas(), a.aceitas(), a.rejeitadas()));
        }
        sb.append(String.format("  Total: lidas=%d aceitas=%d rejeitadas=%d duplicados removidos=%d%n",
                getTotalLidas(), getTotalAceitas(), totalRejeitadas, duplicadosRemovidos));
        motivos.forEach((m, n) -> sb.append("    rejeicao: ").append(m).append(" -> ").append(n).append('\n'));
        valoresAusentes.forEach((c, n) -> sb.append("    ausente/-999 em ").append(c).append(" -> ").append(n).append('\n'));
        return sb.toString();
    }
}
