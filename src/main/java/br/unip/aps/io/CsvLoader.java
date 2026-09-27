package br.unip.aps.io;

import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.util.Textos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/** Leitura, validacao e limpeza dos CSVs de focos do INPE. */
public final class CsvLoader {
    private static final Logger LOG = Logger.getLogger(CsvLoader.class.getName());

    static final double LAT_MIN = -34.5, LAT_MAX = 6.0, LON_MIN = -74.5, LON_MAX = -28.0;

    static final double SENTINELA = -999.0;

    private static final List<DateTimeFormatter> FORMATOS_DATA = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

    /** Colunas reconhecidas e seus sinonimos. */
    enum Coluna {
        ID_BDQ(false, "id_bdq", "id"),
        FOCO_ID(false, "foco_id", "focoid", "uuid"),
        LATITUDE(true, "lat", "latitude"),
        LONGITUDE(true, "lon", "long", "longitude"),
        DATA_HORA(true, "data_pas", "data_hora_gmt", "datahora", "data_hora", "data"),
        PAIS(false, "pais"),
        ESTADO(false, "estado", "uf"),
        MUNICIPIO(true, "municipio", "cidade"),
        BIOMA(true, "bioma"),
        SATELITE(false, "satelite"),
        DIAS_SEM_CHUVA(false, "numero_dias_sem_chuva", "diasemchuva", "dias_sem_chuva", "numero_dias_sem_chuva"),
        PRECIPITACAO(false, "precipitacao"),
        RISCO_FOGO(false, "risco_fogo", "riscofogo"),
        FRP(false, "frp");

        final boolean obrigatoria;
        final String[] sinonimos;

        Coluna(boolean obrigatoria, String... sinonimos) {
            this.obrigatoria = obrigatoria;
            this.sinonimos = sinonimos;
        }
    }

    /** Carrega e unifica varios arquivos CSV. */
    public BaseDeFocos carregar(List<Path> arquivos) throws DataValidationException {
        if (arquivos == null || arquivos.isEmpty()) {
            throw new DataValidationException("Nenhum arquivo CSV foi informado para carga.");
        }
        RelatorioCarga relatorio = new RelatorioCarga();
        List<FocoIncendio> focos = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        for (Path arquivo : arquivos) {
            lerArquivo(arquivo, relatorio, foco -> {
                String chave = foco.getFocoId() != null ? foco.getFocoId() : "id:" + foco.getIdBdq();
                if (vistos.add(chave)) {
                    focos.add(foco);
                } else {
                    relatorio.duplicadoRemovido();
                }
            });
        }
        if (focos.isEmpty()) {
            throw new DataValidationException("Nenhum registro valido foi encontrado nos arquivos informados. "
                    + "Verifique se sao os CSVs de focos do INPE.");
        }
        LOG.info(() -> "Carga concluida: " + focos.size() + " focos validos\n" + relatorio.resumoTexto());
        return new BaseDeFocos(focos, relatorio, arquivos);
    }

    /** Carrega um unico arquivo. */
    public BaseDeFocos carregar(Path arquivo) throws DataValidationException {
        return carregar(List.of(arquivo));
    }

    private void lerArquivo(Path arquivo, RelatorioCarga relatorio,
                            java.util.function.Consumer<FocoIncendio> destino) throws DataValidationException {
        if (!Files.isRegularFile(arquivo)) {
            throw new DataValidationException("Arquivo de dados nao encontrado: " + arquivo.toAbsolutePath()
                    + "\nBaixe os CSVs do INPE para a pasta data/raw (veja docs/DADOS.md).");
        }
        Charset charset = detectarCharset(arquivo);
        long lidas = 0, aceitas = 0, rejeitadas = 0;
        char separador;
        List<String> nomesColunas;

        try (BufferedReader in = Files.newBufferedReader(arquivo, charset)) {
            String cabecalho = in.readLine();
            if (cabecalho == null || cabecalho.isBlank()) {
                throw new DataValidationException("O arquivo " + arquivo.getFileName() + " esta vazio.");
            }
            if (!cabecalho.isEmpty() && cabecalho.charAt(0) == '﻿') {
                cabecalho = cabecalho.substring(1);
            }
            separador = CsvParser.detectarSeparador(cabecalho);
            CsvParser parser = new CsvParser(separador);
            nomesColunas = parser.dividir(cabecalho);
            Map<Coluna, Integer> indices = mapearColunas(nomesColunas, arquivo);
            boolean virgulaDecimal = separador != ',';

            String linha;
            long numeroLinha = 1;
            while ((linha = in.readLine()) != null) {
                numeroLinha++;
                if (linha.isBlank()) continue;
                lidas++;
                try {
                    FocoIncendio foco = converter(parser.dividir(linha), indices, virgulaDecimal, relatorio);
                    destino.accept(foco);
                    aceitas++;
                } catch (RegistroInvalidoException e) {
                    rejeitadas++;
                    relatorio.rejeitar(arquivo, numeroLinha, e.getMessage(), linha);
                } catch (IllegalArgumentException e) {
                    rejeitadas++;
                    relatorio.rejeitar(arquivo, numeroLinha, "linha mal formada (" + e.getMessage() + ")", linha);
                }
            }
        } catch (NoSuchFileException e) {
            throw new DataValidationException("Arquivo de dados nao encontrado: " + arquivo, e);
        } catch (IOException e) {
            throw new DataValidationException("Falha ao ler o arquivo " + arquivo.getFileName() + ": " + e.getMessage(), e);
        }
        relatorio.adicionarArquivo(new RelatorioCarga.ResumoArquivo(arquivo, charset, separador,
                List.copyOf(nomesColunas), lidas, aceitas, rejeitadas));
        final long l = lidas, a = aceitas;
        LOG.fine(() -> arquivo.getFileName() + ": " + a + "/" + l + " linhas aceitas (" + charset + ")");
    }

    static Charset detectarCharset(Path arquivo) throws DataValidationException {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try (InputStream in = Files.newInputStream(arquivo)) {
            byte[] bytes = in.readAllBytes();
            decoder.decode(ByteBuffer.wrap(bytes));
            return StandardCharsets.UTF_8;
        } catch (CharacterCodingException e) {
            return StandardCharsets.ISO_8859_1;
        } catch (IOException e) {
            throw new DataValidationException("Nao foi possivel ler o arquivo " + arquivo + ": " + e.getMessage(), e);
        }
    }

    private static Map<Coluna, Integer> mapearColunas(List<String> nomes, Path arquivo) throws DataValidationException {
        Map<String, Integer> porNome = new HashMap<>();
        for (int i = 0; i < nomes.size(); i++) {
            porNome.putIfAbsent(Textos.semAcentos(nomes.get(i).strip()).replace(' ', '_'), i);
        }
        Map<Coluna, Integer> indices = new HashMap<>();
        List<String> faltando = new ArrayList<>();
        for (Coluna c : Coluna.values()) {
            for (String s : c.sinonimos) {
                Integer idx = porNome.get(s);
                if (idx != null) {
                    indices.put(c, idx);
                    break;
                }
            }
            if (c.obrigatoria && !indices.containsKey(c)) {
                faltando.add(c.sinonimos[0]);
            }
        }
        if (!faltando.isEmpty()) {
            throw new DataValidationException("O arquivo " + arquivo.getFileName()
                    + " nao possui as colunas obrigatorias " + faltando
                    + ".\nColunas encontradas: " + nomes);
        }
        return indices;
    }

    private static FocoIncendio converter(List<String> campos, Map<Coluna, Integer> idx,
                                          boolean virgulaDecimal, RelatorioCarga rel) throws RegistroInvalidoException {
        String municipio = Textos.normalizarMunicipio(Textos.corrigirMojibake(campo(campos, idx, Coluna.MUNICIPIO)));
        if (municipio == null) throw new RegistroInvalidoException("municipio ausente");
        String bioma = Textos.normalizarBioma(Textos.corrigirMojibake(campo(campos, idx, Coluna.BIOMA)));
        if (bioma == null) throw new RegistroInvalidoException("bioma ausente");

        Double lat = numero(campo(campos, idx, Coluna.LATITUDE), virgulaDecimal);
        Double lon = numero(campo(campos, idx, Coluna.LONGITUDE), virgulaDecimal);
        if (lat == null || lon == null) throw new RegistroInvalidoException("coordenada ausente ou nao numerica");
        if (lat < LAT_MIN || lat > LAT_MAX || lon < LON_MIN || lon > LON_MAX) {
            throw new RegistroInvalidoException("coordenada fora do territorio brasileiro");
        }

        LocalDateTime dataHora = data(campo(campos, idx, Coluna.DATA_HORA));

        FocoIncendio.Builder b = FocoIncendio.builder()
                .municipio(municipio)
                .bioma(bioma)
                .latitude(lat)
                .longitude(lon)
                .dataHora(dataHora)
                .focoId(Textos.limpar(campo(campos, idx, Coluna.FOCO_ID)))
                .estado(Textos.normalizarMunicipio(Textos.corrigirMojibake(campo(campos, idx, Coluna.ESTADO))))
                .satelite(Textos.limpar(campo(campos, idx, Coluna.SATELITE)));
        String pais = Textos.limpar(Textos.corrigirMojibake(campo(campos, idx, Coluna.PAIS)));
        if (pais != null) b.pais(pais);

        String id = Textos.limpar(campo(campos, idx, Coluna.ID_BDQ));
        if (id != null) {
            try {
                b.idBdq(Long.parseLong(id));
            } catch (NumberFormatException e) {
                throw new RegistroInvalidoException("id_bdq nao numerico");
            }
        }

        b.numeroDiasSemChuva(inteiroOpcional(campos, idx, Coluna.DIAS_SEM_CHUVA, virgulaDecimal, rel));
        b.precipitacao(opcional(campos, idx, Coluna.PRECIPITACAO, virgulaDecimal, rel));
        b.riscoFogo(opcional(campos, idx, Coluna.RISCO_FOGO, virgulaDecimal, rel));
        b.frp(opcional(campos, idx, Coluna.FRP, virgulaDecimal, rel));
        return b.build();
    }

    private static String campo(List<String> campos, Map<Coluna, Integer> idx, Coluna c) {
        Integer i = idx.get(c);
        if (i == null) return null;
        if (i >= campos.size()) {
            if (c.obrigatoria) throw new IllegalArgumentException("faltam campos na linha");
            return null;
        }
        return campos.get(i);
    }

    private static Double numero(String s, boolean virgulaDecimal) {
        String t = Textos.limpar(s);
        if (t == null) return null;
        if (virgulaDecimal) t = t.replace(',', '.');
        try {
            double v = Double.parseDouble(t);
            return Double.isFinite(v) ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double opcional(List<String> campos, Map<Coluna, Integer> idx, Coluna c,
                                   boolean virgulaDecimal, RelatorioCarga rel) {
        if (!idx.containsKey(c)) return null;
        Double v = numero(campo(campos, idx, c), virgulaDecimal);
        if (v == null || v <= SENTINELA || v < 0) {
            rel.valorAusente(c.sinonimos[0]);
            return null;
        }
        return v;
    }

    private static Integer inteiroOpcional(List<String> campos, Map<Coluna, Integer> idx, Coluna c,
                                           boolean virgulaDecimal, RelatorioCarga rel) {
        Double v = opcional(campos, idx, c, virgulaDecimal, rel);
        return v == null ? null : (int) Math.round(v);
    }

    private static LocalDateTime data(String s) throws RegistroInvalidoException {
        String t = Textos.limpar(s);
        if (t == null) throw new RegistroInvalidoException("data/hora ausente");
        for (DateTimeFormatter f : FORMATOS_DATA) {
            try {
                LocalDateTime d = LocalDateTime.parse(t, f.withLocale(Locale.ROOT));
                int anoAtual = Year.now().getValue();
                if (d.getYear() < 1998 || d.getYear() > anoAtual + 1) {
                    throw new RegistroInvalidoException("ano fora do intervalo plausivel (1998-" + (anoAtual + 1) + ")");
                }
                return d;
            } catch (DateTimeParseException ignorada) {
            }
        }
        throw new RegistroInvalidoException("data/hora invalida");
    }

    /** Erro de validacao de uma linha especifica (descartada, nao interrompe a carga). */
    static final class RegistroInvalidoException extends Exception {
        RegistroInvalidoException(String motivo) {
            super(motivo, null, false, false);
        }
    }
}
