package br.unip.aps.io;

import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.sorting.Ordenacoes;
import br.unip.aps.util.Textos;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Localiza os CSVs de focos no diretorio de dados e delega a carga ao {@link CsvLoader}. */
public final class RepositorioDados {
    private final Path diretorio;
    private final CsvLoader loader = new CsvLoader();
    private boolean usouEmbarcados;

    public RepositorioDados(Path diretorio) {
        this.diretorio = diretorio;
    }

    public Path getDiretorio() {
        return diretorio;
    }

    /** Lista os CSVs de focos (nome contendo "focos" e extensao .csv), em ordem alfabetica. */
    public List<Path> localizarArquivos() throws DataValidationException {
        List<Path> r = new ArrayList<>();
        if (!Files.isDirectory(diretorio)) return r;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(diretorio, "*.{csv,CSV}")) {
            for (Path p : ds) {
                if (Textos.semAcentos(p.getFileName().toString()).contains("focos")) r.add(p);
            }
        } catch (IOException e) {
            throw new DataValidationException("Nao foi possivel listar a pasta " + diretorio.toAbsolutePath(), e);
        }
        return Ordenacoes.ordenar(r, Comparator.comparing(p -> p.getFileName().toString()));
    }

    /** Carrega todos os CSVs de focos da pasta de dados; sem a pasta, usa a base de demonstracao embarcada no JAR. */
    public BaseDeFocos carregarTodos() throws DataValidationException {
        List<Path> arquivos = localizarArquivos();
        usouEmbarcados = false;
        if (arquivos.isEmpty()) {
            List<Path> embarcados = extrairEmbarcados();
            if (!embarcados.isEmpty()) {
                usouEmbarcados = true;
                return loader.carregar(embarcados);
            }
            throw new DataValidationException("Nenhum CSV de focos encontrado em " + diretorio.toAbsolutePath()
                    + ".\nBaixe focos_br_sp_ref_2023.zip e focos_br_sp_ref_2024.zip do INPE, extraia na pasta "
                    + "data/raw ou use a opcao 'Baixar dados do INPE'.");
        }
        return loader.carregar(arquivos);
    }

    /** Carrega arquivos especificos escolhidos pelo usuario. */
    public BaseDeFocos carregar(List<Path> arquivos) throws DataValidationException {
        usouEmbarcados = false;
        return loader.carregar(arquivos);
    }

    /** Indica se a ultima carga completa veio da base de demonstracao embarcada (SP 2023/2024). */
    public boolean usouEmbarcados() {
        return usouEmbarcados;
    }

    /** Copia os CSVs embarcados em {@code /dados} para uma pasta temporaria e devolve os caminhos. */
    public static List<Path> extrairEmbarcados() throws DataValidationException {
        List<Path> r = new ArrayList<>();
        try (java.io.InputStream indice = RepositorioDados.class.getResourceAsStream("/dados/indice.txt")) {
            if (indice == null) return r;
            Path destino = Files.createTempDirectory("aps-queimadas-dados");
            for (String nome : new String(indice.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).split("\\R")) {
                if (nome.isBlank()) continue;
                try (java.io.InputStream in = RepositorioDados.class.getResourceAsStream("/dados/" + nome.strip())) {
                    if (in == null) continue;
                    Path alvo = destino.resolve(nome.strip());
                    Files.copy(in, alvo);
                    alvo.toFile().deleteOnExit();
                    r.add(alvo);
                }
            }
            destino.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new DataValidationException("Nao foi possivel ler a base de demonstracao embarcada", e);
        }
        return r;
    }
}
