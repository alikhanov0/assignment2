package com.alikhanov;

/** Resizable primitive array. Capacity doubles and never shrinks automatically. */
public final class DynamicArray implements IntSequence {
    private int[] elements = new int[16];
    private int size;
    private final Metrics metrics = new Metrics();

    @Override
    public void add(int value) {
        ensureCapacity();
        elements[size++] = value; // A new value is not a shifted existing element.
    }

    @Override
    public void add(int index, int value) {
        checkIndex(index, true);
        ensureCapacity();
        for (int i = size; i > index; i--) {
            elements[i] = read(i - 1);
            metrics.move();
        }
        elements[index] = value;
        size++;
    }

    @Override
    public int remove(int index) {
        checkIndex(index, false);
        int removed = read(index);
        for (int i = index; i < size - 1; i++) {
            elements[i] = read(i + 1);
            metrics.move();
        }
        size--;
        return removed;
    }

    @Override
    public int get(int index) {
        checkIndex(index, false);
        return read(index);
    }

    @Override
    public boolean contains(int value) {
        for (int i = 0; i < size; i++) {
            int current = read(i);
            metrics.compare();
            if (current == value) return true;
        }
        return false;
    }

    @Override
    public int size() { return size; }

    public int capacity() { return elements.length; }

    @Override
    public Metrics metrics() { return metrics; }

    private int read(int index) {
        metrics.step();
        return elements[index];
    }

    private void ensureCapacity() {
        if (size < elements.length) return;
        int[] grown = new int[Math.multiplyExact(elements.length, 2)];
        for (int i = 0; i < size; i++) {
            grown[i] = read(i);
            metrics.move();
        }
        elements = grown;
    }

    private void checkIndex(int index, boolean insertion) {
        if (index < 0 || (insertion ? index > size : index >= size)) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
        }
    }
}
