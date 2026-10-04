package com.alikhanov;

import org.junit.jupiter.api.Test;
import java.util.LinkedList;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class MyLinkedListTest {
    @Test void emptyAndInvalidIndices() {
        MyLinkedList list = new MyLinkedList();
        assertEquals(0, list.size());
        assertFalse(list.contains(7));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(0));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, 7));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(1, 7));
        list.add(7);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(2, 7));
    }

    @Test void tailSurvivesRemovalAndReuse() {
        MyLinkedList list = new MyLinkedList();
        list.add(0, 8);
        assertEquals(8, list.remove(0));
        list.add(9); list.add(10);
        assertEquals(10, list.remove(1));
        list.add(11);
        assertEquals(9, list.get(0));
        assertEquals(11, list.get(1));
        assertEquals(9, list.remove(0));
        assertEquals(11, list.remove(0));
        list.add(0, 12);
        assertEquals(12, list.get(0));
    }

    @Test void duplicateAndExtremeValues() {
        MyLinkedList list = new MyLinkedList();
        list.add(Integer.MIN_VALUE); list.add(4); list.add(4); list.add(Integer.MAX_VALUE);
        assertEquals(Integer.MIN_VALUE, list.get(0));
        assertEquals(Integer.MAX_VALUE, list.get(3));
        assertEquals(4, list.remove(1));
        assertTrue(list.contains(4));
        assertEquals(4, list.remove(1));
        assertFalse(list.contains(4));
    }

    @Test void exactCountersIncludeHeadAndTailLinks() {
        MyLinkedList list = new MyLinkedList();
        list.add(10); list.add(20); list.add(30);
        list.metrics().reset();
        assertEquals(30, list.get(2));
        assertEquals(2, list.metrics().steps());
        list.metrics().reset();
        list.add(0, 5);
        assertEquals(0, list.metrics().steps());
        assertEquals(2, list.metrics().moves());
        list.metrics().reset();
        assertEquals(5, list.remove(0));
        assertEquals(1, list.metrics().steps());
        assertEquals(1, list.metrics().moves());
        list.metrics().reset();
        assertEquals(30, list.remove(2));
        assertEquals(3, list.metrics().steps());
        assertEquals(2, list.metrics().moves());
        list.metrics().reset();
        assertFalse(list.contains(99));
        assertEquals(2, list.metrics().steps());
        assertEquals(2, list.metrics().comparisons());
    }

    @Test void randomizedOperationsMatchLinkedList() {
        MyLinkedList actual = new MyLinkedList();
        LinkedList<Integer> expected = new LinkedList<>();
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
