package br.unip.aps;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regra do enunciado verificada automaticamente: nenhum codigo de producao pode usar as
 * ordenacoes prontas da plataforma Java. Se alguem do grupo usar Collections.sort "sem querer",
 * o build falha.
 */
@DisplayName("Regras de arquitetura")
class ArquiteturaTest {

    private static final List<Pattern> PROIBIDOS = List.of(
            Pattern.compile("Collections\\s*\\.\\s*sort\\s*\\("),
            Pattern.compile("Arrays\\s*\\.\\s*(sort|parallelSort)\\s*\\("),
            Pattern.compile("\\.\\s*sorted\\s*\\("),
            Pattern.compile("FXCollections\\s*\\.\\s*sort\\s*\\("),
            Pattern.compile("\\bnew\\s+(TreeMap|TreeSet|PriorityQueue)\\b"),
            Pattern.compile("\\b(lista|list|r|l|dados|itens)\\s*\\.\\s*sort\\s*\\("));

    @Test
    @DisplayName("codigo de producao nao usa Collections.sort, Arrays.sort, List.sort, sorted() nem TreeMap/TreeSet")
    void semOrdenacaoPronta() throws IOException {
        List<String> violacoes = new ArrayList<>();
        try (Stream<Path> s = Files.walk(Path.of("src/main/java"))) {
            for (Path p : (Iterable<Path>) s.filter(f -> f.toString().endsWith(".java"))::iterator) {
                List<String> linhas = Files.readAllLines(p, StandardCharsets.UTF_8);
                for (int i = 0; i < linhas.size(); i++) {
                    String l = linhas.get(i).strip();
                    if (l.startsWith("*") || l.startsWith("//") || l.startsWith("/*")) continue;
                    for (Pattern proibido : PROIBIDOS) {
                        if (proibido.matcher(l).find()) violacoes.add(p + ":" + (i + 1) + "  " + l);
                    }
                }
            }
        }
        assertTrue(violacoes.isEmpty(), "Ordenacao pronta encontrada:\n" + String.join("\n", violacoes));
    }
}
