package br.unip.aps.util;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

/** Formatacao de numeros, tempos e datas no padrao brasileiro. */
public final class Formatos {
    public static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Formatos() { }

    public static String inteiro(long n) {
        return NumberFormat.getIntegerInstance(Textos.PT_BR).format(n);
    }

    /** Bytes em B, KB ou MB (base 1024), no formato pt-BR. */
    public static String bytes(long b) {
        if (b < 1024) return inteiro(b) + " B";
        if (b < 1024L * 1024) return decimal(b / 1024.0, 1) + " KB";
        return decimal(b / (1024.0 * 1024), 1) + " MB";
    }

    public static String decimal(double v, int casas) {
        NumberFormat nf = NumberFormat.getNumberInstance(Textos.PT_BR);
        nf.setMinimumFractionDigits(casas);
        nf.setMaximumFractionDigits(casas);
        return nf.format(v);
    }

    public static String duracao(long nanos) {
        if (nanos < 1_000_000L) return decimal(nanos / 1_000.0, 1) + " µs";
        if (nanos < 1_000_000_000L) return decimal(nanos / 1_000_000.0, 3) + " ms";
        Duration d = Duration.ofNanos(nanos);
        if (d.toMinutes() < 1) return decimal(nanos / 1_000_000_000.0, 3) + " s";
        return d.toMinutes() + " min " + d.toSecondsPart() + " s";
    }

    public static double ms(long nanos) {
        return nanos / 1_000_000.0;
    }
}
