package br.unip.aps.geo;

import br.unip.aps.analysis.Contagem;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.sorting.Ordenacoes;
import br.unip.aps.util.Textos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Ficha de um municipio: focos por ano e por mes, bioma, posicao no ranking do estado e vizinhos com focos. */
public record PerfilMunicipio(String nome, String nomeInpe, long total, List<Contagem> porAno, List<Mes> porMes,
                              List<Contagem> biomas, int posicao, int municipiosComFocos, double areaKm2,
                              double latitude, double longitude, List<Contagem> vizinhosComFocos, int vizinhos) {

    /** Focos de um mes do periodo. */
    public record Mes(int ano, int mes, long focos) { }

    private static final Comparator<Contagem> MAIOR_PRIMEIRO = Comparator.comparingLong(Contagem::total).reversed()
            .thenComparing(Contagem::chave, Textos.collator());

    private static Vizinhanca vizinhanca;

    public PerfilMunicipio {
        porAno = List.copyOf(porAno);
        porMes = List.copyOf(porMes);
        biomas = List.copyOf(biomas);
        vizinhosComFocos = List.copyOf(vizinhosComFocos);
    }

    private record Estado(Map<String, List<FocoIncendio>> porChave, Map<String, Long> contagem, int anoMin, int anoMax) {
        static Estado de(List<FocoIncendio> focos) {
            Map<String, List<FocoIncendio>> grupos = new LinkedHashMap<>();
            Map<String, Long> contagem = new HashMap<>();
            int anoMin = Integer.MAX_VALUE, anoMax = Integer.MIN_VALUE;
            for (FocoIncendio f : focos) {
                anoMin = Math.min(anoMin, f.getAno());
                anoMax = Math.max(anoMax, f.getAno());
                String k = MalhaMunicipal.chave(f.getMunicipio());
                grupos.computeIfAbsent(k, x -> new ArrayList<>()).add(f);
                contagem.merge(k, 1L, Long::sum);
            }
            return new Estado(grupos, contagem, anoMin, anoMax);
        }
    }

    /** Monta a ficha a partir dos focos; o nome pode vir do INPE ou do IBGE (caixa e acentos sao ignorados). */
    public static PerfilMunicipio de(List<FocoIncendio> focos, String municipio) {
        return montar(Estado.de(focos), MalhaMunicipal.chave(municipio), municipio);
    }

    /** Fichas de todos os municipios com focos, do que mais queimou para o que menos queimou. */
    public static List<PerfilMunicipio> todos(List<FocoIncendio> focos) {
        Estado e = Estado.de(focos);
        List<PerfilMunicipio> r = new ArrayList<>();
        for (String chave : e.porChave().keySet()) r.add(montar(e, chave, chave));
        Ordenacoes.ordenar(r, Comparator.comparingLong(PerfilMunicipio::total).reversed()
                .thenComparing(PerfilMunicipio::nome, Textos.collator()));
        return r;
    }

    private static PerfilMunicipio montar(Estado e, String chave, String pedido) {
        List<FocoIncendio> meus = e.porChave().getOrDefault(chave, List.of());
        MalhaMunicipal malha = MalhaMunicipal.sp();
        MalhaMunicipal.Municipio m = malha.buscar(chave);
        String nomeInpe = meus.isEmpty() ? null : meus.get(0).getMunicipio();
        if (nomeInpe == null) {
            if (m == null) throw new IllegalArgumentException("Município não encontrado: " + pedido);
            nomeInpe = m.nome();
        }
        long total = meus.size();

        Map<String, Long> biomas = new HashMap<>();
        Map<Integer, Long> anos = new HashMap<>();
        boolean temFocos = e.anoMin() <= e.anoMax();
        long[] meses = new long[temFocos ? (e.anoMax() - e.anoMin() + 1) * 12 : 0];
        for (FocoIncendio f : meus) {
            biomas.merge(f.getBioma(), 1L, Long::sum);
            anos.merge(f.getAno(), 1L, Long::sum);
            meses[(f.getAno() - e.anoMin()) * 12 + f.getMes() - 1]++;
        }
        List<Mes> porMes = new ArrayList<>();
        for (int i = 0; i < meses.length; i++) porMes.add(new Mes(e.anoMin() + i / 12, i % 12 + 1, meses[i]));
        List<Contagem> porAno = new ArrayList<>();
        for (int a = e.anoMin(); temFocos && a <= e.anoMax(); a++) porAno.add(new Contagem(String.valueOf(a), anos.getOrDefault(a, 0L)));

        int acima = 0;
        for (long n : e.contagem().values()) if (n > total) acima++;

        List<Contagem> viz = new ArrayList<>();
        Set<String> vizinhos = m == null ? Set.of() : vizinhanca(malha).de(m.chave());
        for (String v : vizinhos) {
            long n = e.contagem().getOrDefault(v, 0L);
            MalhaMunicipal.Municipio mv = malha.buscar(v);
            if (n > 0) viz.add(new Contagem(mv == null ? Textos.nomeProprio(v) : mv.nome(), n));
        }
        Ordenacoes.ordenar(viz, MAIOR_PRIMEIRO);
        List<Contagem> listaBiomas = new ArrayList<>();
        biomas.forEach((b, n) -> listaBiomas.add(new Contagem(b, n)));
        Ordenacoes.ordenar(listaBiomas, MAIOR_PRIMEIRO);

        String nome = m != null ? m.nome() : Textos.nomeProprio(nomeInpe);
        return new PerfilMunicipio(nome, nomeInpe, total, porAno, porMes, listaBiomas,
                total == 0 ? 0 : acima + 1, e.contagem().size(), m == null ? Double.NaN : m.areaKm2(),
                m == null ? Double.NaN : m.latitude(), m == null ? Double.NaN : m.longitude(), viz, vizinhos.size());
    }

    /** Focos por 1.000 km² de area territorial (NaN quando o municipio nao esta na malha do IBGE). */
    public double densidade() {
        return Double.isNaN(areaKm2) || areaKm2 <= 0 ? Double.NaN : total * 1000.0 / areaKm2;
    }

    /** Bioma com mais focos no municipio, ou vazio. */
    public String biomaPrincipal() {
        return biomas.isEmpty() ? "" : biomas.get(0).chave();
    }

    /** Mes com mais focos (o primeiro, em caso de empate), ou null se nao houve focos. */
    public Mes pico() {
        Mes melhor = null;
        for (Mes m : porMes) if (m.focos() > 0 && (melhor == null || m.focos() > melhor.focos())) melhor = m;
        return melhor;
    }

    /** Variacao percentual entre o primeiro e o ultimo ano (NaN se o primeiro ano nao teve focos). */
    public double variacao() {
        if (porAno.size() < 2 || porAno.get(0).total() == 0) return Double.NaN;
        long de = porAno.get(0).total(), ate = porAno.get(porAno.size() - 1).total();
        return 100.0 * (ate - de) / de;
    }

    private static synchronized Vizinhanca vizinhanca(MalhaMunicipal malha) {
        if (vizinhanca == null) vizinhanca = Vizinhanca.da(malha);
        return vizinhanca;
    }
}
