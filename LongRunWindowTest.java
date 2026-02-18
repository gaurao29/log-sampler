import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LongRunWindowTest {

    private static final long WINDOW_MS = 10_000L;
    private static final long DEFAULT_DURATION_MS = 190_000L; // > 3 minutes
    private static final int DEFAULT_THREADS = 8;

    public static void main(String[] args) {
        long durationMs = parsePositiveLongArg(args, 0, DEFAULT_DURATION_MS);
        int threadCount = parsePositiveIntArg(args, 1, DEFAULT_THREADS);

        String[] logTypes = { "api.request", "db.query", "cache.hit", "error.log" };
        double[] rates = { 0.1, 0.2, 0.05, 0.3 };

        List<LogSamplerConfiguration> configs = new ArrayList<>();
        for (int i = 0; i < logTypes.length; i++) {
            configs.add(new LogSamplerConfiguration(logTypes[i], rates[i], WINDOW_MS));
        }
        LogTypeSampler sampler = new LogTypeSampler(configs);

        Map<Long, WindowStats> statsByWindow = new ConcurrentHashMap<>();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completedLatch = new CountDownLatch(threadCount);

        long startTime = System.currentTimeMillis();
        long endTime = startTime + durationMs;

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    long eventCount = 0;
                    while (System.currentTimeMillis() < endTime) {
                        int idx = (int) ((threadId + eventCount) % logTypes.length);
                        String logType = logTypes[idx];
                        long windowStart = (System.currentTimeMillis() / WINDOW_MS) * WINDOW_MS;
                        WindowStats windowStats = statsByWindow.computeIfAbsent(
                                windowStart,
                                k -> new WindowStats(logTypes.length));
                        windowStats.seen[idx].incrementAndGet();
                        if (sampler.shouldSample(logType)) {
                            windowStats.sampled[idx].incrementAndGet();
                        }
                        eventCount++;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completedLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        try {
            if (!completedLatch.await(durationMs + 30_000L, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Threads did not complete in time");
            }
            executor.shutdown();
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Executor did not terminate in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Main thread interrupted", e);
        }

        List<Long> windows = new ArrayList<>(statsByWindow.keySet());
        Collections.sort(windows);
        System.out.println("Window size: " + WINDOW_MS + " ms");
        System.out.println("Duration: " + durationMs + " ms");
        System.out.println("Threads: " + threadCount);
        System.out.println();

        for (Long windowStart : windows) {
            WindowStats windowStats = statsByWindow.get(windowStart);
            System.out.println("Window start: " + windowStart);
            for (int i = 0; i < logTypes.length; i++) {
                int seen = windowStats.seen[i].get();
                int sampled = windowStats.sampled[i].get();
                double actualRate = seen > 0 ? (double) sampled / seen : 0.0;
                System.out.println(
                        "  "
                                + logTypes[i]
                                + ": seen="
                                + seen
                                + ", sampled="
                                + sampled
                                + ", rate="
                                + String.format("%.2f%%", actualRate * 100));
            }
            System.out.println();
        }
    }

    private static int parsePositiveIntArg(String[] args, int index, int defaultValue) {
        if (args == null || args.length <= index) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(args[index]);
            return value > 0 ? value : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static long parsePositiveLongArg(String[] args, int index, long defaultValue) {
        if (args == null || args.length <= index) {
            return defaultValue;
        }
        try {
            long value = Long.parseLong(args[index]);
            return value > 0 ? value : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static class WindowStats {
        final AtomicInteger[] seen;
        final AtomicInteger[] sampled;

        WindowStats(int size) {
            this.seen = new AtomicInteger[size];
            this.sampled = new AtomicInteger[size];
            for (int i = 0; i < size; i++) {
                this.seen[i] = new AtomicInteger(0);
                this.sampled[i] = new AtomicInteger(0);
            }
        }
    }
}
