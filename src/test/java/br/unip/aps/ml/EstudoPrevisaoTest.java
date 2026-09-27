package br.unip.aps.ml;

import br.unip.aps.geo.MalhaMunicipal;
import br.unip.aps.geo.Vizinhanca;
import br.unip.aps.io.CsvLoader;
import br.unip.aps.model.BaseDeFocos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Estudo de previsao com o historico de SP (satelite de referencia 2019-2024 versionado em data/historico). */
@DisplayName("Estudo de previsão (histórico)")
class EstudoPrevisaoTest {

    @Test
    void vizinhancaPelaMalhaDoIbge() {
        Vizinhanca v = Vizinhanca.da(MalhaMunicipal.sp());
        assertTrue(v.municipiosComFronteira() > 600, "quase todos os municipios tem fronteira detectada");
        assertTrue(v.de("SAO PAULO").contains("GUARULHOS"));
        assertTrue(v.de("GUARULHOS").contains("SAO PAULO"), "vizinhanca simetrica");
        assertTrue(v.mediaVizinhos() > 4 && v.mediaVizinhos() < 8);
    }

    @Test
    void validacaoEmJanelasAblacaoEIntervalo() throws Exception {
        List<Path> ref = new ArrayList<>();
        for (int ano = 2019; ano <= 2024; ano++) {
            Path p = Path.of("data", "historico", "focos_br_sp_ref_" + ano + ".csv");
            if (Files.isRegularFile(p)) ref.add(p);
        }
        assumeTrue(ref.size() == 6, "historico de SP 2019-2024 em data/historico");
        BaseDeFocos base = new CsvLoader().carregar(ref);
        EstudoPrevisao.Resultado r = new EstudoPrevisao(20, 42L, null).executar(base.getFocos(), null, 2024);
        assertEquals(4, r.janelas().size(), "testes 2021, 2022, 2023 e 2024");
        assertEquals(3, r.ablacao().size(), "sem meteorologia: base, sazonal e vizinhos");
        assertEquals(r.variaveis().length, r.importancia().size());
        assertTrue(r.intervalo().coberturaCalibracao() >= 0.89, "cobertura conforme na calibracao ~90%");
        assertEquals(3_612, r.caso().real(), "agosto de 2024 teve 3.612 focos");
        assertEquals(12, r.serie().size());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream o = new ObjectOutputStream(bytes)) {
            o.writeObject(r);
        }
        try (ObjectInputStream i = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            EstudoPrevisao.Resultado lido = (EstudoPrevisao.Resultado) i.readObject();
            assertEquals(r.janelas(), lido.janelas());
        }
    }

    @Test
    void meteorologiaIgnoraValoresInvalidos(@TempDir Path dir) throws Exception {
        Path csv = dir.resolve("focos_sp_todos-sats_2024.csv");
        Files.writeString(csv, String.join("\n",
                "latitude,longitude,data_pas,satelite,pais,estado,municipio,bioma,numero_dias_sem_chuva,precipitacao,risco_fogo,id_area_industrial,frp",
                "-21.1,-47.1,2024-08-02 17:00:00,AQUA_M-T,Brasil,SÃO PAULO,PITANGUEIRAS,Cerrado,30,0,1,0,12.5",
                "-21.1,-47.1,2024-08-03 17:00:00,AQUA_M-T,Brasil,SÃO PAULO,PITANGUEIRAS,Cerrado,-999,-999,-999,0,-999",
                "-21.2,-47.2,2024-08-03 17:00:00,AQUA_M-T,Brasil,SÃO PAULO,SERTÃOZINHO,Cerrado,10,2,0.5,0,3"), StandardCharsets.UTF_8);
        MeteoMensal m = MeteoMensal.ler(List.of(csv));
        assertEquals(3, m.linhas());
        java.time.YearMonth ago = java.time.YearMonth.of(2024, 8);
        assertEquals(30, m.media("PITANGUEIRAS", ago, MeteoMensal.DIAS_SEM_CHUVA), 1e-9);
        assertEquals(2, m.media("PITANGUEIRAS", ago, MeteoMensal.FOCOS), 1e-9);
        assertEquals(20, m.mediaEstado(ago, MeteoMensal.DIAS_SEM_CHUVA), 1e-9);
        assertEquals(20, m.media("ADAMANTINA", ago, MeteoMensal.DIAS_SEM_CHUVA), 1e-9, "sem deteccao: media do estado");
    }
}
