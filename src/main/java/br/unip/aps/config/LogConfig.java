package br.unip.aps.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.LogManager;

/** Configura o java.util.logging (console e arquivo em logs/). */
public final class LogConfig {
    private static volatile boolean inicializado;

    private LogConfig() { }

    /** Carrega a configuracao de log uma unica vez (chamadas seguintes nao fazem nada). */
    public static synchronized void inicializar() {
        if (inicializado || System.getProperty("java.util.logging.config.file") != null) {
            inicializado = true;
            return;
        }
        try {
            Files.createDirectories(Path.of("logs"));
        } catch (IOException e) {
            System.err.println("Aviso: nao foi possivel criar a pasta logs: " + e.getMessage());
        }
        try (InputStream in = LogConfig.class.getResourceAsStream("/logging.properties")) {
            if (in != null) LogManager.getLogManager().readConfiguration(in);
        } catch (IOException e) {
            System.err.println("Aviso: configuracao de log invalida: " + e.getMessage());
        }
        inicializado = true;
    }
}
