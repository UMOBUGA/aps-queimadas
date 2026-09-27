package br.unip.aps.ml;

import br.unip.aps.geo.MalhaMunicipal;
import br.unip.aps.io.CsvParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Medias mensais por municipio das variaveis meteorologicas dos focos de todos os satelites do INPE. */
public final class MeteoMensal {
    /** Indices das variaveis acumuladas. */
    public static final int DIAS_SEM_CHUVA = 0, PRECIPITACAO = 1, RISCO = 2, FRP = 3, FOCOS = 4;

    private final Map<String, double[]> municipio = new HashMap<>();
    private final Map<YearMonth, double[]> estado = new HashMap<>();
    private long linhas;

    /** Le os CSVs de todos os satelites (ja filtrados para o estado). Valores negativos (-999) sao ignorados. */
    public static MeteoMensal ler(List<Path> csvs) throws IOException {
        MeteoMensal m = new MeteoMensal();
        for (Path csv : csvs) {
            try (BufferedReader in = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
                String cab = in.readLine();
                if (cab == null) continue;
                if (!cab.isEmpty() && cab.charAt(0) == '﻿') cab = cab.substring(1);
                CsvParser p = new CsvParser(CsvParser.detectarSeparador(cab));
                List<String> nomes = p.dividir(cab);
                int cData = col(nomes, "data_hora_gmt", "data_pas"), cMun = col(nomes, "municipio"),
                        cDsc = col(nomes, "numero_dias_sem_chuva"), cPrec = col(nomes, "precipitacao"),
                        cRisco = col(nomes, "risco_fogo"), cFrp = col(nomes, "frp");
                String linha;
                while ((linha = in.readLine()) != null) {
                    List<String> c = p.dividir(linha);
                    if (c.size() <= Math.max(cData, cMun)) continue;
                    YearMonth ym;
                    try {
                        ym = YearMonth.parse(c.get(cData).strip().substring(0, 7));
                    } catch (RuntimeException e) {
                        continue;
                    }
                    String chave = MalhaMunicipal.chave(c.get(cMun)) + "|" + ym;
                    double[] v = m.municipio.computeIfAbsent(chave, k -> new double[10]);
                    double[] e = m.estado.computeIfAbsent(ym, k -> new double[10]);
                    acumular(v, e, DIAS_SEM_CHUVA, valor(c, cDsc));
                    acumular(v, e, PRECIPITACAO, valor(c, cPrec));
                    acumular(v, e, RISCO, valor(c, cRisco));
                    acumular(v, e, FRP, valor(c, cFrp));
                    v[2 * FOCOS]++;
                    e[2 * FOCOS]++;
                    m.linhas++;
                }
            }
        }
        return m;
    }

    private static int col(List<String> nomes, String... alternativas) {
        for (String a : alternativas) {
            for (int i = 0; i < nomes.size(); i++) if (nomes.get(i).strip().toLowerCase(Locale.ROOT).equals(a)) return i;
        }
        return -1;
    }

    private static double valor(List<String> c, int i) {
        if (i < 0 || i >= c.size()) return Double.NaN;
        String s = c.get(i).strip();
        if (s.isEmpty()) return Double.NaN;
        try {
            double v = Double.parseDouble(s.replace(',', '.'));
            return v < 0 ? Double.NaN : v;
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static void acumular(double[] mun, double[] est, int var, double v) {
        if (Double.isNaN(v)) return;
        mun[2 * var] += v;
        mun[2 * var + 1]++;
        est[2 * var] += v;
        est[2 * var + 1]++;
    }

    /** Media da variavel no municipio e mes; se nao houve deteccao, usa a media do estado; sem dados, 0. */
    public double media(String chaveMunicipio, YearMonth ym, int var) {
        double[] v = municipio.get(chaveMunicipio + "|" + ym);
        if (var == FOCOS) return v == null ? 0 : v[2 * FOCOS];
        if (v != null && v[2 * var + 1] > 0) return v[2 * var] / v[2 * var + 1];
        return mediaEstado(ym, var);
    }

    public double mediaEstado(YearMonth ym, int var) {
        double[] e = estado.get(ym);
        if (e == null) return 0;
        if (var == FOCOS) return e[2 * FOCOS];
        return e[2 * var + 1] > 0 ? e[2 * var] / e[2 * var + 1] : 0;
    }

    public long linhas() {
        return linhas;
    }

    public boolean vazio() {
        return linhas == 0;
    }
}
