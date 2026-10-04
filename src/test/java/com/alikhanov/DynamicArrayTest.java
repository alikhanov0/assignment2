package com.alikhanov;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class DynamicArrayTest {
    @Test void emptyAndInvalidIndices() {
        DynamicArray a = new DynamicArray();
        assertEquals(0, a.size());
        assertFalse(a.contains(7));
        assertThrows(IndexOutOfBoundsException.class, () -> a.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> a.remove(0));
        assertThrows(IndexOutOfBoundsException.class, () -> a.add(-1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> a.add(1, 1));
        a.add(7);
        assertThrows(IndexOutOfBoundsException.class, () -> a.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> a.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> a.remove(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> a.remove(1));
        assertThrows(IndexOutOfBoundsException.class, () -> a.add(2, 0));
        assertEquals(7, a.remove(0));
        assertEquals(0, a.size());
    }

    @Test void duplicatesBoundariesAndExtremeValues() {
        DynamicArray a = new DynamicArray();
        a.add(0, Integer.MIN_VALUE);
        a.add(1, Integer.MAX_VALUE);
        a.add(1, 4);
        a.add(1, 4);
        assertEquals(Integer.MIN_VALUE, a.get(0));
        assertEquals(Integer.MAX_VALUE, a.get(3));
        assertEquals(4, a.remove(1));
        assertTrue(a.contains(4));
        assertEquals(Integer.MAX_VALUE, a.remove(a.size() - 1));
    }

    @Test void capacityDoublesAndPreservesValues() {
        DynamicArray a = new DynamicArray();
        int initial = a.capacity();
        for (int i = 0; i < initial; i++) a.add(i);
        a.metrics().reset();
        a.add(initial);
        assertEquals(initial * 2, a.capacity());
        assertEquals(initial, a.metrics().steps());
        assertEquals(initial, a.metrics().moves());
        for (int i = 0; i <= initial; i++) assertEquals(i, a.get(i));
    }

    @Test void exactCountersForReadSearchAndShifts() {
        DynamicArray a = new DynamicArray();
        a.add(10); a.add(20); a.add(30);
        a.metrics().reset();
        assertEquals(20, a.get(1));
        assertEquals(1, a.metrics().steps());
        a.metrics().reset();
        a.add(1, 15);
        assertEquals(2, a.metrics().steps());
        assertEquals(2, a.metrics().moves());
        assertEquals(0, a.metrics().comparisons());
        a.metrics().reset();
        assertEquals(15, a.remove(1));
        assertEquals(3, a.metrics().steps());
        assertEquals(2, a.metrics().moves());
        a.metrics().reset();
        assertFalse(a.contains(99));
        assertEquals(3, a.metrics().steps());
        assertEquals(3, a.metrics().comparisons());
    }

    @Test void randomizedOperationsMatchArrayList() {
        DynamicArray actual = new DynamicArray();
        ArrayList<Integer> expected = new ArrayList<>();
        Random random = new Random(42);
        for (int operation = 0; operation < 5_000; operation++) {
            int value = random.nextInt(101) - 50;
            switch (random.nextInt(5)) {
                case 0 -> { actual.add(value); expected.add(value); }
                case 1 -> {
                    int index = random.nextInt(expected.size() + 1);
                    actual.add(index, value); expected.add(index, value);
                }
                case 2 -> {
                    if (!expected.isEmpty()) {
                        int index = random.nextInt(expected.size());
                        assertEquals(expected.remove(index).intValue(), actual.remove(index));
                    }
                }
                case 3 -> {
                    if (!expected.isEmpty()) {
                        int index = random.nextInt(expected.size());
                        assertEquals(expected.get(index).intValue(), actual.get(index));
                    }
                }
                default -> assertEquals(expected.contains(value), actual.contains(value));
            }
            assertEquals(expected.size(), actual.size());
        }
        for (int i = 0; i < expected.size(); i++) assertEquals(expected.get(i).intValue(), actual.get(i));
    }
}
