package dev.morling.onebrc.xiaolikey;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * Java Microbenchmark Harness
 *
 * @author xiaolikey
 * @date 2024/12/16
 * @since 0.0.1
 */
public class Jmh {

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(CalculateAverageV1.class.getSimpleName())
                .warmupIterations(0)
                .measurementIterations(1)
                .measurementBatchSize(1)
                .measurementTime(TimeValue.seconds(10))
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}
