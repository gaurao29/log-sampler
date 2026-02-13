import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    public static void main(String[] args) {
        String[] logTypes = { "api.request", "db.query", "cache.hit", "error.log" };
        double[] rates = { 0.1, 0.2, 0.05, 0.3 };
        long[] windowsMs = { 1000L, 1000L, 1000L, 1000L };

        List<LogSamplerConfiguration> configurations = new ArrayList<>();
        for (int i = 0; i < logTypes.length; i++) {
            configurations.add(new LogSamplerConfiguration(logTypes[i], rates[i], windowsMs[i]));
        }

        LogTypeSampler sampler = new LogTypeSampler(configurations);

        int numThreads = parsePositiveIntArg(args, 0, 8);
        int eventsPerThread = parsePositiveIntArg(args, 1, 500);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completedLatch = new CountDownLatch(numThreads);

        AtomicInteger[] sampled = new AtomicInteger[logTypes.length];
        AtomicInteger[] seen = new AtomicInteger[logTypes.length];
        for (int i = 0; i < logTypes.length; i++) {
            sampled[i] = new AtomicInteger(0);
            seen[i] = new AtomicInteger(0);
        }

        // Launch threads that sample different log types concurrently
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(
                    () -> {
                        try {
                            startLatch.await();

                            for (int i = 0; i < eventsPerThread; i++) {
                                int logTypeIdx = (threadId + i) % logTypes.length;
                                seen[logTypeIdx].incrementAndGet();
                                if (sampler.shouldSample(logTypes[logTypeIdx])) {
                                    sampled[logTypeIdx].incrementAndGet();
                                }
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
            if (!completedLatch.await(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Threads did not complete in time");
            }

            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                throw new RuntimeException("Executor did not terminate in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Main thread interrupted", e);
        }

        for (int i = 0; i < logTypes.length; i++) {
            double actualRate = (double) sampled[i].get() / seen[i].get();
            double expectedRate = rates[i];
            double tolerance = expectedRate * 0.3; // 30% tolerance
            System.out.println(
                    String.format(
                            "Log type %s: expected %.2f%%, actual %.2f%% (seen=%d, sampled=%d)",
                            logTypes[i], expectedRate * 100, actualRate * 100, seen[i].get(), sampled[i].get()));
            if (Math.abs(actualRate - expectedRate) > tolerance) {
                throw new RuntimeException(
                        String.format(
                                "Log type %s: expected rate %.2f%%, got %.2f%%",
                                logTypes[i], expectedRate * 100, actualRate * 100));
            }
        }

        System.out.println("Sampling validation passed for all log types.");
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
}