package br.unip.aps.analysis;

import br.unip.aps.model.FocoIncendio;
import br.unip.aps.sorting.Ordenacoes;
import br.unip.aps.util.Textos;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Agregacoes e indicadores sobre um conjunto de focos (graficos do dashboard e relatorios). */
public final class Estatisticas {
    public static final String[] MESES = {"jan", "fev", "mar", "abr", "mai", "jun",
            "jul", "ago", "set", "out", "nov", "dez"};

    private static final Comparator<Contagem> POR_TOTAL_DESC = (a, b) -> {
        int c = Long.compare(b.total(), a.total());
        return c != 0 ? c : Textos.collator().compare(a.chave(), b.chave());
    };

    private final List<FocoIncendio> focos;

    public Estatisticas(List<FocoIncendio> focos) {
        this.focos = focos;
    }

    public int total() {
        return focos.size();
    }

    public Map<Integer, Long> porAno() {
        Map<Integer, Long> m = new HashMap<>();
        for (FocoIncendio f : focos) m.merge(f.getAno(), 1L, Long::sum);
        Map<Integer, Long> r = new LinkedHashMap<>();
        for (Integer ano : Ordenacoes.ordenar(new ArrayList<>(m.keySet()), Comparator.naturalOrder())) {
            r.put(ano, m.get(ano));
        }
        return r;
    }

    public long[] porMes(int ano) {
        long[] v = new long[12];
        for (FocoIncendio f : focos) if (f.getAno() == ano) v[f.getMes() - 1]++;
        return v;
    }

    public Map<YearMonth, Long> serieMensal() {
        Map<YearMonth, Long> m = new HashMap<>();
        YearMonth min = null, max = null;
        for (FocoIncendio f : focos) {
            YearMonth ym = YearMonth.from(f.getDataHora());
            m.merge(ym, 1L, Long::sum);
            if (min == null || ym.isBefore(min)) min = ym;
            if (max == null || ym.isAfter(max)) max = ym;
        }
        Map<YearMonth, Long> r = new LinkedHashMap<>();
        if (min == null) return r;
        for (YearMonth ym = min; !ym.isAfter(max); ym = ym.plusMonths(1)) {
            r.put(ym, m.getOrDefault(ym, 0L));
        }
        return r;
    }

    public List<Contagem> porBioma() {
        Map<String, Long> m = new HashMap<>();
        for (FocoIncendio f : focos) m.merge(f.getBioma(), 1L, Long::sum);
        return ranking(m, Integer.MAX_VALUE);
    }

    public List<Contagem> topMunicipios(int n) {
        Map<String, Long> m = new HashMap<>();
        for (FocoIncendio f : focos) m.merge(f.getMunicipio(), 1L, Long::sum);
        return ranking(m, n);
    }

    public long[] porHoraLocal() {
        long[] v = new long[24];
        for (FocoIncendio f : focos) v[f.getDataHora().minusHours(3).getHour()]++;
        return v;
    }

    public int municipiosAfetados() {
        Set<String> s = new HashSet<>();
        for (FocoIncendio f : focos) s.add(f.getMunicipio());
        return s.size();
    }

    /** Linha do comparativo mensal entre dois anos. */
    public record LinhaComparativo(int mes, long anoA, long anoB, double variacao) { }

    public List<LinhaComparativo> comparativo(int anoA, int anoB) {
        long[] a = porMes(anoA);
        long[] b = porMes(anoB);
        List<LinhaComparativo> r = new ArrayList<>();
        long ta = 0, tb = 0;
        for (int i = 0; i < 12; i++) {
            r.add(new LinhaComparativo(i + 1, a[i], b[i], variacao(a[i], b[i])));
            ta += a[i];
            tb += b[i];
        }
        r.add(new LinhaComparativo(0, ta, tb, variacao(ta, tb)));
        return r;
    }

    public Map.Entry<YearMonth, Long> mesPico() {
        Map.Entry<YearMonth, Long> pico = null;
        for (Map.Entry<YearMonth, Long> e : serieMensal().entrySet()) {
            if (pico == null || e.getValue() > pico.getValue()) pico = e;
        }
        return pico;
    }

    private static double variacao(long a, long b) {
        return a == 0 ? Double.NaN : (b - a) * 100.0 / a;
    }

    private static List<Contagem> ranking(Map<String, Long> m, int n) {
        List<Contagem> lista = new ArrayList<>();
        m.forEach((k, v) -> lista.add(new Contagem(k, v)));
        Ordenacoes.ordenar(lista, POR_TOTAL_DESC);
        return new ArrayList<>(lista.subList(0, Math.min(n, lista.size())));
    }
}
