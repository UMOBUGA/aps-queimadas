package br.unip.aps.jmh;

import br.unip.aps.io.CsvLoader;
import br.unip.aps.io.RepositorioDados;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.sorting.AlgoritmoTipo;
import br.unip.aps.sorting.CenarioEntrada;
import br.unip.aps.sorting.CriterioOrdenacao;
import br.unip.aps.sorting.Ordem;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.ToLongFunction;

/** Microbenchmark JMH dos algoritmos O(n log n) sobre a base real (data/hora, aleatorio, semente 42), com o Arrays.sort do Java como referencia. */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(2)
public class OrdenacaoJmh {
    @Param({"SHELL", "MERGE", "QUICK", "QUICK_3WAY", "QUICK_2PIVOS", "INTRO", "HEAP", "TIM", "RADIX", "JAVA"})
    public String algoritmo;

    static final String JAVA = "JAVA";

    private FocoIncendio[] entrada;
    private Comparator<FocoIncendio> comparador;
    private ToLongFunction<FocoIncendio> chave;
    private AlgoritmoTipo tipo;

    @Setup
    public void preparar() throws Exception {
        List<FocoIncendio> base = new CsvLoader().carregar(RepositorioDados.extrairEmbarcados()).getFocos();
        CriterioOrdenacao c = CriterioOrdenacao.DATA;
        comparador = c.comparador(Ordem.CRESCENTE);
        chave = c.chaveNumerica(Ordem.CRESCENTE);
        entrada = CenarioEntrada.ALEATORIO.preparar(base, comparador, 42L).toArray(new FocoIncendio[0]);
        tipo = JAVA.equals(algoritmo) ? null : AlgoritmoTipo.valueOf(algoritmo);
    }

    @Benchmark
    public Object ordenar() {
        FocoIncendio[] copia = entrada.clone();
        if (tipo == null) {
            Arrays.sort(copia, comparador);
            return copia;
        }
        return tipo.criar().ordenar(copia, comparador, chave);
    }
}
