package br.unip.aps.io;

import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.model.FocoIncendio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Leitura e limpeza do CSV")
class CsvLoaderTest {
    private static final String CABECALHO = "id_bdq,foco_id,lat,lon,data_pas,pais,estado,municipio,bioma";

    @TempDir
    Path dir;

    private Path escrever(String nome, String conteudo, java.nio.charset.Charset cs) throws IOException {
        Path p = dir.resolve(nome);
        Files.writeString(p, conteudo, cs);
        return p;
    }

    @Test
    @DisplayName("le o formato real do INPE (espacos, acentos, UTF-8 com BOM)")
    void formatoInpe() throws Exception {
        Path p = escrever("focos.csv", "﻿" + CABECALHO + "\n"
                + " 1615033254 ,6aeb30de,  -20.939350 ,  -49.125500 ,2023-01-02 17:09:00,Brasil,SÃO PAULO,UCHOA,Mata Atlântica\n"
                + " 1616089573 ,ab959e9e,  -21.069480 ,  -50.599550 ,2023-01-11 17:00:00,Brasil,SÃO PAULO,ARAÇATUBA,Mata Atlântica\n",
                StandardCharsets.UTF_8);
        BaseDeFocos b = new CsvLoader().carregar(p);
        assertEquals(2, b.tamanho());
        FocoIncendio f = b.getFocos().get(1);
        assertEquals("ARAÇATUBA", f.getMunicipio());
        assertEquals("Mata Atlântica", f.getBioma());
        assertEquals(1616089573L, f.getIdBdq());
        assertEquals(-21.06948, f.getLatitude(), 1e-9);
        assertEquals(2023, f.getAno());
        assertEquals(StandardCharsets.UTF_8, b.getRelatorio().getArquivos().get(0).encoding());
    }

    @Test
    @DisplayName("detecta Latin-1, ponto e virgula, virgula decimal e colunas fora de ordem")
    void latin1PontoEVirgula() throws Exception {
        String conteudo = "municipio;bioma;data_hora_gmt;latitude;longitude;frp;precipitacao\n"
                + "\"SÃO JOSÉ DO RIO PRETO\";CERRADO;02/01/2023 17:09;-20,8;-49,3;12,5;-999\n";
        Path p = escrever("latin.csv", conteudo, StandardCharsets.ISO_8859_1);
        BaseDeFocos b = new CsvLoader().carregar(p);
        FocoIncendio f = b.getFocos().get(0);
        assertEquals("SÃO JOSÉ DO RIO PRETO", f.getMunicipio());
        assertEquals("Cerrado", f.getBioma());
        assertEquals(12.5, f.getFrp(), 1e-9);
        assertNull(f.getPrecipitacao(), "-999 vira ausente");
        assertEquals(1L, b.getRelatorio().getValoresAusentes().get("precipitacao"));
        assertEquals(StandardCharsets.ISO_8859_1, b.getRelatorio().getArquivos().get(0).encoding());
        assertEquals(';', b.getRelatorio().getArquivos().get(0).separador());
    }

    @Test
    @DisplayName("linhas invalidas sao rejeitadas com motivo, sem interromper a carga")
    void linhasInvalidas() throws Exception {
        Path p = escrever("sujo.csv", CABECALHO + "\n"
                + "1,a,-20.1,-49.1,2023-01-02 17:09:00,Brasil,SP,ITU,Cerrado\n"
                + "2,b,-20.1,-49.1,data-ruim,Brasil,SP,ITU,Cerrado\n"
                + "3,c,-20.1,-49.1,2023-01-02 17:09:00,Brasil,SP,,Cerrado\n"
                + "4,d,55.0,-49.1,2023-01-02 17:09:00,Brasil,SP,ITU,Cerrado\n"
                + "5,e,abc,-49.1,2023-01-02 17:09:00,Brasil,SP,ITU,Cerrado\n"
                + "6,f,-20.1,-49.1,1970-01-02 17:09:00,Brasil,SP,ITU,Cerrado\n"
                + "\n"
                + "7,g,-20.1\n", StandardCharsets.UTF_8);
        BaseDeFocos b = new CsvLoader().carregar(p);
        RelatorioCarga r = b.getRelatorio();
        assertEquals(1, b.tamanho());
        assertEquals(6, r.getTotalRejeitadas());
        assertTrue(r.getMotivos().containsKey("data/hora invalida"));
        assertTrue(r.getMotivos().containsKey("municipio ausente"));
        assertTrue(r.getMotivos().containsKey("coordenada fora do territorio brasileiro"));
        assertEquals(3, r.getRejeicoes().get(0).linha(), "numero da linha no arquivo (cabecalho = 1)");
    }

    @Test
    @DisplayName("unifica arquivos e remove foco_id duplicado")
    void unificacaoEDeduplicacao() throws Exception {
        String l1 = "1,dup,-20.1,-49.1,2023-01-02 17:09:00,Brasil,SP,ITU,Cerrado\n";
        Path a = escrever("a.csv", CABECALHO + "\n" + l1, StandardCharsets.UTF_8);
        Path b = escrever("b.csv", CABECALHO + "\n" + l1 + "2,novo,-20.2,-49.2,2024-05-02 17:09:00,Brasil,SP,ITU,Cerrado\n",
                StandardCharsets.UTF_8);
        BaseDeFocos base = new CsvLoader().carregar(List.of(a, b));
        assertEquals(2, base.tamanho());
        assertEquals(1, base.getRelatorio().getDuplicadosRemovidos());
        assertEquals(List.of(2023, 2024), base.anos());
    }

    @Test
    @DisplayName("erros amigaveis: arquivo ausente, coluna faltando, arquivo vazio")
    void errosAmigaveis() throws Exception {
        DataValidationException e1 = assertThrows(DataValidationException.class,
                () -> new CsvLoader().carregar(dir.resolve("nao-existe.csv")));
        assertTrue(e1.getMessage().contains("nao encontrado"));

        Path semBioma = escrever("sem.csv", "lat,lon,data_pas,municipio\n-20,-49,2023-01-01 10:00:00,ITU\n", StandardCharsets.UTF_8);
        DataValidationException e2 = assertThrows(DataValidationException.class, () -> new CsvLoader().carregar(semBioma));
        assertTrue(e2.getMessage().contains("bioma"), e2.getMessage());

        Path vazio = escrever("vazio.csv", "", StandardCharsets.UTF_8);
        assertThrows(DataValidationException.class, () -> new CsvLoader().carregar(vazio));
    }

    @Test
    @DisplayName("parser CSV respeita aspas e aspas duplicadas")
    void parser() {
        CsvParser p = new CsvParser(',');
        assertEquals(List.of("a", "b,c", "d\"e", ""), p.dividir("a,\"b,c\",\"d\"\"e\","));
        assertThrows(IllegalArgumentException.class, () -> p.dividir("a,\"b"));
        assertEquals(';', CsvParser.detectarSeparador("a;b;c,d"));
        assertEquals("\"x;y\"", CsvParser.escapar("x;y", ';'));
    }
}
