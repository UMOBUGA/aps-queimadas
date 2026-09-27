package br.unip.aps.sorting;

import br.unip.aps.sorting.algorithms.BubbleSort;
import br.unip.aps.sorting.algorithms.HeapSort;
import br.unip.aps.sorting.algorithms.InsertionSort;
import br.unip.aps.sorting.algorithms.MergeSort;
import br.unip.aps.sorting.algorithms.QuickSort;
import br.unip.aps.sorting.algorithms.QuickSort3Way;
import br.unip.aps.sorting.algorithms.RadixSort;
import br.unip.aps.sorting.algorithms.SelectionSort;
import br.unip.aps.sorting.algorithms.ShellSort;
import br.unip.aps.sorting.algorithms.TimSortSimplificado;

import java.util.function.Supplier;

/** Catalogo dos algoritmos disponiveis. */
public enum AlgoritmoTipo {
    BUBBLE(BubbleSort::new),
    SELECTION(SelectionSort::new),
    INSERTION(InsertionSort::new),
    SHELL(ShellSort::new),
    MERGE(MergeSort::new),
    QUICK(QuickSort::new),
    QUICK_3WAY(QuickSort3Way::new),
    HEAP(HeapSort::new),
    TIM(TimSortSimplificado::new),
    RADIX(RadixSort::new);

    private final Supplier<SortAlgorithm> fabrica;
    private final SortAlgorithm prototipo;

    AlgoritmoTipo(Supplier<SortAlgorithm> fabrica) {
        this.fabrica = fabrica;
        this.prototipo = fabrica.get();
    }

    public SortAlgorithm criar() {
        return fabrica.get();
    }

    public String nome() {
        return prototipo.nome();
    }

    public Complexidade complexidade() {
        return prototipo.complexidade();
    }

    public boolean quadratico() {
        return prototipo.complexidade().quadratico();
    }

    public boolean exigeChaveNumerica() {
        return prototipo.exigeChaveNumerica();
    }

    @Override
    public String toString() {
        return nome();
    }
}
