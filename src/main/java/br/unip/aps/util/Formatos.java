package br.unip.aps.util;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

/**
 * Formatacao de numeros, tempos e datas no padrao brasileiro.
 */
public final class Formatos {

    /** Formato de data/hora exibido ao usuario (dd/MM/yyyy HH:mm). */
    public static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Formato de data exibido ao usuario (dd/MM/yyyy). */
    public static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Formatos() { }

    /**
     * @param n numero inteiro
     * @return numero com separador de milhar pt-BR (1.234.567)
     */
    public static String inteiro(long n) {
        return NumberFormat.getIntegerInstance(Textos.PT_BR).format(n);
    }

    /**
     * @param v     valor
     * @param casas casas decimais
     * @return numero decimal pt-BR (1.234,56)
     */
    public static String decimal(double v, int casas) {
        NumberFormat nf = NumberFormat.getNumberInstance(Textos.PT_BR);
        nf.setMinimumFractionDigits(casas);
        nf.setMaximumFractionDigits(casas);
        return nf.format(v);
    }

    /**
     * @param nanos duracao em nanossegundos
     * @return duracao legivel (ex.: "850 µs", "12,345 ms", "3,210 s")
     */
    public static String duracao(long nanos) {
        if (nanos < 1_000_000L) return decimal(nanos / 1_000.0, 1) + " µs";
        if (nanos < 1_000_000_000L) return decimal(nanos / 1_000_000.0, 3) + " ms";
        Duration d = Duration.ofNanos(nanos);
        if (d.toMinutes() < 1) return decimal(nanos / 1_000_000_000.0, 3) + " s";
        return d.toMinutes() + " min " + d.toSecondsPart() + " s";
    }

    /**
     * @param nanos duracao em nanossegundos
     * @return milissegundos como double
     */
    public static double ms(long nanos) {
        return nanos / 1_000_000.0;
    }
}
