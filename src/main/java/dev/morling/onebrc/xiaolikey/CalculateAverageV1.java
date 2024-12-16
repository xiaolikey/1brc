package dev.morling.onebrc.xiaolikey;

import org.openjdk.jmh.annotations.Benchmark;

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
public class CalculateAverageV1 implements ICalculateAverage {

    @Override
    public void dose(String filePath) throws Exception {
        Map<String, DoubleSummaryStatistics> stations = new ConcurrentHashMap<>();
        try (Stream<String> stream = Files.lines(Paths.get(filePath)).parallel()) {
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
        System.out.println(new TreeMap<String, DoubleSummaryStatistics>(stations));
    }


    @Benchmark
    @Override
    public void dose1m() throws Exception {
        ICalculateAverage.super.dose1m();
    }

    @Benchmark
    @Override
    public void dose100m() throws Exception {
        ICalculateAverage.super.dose1b();
    }
}
