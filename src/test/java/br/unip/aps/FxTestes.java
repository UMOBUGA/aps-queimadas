package br.unip.aps;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Inicia o toolkit JavaFX uma unica vez por JVM de teste; nao e encerrado, pois o JavaFX nao pode ser reiniciado. */
public final class FxTestes {
    private static boolean iniciado;

    private FxTestes() {
    }

    public static synchronized void iniciar() throws InterruptedException {
        if (iniciado) return;
        CountDownLatch pronto = new CountDownLatch(1);
        try {
            Platform.startup(pronto::countDown);
        } catch (IllegalStateException jaIniciado) {
            pronto.countDown();
        }
        if (!pronto.await(20, TimeUnit.SECONDS)) throw new IllegalStateException("JavaFX nao iniciou");
        Platform.setImplicitExit(false);
        iniciado = true;
    }
}
