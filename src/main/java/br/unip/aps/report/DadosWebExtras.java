package br.unip.aps.report;

import br.unip.aps.analysis.Classificacao;
import br.unip.aps.app.Agregados;
import br.unip.aps.geo.MalhaMunicipal;
import br.unip.aps.geo.PerfilMunicipio;
import br.unip.aps.ml.ClusterizacaoHotspots;
import br.unip.aps.ml.Preditor;
import br.unip.aps.model.BaseDeFocos;
import br.unip.aps.model.FocoIncendio;
import br.unip.aps.sorting.AlgoritmoTipo;
import br.unip.aps.sorting.CriterioOrdenacao;
import br.unip.aps.sorting.DescricoesAlgoritmos;
import br.unip.aps.sorting.GravadorPassos;
import br.unip.aps.sorting.Ordenacoes;
import br.unip.aps.util.Json;
import br.unip.aps.util.Textos;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Partes da versao web que contam a historia para quem nao e da area: animacao da ordenacao, historico, horarios e ML. */
final class DadosWebExtras {
    static final int TAMANHO_VETOR = 32;

    private DadosWebExtras() { }

    /** Vetor embaralhado de 32 valores e cada passo de cada algoritmo ao ordena-lo, para a animacao na web. */
    static String passos() {
        int[] vetor = vetorEmbaralhado();
        StringBuilder sb = new StringBuilder("{\"vetor\":");
        sb.append(Arrays.toString(vetor).replace(" ", ""));
        sb.append(",\"campos\":[\"tipo (0 compara, 1 troca, 2 escreve)\",\"i\",\"j\",\"valor\"],\"algoritmos\":[");
        AlgoritmoTipo[] tipos = AlgoritmoTipo.values();
        for (int a = 0; a < tipos.length; a++) {
            AlgoritmoTipo t = tipos[a];
            List<GravadorPassos.Passo> ps = GravadorPassos.gravar(t, vetor);
            sb.append(a > 0 ? ",\n" : "").append("{\"nome\":").append(Json.texto(t.nome()))
                    .append(",\"complexidade\":").append(Json.texto(t.criar().complexidade().casoMedio()))
                    .append(",\"descricao\":").append(Json.texto(DescricoesAlgoritmos.de(t)))
                    .append(",\"passos\":[");
            for (int i = 0; i < ps.size(); i++) {
                GravadorPassos.Passo p = ps.get(i);
                sb.append(i > 0 ? "," : "").append(p.tipo()).append(',').append(p.i()).append(',').append(p.j()).append(',').append(p.valor());
            }
            sb.append("]}");
        }
        return sb.append("]}").toString();
    }

    static int[] vetorEmbaralhado() {
        int[] vetor = new int[TAMANHO_VETOR];
        for (int i = 0; i < vetor.length; i++) vetor[i] = i + 1;
        Random r = new Random(2024);
        for (int i = vetor.length - 1; i > 0; i--) {
            int k = r.nextInt(i + 1), x = vetor[i];
            vetor[i] = vetor[k];
            vetor[k] = x;
        }
        return vetor;
    }

    /** Historico de SP, ranking dos estados, horas e dias da semana, qualidade da carga e classes do mapa por municipio. */
    static String historia(BaseDeFocos base, List<PerfilMunicipio> perfis) {
        StringBuilder sb = new StringBuilder("{\"historico\":[");
        Map<Integer, Long> porAno = new LinkedHashMap<>();
        for (Agregados.Mes m : Agregados.historicoSp()) porAno.merge(m.ano(), m.focos(), Long::sum);
        List<Integer> anos = new ArrayList<>(porAno.keySet());
        Ordenacoes.ordenar(anos, Integer::compare);
        for (int i = 0; i < anos.size(); i++) {
            sb.append(i > 0 ? "," : "").append("{\"ano\":").append(anos.get(i)).append(",\"focos\":").append(porAno.get(anos.get(i))).append('}');
        }

        int anoEstados = 0;
        for (Agregados.Estado e : Agregados.brasil()) anoEstados = Math.max(anoEstados, e.ano());
        List<Agregados.Estado> doAno = new ArrayList<>();
        for (Agregados.Estado e : Agregados.brasil()) if (e.ano() == anoEstados) doAno.add(e);
        Ordenacoes.ordenar(doAno, Comparator.comparingLong(Agregados.Estado::focos).reversed());
        sb.append("],\"estadosAno\":").append(anoEstados).append(",\"estados\":[");
        for (int i = 0; i < doAno.size(); i++) {
            Agregados.Estado e = doAno.get(i);
            sb.append(i > 0 ? "," : "").append("{\"estado\":").append(Json.texto(Textos.nomeProprio(e.estado())))
                    .append(",\"sp\":").append(MalhaMunicipal.chave(e.estado()).equals("SAO PAULO"))
                    .append(",\"focos\":").append(e.focos()).append('}');
        }

        long[] horas = new long[24];
        long[] dias = new long[7];
        Map<LocalDate, Long> porDia = new HashMap<>();
        for (FocoIncendio f : base.getFocos()) {
            horas[CriterioOrdenacao.horaLocal(f)]++;
            LocalDate dia = f.getDataHora().minusHours(3).toLocalDate();
            dias[dia.getDayOfWeek().getValue() - 1]++;
            porDia.merge(dia, 1L, Long::sum);
        }
        LocalDate pico = null;
        for (Map.Entry<LocalDate, Long> e : porDia.entrySet()) {
            long atual = pico == null ? -1 : porDia.get(pico);
            if (e.getValue() > atual || e.getValue() == atual && e.getKey().isBefore(pico)) pico = e.getKey();
        }
        sb.append("],\"horasLocais\":").append(Arrays.toString(horas).replace(" ", ""));
        sb.append(",\"diasSemana\":").append(Arrays.toString(dias).replace(" ", ""));
        sb.append(",\"diaPico\":").append(pico == null ? "null" : "{\"data\":" + Json.texto(pico.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                + ",\"focos\":" + porDia.get(pico) + "}");
        sb.append(",\"diasComFocos\":").append(porDia.size());

        var carga = base.getRelatorio();
        sb.append(",\"carga\":{\"lidas\":").append(carga.getTotalLidas()).append(",\"aceitas\":").append(carga.getTotalAceitas())
                .append(",\"rejeitadas\":").append(carga.getTotalRejeitadas()).append(",\"duplicadas\":").append(carga.getDuplicadosRemovidos())
                .append(",\"arquivos\":").append(base.getFontes().size()).append('}');

        double[] dens = new double[perfis.size()], cont = new double[perfis.size()];
        int n = 0;
        for (PerfilMunicipio p : perfis) {
            if (Double.isNaN(p.densidade())) continue;
            dens[n] = p.densidade();
            cont[n++] = p.total();
        }
        sb.append(",\"classes\":{\"metodo\":\"Quebras naturais (Jenks)\",\"densidade\":")
                .append(numeros(Classificacao.limites(Arrays.copyOf(dens, n), 5, Classificacao.Metodo.JENKS), 2))
                .append(",\"contagem\":").append(numeros(Classificacao.limites(Arrays.copyOf(cont, n), 5, Classificacao.Metodo.JENKS), 0)).append('}');
        return sb.append('}').toString();
    }

    /** Hotspots do DBSCAN e a previsao do Random Forest contra o real, com as previsoes ingenuas para comparar. */
    static String ml(Preditor.ResultadoML ml) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"anoTreino\":").append(ml.parametros().anoTreino()).append(",\"anoTeste\":").append(ml.parametros().anoTeste());
        sb.append(",\"hotspots\":[");
        List<ClusterizacaoHotspots.Hotspot> hs = ml.dbscan().hotspots();
        for (int i = 0; i < hs.size(); i++) {
            ClusterizacaoHotspots.Hotspot h = hs.get(i);
            sb.append(i > 0 ? "," : "").append('[').append(Json.numero(h.latitude(), 4)).append(',').append(Json.numero(h.longitude(), 4)).append(',')
                    .append(h.focos()).append(',').append(Json.numero(h.raioKm(), 2)).append(',').append(Json.texto(h.municipioPrincipal())).append(',')
                    .append(Json.texto(h.biomaPredominante())).append(']');
        }
        sb.append("],\"hotspotsMetodo\":").append(Json.texto(ml.dbscan().metodo())).append(",\"focosIsolados\":").append(ml.dbscan().ruido());
        var reg = ml.regressao();
        sb.append(",\"erro\":{\"modelo\":").append(Json.numero(reg.modelo().mae(), 3))
                .append(",\"repetirAnoAnterior\":").append(Json.numero(reg.persistencia().mae(), 3))
                .append(",\"mediaHistorica\":").append(Json.numero(reg.mediaHistorica().mae(), 3))
                .append(",\"r2\":").append(Json.numero(reg.modelo().r2(), 3)).append('}');
        sb.append(",\"meses\":[");
        List<YearMonth> meses = new ArrayList<>(reg.realPorMes().keySet());
        Ordenacoes.ordenar(meses, YearMonth::compareTo);
        for (int i = 0; i < meses.size(); i++) {
            YearMonth m = meses.get(i);
            sb.append(i > 0 ? "," : "").append('[').append(m.getYear() * 100 + m.getMonthValue()).append(',')
                    .append(Json.numero(reg.realPorMes().get(m), 0)).append(',')
                    .append(Json.numero(reg.previstoPorMes().getOrDefault(m, Double.NaN), 1)).append(']');
        }
        var cls = ml.classificacao();
        sb.append("],\"nivel\":{\"acuracia\":").append(Json.numero(cls.metricas().acuracia(), 3))
                .append(",\"baseline\":").append(Json.numero(cls.baselineAcuracia(), 3)).append('}');
        sb.append(",\"municipios\":").append(ml.municipios());
        return sb.append('}').toString();
    }

    private static String numeros(double[] v, int casas) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) sb.append(i > 0 ? "," : "").append(Json.numero(v[i], casas));
        return sb.append(']').toString();
    }
}
