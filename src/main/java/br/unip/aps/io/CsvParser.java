package br.unip.aps.io;

import java.util.ArrayList;
import java.util.List;

/** Parser de linhas CSV (RFC 4180) implementado manualmente. */
public final class CsvParser {
    private final char separador;

    public CsvParser(char separador) {
        this.separador = separador;
    }

    public char getSeparador() {
        return separador;
    }

    /** Detecta o separador a partir da linha de cabecalho (o mais frequente entre virgula, ponto e virgula, tab e barra). */
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

    /** Divide uma linha em campos. */
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

    /** Escapa um valor para escrita em CSV (aspas quando necessario). */
    public static String escapar(String valor, char separador) {
        if (valor == null) return "";
        boolean precisa = valor.indexOf(separador) >= 0 || valor.indexOf('"') >= 0
                || valor.indexOf('\n') >= 0 || valor.indexOf('\r') >= 0;
        return precisa ? '"' + valor.replace("\"", "\"\"") + '"' : valor;
    }
}
