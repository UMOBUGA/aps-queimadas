package br.unip.aps.util;

import java.text.CollationKey;
import java.text.Collator;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Utilitarios de texto: normalizacao de nomes, remocao de acentos e colacao pt-BR. */
public final class Textos {
    public static final Locale PT_BR = Locale.of("pt", "BR");

    private static final Pattern ESPACOS = Pattern.compile("\\s+");
    private static final Pattern MARCAS = Pattern.compile("\\p{M}+");

    private static final ThreadLocal<Collator> COLLATOR = ThreadLocal.withInitial(() -> {
        Collator c = Collator.getInstance(PT_BR);
        c.setStrength(Collator.TERTIARY);
        c.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        return c;
    });

    private Textos() { }

    public static Collator collator() {
        return COLLATOR.get();
    }

    /** Gera a chave de colacao pt-BR de um texto. */
    public static CollationKey chaveColacao(String texto) {
        return texto == null ? null : collator().getCollationKey(texto);
    }

    /** Nome proprio em caixa de titulo ("SAO JOSE DO RIO PRETO" vira "Sao Jose do Rio Preto"), mantendo preposicoes minusculas. */
    public static String nomeProprio(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String p : s.toLowerCase(PT_BR).split(" ")) {
            if (!sb.isEmpty()) sb.append(' ');
            if (java.util.Set.of("de", "da", "do", "das", "dos", "e", "d'oeste").contains(p)) sb.append(p);
            else if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    /** Remove espacos das pontas e colapsa espacos internos repetidos. */
    public static String limpar(String s) {
        if (s == null) return null;
        String t = ESPACOS.matcher(s.strip()).replaceAll(" ");
        return t.isEmpty() ? null : t;
    }

    /** Normaliza nomes de municipio. */
    public static String normalizarMunicipio(String s) {
        String t = limpar(s);
        return t == null ? null : t.toUpperCase(PT_BR);
    }

    /** Normaliza o nome do bioma ("MATA ATLÂNTICA" vira "Mata Atlântica"). */
    public static String normalizarBioma(String s) {
        String t = limpar(s);
        if (t == null) return null;
        StringBuilder sb = new StringBuilder(t.length());
        boolean inicio = true;
        for (char ch : t.toLowerCase(PT_BR).toCharArray()) {
            sb.append(inicio ? Character.toUpperCase(ch) : ch);
            inicio = ch == ' ' || ch == '-';
        }
        return sb.toString();
    }

    /** Remove acentos e converte para minusculas; usado em buscas ("sao jose" encontra "SÃO JOSÉ"). */
    public static String semAcentos(String s) {
        if (s == null) return "";
        String decomposto = Normalizer.normalize(s, Normalizer.Form.NFD);
        return MARCAS.matcher(decomposto).replaceAll("").toLowerCase(PT_BR);
    }

    /** Corrige textos UTF-8 que foram lidos como ISO-8859-1 ("SÃƒO PAULO" -&gt; "SÃO PAULO"). */
    public static String corrigirMojibake(String s) {
        if (s == null || (s.indexOf('Ã') < 0 && s.indexOf('Â') < 0)) return s;
        String corrigido = new String(s.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1),
                java.nio.charset.StandardCharsets.UTF_8);
        return corrigido.indexOf('�') >= 0 ? s : corrigido;
    }
}
