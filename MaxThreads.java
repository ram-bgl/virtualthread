import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class MaxThreads {
    public static void main(String[] args) throws InterruptedException, IOException {
        if (args.length > 0)
            checkThreadLimit(Integer.valueOf(args[0]));
        else
            checkThreadLimit(1_000);
        /* java -XX:NativeMemoryTracking=summary MaxThreads */
//        analyzeMemoryUsage();
    }

    private static void checkThreadLimit(int limit) throws InterruptedException {

//        final int TOTAL_NUM_THREADS = 1_000;
        final int TOTAL_NUM_THREADS = limit;

        /* Toggle between virtual and platform via builder object. */
//        Thread.Builder builder = Thread.ofVirtual();
        Thread.Builder builder = Thread.ofPlatform();

        var threads = IntStream.range(0, TOTAL_NUM_THREADS).mapToObj(index -> builder.unstarted(() -> {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        })).toList();

        Instant begin = Instant.now();
        var i = 0;
        for (var thread : threads) {
            /* Enable only for platform thread to show where OOM occurs */
            System.out.println(++i);
            thread.start();
        }
        for (var thread : threads) {
            thread.join();
        }
        Instant end = Instant.now();
        System.out.printf("\nTime taken to run = %ss\n", Duration.between(begin, end).toSeconds());
    }

    private static void analyzeMemoryUsage() throws InterruptedException, IOException {

        final int TOTAL_NUM_THREADS = 1_000;

        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch started = new CountDownLatch(TOTAL_NUM_THREADS);
        CountDownLatch exit = new CountDownLatch(1);

        /* Toggle between virtual and platform via builder object. */
//        Thread.Builder builder = Thread.ofVirtual();
        Thread.Builder builder = Thread.ofPlatform();

        System.out.printf("Current process: \"%s\"\n", ManagementFactory.getRuntimeMXBean().getName());
        System.out.printf("jcmd %d VM.native_memory baseline\n", ProcessHandle.current().pid());
        System.out.println("Press enter when done!\n");
        System.in.read();

        var threads = IntStream.range(0, TOTAL_NUM_THREADS).
                mapToObj(index -> builder.start(
                        () -> {
                            try {
                                counter.incrementAndGet();
                                started.countDown();
                                /* Let all threads wait so that we can get the snapshot. */
                                exit.await();
                                counter.decrementAndGet();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        })
                ).toList();

        System.out.println("Waiting for all tasks to start...");
        started.await();
        System.out.println("All tasks started!!!");
        System.out.printf("Now running threads: %d\n\n", counter.get());

        System.out.printf("jcmd %d VM.native_memory summary.diff\n", ProcessHandle.current().pid());
        System.out.println("Hit Enter to end");
        System.in.read();
        /* Release all the threads. */
        exit.countDown();

        for (var thread : threads) {
            thread.join();
        }
        System.out.printf("All threads ended! Number of threads still left:%d \n", counter.get());
    }

    private static long getUsedMemory() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }
}
