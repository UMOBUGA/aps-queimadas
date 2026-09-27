package br.unip.aps.ui.componentes;

/** Ponto unico de feedback nao bloqueante (toasts). */
public final class Feedback {
    /** Destino das mensagens. */
    public interface Destino {
        void mostrar(Toast.Tipo tipo, String titulo, String texto);
    }

    private static volatile Destino destino = (t, a, b) -> { };

    private Feedback() { }

    public static void instalar(Destino d) {
        destino = d == null ? (t, a, b) -> { } : d;
    }

    public static void sucesso(String titulo, String texto) {
        destino.mostrar(Toast.Tipo.SUCESSO, titulo, texto);
    }

    public static void erro(String titulo, String texto) {
        destino.mostrar(Toast.Tipo.ERRO, titulo, texto);
    }

    public static void info(String titulo, String texto) {
        destino.mostrar(Toast.Tipo.INFO, titulo, texto);
    }

    public static void alerta(String titulo, String texto) {
        destino.mostrar(Toast.Tipo.ALERTA, titulo, texto);
    }
}
