package br.unip.aps.busca;

import br.unip.aps.analysis.Contagem;
import br.unip.aps.estruturas.ArvoreAVL;
import br.unip.aps.estruturas.HeapBinario;
import br.unip.aps.estruturas.TabelaHash;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.sorting.OperationCounter;
import br.unip.aps.sorting.OperationMetrics;
import br.unip.aps.sorting.algorithms.MergeSort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Compara as formas de responder consultas sobre os focos: sequencial, ordenacao + busca binaria, indice AVL e tabela hash. */
public final class ServicoConsultas {
    /** Estrategia de consulta. */
    public enum Metodo {
        LINEAR("Busca sequencial", "nenhum preparo; cada consulta examina os n focos"),
        BINARIA("Ordenar + busca binária", "Merge Sort uma vez; cada consulta faz O(log n) comparações"),
        AVL("Índice AVL", "n inserções balanceadas; busca e intervalo em O(log n + k)"),
        HASH("Tabela hash", "n inserções O(1) esperadas; só responde igualdade, não intervalos");

        private final String rotulo;
        private final String resumo;

        Metodo(String rotulo, String resumo) {
            this.rotulo = rotulo;
            this.resumo = resumo;
        }

        public String rotulo() { return rotulo; }
        public String resumo() { return resumo; }
    }

    /** Custo de responder um lote de consultas com um metodo. */
    public record Custo(Metodo metodo, OperationMetrics construcao, OperationMetrics consultas, int quantidade, long resultados,
                        boolean suportado, String detalhe) {
        public long comparacoesTotais() {
            return construcao.comparacoes() + consultas.comparacoes();
        }

        public double comparacoesPorConsulta() {
            return quantidade == 0 ? 0 : (double) consultas.comparacoes() / quantidade;
        }

        public long nanosTotais() {
            return construcao.nanos() + consultas.nanos();
        }

        /** Numero de consultas a partir do qual o preparo compensa em relacao a busca sequencial. */
        public double pontoDeEquilibrio(Custo linear) {
            double economia = linear.comparacoesPorConsulta() - comparacoesPorConsulta();
            return economia <= 0 ? Double.POSITIVE_INFINITY : construcao.comparacoes() / economia;
        }
    }

    /** Top K por heap x por ordenacao completa. */
    public record TopK(List<Contagem> porHeap, OperationMetrics custoHeap, List<Contagem> porOrdenacao, OperationMetrics custoOrdenacao,
                       OperationMetrics custoContagem, int candidatos) { }

    private static final Comparator<String> TEXTO = Comparator.naturalOrder();
    private static final Comparator<LocalDateTime> DATA = Comparator.naturalOrder();

    /** Consultas de igualdade por municipio. */
    public List<Custo> porMunicipio(List<FocoIncendio> focos, List<String> municipios) {
        FocoIncendio[] base = focos.toArray(new FocoIncendio[0]);
        List<Custo> r = new ArrayList<>();

        OperationCounter q = new OperationCounter();
        long achados = 0;
        q.iniciar();
        for (String m : municipios) achados += Buscas.contarLinearIgual(base, FocoIncendio::getMunicipio, TEXTO, m, q);
        q.parar();
        r.add(new Custo(Metodo.LINEAR, vazio(), q.snapshot(), municipios.size(), achados, true, ""));

        FocoIncendio[] ordenado = base.clone();
        OperationMetrics ord = new MergeSort().ordenar(ordenado, Comparator.comparing(FocoIncendio::getMunicipio));
        q = new OperationCounter();
        achados = 0;
        q.iniciar();
        for (String m : municipios) {
            achados += Buscas.intervaloBinario(ordenado, FocoIncendio::getMunicipio, TEXTO, m, m, q).tamanho();
        }
        q.parar();
        r.add(new Custo(Metodo.BINARIA, ord, q.snapshot(), municipios.size(), achados, true, ""));

        OperationCounter c = new OperationCounter();
        ArvoreAVL<String, FocoIncendio> avl = new ArvoreAVL<>(TEXTO, c);
        c.iniciar();
        for (FocoIncendio f : base) avl.inserir(f.getMunicipio(), f);
        c.parar();
        q = new OperationCounter();
        achados = 0;
        q.iniciar();
        for (String m : municipios) achados += buscarContando(avl, m, q);
        q.parar();
        r.add(new Custo(Metodo.AVL, c.snapshot(), q.snapshot(), municipios.size(), achados, true,
                "altura " + avl.altura() + " · " + avl.rotacoesSimples() + " rotações simples e " + avl.rotacoesDuplas() + " duplas"));

        c = new OperationCounter();
        TabelaHash<String, FocoIncendio> hash = TabelaHash.paraTexto(c);
        c.iniciar();
        for (FocoIncendio f : base) hash.colocar(f.getMunicipio(), f);
        c.parar();
        q = new OperationCounter();
        achados = 0;
        q.iniciar();
        for (String m : municipios) achados += contarContando(hash, m, q);
        q.parar();
        r.add(new Custo(Metodo.HASH, c.snapshot(), q.snapshot(), municipios.size(), achados, true,
                hash.chaves() + " chaves · carga " + String.format("%.2f", hash.fatorCarga()) + " · " + hash.colisoes()
                        + " colisões · maior cadeia " + hash.maiorCadeia()));
        return r;
    }

    private static long buscarContando(ArvoreAVL<String, FocoIncendio> avl, String m, OperationCounter q) {
        OperationCounter original = avl.contador();
        long antes = original.getComparacoes(), leiturasAntes = original.getLeituras();
        long n = avl.buscar(m).size();
        q.somar(new OperationMetrics(original.getComparacoes() - antes, 0, 0, original.getLeituras() - leiturasAntes, 0));
        return n;
    }

    private static long contarContando(TabelaHash<String, FocoIncendio> hash, String m, OperationCounter q) {
        OperationCounter original = hash.contador();
        long antes = original.getComparacoes(), leiturasAntes = original.getLeituras();
        long n = hash.contar(m);
        q.somar(new OperationMetrics(original.getComparacoes() - antes, 0, 0, original.getLeituras() - leiturasAntes, 0));
        return n;
    }

    /** Consultas de intervalo de data/hora [de, ate]. */
    public List<Custo> porIntervalo(List<FocoIncendio> focos, List<LocalDateTime[]> intervalos) {
        FocoIncendio[] base = focos.toArray(new FocoIncendio[0]);
        List<Custo> r = new ArrayList<>();

        OperationCounter q = new OperationCounter();
        long achados = 0;
        q.iniciar();
        for (LocalDateTime[] i : intervalos) achados += Buscas.contarLinear(base, FocoIncendio::getDataHora, DATA, i[0], i[1], q);
        q.parar();
        r.add(new Custo(Metodo.LINEAR, vazio(), q.snapshot(), intervalos.size(), achados, true, ""));

        FocoIncendio[] ordenado = base.clone();
        OperationMetrics ord = new MergeSort().ordenar(ordenado, Comparator.comparing(FocoIncendio::getDataHora));
        q = new OperationCounter();
        achados = 0;
        q.iniciar();
        for (LocalDateTime[] i : intervalos) {
            achados += Buscas.intervaloBinario(ordenado, FocoIncendio::getDataHora, DATA, i[0], i[1], q).tamanho();
        }
        q.parar();
        r.add(new Custo(Metodo.BINARIA, ord, q.snapshot(), intervalos.size(), achados, true, ""));

        OperationCounter c = new OperationCounter();
        ArvoreAVL<LocalDateTime, FocoIncendio> avl = new ArvoreAVL<>(DATA, c);
        c.iniciar();
        for (FocoIncendio f : base) avl.inserir(f.getDataHora(), f);
        c.parar();
        OperationMetrics construcao = c.snapshot();
        long cmp0 = c.getComparacoes(), lei0 = c.getLeituras();
        long[] cont = {0};
        long t0 = System.nanoTime();
        for (LocalDateTime[] i : intervalos) avl.intervalo(i[0], i[1], f -> cont[0]++);
        long t1 = System.nanoTime();
        OperationMetrics consultas = new OperationMetrics(c.getComparacoes() - cmp0, 0, 0, c.getLeituras() - lei0, t1 - t0);
        r.add(new Custo(Metodo.AVL, construcao, consultas, intervalos.size(), cont[0], true,
                "altura " + avl.altura() + " · " + avl.nos() + " datas distintas"));

        r.add(new Custo(Metodo.HASH, vazio(), vazio(), intervalos.size(), 0, false,
                "a tabela hash espalha as chaves e não preserva a ordem: não responde intervalos"));
        return r;
    }

    /** Os k municipios com mais focos: contagem com tabela hash, depois heap de tamanho k x ordenar tudo. */
    public TopK topK(List<FocoIncendio> focos, int k) {
        OperationCounter cont = new OperationCounter();
        TabelaHash<String, FocoIncendio> hash = TabelaHash.paraTexto(cont);
        cont.iniciar();
        for (FocoIncendio f : focos) hash.colocar(f.getMunicipio(), f);
        cont.parar();
        List<Contagem> candidatos = new ArrayList<>();
        hash.paraCada((m, n) -> candidatos.add(new Contagem(m, n)));
        Comparator<Contagem> porTotal = Comparator.comparingLong(Contagem::total).thenComparing(Contagem::chave, Comparator.reverseOrder());

        OperationCounter h = new OperationCounter();
        h.iniciar();
        List<Contagem> porHeap = HeapBinario.topK(candidatos, k, porTotal, h);
        h.parar();

        Contagem[] todos = candidatos.toArray(new Contagem[0]);
        OperationMetrics ord = new MergeSort().ordenar(todos, porTotal.reversed());
        List<Contagem> porOrdenacao = new ArrayList<>();
        for (int i = 0; i < Math.min(k, todos.length); i++) porOrdenacao.add(todos[i]);
        return new TopK(porHeap, h.snapshot(), porOrdenacao, ord, cont.snapshot(), candidatos.size());
    }

    /** Sorteia municipios presentes na base (semente fixa: resultados reproduziveis). */
    public static List<String> sortearMunicipios(List<FocoIncendio> focos, int n, long semente) {
        Random rnd = new Random(semente);
        List<String> r = new ArrayList<>(n);
        for (int i = 0; i < n; i++) r.add(focos.get(rnd.nextInt(focos.size())).getMunicipio());
        return r;
    }

    /** Sorteia intervalos de {@code dias} dias que comecam na data de um foco sorteado. */
    public static List<LocalDateTime[]> sortearIntervalos(List<FocoIncendio> focos, int n, int dias, long semente) {
        Random rnd = new Random(semente);
        List<LocalDateTime[]> r = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            LocalDateTime de = focos.get(rnd.nextInt(focos.size())).getData().atStartOfDay();
            r.add(new LocalDateTime[]{de, de.plusDays(dias).minusNanos(1)});
        }
        return r;
    }

    private static OperationMetrics vazio() {
        return new OperationMetrics(0, 0, 0, 0, 0);
    }
}
