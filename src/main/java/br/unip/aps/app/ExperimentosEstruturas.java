package br.unip.aps.app;

import br.unip.aps.analysis.Contagem;
import br.unip.aps.busca.ServicoConsultas;
import br.unip.aps.estruturas.ArvoreAVL;
import br.unip.aps.estruturas.ExternalMergeSort;
import br.unip.aps.estruturas.MedidorMemoria;
import br.unip.aps.estruturas.MergeSortParalelo;
import br.unip.aps.estruturas.TabelaHash;
import br.unip.aps.io.CsvParser;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.sorting.AlgoritmoTipo;
import br.unip.aps.sorting.CriterioOrdenacao;
import br.unip.aps.sorting.OperationCounter;
import br.unip.aps.util.Formatos;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Executa os experimentos de estruturas de dados sobre dados reais e gera o relatorio em Markdown. */
public final class ExperimentosEstruturas {
    private static final int[] LOTES = {1, 10, 100, 1_000};
    private final Consumer<String> log;
    private final StringBuilder md = new StringBuilder();

    public ExperimentosEstruturas(Consumer<String> log) {
        this.log = log == null ? s -> { } : log;
    }

    /** Roda tudo e devolve o Markdown; arquivos do Brasil sao opcionais (lista vazia = so SP). */
    public String executar(List<FocoIncendio> focos, List<Path> csvsBrasil, int memoriaRegistros, Path temp) throws IOException {
        md.append("# Resultados reais: estruturas de dados e busca\n\n");
        md.append("Gerado em ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .append(" · Java ").append(System.getProperty("java.version")).append(" · ")
                .append(Runtime.getRuntime().availableProcessors()).append(" núcleos lógicos · ")
                .append(System.getProperty("os.name")).append("\n\n");
        md.append("Base: ").append(Formatos.inteiro(focos.size())).append(" focos de SP (2023–2024). Consultas sorteadas com semente fixa 42.\n\n");
        consultasMunicipio(focos);
        consultasIntervalo(focos);
        indices(focos);
        topK(focos);
        memoria(focos);
        paralelo(focos, csvsBrasil);
        espaciais(focos);
        if (!csvsBrasil.isEmpty()) externo(csvsBrasil, memoriaRegistros, temp);
        return md.toString();
    }

    /** So a ordenacao externa, para rodar com heap pequeno (-Xmx) e demonstrar o limite de memoria. */
    public String somenteExterno(List<Path> csvs, int memoria, Path temp) throws IOException {
        long max = Runtime.getRuntime().maxMemory();
        md.append("# External Merge Sort com heap limitado\n\n");
        md.append("JVM com heap máximo de ").append(Formatos.decimal(max / 1048576.0, 0)).append(" MB (-Xmx) · Java ")
                .append(System.getProperty("java.version")).append("\n\n");
        externo(csvs, memoria, temp);
        return md.toString();
    }

    private void consultasMunicipio(List<FocoIncendio> focos) {
        log.accept("Consultas por município");
        md.append("## 1. Consultas por município (igualdade)\n\n");
        md.append("| Consultas | Método | Preparo (comparações) | Comparações por consulta | Total de comparações | Tempo total |\n|---:|---|---:|---:|---:|---:|\n");
        ServicoConsultas s = new ServicoConsultas();
        List<ServicoConsultas.Custo> mil = null;
        for (int n : LOTES) {
            List<ServicoConsultas.Custo> r = s.porMunicipio(focos, ServicoConsultas.sortearMunicipios(focos, n, 42));
            for (ServicoConsultas.Custo c : r) linhaCusto(n, c);
            mil = r;
        }
        equilibrio(mil);
    }

    private void consultasIntervalo(List<FocoIncendio> focos) {
        log.accept("Consultas por intervalo de datas");
        md.append("\n## 2. Consultas por intervalo de datas (janelas de 7 dias)\n\n");
        md.append("| Consultas | Método | Preparo (comparações) | Comparações por consulta | Total de comparações | Tempo total |\n|---:|---|---:|---:|---:|---:|\n");
        ServicoConsultas s = new ServicoConsultas();
        List<ServicoConsultas.Custo> mil = null;
        for (int n : LOTES) {
            List<ServicoConsultas.Custo> r = s.porIntervalo(focos, ServicoConsultas.sortearIntervalos(focos, n, 7, 42));
            for (ServicoConsultas.Custo c : r) linhaCusto(n, c);
            mil = r;
        }
        equilibrio(mil);
    }

    private void linhaCusto(int n, ServicoConsultas.Custo c) {
        if (!c.suportado()) {
            md.append("| ").append(Formatos.inteiro(n)).append(" | ").append(c.metodo().rotulo()).append(" | — | não se aplica | — | — |\n");
            return;
        }
        md.append("| ").append(Formatos.inteiro(n)).append(" | ").append(c.metodo().rotulo()).append(" | ")
                .append(Formatos.inteiro(c.construcao().comparacoes())).append(" | ")
                .append(Formatos.decimal(c.comparacoesPorConsulta(), 1)).append(" | ")
                .append(Formatos.inteiro(c.comparacoesTotais())).append(" | ")
                .append(Formatos.duracao(c.nanosTotais())).append(" |\n");
    }

    private void equilibrio(List<ServicoConsultas.Custo> r) {
        ServicoConsultas.Custo linear = r.get(0);
        md.append("\nPonto de equilíbrio (a partir de quantas consultas o preparo compensa, em comparações):\n\n");
        for (int i = 1; i < r.size(); i++) {
            ServicoConsultas.Custo c = r.get(i);
            if (!c.suportado()) continue;
            md.append("- **").append(c.metodo().rotulo()).append(":** ")
                    .append(Formatos.decimal(c.pontoDeEquilibrio(linear), 1)).append(" consultas");
            if (!c.detalhe().isBlank()) md.append(" (").append(c.detalhe()).append(")");
            md.append("\n");
        }
    }

    private void indices(List<FocoIncendio> focos) {
        log.accept("Índices AVL e hash");
        md.append("\n## 3. Índices construídos sobre a base\n\n");
        OperationCounter k = new OperationCounter();
        ArvoreAVL<LocalDateTime, FocoIncendio> porData = new ArvoreAVL<>(Comparator.naturalOrder(), k);
        for (FocoIncendio f : focos) porData.inserir(f.getDataHora(), f);
        OperationCounter k2 = new OperationCounter();
        ArvoreAVL<String, FocoIncendio> porMunicipio = new ArvoreAVL<>(Comparator.naturalOrder(), k2);
        for (FocoIncendio f : focos) porMunicipio.inserir(f.getMunicipio(), f);
        md.append("| Índice AVL | Chaves distintas | Altura | Limite AVL 1,44·log₂(n+2) | log₂(n) | Rotações simples | Rotações duplas | Comparações na construção |\n|---|---:|---:|---:|---:|---:|---:|---:|\n");
        linhaAvl("Data/hora", porData);
        linhaAvl("Município", porMunicipio);
        TabelaHash<String, FocoIncendio> h = TabelaHash.paraTexto(new OperationCounter());
        for (FocoIncendio f : focos) h.colocar(f.getMunicipio(), f);
        int[] dist = h.distribuicaoCadeias();
        md.append("\n| Tabela hash (município, FNV-1a, encadeamento) | Valor |\n|---|---:|\n");
        md.append("| Chaves (municípios) | ").append(h.chaves()).append(" |\n");
        md.append("| Capacidade final (baldes) | ").append(h.capacidade()).append(" |\n");
        md.append("| Fator de carga | ").append(Formatos.decimal(h.fatorCarga(), 3)).append(" |\n");
        md.append("| Redimensionamentos (16 → ").append(h.capacidade()).append(") | ").append(h.redimensionamentos()).append(" |\n");
        md.append("| Colisões na inserção | ").append(h.colisoes()).append(" |\n");
        md.append("| Maior cadeia | ").append(h.maiorCadeia()).append(" |\n");
        StringBuilder d = new StringBuilder();
        for (int i = 0; i < dist.length; i++) d.append(i == 0 ? "" : " · ").append(i).append(": ").append(dist[i]);
        md.append("| Baldes por comprimento de cadeia | ").append(d).append(" |\n");
    }

    private void linhaAvl(String nome, ArvoreAVL<?, ?> t) {
        double limite = 1.4405 * (Math.log(t.nos() + 2) / Math.log(2)) - 0.3277;
        md.append("| ").append(nome).append(" | ").append(Formatos.inteiro(t.nos())).append(" | ").append(t.altura()).append(" | ")
                .append(Formatos.decimal(limite, 1)).append(" | ").append(Formatos.decimal(Math.log(t.nos()) / Math.log(2), 1)).append(" | ")
                .append(Formatos.inteiro(t.rotacoesSimples())).append(" | ").append(Formatos.inteiro(t.rotacoesDuplas())).append(" | ")
                .append(Formatos.inteiro(t.contador().getComparacoes())).append(" |\n");
    }

    private void topK(List<FocoIncendio> focos) {
        log.accept("Top K");
        md.append("\n## 4. Top K municípios: heap × ordenar tudo\n\n");
        md.append("| K | Candidatos | Comparações (heap de K) | Comparações (Merge Sort de todos) | Mesmo resultado |\n|---:|---:|---:|---:|---|\n");
        ServicoConsultas s = new ServicoConsultas();
        for (int k : new int[]{1, 10, 50}) {
            ServicoConsultas.TopK t = s.topK(focos, k);
            md.append("| ").append(k).append(" | ").append(t.candidatos()).append(" | ")
                    .append(Formatos.inteiro(t.custoHeap().comparacoes())).append(" | ")
                    .append(Formatos.inteiro(t.custoOrdenacao().comparacoes())).append(" | ")
                    .append(t.porHeap().equals(t.porOrdenacao()) ? "sim" : "não").append(" |\n");
        }
        ServicoConsultas.TopK t = s.topK(focos, 10);
        md.append("\nContagem por município com a tabela hash (uma passada): ").append(Formatos.inteiro(t.custoContagem().comparacoes()))
                .append(" comparações para ").append(Formatos.inteiro(focos.size())).append(" focos. Líder: ");
        Contagem c = t.porHeap().get(0);
        md.append(c.chave()).append(" (").append(c.total()).append(" focos).\n");
    }

    private void memoria(List<FocoIncendio> focos) {
        log.accept("Memória por algoritmo");
        md.append("\n## 5. Memória extra por algoritmo (critério data/hora, n = ").append(Formatos.inteiro(focos.size())).append(")\n\n");
        if (!MedidorMemoria.suportado()) {
            md.append("A JVM atual não informa bytes alocados por thread.\n");
            return;
        }
        FocoIncendio[] a = focos.toArray(new FocoIncendio[0]);
        CriterioOrdenacao crit = CriterioOrdenacao.DATA;
        md.append("| Algoritmo | Espaço extra teórico | Bytes alocados (medidos) | Bytes por elemento |\n|---|---|---:|---:|\n");
        var ordem = br.unip.aps.sorting.Ordem.CRESCENTE;
        for (MedidorMemoria.Medicao m : MedidorMemoria.medir(a, crit.comparador(ordem), crit.chaveNumerica(ordem), List.of(AlgoritmoTipo.values()))) {
            md.append("| ").append(m.algoritmo()).append(" | ").append(m.espacoTeorico()).append(" | ")
                    .append(Formatos.inteiro(m.bytesAlocados())).append(" | ").append(Formatos.inteiro(m.bytesPorElemento())).append(" |\n");
        }
        md.append("\nMedição: `ThreadMXBean.getThreadAllocatedBytes` antes e depois de cada ordenação (inclui objetos temporários, não a pilha de recursão).\n");
    }

    private void paralelo(List<FocoIncendio> focos, List<Path> brasil) throws IOException {
        log.accept("Merge Sort paralelo");
        Long[] chaves = brasil.isEmpty() ? chavesSp(focos) : chavesBrasil(brasil);
        int nucleos = Runtime.getRuntime().availableProcessors();
        List<Integer> ts = new ArrayList<>();
        for (int p = 1; p <= nucleos; p *= 2) ts.add(p);
        if (ts.get(ts.size() - 1) != nucleos) ts.add(nucleos);
        int[] threads = ts.stream().mapToInt(Integer::intValue).toArray();
        md.append("\n## 6. Merge Sort paralelo (Fork/Join): speedup e Lei de Amdahl\n\n");
        md.append("Entrada: ").append(Formatos.inteiro(chaves.length)).append(brasil.isEmpty() ? " datas de SP" : " datas/horas dos focos do Brasil")
                .append("; limiar sequencial de ").append(Formatos.inteiro(MergeSortParalelo.LIMIAR_PADRAO))
                .append(" elementos; 2 aquecimentos e 5 repetições por configuração.\n\n");
        md.append("| Threads | Tempo médio | Speedup | Eficiência | Fração serial (Karp–Flatt) | Comparações | Ordenado |\n|---:|---:|---:|---:|---:|---:|---|\n");
        for (MergeSortParalelo.Medicao m : MergeSortParalelo.speedup(chaves, Comparator.naturalOrder(), threads, 2, 5)) {
            md.append("| ").append(m.threads()).append(" | ").append(Formatos.decimal(m.mediaMs(), 1)).append(" ms | ")
                    .append(Formatos.decimal(m.speedup(), 2)).append("× | ").append(Formatos.decimal(100 * m.eficiencia(), 0)).append("% | ")
                    .append(m.threads() == 1 ? "—" : Formatos.decimal(m.fracaoSerial(), 3)).append(" | ")
                    .append(Formatos.inteiro(m.metricas().comparacoes())).append(" | ").append(m.ordenado() ? "sim" : "não").append(" |\n");
        }
    }

    private void espaciais(List<FocoIncendio> focos) {
        log.accept("Trie, arvore k-d e grafo");
        br.unip.aps.busca.ServicoGeografico g = new br.unip.aps.busca.ServicoGeografico();
        md.append("\n## 7. Trie, árvore k-d e grafo de municípios\n\n");
        md.append("### Autocompletar (Trie × testar o prefixo em todos os nomes)\n\n");
        md.append("| Prefixo | Nomes encontrados | Nós visitados na Trie | Caracteres comparados testando todos |\n|---|---:|---:|---:|\n");
        for (String prefixo : List.of("S", "SAO", "SAO J", "SANTA", "RIB")) {
            br.unip.aps.busca.ServicoGeografico.Prefixo r = g.prefixo(focos, prefixo, 1_000);
            md.append("| ").append(prefixo).append(" | ").append(Formatos.inteiro(r.total())).append(" | ")
                    .append(Formatos.inteiro(r.passosTrie())).append(" | ").append(Formatos.inteiro(r.comparacoesLinear())).append(" |\n");
        }
        String lider = new br.unip.aps.analysis.Estatisticas(focos).topMunicipios(1).get(0).chave();
        md.append("\n### Focos num raio a partir do centro de ").append(lider).append(" (árvore k-d × calcular todas as distâncias)\n\n");
        md.append("| Raio | Focos no raio | Distâncias calculadas (árvore) | Distâncias calculadas (todas) | Economia |\n|---:|---:|---:|---:|---:|\n");
        for (int raio : new int[]{10, 25, 50, 100}) {
            br.unip.aps.busca.ServicoGeografico.Raio r = g.raio(focos, lider, raio);
            md.append("| ").append(raio).append(" km | ").append(Formatos.inteiro(r.focos().size())).append(" | ")
                    .append(Formatos.inteiro(r.distanciasArvore())).append(" | ").append(Formatos.inteiro(r.distanciasLinear())).append(" | ")
                    .append(Formatos.decimal(100.0 * (1 - (double) r.distanciasArvore() / r.distanciasLinear()), 1)).append("% |\n");
        }
        br.unip.aps.busca.ServicoGeografico.Propagacao p = g.propagacao(focos);
        md.append("\n### O fogo pula para o vizinho? (grafo de fronteiras do IBGE + busca em largura)\n\n");
        md.append("Grafo com ").append(Formatos.inteiro(p.municipiosNoGrafo())).append(" municípios e ").append(Formatos.inteiro(p.arestas()))
                .append(" fronteiras. Caso = município sem focos num mês; verifica-se se ele teve focos no mês seguinte, separando quem tinha")
                .append(" um vizinho com focos no mês do caso.\n\n");
        md.append("| Situação no mês | Casos | Com focos no mês seguinte | Taxa |\n|---|---:|---:|---:|\n");
        md.append("| Algum vizinho com focos | ").append(Formatos.inteiro(p.casosComVizinho())).append(" | ").append(Formatos.inteiro(p.novosComVizinho()))
                .append(" | ").append(Formatos.decimal(100 * p.taxaComVizinho(), 1)).append("% |\n");
        md.append("| Nenhum vizinho com focos | ").append(Formatos.inteiro(p.casosSemVizinho())).append(" | ").append(Formatos.inteiro(p.novosSemVizinho()))
                .append(" | ").append(Formatos.decimal(100 * p.taxaSemVizinho(), 1)).append("% |\n\n");
        md.append("Razão entre as taxas: **").append(Double.isNaN(p.razao()) ? "—" : Formatos.decimal(p.razao(), 2) + "×")
                .append("**. É uma associação, não prova de causa: municípios vizinhos também dividem clima, relevo e uso do solo.")
                .append(" Focos sem município na malha: ").append(Formatos.inteiro(p.focosSemMunicipio())).append(".\n");
    }

    private static Long[] chavesSp(List<FocoIncendio> focos) {
        Long[] r = new Long[focos.size()];
        for (int i = 0; i < r.length; i++) r[i] = focos.get(i).getDataHora().toEpochSecond(ZoneOffset.UTC);
        return r;
    }

    private static Long[] chavesBrasil(List<Path> csvs) throws IOException {
        List<Long> r = new ArrayList<>();
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (Path csv : csvs) {
            try (BufferedReader in = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
                String cab = in.readLine();
                if (cab == null) continue;
                if (cab.charAt(0) == '﻿') cab = cab.substring(1);
                CsvParser p = new CsvParser(CsvParser.detectarSeparador(cab));
                List<String> nomes = p.dividir(cab);
                int col = -1;
                for (int i = 0; i < nomes.size(); i++) {
                    String n = nomes.get(i).strip().toLowerCase(Locale.ROOT);
                    if (n.equals("data_pas") || n.equals("data_hora_gmt")) col = i;
                }
                if (col < 0) continue;
                String linha;
                while ((linha = in.readLine()) != null) {
                    List<String> c = p.dividir(linha);
                    if (col >= c.size()) continue;
                    String v = c.get(col).strip();
                    if (v.length() >= 19) {
                        try {
                            r.add(LocalDateTime.parse(v.substring(0, 19).replace('T', ' '), f).toEpochSecond(ZoneOffset.UTC));
                        } catch (RuntimeException ignorada) {
                            continue;
                        }
                    }
                }
            }
        }
        return r.toArray(new Long[0]);
    }

    private void externo(List<Path> csvs, int memoria, Path temp) throws IOException {
        log.accept("External Merge Sort do Brasil");
        md.append("\n## 8. External Merge Sort: focos do Brasil com memória limitada\n\n");
        Path saida = temp.resolve("brasil-ordenado.csv");
        ExternalMergeSort.Resultado r = new ExternalMergeSort().ordenar(csvs, saida, memoria, log);
        md.append("| Medida | Valor |\n|---|---:|\n");
        md.append("| Arquivos de entrada | ").append(r.arquivos()).append(" (").append(Formatos.decimal(r.bytesEntrada() / 1048576.0, 1)).append(" MB) |\n");
        md.append("| Registros ordenados | ").append(Formatos.inteiro(r.registros())).append(" |\n");
        md.append("| Limite de memória | ").append(Formatos.inteiro(r.registrosPorRun())).append(" registros por run |\n");
        md.append("| Runs gravados em disco | ").append(r.runs()).append(" |\n");
        md.append("| Comparações na fase 1 (Merge Sort de cada run) | ").append(Formatos.inteiro(r.fase1().comparacoes())).append(" |\n");
        md.append("| Comparações na fase 2 (intercalação ").append(r.runs()).append("-way com heap) | ").append(Formatos.inteiro(r.fase2().comparacoes())).append(" |\n");
        md.append("| Tempo da fase 1 | ").append(Formatos.duracao(r.nanosFase1())).append(" |\n");
        md.append("| Tempo da fase 2 | ").append(Formatos.duracao(r.nanosFase2())).append(" |\n");
        md.append("| Pico de heap da JVM observado | ").append(Formatos.decimal(r.memoriaPicoBytes() / 1048576.0, 0)).append(" MB |\n");
        md.append("| Saída verificada em ordem | ").append(r.ordenado() ? "sim" : "não").append(" |\n");
        Files.deleteIfExists(saida);
    }
}
