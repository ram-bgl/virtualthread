// Credit: Optimizing Java Book
public class Caching {
    private final int ARR_SIZE = 2 * 1024 * 1024;
    private final int[] testData = new int[ARR_SIZE];

    private void run() {
        System.out.println("Warmup started");
        for (int i = 0; i < 15_000; i += 1) {
            touchEveryLine();
            touchEveryItem();
        }
        System.out.println("Warmup finished");
        System.out.println("Line     Item");
        for (int i = 0; i < 100; i += 1) {
            long t0 = System.nanoTime();
            touchEveryLine();
            long t1 = System.nanoTime();
            touchEveryItem();
            long t2 = System.nanoTime();
            long elEveryLine = t1 - t0;
            long elEveryItem = t2 - t1;
            double diff = elEveryItem - elEveryLine;
            System.out.println(elEveryLine + " " + elEveryItem + " " + (diff * 100 / elEveryLine));
        }
    }

    private void touchEveryItem() {
        for (int i = 0; i < testData.length; i += 1)
            testData[i] += 1;
    }

    private void touchEveryLine() {
        for (int i = 0; i < testData.length; i += 32)
            testData[i] += 1;
    }

    public static void main(String[] args) {
        Caching c = new Caching();
        c.run();
    }
}
// end::CACHE_EFFECT[]
