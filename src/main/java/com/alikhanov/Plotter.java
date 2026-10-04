package com.alikhanov;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Dependency-free PNG charts; primitive arrays hold series data. */
public final class Plotter {
    private static final Color[] COLORS = {new Color(33, 104, 178), new Color(224, 101, 35),
            new Color(29, 143, 112), new Color(139, 76, 172)};
    private static final String[] METRICS = {"Time (ms)", "Steps (count)", "Moves (count)", "Comparisons (count)"};

    private Plotter() { }

    public static void main(String[] args) throws IOException {
        Path directory = Path.of(args.length == 0 ? "results" : args[0]);
        writePlots(readCsv(directory.resolve("results.csv"), 36),
                readCsv(directory.resolve("build_heap.csv"), 16), directory.resolve("plots"));
    }

    private static Benchmark.Result[] readCsv(Path file, int count) throws IOException {
        Benchmark.Result[] results = new Benchmark.Result[count];
        try (var reader = Files.newBufferedReader(file)) {
            reader.readLine();
            for (int i = 0; i < count; i++) {
                String line = reader.readLine();
                if (line == null) throw new IOException("missing rows in " + file);
                String[] fields = line.split(",");
                results[i] = new Benchmark.Result(fields[0], fields[1], fields[2],
                        Integer.parseInt(fields[3]), Double.parseDouble(fields[4]),
                        Long.parseLong(fields[5]), Long.parseLong(fields[6]), Long.parseLong(fields[7]));
            }
            if (reader.readLine() != null) throw new IOException("unexpected extra rows in " + file);
        }
        return results;
    }

    static void writePlots(Benchmark.Result[] results, Benchmark.Result[] bonus, Path directory) throws IOException {
        Files.createDirectories(directory);
        for (String workload : new String[]{"W1", "W2", "W3", "W4"}) {
            String title = switch (workload) {
                case "W1" -> "W1 - 10,000 random indexed reads";
                case "W2" -> "W2 - 1,000 searches (50% present)";
                case "W3" -> "W3 - 1,000 insertions + 1,000 removals";
                default -> "W4 - n insertions + n minimum extractions";
            };
            String[] structures = workload.equals("W4") ? new String[]{"MinHeap"}
                    : workload.equals("W3") ? new String[]{"DynamicArray", "MyLinkedList", "DynamicArray", "MyLinkedList"}
                    : new String[]{"DynamicArray", "MyLinkedList"};
            String[] variants = workload.equals("W3") ? new String[]{"head", "head", "middle", "middle"}
                    : new String[]{"-", "-"};
            draw(results, workload, structures, variants, title, directory.resolve(workload.toLowerCase(Locale.ROOT) + ".png"));
        }
        draw(bonus, "B", new String[]{"Floyd", "RepeatedInsert", "Floyd", "RepeatedInsert"},
                new String[]{"random", "random", "descending", "descending"},
                "Bonus B - Floyd construction vs repeated insertions", directory.resolve("build_heap.png"));
    }

    private static void draw(Benchmark.Result[] rows, String workload, String[] structures,
                             String[] variants, String title, Path output) throws IOException {
        BufferedImage image = new BufferedImage(1600, 1000, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 1600, 1000);
        g.setColor(new Color(25, 40, 65));
        g.setFont(new Font("SansSerif", Font.BOLD, 32));
        g.drawString(title, 38, 44);
        for (int metric = 0; metric < 4; metric++) {
            panel(g, rows, workload, structures, variants, metric, 32 + (metric % 2) * 790, 76 + (metric / 2) * 410);
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, 23));
        for (int i = 0; i < structures.length; i++) {
            int x = 50 + (i % 2) * 780;
            int y = 923 + (i / 2) * 34;
            g.setColor(COLORS[i]);
            g.setStroke(i >= 2 ? new BasicStroke(4, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND, 10, new float[]{10, 7}, 0) : new BasicStroke(4));
            g.drawLine(x, y - 8, x + 32, y - 8);
            g.setColor(new Color(40, 50, 70));
            g.drawString(structures[i] + (variants[i].equals("-") ? "" : " / " + variants[i]), x + 45, y);
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.setColor(Color.DARK_GRAY);
        g.drawString("Median of 5 after 2 warm-ups. x: log10(n). y: log10(1 + value/unit); unit = 0.001 ms or 1 operation.", 40, 986);
        g.dispose();
        ImageIO.write(image, "png", output.toFile());
    }

    private static void panel(Graphics2D g, Benchmark.Result[] rows, String workload,
                              String[] structures, String[] variants, int metric, int x, int y) {
        int left = x + 115;
        int right = x + 715;
        int top = y + 40;
        int bottom = y + 320;
        double unit = metric == 0 ? 0.001 : 1;
        double maximum = 0;
        for (Benchmark.Result row : rows) {
            if (row.workload().equals(workload)) maximum = Math.max(maximum, value(row, metric));
        }
        double ymax = Math.log10(1 + Math.max(maximum, unit) / unit) * 1.09;
        g.setColor(new Color(25, 40, 65));
        g.setFont(new Font("SansSerif", Font.BOLD, 25));
        g.drawString(METRICS[metric], left, y + 19);
        g.setFont(new Font("SansSerif", Font.PLAIN, 21));
        int powers = (int) Math.ceil(Math.log10(Math.max(maximum / unit, 1)));
        int stride = powers > 6 ? 2 : 1;
        for (int tick = -1; tick <= powers; tick += tick == -1 ? 1 : stride) {
            double value = tick < 0 ? 0 : unit * Math.pow(10, tick);
            double transformed = Math.log10(1 + value / unit);
            if (transformed > ymax) continue;
            int py = bottom - (int) (transformed / ymax * (bottom - top));
            if (value > 0 && bottom - py < 24) continue;
            g.setColor(new Color(224, 231, 239));
            g.drawLine(left, py, right, py);
            g.setColor(new Color(65, 75, 90));
            String label = format(value);
            g.drawString(label, left - 12 - g.getFontMetrics().stringWidth(label), py + 7);
        }
        for (int i = 0; i < Benchmark.SIZES.length; i++) {
            int px = left + i * (right - left) / 3;
            g.setColor(new Color(224, 231, 239));
            g.drawLine(px, top, px, bottom);
            g.setColor(new Color(65, 75, 90));
            String label = new String[]{"100", "1,000", "10,000", "100,000"}[i];
            g.drawString(label, px - g.getFontMetrics().stringWidth(label) / 2, bottom + 29);
        }
        g.drawString("n (initial elements)", left + 205, bottom + 60);
        g.drawLine(left, top, left, bottom);
        g.drawLine(left, bottom, right, bottom);
        for (int series = 0; series < structures.length; series++) {
            g.setColor(COLORS[series]);
            g.setStroke(series >= 2 ? new BasicStroke(3.5f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND, 10, new float[]{10, 7}, 0) : new BasicStroke(3.5f));
            int previousX = 0;
            int previousY = 0;
            boolean first = true;
            for (int n : Benchmark.SIZES) {
                Benchmark.Result match = null;
                for (Benchmark.Result row : rows) {
                    if (row.workload().equals(workload) && row.structure().equals(structures[series])
                            && row.variant().equals(variants[series]) && row.n() == n) match = row;
                }
                if (match == null) throw new IllegalStateException("missing plot row");
                int px = left + (int) ((Math.log10(n) - 2) / 3 * (right - left));
                int py = bottom - (int) (Math.log10(1 + value(match, metric) / unit) / ymax * (bottom - top));
                if (!first) g.drawLine(previousX, previousY, px, py);
                g.fillOval(px - 5, py - 5, 10, 10);
                previousX = px;
                previousY = py;
                first = false;
            }
        }
        g.setStroke(new BasicStroke(1));
    }

    private static double value(Benchmark.Result row, int metric) {
        return switch (metric) {
            case 0 -> row.timeMs();
            case 1 -> row.steps();
            case 2 -> row.moves();
            default -> row.comparisons();
        };
    }

    private static String format(double number) {
        if (number == 0) return "0";
        if (number >= 10_000 || number < 0.01) return String.format(Locale.ROOT, "%.0e", number);
        if (number >= 1) return String.format(Locale.ROOT, "%.0f", number);
        return String.format(Locale.ROOT, "%.2f", number);
    }
}
