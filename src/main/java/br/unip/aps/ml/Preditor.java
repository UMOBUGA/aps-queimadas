package br.unip.aps.ml;

import br.unip.aps.model.FocoIncendio;
import br.unip.aps.sorting.OperationMetrics;

import java.util.List;
import java.util.logging.Logger;

/** Fachada (padrao Facade) do modulo de Machine Learning. */
public final class Preditor {
    private static final Logger LOG = Logger.getLogger(Preditor.class.getName());

    /** Parametros do modulo de ML. */
    public record Parametros(int anoTreino, int anoTeste, int arvores, int k, double epsKm, int minPts, long semente) {
        public static Parametros padrao(int anoTreino, int anoTeste) {
            return new Parametros(anoTreino, anoTeste, 200, 8, 10.0, 30, 42L);
        }
    }

    /** Resultado consolidado. */
    public record ResultadoML(Parametros parametros, OperationMetrics ordenacaoPreparo, int municipios,
                              PrevisaoFocos.Resultado regressao, ClassificadorNivel.Resultado classificacao,
                              ClusterizacaoHotspots.Resultado kMeans, ClusterizacaoHotspots.Resultado dbscan,
                              double[] cotovelo, long duracaoMs) { }

    /** Executa todas as analises. */
    public ResultadoML executar(List<FocoIncendio> focos, Parametros p) {
        long t0 = System.nanoTime();
        boolean temTreino = false, temTeste = false;
        for (FocoIncendio f : focos) {
            temTreino |= f.getAno() == p.anoTreino();
            temTeste |= f.getAno() == p.anoTeste();
        }
        if (!temTreino || !temTeste) {
            throw new IllegalArgumentException("A base precisa conter focos de " + p.anoTreino() + " (treino) e "
                    + p.anoTeste() + " (teste).");
        }
        BaseMensal base = new BaseMensal(focos);
        PrevisaoFocos.Resultado reg = new PrevisaoFocos(p.arvores(), p.semente()).executar(base, p.anoTreino(), p.anoTeste());
        ClassificadorNivel.Resultado cls = new ClassificadorNivel(p.arvores(), p.semente()).executar(base, p.anoTreino(), p.anoTeste());
        ClusterizacaoHotspots cl = new ClusterizacaoHotspots(p.semente());
        ClusterizacaoHotspots.Resultado km = cl.kMeans(focos, Math.min(p.k(), focos.size()));
        ClusterizacaoHotspots.Resultado db = cl.dbscan(focos, p.epsKm(), p.minPts());
        double[] cot = cl.cotovelo(focos, Math.min(10, focos.size()));
        long ms = (System.nanoTime() - t0) / 1_000_000;
        LOG.info(() -> String.format("ML concluido em %d ms: RF MAE=%.3f (persistencia %.3f), F1 macro=%.3f, %d hotspots DBSCAN",
                ms, reg.modelo().mae(), reg.persistencia().mae(), cls.metricas().f1Macro(), db.hotspots().size()));
        return new ResultadoML(p, base.metricasOrdenacao(), base.municipios().size(), reg, cls, km, db, cot, ms);
    }
}
