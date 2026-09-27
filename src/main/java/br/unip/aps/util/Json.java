package br.unip.aps.util;

import java.util.Locale;

/** Serializacao JSON minima (strings e numeros) para enviar dados ao mapa Leaflet, sem depender de bibliotecas externas. */
public final class Json {
    private Json() { }

    public static String texto(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder(s.length() + 2).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '<' -> sb.append("\\u003c");
                case ' ' -> sb.append("\\u2028");
                case ' ' -> sb.append("\\u2029");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }

    public static String numero(double v, int casas) {
        if (!Double.isFinite(v)) return "null";
        return String.format(Locale.ROOT, "%." + casas + "f", v);
    }
}
