package br.unip.aps.cli;

import br.unip.aps.model.FocoIncendio;
import br.unip.aps.util.Formatos;

import java.io.PrintStream;
import java.util.List;

/**
 * Impressao de tabelas de largura fixa no terminal.
 */
public final class TabelaConsole {

    private TabelaConsole() { }

    /**
     * Imprime as primeiras linhas de uma lista de focos.
     *
     * @param out    saida
     * @param focos  focos (ja ordenados)
     * @param limite maximo de linhas exibidas
     */
    public static void focos(PrintStream out, List<FocoIncendio> focos, int limite) {
        String fmt = "%6s  %-16s  %-32s  %-15s  %10s  %10s%n";
        out.printf(fmt, "#", "Data/hora (GMT)", "Municipio", "Bioma", "Latitude", "Longitude");
        out.println("-".repeat(98));
        int n = Math.min(limite, focos.size());
        for (int i = 0; i < n; i++) {
            FocoIncendio f = focos.get(i);
            out.printf(fmt, i + 1, f.getDataHora().format(Formatos.DATA_HORA), cortar(f.getMunicipio(), 32),
                    cortar(f.getBioma(), 15), Formatos.decimal(f.getLatitude(), 5), Formatos.decimal(f.getLongitude(), 5));
        }
        if (focos.size() > n) {
            out.println("... (" + Formatos.inteiro(focos.size() - n) + " linhas omitidas)");
        }
    }

    /**
     * Imprime uma tabela generica.
     *
     * @param out       saida
     * @param cabecalho titulos das colunas
     * @param linhas    linhas (mesmo numero de colunas)
     */
    public static void imprimir(PrintStream out, String[] cabecalho, List<String[]> linhas) {
        int[] larg = new int[cabecalho.length];
        for (int i = 0; i < cabecalho.length; i++) larg[i] = cabecalho[i].length();
        for (String[] l : linhas) {
            for (int i = 0; i < l.length && i < larg.length; i++) larg[i] = Math.max(larg[i], l[i] == null ? 0 : l[i].length());
        }
        StringBuilder sep = new StringBuilder();
        for (int w : larg) sep.append("-".repeat(w)).append("  ");
        linha(out, cabecalho, larg);
        out.println(sep.toString().stripTrailing());
        for (String[] l : linhas) linha(out, l, larg);
    }

    private static void linha(PrintStream out, String[] v, int[] larg) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < larg.length; i++) {
            String s = i < v.length && v[i] != null ? v[i] : "";
            boolean numero = !s.isEmpty() && (Character.isDigit(s.charAt(0)) || s.charAt(0) == '-') && i > 0;
            sb.append(numero ? " ".repeat(larg[i] - s.length()) + s : s + " ".repeat(larg[i] - s.length())).append("  ");
        }
        out.println(sb.toString().stripTrailing());
    }

    private static String cortar(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
