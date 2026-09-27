package br.unip.aps.ui.componentes;

/**
 * Ponto unico de feedback nao bloqueante (toasts). A janela principal instala o destino; os
 * componentes (ex.: {@link ChartCard} ao exportar PNG) apenas chamam os metodos estaticos.
 */
public final class Feedback {

    /** Destino das mensagens. */
    public interface Destino {
        /**
         * @param tipo   tipo da mensagem
         * @param titulo titulo curto
         * @param texto  detalhe (pode ser {@code null})
         */
        void mostrar(Toast.Tipo tipo, String titulo, String texto);
    }

    private static volatile Destino destino = (t, a, b) -> { };

    private Feedback() { }

    /** @param d destino (normalmente o container de toasts da janela principal) */
    public static void instalar(Destino d) {
        destino = d == null ? (t, a, b) -> { } : d;
    }

    /**
     * @param titulo titulo
     * @param texto  detalhe
     */
    public static void sucesso(String titulo, String texto) {
        destino.mostrar(Toast.Tipo.SUCESSO, titulo, texto);
    }

    /**
     * @param titulo titulo
     * @param texto  detalhe
     */
    public static void erro(String titulo, String texto) {
        destino.mostrar(Toast.Tipo.ERRO, titulo, texto);
    }

    /**
     * @param titulo titulo
     * @param texto  detalhe
     */
    public static void info(String titulo, String texto) {
        destino.mostrar(Toast.Tipo.INFO, titulo, texto);
    }

    /**
     * @param titulo titulo
     * @param texto  detalhe
     */
    public static void alerta(String titulo, String texto) {
        destino.mostrar(Toast.Tipo.ALERTA, titulo, texto);
    }
}
