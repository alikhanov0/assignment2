package com.alikhanov;

/** Counts physical operations inside the structures, independently of timing. */
public final class Metrics {
    private long steps;
    private long moves;
    private long comparisons;

    void step() { steps++; }
    void move() { moves++; }
    void compare() { comparisons++; }

    public long steps() { return steps; }
    public long moves() { return moves; }
    public long comparisons() { return comparisons; }

    public void reset() {
        steps = 0;
        moves = 0;
        comparisons = 0;
    }
}
