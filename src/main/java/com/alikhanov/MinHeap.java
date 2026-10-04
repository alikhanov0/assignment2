package com.alikhanov;

/** Array-based binary min-heap using holes instead of three-assignment swaps. */
public final class MinHeap {
    private int[] elements = new int[16];
    private int size;
    private final Metrics metrics = new Metrics();

    public void insert(int value) {
        ensureCapacity();
        int hole = size++;
        while (hole > 0) {
            int parent = (hole - 1) / 2;
            int parentValue = read(parent);
            metrics.compare();
            if (parentValue <= value) break;
            elements[hole] = parentValue;
            metrics.move();
            hole = parent;
        }
        elements[hole] = value;
    }

    public int peekMin() {
        checkNotEmpty();
        return read(0);
    }

    public int extractMin() {
        checkNotEmpty();
        int minimum = read(0);
        size--;
        if (size > 0) {
            int last = read(size);
            siftDown(0, last, true);
        }
        return minimum;
    }

    /** Floyd construction; owns a copy, so later changes to input cannot affect it. */
    public static MinHeap buildHeap(int[] input) {
        if (input == null) throw new NullPointerException("input");
        MinHeap heap = new MinHeap();
        heap.elements = new int[Math.max(16, input.length)];
        heap.size = input.length;
        for (int i = 0; i < input.length; i++) {
            heap.metrics.step();
            heap.elements[i] = input[i];
            heap.metrics.move();
        }
        for (int i = heap.size / 2 - 1; i >= 0; i--) {
            int value = heap.read(i);
            heap.siftDown(i, value, false);
        }
        return heap;
    }

    public int size() { return size; }
    public int capacity() { return elements.length; }
    public Metrics metrics() { return metrics; }

    /** Defensive snapshot for validation only; diagnostics do not change counters. */
    public int[] snapshot() {
        int[] copy = new int[size];
        System.arraycopy(elements, 0, copy, 0, size);
        return copy;
    }

    private void siftDown(int start, int value, boolean relocated) {
        int hole = start;
        while (hole < size / 2) {
            int child = 2 * hole + 1;
            int childValue = read(child);
            if (child + 1 < size) {
                int rightValue = read(child + 1);
                metrics.compare();
                if (rightValue < childValue) {
                    child++;
                    childValue = rightValue;
                }
            }
            metrics.compare();
            if (value <= childValue) break;
            elements[hole] = childValue;
            metrics.move();
            hole = child;
        }
        // Count the final relocation of an existing value, but not a no-op write.
        if (hole != start || relocated) {
            elements[hole] = value;
            metrics.move();
        }
    }

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

    private void checkNotEmpty() {
        if (size == 0) throw new IllegalStateException("heap is empty");
    }
}
