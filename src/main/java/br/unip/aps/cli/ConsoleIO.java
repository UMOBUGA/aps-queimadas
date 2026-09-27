package br.unip.aps.cli;

import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

/**
 * Leitura validada de opcoes no terminal: entradas invalidas geram nova pergunta, nunca uma
 * excecao para o usuario.
 */
final class ConsoleIO {

    private final Scanner in;
    private final PrintStream out;

    ConsoleIO(Scanner in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    PrintStream out() {
        return out;
    }

    String texto(String pergunta, String padrao) {
        out.print(pergunta + (padrao != null ? " [" + padrao + "]" : "") + ": ");
        out.flush();
        if (!in.hasNextLine()) return padrao;
        String s = in.nextLine().strip();
        return s.isEmpty() ? padrao : s;
    }

    int inteiro(String pergunta, int min, int max, int padrao) {
        while (true) {
            String s = texto(pergunta + " (" + min + "-" + max + ")", String.valueOf(padrao));
            if (s == null) return padrao;
            try {
                int v = Integer.parseInt(s.replace(".", "").replace("_", ""));
                if (v >= min && v <= max) return v;
            } catch (NumberFormatException ignorada) {
                // pergunta novamente
            }
            out.println("  Valor invalido. Digite um numero entre " + min + " e " + max + ".");
        }
    }

    <T> T escolher(String titulo, List<T> opcoes, int padrao) {
        out.println(titulo);
        for (int i = 0; i < opcoes.size(); i++) out.printf("  %2d) %s%n", i + 1, opcoes.get(i));
        return opcoes.get(inteiro("Opcao", 1, opcoes.size(), padrao + 1) - 1);
    }

    boolean simNao(String pergunta, boolean padrao) {
        String s = texto(pergunta + " (s/n)", padrao ? "s" : "n");
        return s != null && s.toLowerCase().startsWith("s");
    }

    void pausar() {
        texto("Pressione ENTER para continuar", null);
    }
}
