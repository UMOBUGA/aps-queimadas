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

/**
 * Localiza os CSVs de focos no diretorio de dados e delega a carga ao {@link CsvLoader}.
 */
public final class RepositorioDados {

    private final Path diretorio;
    private final CsvLoader loader = new CsvLoader();

    /** @param diretorio pasta com os CSVs (ex.: data/raw) */
    public RepositorioDados(Path diretorio) {
        this.diretorio = diretorio;
    }

    /** @return pasta de dados */
    public Path getDiretorio() {
        return diretorio;
    }

    /**
     * Lista os CSVs de focos (nome contendo "focos" e extensao .csv), em ordem alfabetica.
     *
     * @return arquivos encontrados (possivelmente vazio)
     * @throws DataValidationException se a pasta nao puder ser lida
     */
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

    /**
     * Carrega todos os CSVs de focos da pasta de dados.
     *
     * @return base unificada
     * @throws DataValidationException se nao houver arquivos ou se a carga falhar
     */
    public BaseDeFocos carregarTodos() throws DataValidationException {
        List<Path> arquivos = localizarArquivos();
        if (arquivos.isEmpty()) {
            throw new DataValidationException("Nenhum CSV de focos encontrado em " + diretorio.toAbsolutePath()
                    + ".\nBaixe focos_br_sp_ref_2023.zip e focos_br_sp_ref_2024.zip do INPE, extraia na pasta "
                    + "data/raw ou use a opcao 'Baixar dados do INPE'.");
        }
        return loader.carregar(arquivos);
    }

    /**
     * Carrega arquivos especificos escolhidos pelo usuario.
     *
     * @param arquivos CSVs
     * @return base unificada
     * @throws DataValidationException se a carga falhar
     */
    public BaseDeFocos carregar(List<Path> arquivos) throws DataValidationException {
        return loader.carregar(arquivos);
    }
}
