package com.alikhanov;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class MinHeapTest {
    private static void assertHeapProperty(MinHeap heap) {
        int[] values = heap.snapshot();
        assertEquals(heap.size(), values.length);
        for (int child = 1; child < values.length; child++) {
            assertTrue(values[(child - 1) / 2] <= values[child], "bad child index " + child);
        }
    }

    @Test void emptySingletonAndReuse() {
        MinHeap heap = new MinHeap();
        assertThrows(IllegalStateException.class, heap::peekMin);
        assertThrows(IllegalStateException.class, heap::extractMin);
        heap.insert(4);
        assertHeapProperty(heap);
        assertEquals(4, heap.peekMin());
        assertEquals(4, heap.extractMin());
        assertHeapProperty(heap);
        assertEquals(0, heap.size());
        assertThrows(IllegalStateException.class, heap::extractMin);
        heap.insert(-9);
        assertEquals(-9, heap.extractMin());
    }

    @Test void duplicateExtremeValuesAndSortedOutput() {
        int[] input = {Integer.MAX_VALUE, 4, 4, -1, Integer.MIN_VALUE, 0, 4};
        MinHeap heap = new MinHeap();
        for (int value : input) { heap.insert(value); assertHeapProperty(heap); }
        Arrays.sort(input);
        for (int value : input) {
            assertEquals(value, heap.extractMin());
            assertHeapProperty(heap);
        }
    }

    @Test void mixedRandomOperationsMatchPriorityQueue() {
        MinHeap actual = new MinHeap();
        PriorityQueue<Integer> expected = new PriorityQueue<>();
        Random random = new Random(42);
        for (int i = 0; i < 5_000; i++) {
            if (expected.isEmpty() || random.nextInt(3) != 0) {
                int value = random.nextInt(1_001) - 500;
                actual.insert(value); expected.add(value);
            } else {
                assertEquals(expected.remove().intValue(), actual.extractMin());
            }
            assertHeapProperty(actual);
            assertEquals(expected.size(), actual.size());
            if (!expected.isEmpty()) assertEquals(expected.peek().intValue(), actual.peekMin());
        }
        while (!expected.isEmpty()) {
            assertEquals(expected.remove().intValue(), actual.extractMin());
            assertHeapProperty(actual);
        }
    }

    @Test void ascendingDescendingAndEqualInputs() {
        for (int mode = 0; mode < 3; mode++) {
            MinHeap heap = new MinHeap();
            for (int i = 0; i < 257; i++) {
                heap.insert(mode == 0 ? i : mode == 1 ? -i : 7);
                assertHeapProperty(heap);
            }
            int previous = Integer.MIN_VALUE;
            while (heap.size() > 0) {
                int value = heap.extractMin();
                assertTrue(value >= previous);
                previous = value;
                assertHeapProperty(heap);
            }
        }
    }

    @Test void exactBubbleCountersAndSnapshotIsolation() {
        MinHeap heap = new MinHeap();
        heap.insert(3);
        heap.metrics().reset();
        heap.insert(1);
        assertEquals(1, heap.metrics().steps());
        assertEquals(1, heap.metrics().moves());
        assertEquals(1, heap.metrics().comparisons());
        heap.insert(2);
        heap.metrics().reset();
        int[] copy = heap.snapshot(); copy[0] = 999;
        assertEquals(0, heap.metrics().steps());
        assertEquals(1, heap.extractMin());
        assertEquals(3, heap.metrics().steps());
        assertEquals(1, heap.metrics().moves());
        assertEquals(1, heap.metrics().comparisons());
        heap.metrics().reset();
        assertEquals(2, heap.peekMin());
        assertEquals(1, heap.metrics().steps());
    }

    @Test void capacityDoubles() {
        MinHeap heap = new MinHeap();
        int initial = heap.capacity();
        for (int i = 0; i <= initial; i++) heap.insert(i);
        assertEquals(initial * 2, heap.capacity());
        assertHeapProperty(heap);
    }

    @Test void floydBuildMatchesSortingAndDoesNotAliasInput() {
        Random random = new Random(42);
        for (int n : new int[]{0, 1, 2, 3, 16, 101, 1_000}) {
            int[] input = random.ints(n, -100, 101).toArray();
            int[] expected = input.clone();
            MinHeap heap = MinHeap.buildHeap(input);
            assertArrayEquals(expected, input);
            if (n > 0) input[0] = Integer.MAX_VALUE;
            assertHeapProperty(heap);
            // At most two element comparisons per visited level; Floyd is linear.
            assertTrue(heap.metrics().comparisons() <= 2L * n);
            Arrays.sort(expected);
            for (int value : expected) {
                assertEquals(value, heap.extractMin());
                assertHeapProperty(heap);
            }
        }
        assertThrows(NullPointerException.class, () -> MinHeap.buildHeap(null));
    }
}
