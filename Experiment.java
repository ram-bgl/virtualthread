import jdk.jfr.consumer.EventStream;

import java.math.BigInteger;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import static java.math.BigInteger.ZERO;
import static java.math.BigInteger.valueOf;

// Credit: Victor Rentea: https://github.com/victorrentea/java-latest
public class Experiment {

    public static void main(String[] args) throws Exception {
//        listenForJFREvents();
        Util.sleepMillis(1000);
        RunMonitor monitor = new RunMonitor();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int taskId = 0; taskId < 30; taskId++) {
                Runnable work = () -> {
                    io();
//                    cpu();
//                    locks();
                };
                executor.submit(monitor.run(taskId, work));
            }

            System.out.println("Waiting for tasks to finish...");
        }
        monitor.printExecutionTimes();
    }

    private static void listenForJFREvents() {
        Thread listenerThread = new Thread(() -> {
            System.out.println("------------Listening------------");
            try (EventStream stream = EventStream.openRepository()) {
                stream.onEvent("jdk.VirtualThreadPinned", event -> {
                    System.out.println("------------Virtual Thread Pinned Event Detected------------");
                });
                stream.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        listenerThread.setDaemon(true); // Ensure the listener does not block application shutdown
        listenerThread.start();
    }

    /* ============ I/O ============= */
    private static void io() {
        Util.sleepMillis(100);
    }

    /* ============ CPU ============= */
    public static long blackHole;

    public static void cpu() {
        BigInteger res = ZERO;
        for (int j = 0; j < 10_000_000; j++) { // decrease this number for slower machines
            res = res.add(valueOf(j).sqrt());
        }
        blackHole = res.longValue();
    }

    /* ============ LOCKS ============= */
    static int sharedMutable;
    static ReentrantLock lock = new ReentrantLock();

    public synchronized static void locks() {
        sharedMutable++;
        Util.sleepMillis(100); // long operation (eg network) in sync block
    }

}

