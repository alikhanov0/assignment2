# DAA Assignment 2 - Data Structures

**Yerbol Alikhan | SE-2517**

Public repository: https://github.com/alikhanov0/assignment2  
Submission branch: `main`; release tag: `v1.0`.

Primitive-int implementations of a dynamic array, singly linked list and binary min-heap, with operation counters, JUnit 5 tests and reproducible workloads. Bonus B (Floyd's linear-time heap construction) is included. Bonus A (JOL memory measurements) is not included.

## Build and run

Requirements: JDK 17 or newer and Maven 3.9+. Java sources target release 17; the recorded results used JDK 19.0.2 on Windows 11. The first Maven run needs internet access to download build/test dependencies. No Python, plotting package or external library is needed to run the Java project or produce PNG charts.

```bash
mvn clean verify
mvn compile exec:java
```

The first command runs all 20 tests. The second runs every required workload and bonus comparison and writes the CSV files and all five PNG charts. Allow roughly a minute depending on the computer. Run from the directory containing `pom.xml`.

To regenerate only charts from existing CSV files:

```bash
mvn compile
java -cp target/classes com.alikhanov.Plotter
```

## Files

| Path | Contents |
|---|---|
| `src/main/java/com/alikhanov/` | Three structures, shared interface, counters, benchmark and PNG plotter |
| `src/test/java/com/alikhanov/` | JUnit 5 correctness, edge-case, heap-property and counter tests |
| `results/results.csv` | 36 required cases, exact assignment column order |
| `results/runs.csv` | All 364 per-case runs: 104 warm-ups and 260 measured observations, including bonus cases |
| `results/build_heap.csv` | 16 bonus cases: Floyd vs insertion on random and descending data |
| `results/environment.txt` | Runtime, timing scope and warm-up configuration |
| `results/plots/` | W1-W4 and bonus charts; each has time and all three counters |
| `REPORT.md` | Report source with five explicit page sections |
| `output/pdf/REPORT.pdf` | The same report rendered as a five-page PDF |
| `scripts/render_report.py` | Optional PDF renderer (Python, reportlab and Pillow) |

## Workload definitions

Every size is tested: 100, 1,000, 10,000 and 100,000. Data comes from `new Random(42)` and is identical for competing structures. Initial values are nonnegative primitive ints; absent search keys are negative, so exactly half of the 1,000 search queries succeed. W1 uses 10,000 precomputed random indices. W3 performs all 1,000 insertions, then all 1,000 removals at a fixed index: 0 or the **original** n/2. This restores the initial sequence. W4 times all n insertions and all n extractions and verifies sorted output afterwards.

The JVM first performs 60 discarded rounds of all workload paths at n=1,000, then two discarded warm-ups for each case. The median of five measured times is recorded together with counters from that same run. All measured operations are instrumented. Input generation and validation are outside the timer. Initial filling is excluded in W1-W3 and included in W4. Case order is fixed, and timings are machine-dependent; operation counts are deterministic. `runs.csv` retains warm-ups for audit but they are excluded from medians.

## Counter conventions

- **steps:** one read of an array cell, or one traversal of a `next` link (including a traversal to null). A read of a node's primitive value is not a step under the assignment's definition.
- **moves:** one relocation of an existing int, including copies during growth, or one explicit update to a structural link (`head`, `tail`, `next`). New int writes are not shifts. Local cursor assignments and automatic default-null initialization are not structural link updates.
- **comparisons:** one comparison of two element values. Index, size, capacity and null checks are excluded.

`get(0)` therefore has one step in DynamicArray and zero traversal steps in MyLinkedList, although both take constant time. List head insertion/removal has nonzero **moves**. Diagnostic heap snapshots and verification are outside recorded counters. Heap storage doubles on demand; a resizing insertion can take linear time even though bubble-up alone is logarithmic.

## Design and tests

DynamicArray and MyLinkedList implement `IntSequence`. The list stores both head and tail: append is constant time, but indexed access starts at the head. MinHeap uses hole-based bubble-up/down and owns its backing array. `buildHeap(int[])` copies its input and processes internal nodes from the bottom up. Production structures contain no Java collection, generic element type or boxed-int storage. Only tests use standard collections as correctness oracles.

Tests compare thousands of mixed operations against `ArrayList`, `LinkedList` and `PriorityQueue`; inspect the heap after every insertion and extraction; cover empty/singleton structures, duplicates, int extremes, invalid indices and tail reuse; and check physical counters against hand-computed examples. Benchmark tests also check the exact hit ratio and exclusion of initial-fill counters.

## Report and submission

Read [REPORT.md](REPORT.md) or [the PDF](output/pdf/REPORT.pdf). Optional PDF regeneration:

```bash
python -m pip install reportlab pillow
python scripts/render_report.py
```

On Windows the renderer uses Arial; on Linux it uses DejaVu Sans when available. Java benchmarks regenerate their data and charts but do not rewrite the report's prose or example measurements. If you rerun them on another machine, update the numerical discussion before regenerating the PDF.

The Git history preserves `feature/array`, `feature/list`, `feature/heap` and `feature/metrics`, merged into `main`. The submission archive is named `assignment2.zip` as requested; it contains the tagged source, results and report, excluding build output and temporary files. Upload the archive to Moodle and include the public repository link above.
