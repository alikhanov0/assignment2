package com.alikhanov;

/** Singly linked int list with a tail pointer for constant-time append. */
public final class MyLinkedList implements IntSequence {
    private static final class Node {
        final int value;
        Node next;
        Node(int value) { this.value = value; }
    }

    private Node head;
    private Node tail;
    private int size;
    private final Metrics metrics = new Metrics();

    @Override
    public void add(int value) {
        Node node = new Node(value);
        if (tail == null) {
            head = node;
            metrics.move();
        } else {
            tail.next = node;
            metrics.move();
        }
        tail = node;
        metrics.move();
        size++;
    }

    @Override
    public void add(int index, int value) {
        checkIndex(index, true);
        if (index == size) {
            add(value);
            return;
        }
        Node node = new Node(value);
        if (index == 0) {
            node.next = head;
            metrics.move();
            head = node;
            metrics.move();
        } else {
            Node previous = nodeAt(index - 1);
            node.next = previous.next;
            metrics.move();
            previous.next = node;
            metrics.move();
        }
        size++;
    }

    @Override
    public int remove(int index) {
        checkIndex(index, false);
        Node removed;
        if (index == 0) {
            removed = head;
            head = next(head);
            metrics.move();
            if (size == 1) {
                tail = null;
                metrics.move();
            }
        } else {
            Node previous = nodeAt(index - 1);
            removed = next(previous);
            previous.next = next(removed);
            metrics.move();
            if (index == size - 1) {
                tail = previous;
                metrics.move();
            }
        }
        size--;
        return removed.value;
    }

    @Override
    public int get(int index) {
        checkIndex(index, false);
        return nodeAt(index).value;
    }

    @Override
    public boolean contains(int value) {
        Node current = head;
        while (current != null) {
            metrics.compare();
            if (current.value == value) return true;
            current = next(current);
        }
        return false;
    }

    @Override
    public int size() { return size; }

    @Override
    public Metrics metrics() { return metrics; }

    private Node nodeAt(int index) {
        Node current = head;
        for (int i = 0; i < index; i++) current = next(current);
        return current;
    }

    private Node next(Node node) {
        metrics.step();
        return node.next;
    }

    private void checkIndex(int index, boolean insertion) {
        if (index < 0 || (insertion ? index > size : index >= size)) {
            throw new IndexOutOfBoundsException("index=" + index + ", size=" + size);
        }
    }
}
