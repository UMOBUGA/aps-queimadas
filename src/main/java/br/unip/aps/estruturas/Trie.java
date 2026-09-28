package br.unip.aps.estruturas;

import br.unip.aps.sorting.OperationCounter;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/** Arvore de prefixos (Trie): acha todos os nomes que comecam com um prefixo examinando so os caracteres do prefixo. */
public final class Trie<V> {
    private static final int ALFABETO = 40;

    private static final class No<V> {
        @SuppressWarnings("unchecked")
        private final No<V>[] filhos = (No<V>[]) new No[ALFABETO];
        private String nome;
        private final Vetor<V> valores = new Vetor<>(1);
        private int abaixo;
    }

    private final No<V> raiz = new No<>();
    private final OperationCounter contador;
    private int chaves;
    private int nos = 1;

    public Trie(OperationCounter contador) {
        this.contador = contador;
    }

    /** Chave normalizada: maiusculas sem acentos, como nos arquivos do INPE. */
    public static String normalizar(String s) {
        String t = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return t.toUpperCase(Locale.ROOT).strip();
    }

    private static int indice(char c) {
        if (c >= 'A' && c <= 'Z') return c - 'A';
        if (c >= '0' && c <= '9') return 26 + c - '0';
        if (c == ' ') return 36;
        if (c == '\'') return 37;
        if (c == '-') return 38;
        return 39;
    }

    /** Insere o nome (exibido como veio) associado a um valor. */
    public void inserir(String nome, V valor) {
        String chave = normalizar(nome);
        No<V> no = raiz;
        no.abaixo++;
        for (int i = 0; i < chave.length(); i++) {
            int k = indice(chave.charAt(i));
            contador.leitura();
            if (no.filhos[k] == null) {
                no.filhos[k] = new No<>();
                nos++;
            }
            no = no.filhos[k];
            no.abaixo++;
        }
        if (no.nome == null) {
            no.nome = nome;
            chaves++;
        }
        no.valores.adicionar(valor);
        contador.atribuicao();
    }

    private No<V> descer(String prefixo) {
        String chave = normalizar(prefixo);
        No<V> no = raiz;
        for (int i = 0; i < chave.length() && no != null; i++) {
            contador.leitura();
            no = no.filhos[indice(chave.charAt(i))];
        }
        return no;
    }

    /** Valores da chave exata (vazio se nao existir). */
    public List<V> buscar(String nome) {
        No<V> no = descer(nome);
        return no == null || no.nome == null ? List.of() : no.valores.paraLista();
    }

    /** Quantas chaves comecam com o prefixo (sem percorrer a subarvore). */
    public int contarPrefixo(String prefixo) {
        No<V> no = descer(prefixo);
        return no == null ? 0 : no.abaixo;
    }

    /** Ate {@code limite} nomes que comecam com o prefixo, em ordem alfabetica (percurso em profundidade). */
    public List<String> comPrefixo(String prefixo, int limite) {
        Vetor<String> r = new Vetor<>();
        No<V> no = descer(prefixo);
        if (no != null) coletar(no, r, limite);
        return r.paraLista();
    }

    private void coletar(No<V> no, Vetor<String> r, int limite) {
        if (r.tamanho() >= limite) return;
        if (no.nome != null) r.adicionar(no.nome);
        for (No<V> f : no.filhos) {
            if (f == null) continue;
            contador.leitura();
            coletar(f, r, limite);
            if (r.tamanho() >= limite) return;
        }
    }

    public int chaves() {
        return chaves;
    }

    public int nos() {
        return nos;
    }
}
