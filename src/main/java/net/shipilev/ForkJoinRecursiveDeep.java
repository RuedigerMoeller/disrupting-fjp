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
        static AtomicInteger computed = new AtomicInteger(0);
        private final int slices;

        public PiForkJoinTask(int slices) {
            this.slices = slices;
        }

        @Override
        protected Double compute() {
            if (slices <= 1) {
                double acc = 0D;
                for (int s = 0; s < slices; s++) {
                    acc += Shared.calculatePi(s);
                }
                computed.incrementAndGet();
                return acc;
            }

            int lslices = slices / 2;
            int rslices = slices - lslices;
            PiForkJoinTask t1 = new PiForkJoinTask(lslices);
            PiForkJoinTask t2 = new PiForkJoinTask(rslices);

            ForkJoinTask.invokeAll(t1, t2);

            return t1.join() + t2.join();
        }
    }

    @GenerateMicroBenchmark
    public double run() throws InterruptedException {
        return new PiForkJoinTask(Shared.SLICES).invoke();
    }

    public static void main( String arg[] ) throws InterruptedException {
        while ( true ) {
            PiForkJoinTask.computed.set(0);
            long tim = System.currentTimeMillis();
            final ForkJoinRecursiveDeep dis = new ForkJoinRecursiveDeep();
//            dis.setup();
            dis.run();
            System.out.println("Time:"+(System.currentTimeMillis()-tim)+" "+PiForkJoinTask.computed.get());
        }
    }

}
