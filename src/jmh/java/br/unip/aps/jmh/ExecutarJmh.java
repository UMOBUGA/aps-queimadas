package br.unip.aps.jmh;

import br.unip.aps.sorting.AlgoritmoTipo;
import br.unip.aps.util.Formatos;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/** Roda o JMH e compara com o benchmark proprio (docs/resultados/benchmark.csv), gravando docs/resultados/jmh.md. */
public final class ExecutarJmh {
    private static final double T_95_GL4 = 2.776;

    private ExecutarJmh() {
    }

    public static void main(String[] args) throws Exception {
        Options op = new OptionsBuilder().include(OrdenacaoJmh.class.getSimpleName()).build();
        Collection<RunResult> resultados = new Runner(op).run();
        List<String> proprio = Files.readAllLines(Path.of("docs", "resultados", "benchmark.csv"), StandardCharsets.UTF_8);
        StringBuilder md = new StringBuilder("# Validação do benchmark com JMH\n\n");
        md.append("Critério data/hora, cenário aleatório (semente 42), n = 10.378. JMH 1.37: 2 forks × (5 aquecimentos + 10 medições de 1 s);")
                .append(" o erro é o intervalo de confiança de 99,9% calculado pelo JMH. Benchmark próprio: 2 aquecimentos + 5 repetições;")
                .append(" IC de 95% = média ± t(0,975; 4) · s/√5 com t = 2,776.\n\n");
        md.append("Java ").append(System.getProperty("java.version")).append(" · ").append(Runtime.getRuntime().availableProcessors())
                .append(" núcleos lógicos · ").append(System.getProperty("os.name")).append("\n\n");
        md.append("| Algoritmo | JMH (ms, IC 99,9%) | Benchmark próprio (ms, IC 95%) | Diferença das médias | Intervalos se sobrepõem |\n");
        md.append("|---|---:|---:|---:|---|\n");
        for (RunResult r : resultados) {
            String alg = r.getParams().getParam("algoritmo");
            double m = r.getPrimaryResult().getScore(), e = r.getPrimaryResult().getScoreError();
            String nome = AlgoritmoTipo.valueOf(alg).nome();
            double[] p = proprio(proprio, nome);
            md.append("| ").append(nome).append(" | ").append(Formatos.decimal(m, 3)).append(" ± ").append(Formatos.decimal(e, 3)).append(" | ");
            if (p == null) {
                md.append("— | — | — |\n");
                continue;
            }
            double ic = T_95_GL4 * p[1] / Math.sqrt(5);
            boolean sobrepoe = m - e <= p[0] + ic && p[0] - ic <= m + e;
            md.append(Formatos.decimal(p[0], 3)).append(" ± ").append(Formatos.decimal(ic, 3)).append(" | ")
                    .append(Formatos.decimal(100 * (p[0] - m) / m, 1)).append("% | ").append(sobrepoe ? "sim" : "não").append(" |\n");
        }
        Path destino = Path.of("docs", "resultados", "jmh.md");
        Files.writeString(destino, md.toString(), StandardCharsets.UTF_8);
        System.out.println(md);
        System.out.println("Gravado em " + destino.toAbsolutePath());
    }

    private static double[] proprio(List<String> linhas, String algoritmo) {
        for (String l : linhas) {
            String[] c = l.split(";");
            if (c.length > 6 && c[0].equals(algoritmo) && c[1].equals("Data/hora") && c[2].equals("Aleatória") && c[3].equals("10378")) {
                return new double[]{dec(c[5]), dec(c[6])};
            }
        }
        return null;
    }

    private static double dec(String s) {
        return Double.parseDouble(s.replace(".", "").replace(',', '.'));
    }
}
