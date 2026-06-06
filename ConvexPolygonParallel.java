import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ConvexPolygonParallel {
    // Basit bir 2B nokta sınıfı
    static class Point {
        final int x;
        final int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // Her thread belirli bir nokta aralığındaki cross product hesaplamalarından sorumlu.
    static class CrossProductTask extends Thread {
        private final List<Point> points;
        private final int startIndex;
        private final int endIndex;
        private final AtomicInteger signHolder;
        private final AtomicBoolean isConvex;

        CrossProductTask(List<Point> points, int startIndex, int endIndex, AtomicInteger signHolder, AtomicBoolean isConvex) {
            this.points = points;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.signHolder = signHolder;
            this.isConvex = isConvex;
        }

        @Override
        public void run() {
            int n = points.size();

            for (int i = startIndex; i < endIndex && isConvex.get(); i++) {
                Point a = points.get((i - 1 + n) % n);
                Point b = points.get(i);
                Point c = points.get((i + 1) % n);

                long cross = crossProduct(a, b, c);

                if (cross == 0) {
                    // Koliner üç nokta kendine özel bir yön belirlemez; bu durum normal kabul edilir.
                    continue;
                }

                int currentSign = cross > 0 ? 1 : -1;
                int prevSign = signHolder.get();

                if (prevSign == 0) {
                    // İlk geçerli yönü kaydetmeye çalışıyoruz.
                    signHolder.compareAndSet(0, currentSign);
                    prevSign = signHolder.get();
                }

                if (prevSign != 0 && currentSign != prevSign) {
                    // Eğer yön değişimi varsa çokgen convex değildir.
                    isConvex.set(false);
                    break;
                }
            }
        }

        // Üç nokta için cross product hesaplama.
        private long crossProduct(Point a, Point b, Point c) {
            long ux = b.x - a.x;
            long uy = b.y - a.y;
            long vx = c.x - b.x;
            long vy = c.y - b.y;
            return ux * vy - uy * vx;
        }
    }

    // int[] girişi Point nesnelerine dönüştürür.
    private static List<Point> convertToPoints(int[] coords) {
        List<Point> points = new ArrayList<>();
        for (int i = 0; i + 1 < coords.length; i += 2) {
            points.add(new Point(coords[i], coords[i + 1]));
        }
        return points;
    }

    // Çokgenin convex olup olmadığını paralel olarak kontrol eden overload'lar.
    // int[] girdi ile: varsayılan iş parçacığı sayısını coords uzunluğuna göre belirler.
    public static boolean isConvexPolygon(int[] coords) {
        return isConvexPolygon(coords, Math.min(4, coords == null ? 0 : coords.length / 2));
    }

    // int[] girdi + threadCount: convertToPoints sadece bir kere çağrılır ve
    // paralel kontrol List<Point> üzerinden yapılır (benchmark için seri maliyet ayrıştırıldı).
    public static boolean isConvexPolygon(int[] coords, int threadCount) {
        if (coords == null || coords.length % 2 != 0) {
            return false; // Uygun formatta değil.
        }
        List<Point> points = convertToPoints(coords);
        return isConvexPolygon(points, threadCount);
    }

    // List<Point> doğrudan verilirse dönüşüme gerek kalmaz.
    public static boolean isConvexPolygon(List<Point> points, int threadCount) {
        if (points == null || points.size() < 3) {
            return false;
        }

        int n = points.size();
        threadCount = Math.max(1, Math.min(threadCount, n));
        AtomicInteger signHolder = new AtomicInteger(0);
        AtomicBoolean isConvex = new AtomicBoolean(true);
        Thread[] workers = new Thread[threadCount];

        int range = (n + threadCount - 1) / threadCount;

        for (int t = 0; t < threadCount; t++) {
            int start = t * range;
            int end = Math.min(start + range, n);
            workers[t] = new CrossProductTask(points, start, end, signHolder, isConvex);
            workers[t].start();
        }

        // Tüm threadlerin bitmesini bekliyoruz.
        for (Thread worker : workers) {
            try {
                if (worker != null) {
                    worker.join();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                isConvex.set(false);
            }
        }

        return isConvex.get();
    }

    private static int[] generateConvexPolygon(int pointCount) {
        if (pointCount < 4) {
            throw new IllegalArgumentException("Nokta sayısı en az 4 olmalıdır.");
        }

        int baseSide = Math.max(1, pointCount / 4);
        int extra = pointCount - baseSide * 4;
        int[] lengths = new int[4];
        for (int i = 0; i < 4; i++) {
            lengths[i] = baseSide + (i < extra ? 1 : 0);
        }

        int[] coords = new int[pointCount * 2];
        int idx = 0;
        int width = baseSide + (extra > 0 ? 1 : 0);
        int height = baseSide + (extra > 1 ? 1 : 0);

        // Alt kenar
        for (int i = 0; i < lengths[0]; i++) {
            coords[2 * idx] = i;
            coords[2 * idx + 1] = 0;
            idx++;
        }

        // Sağ kenar
        for (int i = 0; i < lengths[1]; i++) {
            coords[2 * idx] = width;
            coords[2 * idx + 1] = i;
            idx++;
        }

        // Üst kenar
        for (int i = 0; i < lengths[2]; i++) {
            coords[2 * idx] = width - i;
            coords[2 * idx + 1] = height;
            idx++;
        }

        // Sol kenar
        for (int i = 0; i < lengths[3]; i++) {
            coords[2 * idx] = 0;
            coords[2 * idx + 1] = height - i;
            idx++;
        }

        return coords;
    }

    // Benchmark fonksiyonu artık List<Point> alır: dönüşüm yalnızca bir kere yapılır.
    private static long benchmarkConvexCheck(List<Point> points, int threadCount, int runs) {
        // İlk geçiş, JIT derleme ve JVM ısınması için.
        isConvexPolygon(points, threadCount);
        long totalNanos = 0;
        for (int i = 0; i < runs; i++) {
            long start = System.nanoTime();
            boolean convex = isConvexPolygon(points, threadCount);
            long elapsed = System.nanoTime() - start;
            if (!convex) {
                throw new IllegalStateException("Beklenen konveks çokgen değil.");
            }
            totalNanos += elapsed;
        }
        return totalNanos / runs;
    }

    private static double estimateSerialFraction(double speedup, int threadCount) {
        if (threadCount <= 1 || speedup <= 0) {
            return 0.0;
        }
        double p = threadCount;
        double f = (1.0 / speedup - 1.0 / p) / (1.0 - 1.0 / p);
        return Math.max(0.0, Math.min(1.0, f));
    }

    // Nanosaneyi milisaniyeye çevirir (sayı olarak döner, yazdırma formatı printf ile yapılır)
    private static double toMillis(long nanos) {
        return nanos / 1_000_000.0;
    }

    public static void main(String[] args) {
        // Birkaç test örneği içeren main metodu.
        int[] square = {0, 0, 4, 0, 4, 4, 0, 4};
        int[] concave = {0, 0, 4, 0, 2, 2, 4, 4, 0, 4};
        int[] triangle = {0, 0, 2, 0, 1, 1};
        int[] invalid = {0, 0, 1, 1};

        System.out.println("Kare: " + (isConvexPolygon(square) ? "Konveks" : "Konveks değil"));
        System.out.println("İçbükey (concave): " + (isConvexPolygon(concave) ? "Konveks" : "Konveks değil"));
        System.out.println("Üçgen: " + (isConvexPolygon(triangle) ? "Konveks" : "Konveks değil"));
        System.out.println("Geçersiz (2 nokta): " + (isConvexPolygon(invalid) ? "Konveks" : "Konveks değil"));
        // Varsayılan parametreler; isterseniz komut satırından değiştirebilirsiniz.
        int pointCount = 500_000;
        int[] threadCounts = {1, 2, 4, 8};
        int runs = 3;
        String csvFile = null; // eğer belirtilirse sonuçlar CSV'ye yazılacak

        // Komut satırı argümanlarını işle
        // Beklenen format örnekleri:
        // java ConvexPolygonParallel 100000 5 1,2,4,8 --csv=sonuc.csv
        if (args.length >= 1) {
            try { pointCount = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }
        if (args.length >= 2) {
            try { runs = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
        }
        if (args.length >= 3) {
            try {
                String[] parts = args[2].split(",");
                int[] tcs = new int[parts.length];
                for (int i = 0; i < parts.length; i++) tcs[i] = Integer.parseInt(parts[i].trim());
                threadCounts = tcs;
            } catch (Exception ignored) {}
        }
        for (String a : args) {
            if (a.startsWith("--csv=")) {
                csvFile = a.substring("--csv=".length());
            }
        }

        int[] polygon = generateConvexPolygon(pointCount);
        List<Point> polygonPoints = convertToPoints(polygon);

        System.out.println();
        System.out.println("Performans karşılaştırması: " + pointCount + " noktalı konveks çokgen için (ortalama " + runs + " çalıştırma):");
        // Başlıkları hizalanmış sütunlarla yazdırıyoruz
        System.out.printf("%12s | %12s | %8s | %12s%n", "İşParçacıkları", "Süre (ms)", "Hızlanma", "Tahmini F");
        System.out.printf("%12s-+-%12s-+-%8s-+-%12s%n", "------------", "------------", "--------", "------------");
        long baselineNanos = -1;

        // Eğer CSV belirtildiyse, hazırlık yap
        java.io.PrintWriter csvWriter = null;
        try {
            if (csvFile != null) csvWriter = new java.io.PrintWriter(new java.io.FileWriter(csvFile, false));
        } catch (java.io.IOException e) {
            System.err.println("CSV dosyası açılamadı: " + e.getMessage());
            csvWriter = null;
        }

        if (csvWriter != null) {
            csvWriter.println("threads,ms,speedup,F");
        }

        for (int threadCount : threadCounts) {
            long avgNanos = benchmarkConvexCheck(polygonPoints, threadCount, runs);
            if (threadCount == 1) {
                baselineNanos = avgNanos;
            }
            double speedup = baselineNanos > 0 ? (double) baselineNanos / avgNanos : 1.0;
            double serialFraction = threadCount == 1 ? 0.0 : estimateSerialFraction(speedup, threadCount);
            System.out.printf("%12d | %12.2f | %8.2f | %12.4f%n",
                    threadCount, toMillis(avgNanos), speedup, serialFraction);
            if (csvWriter != null) {
                csvWriter.printf("%d,%.4f,%.4f,%.6f%n", threadCount, toMillis(avgNanos), speedup, serialFraction);
            }
        }

        if (csvWriter != null) {
            csvWriter.flush();
            csvWriter.close();
            System.out.println("Sonuçlar CSV dosyasına yazıldı: " + csvFile);
        }

        System.out.println();
        System.out.println("Amdahl yasası: S(P) = 1 / (F + (1-F)/P)");
        System.out.println("Tahmini F, gözlemlenen hızlanmadan her iş parçacığı sayısı için hesaplanır.");
    }
}
