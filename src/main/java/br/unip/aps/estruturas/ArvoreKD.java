package br.unip.aps.estruturas;

import br.unip.aps.sorting.OperationCounter;
import br.unip.aps.sorting.Ordenacoes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;

/** Arvore k-d de duas dimensoes (latitude, longitude): responde "o que esta a ate R km daqui" sem olhar todos os pontos. */
public final class ArvoreKD<T> {
    private static final double KM_POR_GRAU_LAT = 110.57;
    private static final double KM_POR_GRAU_LON = 111.32;

    private static final class No<T> {
        private final T item;
        private final double lat;
        private final double lon;
        private final boolean porLatitude;
        private No<T> esquerda;
        private No<T> direita;

        private No(T item, double lat, double lon, boolean porLatitude) {
            this.item = item;
            this.lat = lat;
            this.lon = lon;
            this.porLatitude = porLatitude;
        }
    }

    private final ToDoubleFunction<? super T> latitude;
    private final ToDoubleFunction<? super T> longitude;
    private final OperationCounter contador;
    private final No<T> raiz;
    private final int tamanho;
    private int altura;

    /** Constroi a arvore balanceada, cortando pela mediana e alternando o eixo a cada nivel. */
    public ArvoreKD(List<T> itens, ToDoubleFunction<? super T> latitude, ToDoubleFunction<? super T> longitude,
                    OperationCounter contador) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.contador = contador;
        this.tamanho = itens.size();
        this.raiz = construir(new ArrayList<>(itens), true, 1);
    }

    private No<T> construir(List<T> pontos, boolean porLatitude, int nivel) {
        if (pontos.isEmpty()) return null;
        altura = Math.max(altura, nivel);
        Comparator<T> eixo = porLatitude ? Comparator.comparingDouble(latitude) : Comparator.comparingDouble(longitude);
        List<T> ordenados = Ordenacoes.ordenar(pontos, eixo);
        int meio = ordenados.size() / 2;
        T m = ordenados.get(meio);
        No<T> no = new No<>(m, latitude.applyAsDouble(m), longitude.applyAsDouble(m), porLatitude);
        contador.atribuicao();
        no.esquerda = construir(new ArrayList<>(ordenados.subList(0, meio)), !porLatitude, nivel + 1);
        no.direita = construir(new ArrayList<>(ordenados.subList(meio + 1, ordenados.size())), !porLatitude, nivel + 1);
        return no;
    }

    /** Distancia aproximada em km (projecao equiretangular, precisa o bastante para raios de dezenas de km). */
    public static double distanciaKm(double lat1, double lon1, double lat2, double lon2) {
        double dx = (lon1 - lon2) * KM_POR_GRAU_LON * Math.cos(Math.toRadians((lat1 + lat2) / 2));
        double dy = (lat1 - lat2) * KM_POR_GRAU_LAT;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /** Entrega cada item a ate {@code raioKm} do centro; devolve quantas distancias foram calculadas. */
    public long noRaio(double lat, double lon, double raioKm, Consumer<? super T> destino) {
        long[] calculos = new long[1];
        visitar(raiz, lat, lon, raioKm, destino, calculos);
        return calculos[0];
    }

    private void visitar(No<T> no, double lat, double lon, double raioKm, Consumer<? super T> destino, long[] calculos) {
        if (no == null) return;
        contador.leitura();
        calculos[0]++;
        if (distanciaKm(lat, lon, no.lat, no.lon) <= raioKm) destino.accept(no.item);
        double diferenca = no.porLatitude ? (lat - no.lat) * KM_POR_GRAU_LAT
                : (lon - no.lon) * KM_POR_GRAU_LON * Math.cos(Math.toRadians(lat));
        No<T> perto = diferenca < 0 ? no.esquerda : no.direita;
        No<T> longe = diferenca < 0 ? no.direita : no.esquerda;
        visitar(perto, lat, lon, raioKm, destino, calculos);
        if (Math.abs(diferenca) <= raioKm * 1.02) visitar(longe, lat, lon, raioKm, destino, calculos);
    }

    public int tamanho() {
        return tamanho;
    }

    public int altura() {
        return altura;
    }
}
