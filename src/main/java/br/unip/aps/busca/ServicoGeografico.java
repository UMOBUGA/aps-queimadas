package br.unip.aps.busca;

import br.unip.aps.estruturas.ArvoreKD;
import br.unip.aps.estruturas.Grafo;
import br.unip.aps.estruturas.TabelaHash;
import br.unip.aps.estruturas.Trie;
import br.unip.aps.estruturas.Vetor;
import br.unip.aps.geo.MalhaMunicipal;
import br.unip.aps.geo.Vizinhanca;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.sorting.OperationCounter;

import java.util.List;
import java.util.Set;

/** Consultas espaciais e por nome sobre os focos: Trie (prefixo), arvore k-d (raio) e grafo de vizinhos (propagacao). */
public final class ServicoGeografico {
    /** Resultado do autocompletar: nomes encontrados e custo da Trie contra a varredura de todos os nomes. */
    public record Prefixo(String prefixo, List<String> nomes, int total, long passosTrie, long comparacoesLinear, int nomesDistintos) { }

    /** Focos a ate R km de um ponto, com o custo da arvore k-d contra o calculo de todas as distancias. */
    public record Raio(String municipio, double raioKm, List<FocoIncendio> focos, long distanciasArvore, long distanciasLinear,
                       long nanosArvore, long nanosLinear, int alturaArvore) { }

    /** Municipios a d fronteiras de distancia de uma origem (busca em largura) e seus focos. */
    public record Camada(int distancia, int municipios, int comFocos, long focos) { }

    /** Os focos "pulam" para o vizinho? Compara o surgimento de focos com e sem vizinho em chamas no mes anterior. */
    public record Propagacao(int mesesComparados, long casosComVizinho, long novosComVizinho, long casosSemVizinho,
                             long novosSemVizinho, int municipiosNoGrafo, int arestas, long focosSemMunicipio) {
        public double taxaComVizinho() {
            return casosComVizinho == 0 ? 0 : (double) novosComVizinho / casosComVizinho;
        }

        public double taxaSemVizinho() {
            return casosSemVizinho == 0 ? 0 : (double) novosSemVizinho / casosSemVizinho;
        }

        /** Quantas vezes mais provavel e o surgimento de focos quando um vizinho queimou no mes anterior. */
        public double razao() {
            return taxaSemVizinho() == 0 ? Double.NaN : taxaComVizinho() / taxaSemVizinho();
        }
    }

    private Grafo grafo;

    /** Autocompletar por prefixo com a Trie, comparado com testar o prefixo em todos os nomes. */
    public Prefixo prefixo(List<FocoIncendio> focos, String prefixo, int limite) {
        OperationCounter montagem = new OperationCounter();
        Trie<String> trie = new Trie<>(montagem);
        Vetor<String> distintos = new Vetor<>();
        TabelaHash<String, Boolean> vistos = TabelaHash.paraTexto(new OperationCounter());
        for (FocoIncendio f : focos) {
            if (!vistos.buscar(f.getMunicipio()).isEmpty()) continue;
            vistos.colocar(f.getMunicipio(), true);
            distintos.adicionar(f.getMunicipio());
            trie.inserir(f.getMunicipio(), f.getMunicipio());
        }
        String alvo = Trie.normalizar(prefixo);
        long linear = 0;
        for (int i = 0; i < distintos.tamanho(); i++) {
            String nome = Trie.normalizar(distintos.obter(i));
            int k = 0;
            while (k < alvo.length() && k < nome.length()) {
                linear++;
                if (nome.charAt(k) != alvo.charAt(k)) break;
                k++;
            }
            if (k < alvo.length() && k == nome.length()) linear++;
        }
        List<String> nomes = trie.comPrefixo(prefixo, limite);
        long antes = montagem.getLeituras();
        int total = trie.contarPrefixo(prefixo);
        long passos = montagem.getLeituras() - antes;
        return new Prefixo(prefixo, nomes, total, passos, linear, distintos.tamanho());
    }

    /** Focos a ate {@code raioKm} do centro do municipio, pela arvore k-d e pela varredura completa. */
    public Raio raio(List<FocoIncendio> focos, String municipio, double raioKm) {
        MalhaMunicipal.Municipio m = MalhaMunicipal.sp().buscar(municipio);
        if (m == null) throw new IllegalArgumentException("Município não encontrado na malha do IBGE: " + municipio);
        ArvoreKD<FocoIncendio> arvore = new ArvoreKD<>(focos, FocoIncendio::getLatitude, FocoIncendio::getLongitude, new OperationCounter());
        Vetor<FocoIncendio> achados = new Vetor<>();
        long t0 = System.nanoTime();
        long pelaArvore = arvore.noRaio(m.latitude(), m.longitude(), raioKm, achados::adicionar);
        long nanosArvore = System.nanoTime() - t0;
        long t1 = System.nanoTime();
        long conferidos = 0;
        for (FocoIncendio f : focos) {
            if (ArvoreKD.distanciaKm(m.latitude(), m.longitude(), f.getLatitude(), f.getLongitude()) <= raioKm) conferidos++;
        }
        long nanosLinear = System.nanoTime() - t1;
        if (conferidos != achados.tamanho()) {
            throw new IllegalStateException("Árvore k-d e varredura divergiram: " + achados.tamanho() + " × " + conferidos);
        }
        return new Raio(m.nome(), raioKm, achados.paraLista(), pelaArvore, focos.size(), nanosArvore, nanosLinear, arvore.altura());
    }

    /** Grafo dos municipios de SP ligados por fronteira (malha do IBGE), montado uma vez. */
    public synchronized Grafo grafo() {
        if (grafo == null) {
            MalhaMunicipal malha = MalhaMunicipal.sp();
            Vizinhanca viz = Vizinhanca.da(malha);
            Grafo g = new Grafo(new OperationCounter());
            for (MalhaMunicipal.Municipio m : malha.municipios()) {
                g.vertice(m.chave());
                Set<String> vs = viz.de(m.chave());
                for (String v : vs) g.ligar(m.chave(), v);
            }
            grafo = g;
        }
        return grafo;
    }

    /** Camadas de vizinhanca (0 = o proprio municipio, 1 = vizinhos, 2 = vizinhos dos vizinhos...). */
    public List<Camada> camadas(List<FocoIncendio> focos, String municipio, int maxDistancia) {
        Grafo g = grafo();
        int origem = g.indiceDe(MalhaMunicipal.chave(municipio));
        if (origem < 0) throw new IllegalArgumentException("Município não encontrado na malha do IBGE: " + municipio);
        int[] dist = g.distancias(origem);
        long[] focosPorVertice = new long[g.vertices()];
        for (FocoIncendio f : focos) {
            int v = g.indiceDe(MalhaMunicipal.chave(f.getMunicipio()));
            if (v >= 0) focosPorVertice[v]++;
        }
        Vetor<Camada> r = new Vetor<>();
        for (int d = 0; d <= maxDistancia; d++) {
            int municipios = 0, comFocos = 0;
            long soma = 0;
            for (int v = 0; v < dist.length; v++) {
                if (dist[v] != d) continue;
                municipios++;
                if (focosPorVertice[v] > 0) comFocos++;
                soma += focosPorVertice[v];
            }
            r.adicionar(new Camada(d, municipios, comFocos, soma));
        }
        return r.paraLista();
    }

    /** Mes a mes: entre municipios sem focos, com que frequencia surgem focos quando um vizinho queimou no mes anterior. */
    public Propagacao propagacao(List<FocoIncendio> focos) {
        Grafo g = grafo();
        int anoMin = Integer.MAX_VALUE, anoMax = Integer.MIN_VALUE;
        for (FocoIncendio f : focos) {
            anoMin = Math.min(anoMin, f.getAno());
            anoMax = Math.max(anoMax, f.getAno());
        }
        if (focos.isEmpty()) return new Propagacao(0, 0, 0, 0, 0, g.vertices(), g.arestas(), 0);
        int meses = (anoMax - anoMin + 1) * 12;
        boolean[][] queimou = new boolean[meses][g.vertices()];
        long semMunicipio = 0;
        for (FocoIncendio f : focos) {
            int v = g.indiceDe(MalhaMunicipal.chave(f.getMunicipio()));
            if (v < 0) {
                semMunicipio++;
                continue;
            }
            queimou[(f.getAno() - anoMin) * 12 + f.getMes() - 1][v] = true;
        }
        long casosCom = 0, novosCom = 0, casosSem = 0, novosSem = 0;
        for (int m = 0; m + 1 < meses; m++) {
            for (int v = 0; v < g.vertices(); v++) {
                if (queimou[m][v]) continue;
                boolean vizinhoQueimou = false;
                for (String w : g.vizinhosDe(g.nome(v))) {
                    if (queimou[m][g.indiceDe(w)]) {
                        vizinhoQueimou = true;
                        break;
                    }
                }
                if (vizinhoQueimou) {
                    casosCom++;
                    if (queimou[m + 1][v]) novosCom++;
                } else {
                    casosSem++;
                    if (queimou[m + 1][v]) novosSem++;
                }
            }
        }
        return new Propagacao(meses - 1, casosCom, novosCom, casosSem, novosSem, g.vertices(), g.arestas(), semMunicipio);
    }
}
