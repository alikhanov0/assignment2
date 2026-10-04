package com.alikhanov;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Random;

/** Reproducible instrumented workloads; run with mvn compile exec:java. */
public final class Benchmark {
    static final int[] SIZES = {100, 1_000, 10_000, 100_000};
    static final int WARMUPS = 2;
    static final int REPEATS = 5;
    static final int ACCESSES = 10_000;
    static final int SEARCHES = 1_000;
    static final int CHANGES = 1_000;
    private static volatile long blackhole;

    public record Result(String workload, String variant, String structure, int n,
                         double timeMs, long steps, long moves, long comparisons) {
        String csv() {
            return String.format(Locale.ROOT, "%s,%s,%s,%d,%.6f,%d,%d,%d",
                    workload, variant, structure, n, timeMs, steps, moves, comparisons);
        }
    }

    static final class Input {
        final int[] values;
        final int[] indices = new int[ACCESSES];
        final int[] queries = new int[SEARCHES];
        final int[] inserted = new int[CHANGES];

        Input(int n) {
            values = new int[n];
            Random dataRandom = new Random(42);
            for (int i = 0; i < n; i++) values[i] = dataRandom.nextInt(1_000_000);
            Random queryRandom = new Random(42);
            for (int i = 0; i < indices.length; i++) indices[i] = queryRandom.nextInt(n);
            for (int i = 0; i < queries.length; i++) {
                queries[i] = i % 2 == 0 ? values[queryRandom.nextInt(n)] : -1 - i;
            }
            for (int i = 0; i < inserted.length; i++) inserted[i] = dataRandom.nextInt(1_000_000);
        }
    }

    private Benchmark() { }

    public static void main(String[] args) throws IOException {
        Path output = Path.of(args.length == 0 ? "results" : args[0]);
        Files.createDirectories(output);
        primeJvm();
        Result[] results = new Result[36];
        Result[] bonus = new Result[16];
        int resultIndex = 0;
        int bonusIndex = 0;
        String header = "workload,variant,structure,n,time_ms,steps,moves,comparisons";
        try (PrintWriter summary = writer(output.resolve("results.csv"));
             PrintWriter raw = writer(output.resolve("runs.csv"));
             PrintWriter bonusCsv = writer(output.resolve("build_heap.csv"))) {
            summary.println(header);
            raw.println(header + ",phase,trial");
            bonusCsv.println(header);
            for (int n : SIZES) {
                Input input = new Input(n);
                for (String workload : new String[]{"W1", "W2", "W3"}) {
                    String[] variants = workload.equals("W3") ? new String[]{"head", "middle"} : new String[]{"-"};
                    for (String variant : variants) {
                        for (String structure : new String[]{"DynamicArray", "MyLinkedList"}) {
                            Result median = runCase(workload, variant, structure, input, raw);
                            results[resultIndex++] = median;
                            summary.println(median.csv());
                            System.out.println(median.csv());
                        }
                    }
                }
                Result heap = runCase("W4", "-", "MinHeap", input, raw);
                results[resultIndex++] = heap;
                summary.println(heap.csv());
                System.out.println(heap.csv());
                for (String variant : new String[]{"random", "descending"}) {
                    for (String method : new String[]{"Floyd", "RepeatedInsert"}) {
                        Result median = runCase("B", variant, method, input, raw);
                        bonus[bonusIndex++] = median;
                        bonusCsv.println(median.csv());
                    }
                }
            }
        }
        Plotter.writePlots(results, bonus, output.resolve("plots"));
        writeMetadata(output);
        System.out.println("Saved 36 required cases, 16 bonus cases and PNG plots in " + output.toAbsolutePath());
    }

    private static PrintWriter writer(Path path) throws IOException {
        return new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8));
    }

    private static void primeJvm() {
        // Compile the shared operation and workload paths before collecting small-n cases.
        Input input = new Input(1_000);
        for (int round = 0; round < 60; round++) {
            for (String structure : new String[]{"DynamicArray", "MyLinkedList"}) {
                runSequence("W1", "-", structure, input);
                runSequence("W2", "-", structure, input);
                runSequence("W3", "head", structure, input);
                runSequence("W3", "middle", structure, input);
            }
            runHeap(input);
            for (String variant : new String[]{"random", "descending"}) {
                runBuild(variant, "Floyd", input);
                runBuild(variant, "RepeatedInsert", input);
            }
        }
        System.out.println("JVM primed: 60 untimed rounds at n=1000; beginning per-case warm-ups and measurements.");
    }

    private static Result runCase(String workload, String variant, String structure,
                                  Input input, PrintWriter raw) {
        Result[] measured = new Result[REPEATS];
        for (int run = 0; run < WARMUPS + REPEATS; run++) {
            Result result = workload.equals("W4") ? runHeap(input)
                    : workload.equals("B") ? runBuild(variant, structure, input)
                    : runSequence(workload, variant, structure, input);
            boolean warmup = run < WARMUPS;
            raw.println(result.csv() + "," + (warmup ? "warmup" : "measured")
                    + "," + (warmup ? run + 1 : run - WARMUPS + 1));
            if (!warmup) measured[run - WARMUPS] = result;
        }
        // Five observations: insertion sort avoids a collection or boxed values.
        for (int i = 1; i < measured.length; i++) {
            Result value = measured[i];
            int j = i - 1;
            while (j >= 0 && measured[j].timeMs() > value.timeMs()) {
                measured[j + 1] = measured[j];
                j--;
            }
            measured[j + 1] = value;
        }
        return measured[REPEATS / 2];
    }

    static Result runSequence(String workload, String variant, String structure, Input input) {
        IntSequence sequence = structure.equals("DynamicArray") ? new DynamicArray() : new MyLinkedList();
        for (int value : input.values) sequence.add(value);
        sequence.metrics().reset();
        int[] removed = new int[CHANGES];
        long checksum = 0;
        long start = System.nanoTime();
        switch (workload) {
            case "W1" -> {
                for (int index : input.indices) checksum += sequence.get(index);
            }
            case "W2" -> {
                for (int query : input.queries) if (sequence.contains(query)) checksum++;
            }
            case "W3" -> {
                int index = variant.equals("head") ? 0 : input.values.length / 2;
                for (int value : input.inserted) sequence.add(index, value);
                for (int i = 0; i < CHANGES; i++) removed[i] = sequence.remove(index);
            }
            default -> throw new IllegalArgumentException("unknown workload " + workload);
        }
        long elapsed = System.nanoTime() - start;
        Result result = result(workload, variant, structure, input.values.length, elapsed, sequence.metrics());
        if (workload.equals("W1")) {
            long expected = 0;
            for (int index : input.indices) expected += input.values[index];
            require(checksum == expected, "W1 checksum");
        } else if (workload.equals("W2")) {
            require(checksum == SEARCHES / 2, "W2 must have 500 hits and 500 misses");
        } else {
            require(sequence.size() == input.values.length, "W3 size");
            for (int i = 0; i < CHANGES; i++) {
                require(removed[i] == input.inserted[CHANGES - 1 - i], "W3 removal order");
                checksum += removed[i];
            }
            require(sequence.get(0) == input.values[0], "W3 first element");
            require(sequence.get(sequence.size() - 1) == input.values[input.values.length - 1], "W3 last element");
        }
        blackhole = checksum;
        return result;
    }

    static Result runHeap(Input input) {
        MinHeap heap = new MinHeap();
        int[] extracted = new int[input.values.length];
        long start = System.nanoTime();
        for (int value : input.values) heap.insert(value);
        for (int i = 0; i < extracted.length; i++) extracted[i] = heap.extractMin();
        long elapsed = System.nanoTime() - start;
        Result result = result("W4", "-", "MinHeap", input.values.length, elapsed, heap.metrics());
        long checksum = validateSorted(extracted);
        long expected = 0;
        for (int value : input.values) expected += value;
        require(checksum == expected && heap.size() == 0, "W4 contents");
        blackhole = checksum;
        return result;
    }

    private static Result runBuild(String variant, String method, Input input) {
        int[] data = input.values;
        if (variant.equals("descending")) {
            data = new int[input.values.length];
            for (int i = 0; i < data.length; i++) data[i] = data.length - i;
        }
        MinHeap heap;
        long start = System.nanoTime();
        if (method.equals("Floyd")) heap = MinHeap.buildHeap(data);
        else {
            heap = new MinHeap();
            for (int value : data) heap.insert(value);
        }
        long elapsed = System.nanoTime() - start;
        Result result = result("B", variant, method, data.length, elapsed, heap.metrics());
        int[] extracted = new int[data.length];
        for (int i = 0; i < data.length; i++) extracted[i] = heap.extractMin();
        blackhole = validateSorted(extracted);
        return result;
    }

    private static Result result(String workload, String variant, String structure,
                                 int n, long nanos, Metrics metrics) {
        return new Result(workload, variant, structure, n, nanos / 1_000_000.0,
                metrics.steps(), metrics.moves(), metrics.comparisons());
    }

    private static long validateSorted(int[] values) {
        long sum = 0;
        for (int i = 0; i < values.length; i++) {
            require(i == 0 || values[i - 1] <= values[i], "heap output must be non-decreasing");
            sum += values[i];
        }
        return sum;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void writeMetadata(Path output) throws IOException {
        String text = "Seed: 42\nGlobal JVM priming: 60 discarded rounds of all workloads at n=1000\nWarm-up runs per case: " + WARMUPS + "\nMeasured runs per case: " + REPEATS
                + "\nStatistic: median time; counters from the same run\n"
                + "W1-W3: initial fill excluded; W4: insertion and extraction included\n"
                + "W3: 1000 insertions, then 1000 removals; fixed index 0 or original n/2\n"
                + "Verification, data generation and plotting excluded from timing/counters\n"
                + "Instrumentation enabled during timings; fixed ascending n and array-before-list case order\n"
                + "Clock: System.nanoTime; no forced GC; single JVM and single benchmark thread\n"
                + "Java: " + System.getProperty("java.runtime.version") + "\nVM: " + System.getProperty("java.vm.name")
                + "\nOS: " + System.getProperty("os.name") + " " + System.getProperty("os.version")
                + " " + System.getProperty("os.arch") + "\nAvailable processors: " + Runtime.getRuntime().availableProcessors()
                + "\nMax JVM heap bytes: " + Runtime.getRuntime().maxMemory() + "\n";
        Files.writeString(output.resolve("environment.txt"), text, StandardCharsets.UTF_8);
    }
}
