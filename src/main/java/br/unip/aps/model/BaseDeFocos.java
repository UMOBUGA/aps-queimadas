package br.unip.aps.model;

import br.unip.aps.io.RelatorioCarga;
import br.unip.aps.sorting.Ordenacoes;
import br.unip.aps.util.Textos;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Base unificada de focos (todos os anos carregados), imutavel.
 *
 * <p>A lista interna preserva a ordem original dos arquivos; toda ordenacao trabalha sobre uma
 * copia (ver {@link br.unip.aps.sorting.ServicoOrdenacao}), garantindo que os dados originais
 * nunca sejam alterados.</p>
 */
public final class BaseDeFocos {

    private final List<FocoIncendio> focos;
    private final RelatorioCarga relatorio;
    private final List<Path> fontes;

    /**
     * @param focos     registros validos
     * @param relatorio relatorio de qualidade da carga
     * @param fontes    arquivos de origem
     */
    public BaseDeFocos(List<FocoIncendio> focos, RelatorioCarga relatorio, List<Path> fontes) {
        this.focos = List.copyOf(focos);
        this.relatorio = relatorio;
        this.fontes = List.copyOf(fontes);
    }

    /** @return todos os focos, na ordem original (lista imutavel) */
    public List<FocoIncendio> getFocos() { return focos; }

    /** @return relatorio de qualidade da carga */
    public RelatorioCarga getRelatorio() { return relatorio; }

    /** @return arquivos de origem */
    public List<Path> getFontes() { return fontes; }

    /** @return quantidade de focos */
    public int tamanho() { return focos.size(); }

    /**
     * @param filtro predicado de selecao
     * @return nova lista com os focos que atendem ao filtro (ordem original)
     */
    public List<FocoIncendio> filtrar(Predicate<FocoIncendio> filtro) {
        List<FocoIncendio> r = new ArrayList<>();
        for (FocoIncendio f : focos) if (filtro.test(f)) r.add(f);
        return r;
    }

    /** @return anos presentes, em ordem crescente */
    public List<Integer> anos() {
        Set<Integer> s = new HashSet<>();
        for (FocoIncendio f : focos) s.add(f.getAno());
        return Ordenacoes.ordenar(new ArrayList<>(s), Comparator.naturalOrder());
    }

    /** @return biomas distintos, em ordem alfabetica pt-BR */
    public List<String> biomas() {
        Set<String> s = new HashSet<>();
        for (FocoIncendio f : focos) s.add(f.getBioma());
        return Ordenacoes.ordenar(new ArrayList<>(s), Textos.collator()::compare);
    }

    /** @return municipios distintos, em ordem alfabetica pt-BR */
    public List<String> municipios() {
        Set<String> s = new HashSet<>();
        for (FocoIncendio f : focos) s.add(f.getMunicipio());
        return Ordenacoes.ordenar(new ArrayList<>(s), Textos.collator()::compare);
    }

    /** @return data do foco mais antigo */
    public LocalDate dataInicial() {
        LocalDate min = null;
        for (FocoIncendio f : focos) if (min == null || f.getData().isBefore(min)) min = f.getData();
        return min;
    }

    /** @return data do foco mais recente */
    public LocalDate dataFinal() {
        LocalDate max = null;
        for (FocoIncendio f : focos) if (max == null || f.getData().isAfter(max)) max = f.getData();
        return max;
    }

    /**
     * Verifica se alguma linha trouxe a coluna opcional (usado para habilitar criterios extras).
     *
     * @param extrator acesso ao campo opcional
     * @return {@code true} se ao menos um foco tiver o valor preenchido
     */
    public boolean possuiCampo(java.util.function.Function<FocoIncendio, ?> extrator) {
        for (FocoIncendio f : focos) if (extrator.apply(f) != null) return true;
        return false;
    }

    /** @return estados (UF por extenso) presentes na base */
    public List<String> estados() {
        Set<String> s = new HashSet<>();
        for (FocoIncendio f : focos) if (f.getEstado() != null) s.add(f.getEstado());
        return Collections.unmodifiableList(Ordenacoes.ordenar(new ArrayList<>(s), Textos.collator()::compare));
    }
}
