package br.unip.aps;

import javafx.application.Platform;

/** Inicia o toolkit JavaFX (pelo TestFX, compartilhado com os testes de interacao) uma unica vez por JVM de teste. */
public final class FxTestes {
    private static boolean iniciado;

    private FxTestes() {
    }

    public static synchronized void iniciar() throws Exception {
        if (iniciado) return;
        org.testfx.api.FxToolkit.registerPrimaryStage();
        Platform.setImplicitExit(false);
        iniciado = true;
    }
}
