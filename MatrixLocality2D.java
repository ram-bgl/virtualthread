import java.util.Random;

// Credit: AI-Generated
public class MatrixLocality2D {
    private static final int N = 512;
    private static final int WARMUP = 3;
    private static final int RUNS = 5;
    private static volatile double sink;

    public static void main(String[] args) {
        double[][] A = new double[N][N];
        double[][] B = new double[N][N];
        double[][] C = new double[N][N];

        fill(A, B);

        // Warmup
        for (int i = 0; i < WARMUP; i++) {
            multiplyIJK(A, B, C);
            sink = checksum(C);
            multiplyIKJ(A, B, C);
            sink = checksum(C);
        }

        long ijkTime = 0;
        long ikjTime = 0;

        for (int r = 0; r < RUNS; r++) {

            long t0 = System.nanoTime();
            multiplyIJK(A, B, C);
            long t1 = System.nanoTime();
            sink = checksum(C);
            ijkTime += (t1 - t0);

            zero(C);

            long t2 = System.nanoTime();
            multiplyIKJ(A, B, C);
            long t3 = System.nanoTime();
            sink = checksum(C);
            ikjTime += (t3 - t2);

            zero(C);
        }

        System.out.printf("ijk: %.3f ms%n", ijkTime / 1_000_000.0 / RUNS);
        System.out.printf("ikj: %.3f ms%n", ikjTime / 1_000_000.0 / RUNS);
        System.out.printf("ratio (ijk/ikj): %.2f%n", (double) ijkTime / ikjTime);
    }

    private static void multiplyIJK(double[][] A, double[][] B, double[][] C) {
        int n = A.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double sum = 0;
                for (int k = 0; k < n; k++) {
                    sum += A[i][k] * B[k][j]; // poor locality for B
                }
                C[i][j] = sum;
            }
        }
    }

    private static void multiplyIKJ(double[][] A, double[][] B, double[][] C) {
        int n = A.length;
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < n; k++) {
                double aik = A[i][k];
                for (int j = 0; j < n; j++) {
                    C[i][j] += aik * B[k][j]; // good locality
                }
            }
        }
    }

    private static void fill(double[][] A, double[][] B) {
        Random r = new Random(42);
        for (int i = 0; i < A.length; i++) {
            for (int j = 0; j < A.length; j++) {
                A[i][j] = r.nextDouble();
                B[i][j] = r.nextDouble();
            }
        }
    }

    private static void zero(double[][] C) {
        for (int i = 0; i < C.length; i++) {
            for (int j = 0; j < C.length; j++) {
                C[i][j] = 0;
            }
        }
    }

    private static double checksum(double[][] C) {
        double s = 0;
        for (double[] row : C) {
            for (double v : row) {
                s += v;
            }
        }
        return s;
    }
}