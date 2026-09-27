package br.unip.aps.util;

import java.text.CollationKey;
import java.text.Collator;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utilitarios de texto: normalizacao de nomes, remocao de acentos e colacao pt-BR.
 *
 * <p>A ordenacao alfabetica "ingenua" ({@link String#compareTo}) compara pontos de codigo Unicode,
 * o que coloca "ÁGUAS DE LINDOIA" depois de "ZACARIAS" (porque 'Á' = U+00C1 &gt; 'Z' = U+005A).
 * O {@link Collator} do locale pt-BR resolve isso aplicando as regras do idioma.</p>
 */
public final class Textos {

    /** Locale usado em todo o sistema para textos, numeros e colacao. */
    public static final Locale PT_BR = Locale.of("pt", "BR");

    private static final Pattern ESPACOS = Pattern.compile("\\s+");
    private static final Pattern MARCAS = Pattern.compile("\\p{M}+");

    /*
     * Collator nao e thread-safe; cada thread recebe o seu (as ordenacoes do dashboard rodam
     * em threads de background).
     */
    private static final ThreadLocal<Collator> COLLATOR = ThreadLocal.withInitial(() -> {
        Collator c = Collator.getInstance(PT_BR);
        c.setStrength(Collator.TERTIARY);
        c.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        return c;
    });

    private Textos() { }

    /** @return collator pt-BR da thread atual */
    public static Collator collator() {
        return COLLATOR.get();
    }

    /**
     * Gera a chave de colacao pt-BR de um texto.
     *
     * @param texto texto (pode ser {@code null})
     * @return chave comparavel, ou {@code null} se o texto for {@code null}
     */
    public static CollationKey chaveColacao(String texto) {
        return texto == null ? null : collator().getCollationKey(texto);
    }

    /**
     * Remove espacos das pontas e colapsa espacos internos repetidos.
     *
     * @param s texto de entrada
     * @return texto limpo, ou {@code null} se vazio
     */
    public static String limpar(String s) {
        if (s == null) return null;
        String t = ESPACOS.matcher(s.strip()).replaceAll(" ");
        return t.isEmpty() ? null : t;
    }

    /**
     * Normaliza nomes de municipio: limpa espacos e converte para maiusculas (pt-BR), mantendo
     * os acentos (padrao do INPE/IBGE).
     *
     * @param s nome bruto
     * @return nome normalizado, ou {@code null} se vazio
     */
    public static String normalizarMunicipio(String s) {
        String t = limpar(s);
        return t == null ? null : t.toUpperCase(PT_BR);
    }

    /**
     * Normaliza o nome do bioma para "Primeira Letra Maiuscula" preservando acentos
     * ("MATA ATLÂNTICA" e "mata atlântica" viram "Mata Atlântica").
     *
     * @param s nome bruto
     * @return nome normalizado, ou {@code null} se vazio
     */
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

    /**
     * Remove acentos e converte para minusculas; usado em buscas ("sao jose" encontra "SÃO JOSÉ").
     *
     * @param s texto
     * @return texto sem diacriticos, em minusculas ({@code ""} se {@code null})
     */
    public static String semAcentos(String s) {
        if (s == null) return "";
        String decomposto = Normalizer.normalize(s, Normalizer.Form.NFD);
        return MARCAS.matcher(decomposto).replaceAll("").toLowerCase(PT_BR);
    }

    /**
     * Corrige textos UTF-8 que foram lidos como ISO-8859-1 ("SÃƒO PAULO" -&gt; "SÃO PAULO").
     *
     * @param s texto possivelmente com mojibake
     * @return texto corrigido (ou o proprio texto, se nao houver sinais de mojibake)
     */
    public static String corrigirMojibake(String s) {
        if (s == null || (s.indexOf('Ã') < 0 && s.indexOf('Â') < 0)) return s;
        String corrigido = new String(s.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1),
                java.nio.charset.StandardCharsets.UTF_8);
        return corrigido.indexOf('�') >= 0 ? s : corrigido;
    }
}
