package br.unip.aps.config;

/** Versao do sistema, lida do manifesto do JAR (Implementation-Version). */
public final class Versao {
    public static final String PADRAO = "2.0.0";

    private Versao() { }

    /** Versao empacotada, ou a versao padrao quando executado fora do JAR (IDE, testes). */
    public static String atual() {
        String v = Versao.class.getPackage().getImplementationVersion();
        return v == null || v.isBlank() ? PADRAO : v;
    }
}
