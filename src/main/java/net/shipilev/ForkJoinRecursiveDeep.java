package net.shipilev;

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@State(Scope.Benchmark)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 20, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ForkJoinRecursiveDeep {

    /*
      The fork-join task below deeply recurses, up until the leaf
      contains a single slice.
     */

    static class PiForkJoinTask extends RecursiveTask<Double> {
        private final int slices;
        private final int slicesOffset;

        public PiForkJoinTask(int slices, int offset) {
            this.slices = slices;
            this.slicesOffset = offset;
        }

        @Override
        protected Double compute() {
            if ( slices == 0 )
                return 0.0;
            if (slices == 1) {
                return Shared.calculatePi(slicesOffset);
            }

            int lslices = slices / 2;
            int rslices = slices - lslices;
            PiForkJoinTask t1 = new PiForkJoinTask(lslices,slicesOffset);
            PiForkJoinTask t2 = new PiForkJoinTask(rslices,lslices+slicesOffset);

            ForkJoinTask.invokeAll(t1, t2);

            return t1.join() + t2.join();
        }
    }

    @GenerateMicroBenchmark
    public double run() throws InterruptedException {
        return pool.invoke(new PiForkJoinTask(Shared.SLICES,0));
    }

    static ForkJoinPool pool;
    public static void main(String arg[] ) throws InterruptedException {
        int numSlice = 1000000;
        int numIter = 100;

        int NUM_CORE = 4;
        String res[] = new String[NUM_CORE];
        for ( int i = 1; i <= NUM_CORE; i++ ) {
            long sum = 0;
            System.out.println("--------------------------");
            pool = new ForkJoinPool(i);
            for ( int ii = 0; ii < 20; ii++ ) {
                long tim = System.currentTimeMillis();
                final ForkJoinRecursiveDeep dis = new ForkJoinRecursiveDeep();
                System.out.println(dis.run());
                long t = System.currentTimeMillis()-tim;
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
