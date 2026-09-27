package br.unip.aps.io;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser de linhas CSV (RFC 4180) implementado manualmente.
 *
 * <p>Suporta campos entre aspas contendo o separador ou aspas duplicadas ({@code ""}), e
 * separadores configuraveis (virgula no INPE; ponto e virgula em arquivos reexportados pelo
 * Excel brasileiro). Campos multilinha nao sao necessarios nesta base e nao sao suportados.</p>
 */
public final class CsvParser {

    private final char separador;

    /** @param separador caractere separador de campos */
    public CsvParser(char separador) {
        this.separador = separador;
    }

    /** @return separador usado por este parser */
    public char getSeparador() {
        return separador;
    }

    /**
     * Detecta o separador a partir da linha de cabecalho (o caractere mais frequente entre
     * {@code , ; \t |}).
     *
     * @param cabecalho primeira linha do arquivo
     * @return separador detectado (virgula por padrao)
     */
    public static char detectarSeparador(String cabecalho) {
        char[] candidatos = {',', ';', '\t', '|'};
        char melhor = ',';
        int maior = 0;
        for (char c : candidatos) {
            int n = 0;
            for (int i = 0; i < cabecalho.length(); i++) {
                if (cabecalho.charAt(i) == c) n++;
            }
            if (n > maior) {
                maior = n;
                melhor = c;
            }
        }
        return melhor;
    }

    /**
     * Divide uma linha em campos.
     *
     * @param linha linha do arquivo (sem quebra de linha)
     * @return campos, sem as aspas delimitadoras (espacos internos preservados)
     * @throws IllegalArgumentException se houver aspas nao fechadas
     */
    public List<String> dividir(String linha) {
        List<String> campos = new ArrayList<>(16);
        StringBuilder atual = new StringBuilder(32);
        boolean entreAspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (entreAspas) {
                if (c == '"') {
                    if (i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                        atual.append('"');
                        i++;
                    } else {
                        entreAspas = false;
                    }
                } else {
                    atual.append(c);
                }
            } else if (c == '"') {
                entreAspas = true;
            } else if (c == separador) {
                campos.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        if (entreAspas) {
            throw new IllegalArgumentException("aspas nao fechadas");
        }
        campos.add(atual.toString());
        return campos;
    }

    /**
     * Escapa um valor para escrita em CSV (aspas quando necessario).
     *
     * @param valor     valor (pode ser {@code null})
     * @param separador separador em uso
     * @return valor pronto para escrever
     */
    public static String escapar(String valor, char separador) {
        if (valor == null) return "";
        boolean precisa = valor.indexOf(separador) >= 0 || valor.indexOf('"') >= 0
                || valor.indexOf('\n') >= 0 || valor.indexOf('\r') >= 0;
        return precisa ? '"' + valor.replace("\"", "\"\"") + '"' : valor;
    }
}
