package net.shipilev;

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(5)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ForkJoinReuse {

     /*
      The fork-join task below is used as "just" the Callable,
      and "reuses" the submitted tasks.
     */

    static class PiForkJoinTask extends RecursiveTask<Double> {
        private int slice;

        @Override
        protected Double compute() {
            return Shared.calculatePi(slice);
        }
    }

    @GenerateMicroBenchmark
    public double run() throws InterruptedException {
        List<PiForkJoinTask> tasks = new ArrayList<>();

        int stride = Shared.THREADS * 100;
        for (int i = 0; i < stride; i++) {
            PiForkJoinTask task = new PiForkJoinTask();
            task.slice = i;
            task.fork();
            tasks.add(task);
        }

        double acc = 0D;
        int s = stride;
        while (s < Shared.SLICES) {
            for (PiForkJoinTask task : tasks) {
                acc += task.join();
                task.reinitialize();
                task.slice = s;
                task.fork();
            }
            s += stride;
        }

        for (PiForkJoinTask task : tasks) {
            acc += task.join();
        }

        return acc;
    }

    static ForkJoinPool pool;
    public static void main(String arg[] ) throws InterruptedException, ExecutionException {
        int NUM_CORE = 4;
        String res[] = new String[NUM_CORE];
        for ( int i = 1; i <= NUM_CORE; i++ ) {
            long sum = 0;
            System.out.println("--------------------------");
            pool = new ForkJoinPool(i);
            for ( int ii = 0; ii < 20; ii++ ) {
                long tim = System.currentTimeMillis();
                final ForkJoinReuse dis = new ForkJoinReuse();
                ForkJoinTask<Double> submit = pool.submit(
                        new Callable<Double>() {
                            @Override
                            public Double call() {
                                try {
                                    return dis.run();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                return null;
                            }
                        }
                );
                Double aDouble = submit.get();
                long t = System.currentTimeMillis()-tim;
                System.out.println("pi "+ aDouble +" t "+t);
                if ( ii >= 10 )
                    sum += t;
            }
            res[i-1] = i+": "+(sum/10);
            pool.shutdown();
        }
        for (int i = 0; i < res.length; i++) {
            String re = res[i];
            System.out.println(re);
        }
    }
    
}
