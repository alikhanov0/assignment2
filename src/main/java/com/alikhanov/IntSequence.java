package com.alikhanov;

/** Shared primitive-int API used by both sequence implementations and workloads. */
public interface IntSequence {
    void add(int value);
    void add(int index, int value);
    int remove(int index);
    int get(int index);
    boolean contains(int value);
    int size();
    Metrics metrics();
}
