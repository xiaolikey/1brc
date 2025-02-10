
package dev.morling.onebrc.xiaolikey;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

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
//                .include(CalculateAverageV1.class.getSimpleName())
                .include(Jmh.class.getSimpleName())
                .warmupIterations(1)
                .measurementIterations(1)
                .measurementBatchSize(1)
                .mode(Mode.SingleShotTime)
                .timeUnit(TimeUnit.MICROSECONDS)
                .measurementTime(TimeValue.seconds(10))
                .forks(1)
                .build();

        new Runner(opt).run();
    }

    @Benchmark
    public void testV1(){
        try{
            CalculateAverageV1.main(new String[]{});
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}