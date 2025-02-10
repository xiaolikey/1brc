package dev.morling.onebrc.xiaolikey;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.DoubleSummaryStatistics;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Calculate Average version1 implementation
 *
 * @author xiaolikey
 * @date 2024/12/16
 * @since 0.0.1
 */
public class CalculateAverageV1 {
    public static final String FILE_PATH = "./measurements.txt";

    public static void main(String[] args) throws Exception {
        Map<String, DoubleSummaryStatistics> stations = new ConcurrentHashMap<>();
        try (Stream<String> stream = Files.lines(Paths.get(FILE_PATH)).parallel()) {
            stream.forEach(line -> {
                String[] split = line.split(";");
                String key = split[0];
                double value = Double.parseDouble(split[1]);
                stations.computeIfAbsent(key, k -> new DoubleSummaryStatistics() {
                    @Override
                    public synchronized void accept(double value) {
                        super.accept(value);
                    }

                    @Override
                    public String toString() {
                        return String.format("%.1f/%.1f/%.1f", getMin(), getAverage(), getMax());
                    }
                }).accept(value);
            });
        }
        System.out.println(new TreeMap<>(stations));
    }
}
