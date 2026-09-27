package br.unip.aps.ml;

import br.unip.aps.model.FocoIncendio;
import br.unip.aps.sorting.OperationMetrics;

import java.util.List;
import java.util.logging.Logger;

/**
 * Fachada (padrao <b>Facade</b>) do modulo de Machine Learning: monta a base mensal a partir dos
 * focos ordenados e executa previsao, classificacao e clusterizacao com uma unica chamada.
 */
public final class Preditor {

    private static final Logger LOG = Logger.getLogger(Preditor.class.getName());

    /**
     * Parametros do modulo de ML.
     *
     * @param anoTreino  ano de treino (ex.: 2023)
     * @param anoTeste   ano de teste (ex.: 2024)
     * @param arvores    arvores das florestas aleatorias
     * @param k          grupos do K-Means
     * @param epsKm      raio do DBSCAN (km)
     * @param minPts     vizinhos minimos do DBSCAN
     * @param semente    semente global
     */
    public record Parametros(int anoTreino, int anoTeste, int arvores, int k, double epsKm, int minPts, long semente) {
        /**
         * @param anoTreino ano de treino
         * @param anoTeste  ano de teste
         * @return parametros padrao (200 arvores, k=8, eps=10 km, minPts=30)
         */
        public static Parametros padrao(int anoTreino, int anoTeste) {
            return new Parametros(anoTreino, anoTeste, 200, 8, 10.0, 30, 42L);
        }
    }

    /**
     * Resultado consolidado.
     *
     * @param parametros          parametros usados
     * @param ordenacaoPreparo    metricas da ordenacao municipio→data usada no pre-processamento
     * @param municipios          municipios na base mensal
     * @param regressao           previsao de focos (Random Forest de regressao)
     * @param classificacao       nivel de atividade (Random Forest de classificacao)
     * @param kMeans              hotspots por K-Means
     * @param dbscan              hotspots por DBSCAN
     * @param cotovelo            WCSS para k = 1..10 (metodo do cotovelo)
     * @param duracaoMs           tempo total (ms)
     */
    public record ResultadoML(Parametros parametros, OperationMetrics ordenacaoPreparo, int municipios,
                              PrevisaoFocos.Resultado regressao, ClassificadorNivel.Resultado classificacao,
                              ClusterizacaoHotspots.Resultado kMeans, ClusterizacaoHotspots.Resultado dbscan,
                              double[] cotovelo, long duracaoMs) { }

    /**
     * Executa todas as analises.
     *
     * @param focos focos (base inteira)
     * @param p     parametros
     * @return resultados
     * @throws IllegalArgumentException se os anos de treino/teste nao existirem na base
     */
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
