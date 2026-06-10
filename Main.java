import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    // Bir thread konveks olmayan durum bulduğunda diğerlerini durdurmak için kullanılır.
    static class StopSignal {
        volatile boolean durdur;
    }

    // Her thread kendi sonucunu ayrı tuttuğu için ortak değişkene yazma sorunu oluşmaz.
    static class ThreadResult {
        private final boolean locallyConvex;
        private final int direction;
        private final int startIndex;
        private final int endIndex;

        ThreadResult(boolean locallyConvex, int direction, int startIndex, int endIndex) {
            this.locallyConvex = locallyConvex;
            this.direction = direction;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        boolean isLocallyConvex() {
            return locallyConvex;
        }

        int getDirection() {
            return direction;
        }

        int getStartIndex() {
            return startIndex;
        }

        int getEndIndex() {
            return endIndex;
        }
    }

    // Kendisine verilen nokta aralığında konvekslik kontrolü yapan worker thread.
    static class ConvexWorkerThread extends Thread {
        private final int[] xCoords;
        private final int[] yCoords;
        private final int startIndex;
        private final int endIndex;
        private final StopSignal stopSignal;
        private ThreadResult result;

        ConvexWorkerThread(int[] xCoords, int[] yCoords, int startIndex, int endIndex, StopSignal stopSignal) {
            this.xCoords = xCoords;
            this.yCoords = yCoords;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.stopSignal = stopSignal;
        }

        @Override
        public void run() {
            result = checkRange(startIndex, endIndex);
        }

        ThreadResult getResult() {
            return result;
        }

        private ThreadResult checkRange(int start, int end) {
            int pointCount = xCoords.length;
            int referenceDirection = 0;
            boolean isLocallyConvex = true;

            for (int i = start; i < end && !stopSignal.durdur; i++) {
                // Çokgen döngüsel olduğu için son noktadan sonra ilk noktaya dönülür.
                int index0 = i;
                int index1 = (i + 1) % pointCount;
                int index2 = (i + 2) % pointCount;

                int x0 = xCoords[index0];
                int y0 = yCoords[index0];
                int x1 = xCoords[index1];
                int y1 = yCoords[index1];
                int x2 = xCoords[index2];
                int y2 = yCoords[index2];

                // Cross product işareti dönüş yönünü verir.
                long cross = (long) (x1 - x0) * (y2 - y0) - (long) (y1 - y0) * (x2 - x0);
                int currentDirection = sign(cross);

                if (currentDirection == 0) {
                    continue;
                }

                if (referenceDirection == 0) {
                    referenceDirection = currentDirection;
                    continue;
                }

                if (currentDirection != referenceDirection) {
                    isLocallyConvex = false;
                    stopSignal.durdur = true;
                    break;
                }
            }

            return new ThreadResult(isLocallyConvex, referenceDirection, start, end);
        }

        private int sign(long value) {
            if (value > 0) {
                return 1;
            }
            if (value < 0) {
                return -1;
            }
            return 0;
        }
    }

    static class ConvexChecker {
        static final int[] VARSAYILAN_THREAD_SAYILARI = {1, 2, 4, 8};

        boolean isConvex(int[] coords) {
            return isConvex(coords, 4);
        }

        boolean isConvex(int[] coords, int threadCount) {
            if (coords == null || coords.length < 6 || coords.length % 2 != 0) {
                return false;
            }

            int pointCount = coords.length / 2;
            if (pointCount < 3) {
                return false;
            }

            int[] xCoords = new int[pointCount];
            int[] yCoords = new int[pointCount];
            for (int i = 0; i < pointCount; i++) {
                xCoords[i] = coords[2 * i];
                yCoords[i] = coords[2 * i + 1];
            }

            StopSignal stopSignal = new StopSignal();
            threadCount = Math.max(1, Math.min(threadCount, pointCount));
            int parcaBoyutu = (pointCount + threadCount - 1) / threadCount;

            List<ConvexWorkerThread> threads = new ArrayList<ConvexWorkerThread>();

            // Noktalar thread sayısına göre parçalara bölünür ve thread'ler başlatılır.
            for (int t = 0; t < threadCount; t++) {
                int start = t * parcaBoyutu;
                int end = start + parcaBoyutu;
                if (end > pointCount) {
                    end = pointCount;
                }
                if (start >= end) {
                    break;
                }

                ConvexWorkerThread thread = new ConvexWorkerThread(
                        xCoords, yCoords, start, end, stopSignal);
                threads.add(thread);
                thread.start();
            }

            List<ThreadResult> results = new ArrayList<ThreadResult>();

            // Ana thread, worker thread'lerin bitmesini bekler.
            for (int i = 0; i < threads.size(); i++) {
                ConvexWorkerThread thread = threads.get(i);
                try {
                    while (thread.isAlive()) {
                        thread.join();
                    }
                    results.add(thread.getResult());
                } catch (InterruptedException e) {
                    System.out.println(e);
                    Thread.currentThread().interrupt();
                    return false;
                }
            }

            return mergeResults(results);
        }

        boolean isConvexSerial(int[] coords) {
            if (coords == null || coords.length < 6 || coords.length % 2 != 0) {
                return false;
            }

            int pointCount = coords.length / 2;
            if (pointCount < 3) {
                return false;
            }

            int referenceDirection = 0;

            for (int i = 0; i < pointCount; i++) {
                int x0 = coords[2 * i];
                int y0 = coords[2 * i + 1];
                int x1 = coords[2 * ((i + 1) % pointCount)];
                int y1 = coords[2 * ((i + 1) % pointCount) + 1];
                int x2 = coords[2 * ((i + 2) % pointCount)];
                int y2 = coords[2 * ((i + 2) % pointCount) + 1];

                long cross = (long) (x1 - x0) * (y2 - y0) - (long) (y1 - y0) * (x2 - x0);
                int currentDirection = sign(cross);

                if (currentDirection == 0) {
                    continue;
                }
                if (referenceDirection == 0) {
                    referenceDirection = currentDirection;
                    continue;
                }
                if (currentDirection != referenceDirection) {
                    return false;
                }
            }
            return true;
        }

        // Thread sonuçları birleştirilerek nihai konvekslik kararı verilir.
        private boolean mergeResults(List<ThreadResult> results) {
            int globalDirection = 0;

            for (int i = 0; i < results.size(); i++) {
                ThreadResult result = results.get(i);

                if (!result.isLocallyConvex()) {
                    return false;
                }

                int localDirection = result.getDirection();
                if (localDirection == 0) {
                    continue;
                }

                if (globalDirection == 0) {
                    globalDirection = localDirection;
                    continue;
                }

                if (localDirection != globalDirection) {
                    return false;
                }
            }
            return true;
        }

        private int sign(long value) {
            if (value > 0) return 1;
            if (value < 0) return -1;
            return 0;
        }

        long benchmark(int[] coords, int threadCount, int runs) {
            isConvex(coords, threadCount);

            long total = 0;
            for (int i = 0; i < runs; i++) {
                long start = System.nanoTime();
                boolean sonuc = isConvex(coords, threadCount);
                long elapsed = System.nanoTime() - start;

                if (!sonuc) {
                    throw new IllegalStateException("Beklenen konveks çokgen değil.");
                }
                total += elapsed;
            }

            return total / runs;
        }

        // Amdahl yasasına göre seri bölüm oranı tahmini yapılır.
        double estimateSerialFraction(double speedup, int threadCount) {
            if (threadCount <= 1 || speedup <= 0) {
                return 0.0;
            }
            double p = threadCount;
            double f = (1.0 / speedup - 1.0 / p) / (1.0 - 1.0 / p);

            if (f < 0.0) return 0.0;
            if (f > 1.0) return 1.0;
            return f;
        }

        int[] generateConvexPolygon(int pointCount) {
            if (pointCount < 4) {
                throw new IllegalArgumentException("Nokta sayısı en az 4 olmalıdır.");
            }

            int kenarBasi = Math.max(1, pointCount / 4);
            int ekstra = pointCount - kenarBasi * 4;
            int[] kenarUzunluk = new int[4];
            for (int i = 0; i < 4; i++) {
                kenarUzunluk[i] = kenarBasi + (i < ekstra ? 1 : 0);
            }

            int[] coords = new int[pointCount * 2];
            int idx = 0;
            int genislik = kenarBasi + (ekstra > 0 ? 1 : 0);
            int yukseklik = kenarBasi + (ekstra > 1 ? 1 : 0);

            for (int i = 0; i < kenarUzunluk[0]; i++) {
                coords[2 * idx] = i;
                coords[2 * idx + 1] = 0;
                idx++;
            }
            for (int i = 0; i < kenarUzunluk[1]; i++) {
                coords[2 * idx] = genislik;
                coords[2 * idx + 1] = i;
                idx++;
            }
            for (int i = 0; i < kenarUzunluk[2]; i++) {
                coords[2 * idx] = genislik - i;
                coords[2 * idx + 1] = yukseklik;
                idx++;
            }
            for (int i = 0; i < kenarUzunluk[3]; i++) {
                coords[2 * idx] = 0;
                coords[2 * idx + 1] = yukseklik - i;
                idx++;
            }
            return coords;
        }
    }

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        ConvexChecker checker = new ConvexChecker();

        System.out.println("=== Paralel Konveks Kontrol Projesi ===\n");

        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        int[] kullaniciCokgen = okuCokgen(scanner);
        int kullaniciThread = Math.min(4, Math.max(1, kullaniciCokgen.length / 2));

        runTest(checker, "Kullanici girisi", kullaniciCokgen, kullaniciThread);

        int pointCount = 500000;
        int runs = 5;
        int[] threadCounts = ConvexChecker.VARSAYILAN_THREAD_SAYILARI;
        String csvFile = null;

        if (args.length >= 1) {
            try { pointCount = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }
        if (args.length >= 2) {
            try { runs = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
        }
        if (args.length >= 3) {
            try {
                String[] parts = args[2].split(",");
                threadCounts = new int[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    threadCounts[i] = Integer.parseInt(parts[i].trim());
                }
            } catch (Exception ignored) {}
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--csv=")) {
                csvFile = args[i].substring("--csv=".length());
            }
        }
        int[] buyukCokgen = checker.generateConvexPolygon(pointCount);

        System.out.println("--- Amdahl Benchmark (" + pointCount + " nokta, " + runs + " tekrar) ---");
        System.out.printf("%12s | %12s | %8s | %12s%n",
                "Thread", "Sure (ms)", "Hizlanma", "Tahmini F");
        System.out.printf("%12s-+-%12s-+-%8s-+-%12s%n",
                "------------", "------------", "--------", "------------");

        PrintWriter csvWriter = null;
        try {
            if (csvFile != null) {
                csvWriter = new PrintWriter(new FileWriter(csvFile, false));
                csvWriter.println("threads,ms,speedup,F");
            }
        } catch (IOException e) {
            System.err.println("CSV acilamadi: " + e.getMessage());
        }

        long baselineNanos = -1;

        for (int i = 0; i < threadCounts.length; i++) {
            int threadCount = threadCounts[i];

            long avgNanos = checker.benchmark(buyukCokgen, threadCount, runs);

            if (threadCount == 1) {
                baselineNanos = avgNanos;
            }

            double speedup = baselineNanos > 0 ? (double) baselineNanos / avgNanos : 1.0;
            double serialFraction = threadCount == 1 ? 0.0
                    : checker.estimateSerialFraction(speedup, threadCount);
            double ms = avgNanos / 1000000.0;

            System.out.printf("%12d | %12.2f | %8.2f | %12.4f%n",
                    threadCount, ms, speedup, serialFraction);

            if (csvWriter != null) {
                csvWriter.printf("%d,%.4f,%.4f,%.6f%n", threadCount, ms, speedup, serialFraction);
            }
        }

        if (csvWriter != null) {
            csvWriter.flush();
            csvWriter.close();
            System.out.println("CSV yazildi: " + csvFile);
        }

        System.out.println();
        System.out.println("Amdahl yasasi: S(P) = 1 / (F + (1-F)/P)");
        System.out.println("F = seri bolum orani, P = thread sayisi");
        scanner.close();
    }

    private static int[] okuCokgen(Scanner scanner) {
        System.out.print("Nokta sayisi : ");
        int noktaSayisi = scanner.nextInt();
        scanner.nextLine();

        int[] coords = new int[noktaSayisi * 2];
        for (int i = 0; i < noktaSayisi; i++) {
            System.out.print((i + 1) + ". nokta : ");
            String satir = scanner.nextLine().trim();
            String[] parcalar = satir.split("[,\\s]+");
            coords[2 * i] = Integer.parseInt(parcalar[0].trim());
            coords[2 * i + 1] = Integer.parseInt(parcalar[1].trim());
        }
        return coords;
    }

    private static void runTest(ConvexChecker checker, String testName, int[] coords, int threadCount) {
        boolean sonuc = checker.isConvex(coords, threadCount);
        System.out.println("Test: " + testName);
        System.out.println("Nokta sayisi: " + (coords.length / 2));
        System.out.println("Sonuc: " + (sonuc ? "CONVEX" : "CONVEX DEGIL"));
        System.out.println();
    }
}
