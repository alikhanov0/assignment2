package com.alikhanov;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BenchmarkTest {
    @Test void inputIsRepeatableAndSearchesAreHalfAbsent() {
        Benchmark.Input first = new Benchmark.Input(100);
        Benchmark.Input second = new Benchmark.Input(100);
        assertArrayEquals(first.values, second.values);
        assertArrayEquals(first.indices, second.indices);
        assertArrayEquals(first.queries, second.queries);
        int hits = 0;
        for (int query : first.queries) {
            boolean found = false;
            for (int value : first.values) if (query == value) found = true;
            if (found) hits++;
        }
        assertEquals(500, hits);
    }

    @Test void workloadsExcludeFillAndHaveExactReadCounts() {
        Benchmark.Input input = new Benchmark.Input(100);
        Benchmark.Result array = Benchmark.runSequence("W1", "-", "DynamicArray", input);
        Benchmark.Result list = Benchmark.runSequence("W1", "-", "MyLinkedList", input);
        long expected = 0;
        for (int index : input.indices) expected += index;
        assertEquals(10_000, array.steps());
        assertEquals(expected, list.steps());
        assertEquals(0, array.moves());
        assertEquals(0, list.moves());
        Benchmark.Result arraySearch = Benchmark.runSequence("W2", "-", "DynamicArray", input);
        Benchmark.Result listSearch = Benchmark.runSequence("W2", "-", "MyLinkedList", input);
        assertEquals(arraySearch.comparisons(), listSearch.comparisons());
        assertEquals(arraySearch.steps() - 500, listSearch.steps());
    }

    @Test void w3CountsLinksAndFixedMiddleTraversal() {
        Benchmark.Input input = new Benchmark.Input(100);
        Benchmark.Result head = Benchmark.runSequence("W3", "head", "MyLinkedList", input);
        Benchmark.Result middle = Benchmark.runSequence("W3", "middle", "MyLinkedList", input);
        assertEquals(1_000, head.steps());
        assertEquals(3_000, head.moves());
        assertEquals(100_000, middle.steps());
        assertEquals(3_000, middle.moves());
        assertEquals(0, head.comparisons());
        assertEquals(0, middle.comparisons());
        Benchmark.runSequence("W3", "head", "DynamicArray", input);
        Benchmark.runSequence("W3", "middle", "DynamicArray", input);
        assertEquals(100, Benchmark.runHeap(input).n());
    }
}
