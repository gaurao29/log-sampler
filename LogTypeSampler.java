import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-performance, thread-safe sampler for multiple log types with time-based
 * windows.
 *
 * <p>
 * This sampler tracks sampling rates for different log types independently,
 * using fixed-duration
 * time windows (default 10 seconds). For each log type, it maintains counters
 * for the current
 * window and determines whether a log should be sampled based on the target
 * sample rate.
 *
 * <p>
 * Key features:
 * <ul>
 * <li><strong>Thread-safe:</strong> Uses lock-free algorithms with atomic
 * operations for maximum
 * performance
 * <li><strong>Per-log-type tracking:</strong> Each log type has independent
 * counters and sample
 * rates
 * <li><strong>Time-based windows:</strong> Windows are defined by duration
 * (default 10 seconds)
 * <li><strong>Configurable sample rates:</strong> Each log type can have a
 * different target
 * sample rate (0.0 to 1.0)
 * <li><strong>High performance:</strong> Minimal synchronization overhead,
 * designed for
 * high-throughput scenarios
 * </ul>
 *
 * <h3>Usage Example</h3>
 *
 * <pre>{@code
 * List<LogSamplerConfiguration> configs = new ArrayList<>();
 * configs.add(new LogSamplerConfiguration("api.request", 0.1, 1000L));
 * configs.add(new LogSamplerConfiguration("db.query", 0.05, 1000L));
 *
 * LogTypeSampler sampler = new LogTypeSampler(configs);
 *
 * // In your logging code (called from multiple threads):
 * if (sampler.shouldSample("api.request")) { // Sample 10% of API requests
 *     logger.info("API request: {}", request);
 * }
 *
 * if (sampler.shouldSample("db.query")) { // Sample 5% of DB queries
 *     logger.debug("DB query: {}", query);
 * }
 * }</pre>
 *
 * <h3>Thread Safety</h3>
 *
 * <p>
 * This class is thread-safe and can be safely used by multiple threads
 * concurrently. It uses
 * lock-free algorithms with atomic operations to minimize contention and
 * maximize performance.
 */
public class LogTypeSampler {

    private static final long DEFAULT_WINDOW_DURATION_MS = 10_000L; // 10 seconds
    public static final long MIN_WINDOW_DURATION_MS = 1_000L; // 1 second

    private final long windowDurationMs;
    private final ConcurrentHashMap<String, WindowState> logTypeStates = new ConcurrentHashMap<>();
    private final Map<String, Long> windowDurationsByLogType;
    private final Map<String, Double> sampleRatesByLogType;

    /**
     * Creates a LogTypeSampler with the default 10-second window duration.
     */
    public LogTypeSampler() {
        this(DEFAULT_WINDOW_DURATION_MS);
    }

    /**
     * Creates a LogTypeSampler with per-log-type configuration.
     *
     * @param configurations list of per-log-type configurations
     * @throws IllegalArgumentException if configurations is null or empty
     */
    public LogTypeSampler(List<LogSamplerConfiguration> configurations) {
        if (configurations == null || configurations.isEmpty()) {
            throw new IllegalArgumentException("configurations must be non-empty");
        }
        Map<String, Long> windowDurations = new HashMap<>();
        Map<String, Double> sampleRates = new HashMap<>();
        for (LogSamplerConfiguration config : configurations) {
            if (config == null) {
                throw new IllegalArgumentException("configurations must not contain null entries");
            }
            windowDurations.put(config.logType, config.windowSizeMs);
            sampleRates.put(config.logType, config.sampleRate);
        }
        this.windowDurationsByLogType = Collections.unmodifiableMap(new HashMap<>(windowDurations));
        this.sampleRatesByLogType = Collections.unmodifiableMap(new HashMap<>(sampleRates));
        this.windowDurationMs = DEFAULT_WINDOW_DURATION_MS;
    }

    /**
     * Determines whether a log of the given type should be sampled using the
     * configured sample rate for that log type.
     *
     * @param logType the type/category of the log
     * @return true if this log should be sampled, false otherwise
     * @throws IllegalArgumentException if no sample rate configured for logType
     */
    public boolean shouldSample(String logType) {
        if (logType == null) {
            throw new IllegalArgumentException("logType cannot be null");
        }
        Double targetRate = sampleRatesByLogType.get(logType);
        if (targetRate == null) {
            throw new IllegalArgumentException(
                    "No sample rate configured for log type: " + logType);
        }
        return shouldSample(logType, targetRate);
    }

    /**
     * Creates a LogTypeSampler with a custom window duration.
     *
     * @param windowDurationMs window duration in milliseconds (must be >= 1000)
     * @throws IllegalArgumentException if windowDurationMs < 1000
     */
    public LogTypeSampler(long windowDurationMs) {
        if (windowDurationMs < MIN_WINDOW_DURATION_MS) {
            throw new IllegalArgumentException(
                    "windowDurationMs must be >= " + MIN_WINDOW_DURATION_MS + " ms");
        }
        this.windowDurationMs = windowDurationMs;
        this.windowDurationsByLogType = Collections.emptyMap();
        this.sampleRatesByLogType = Collections.emptyMap();
    }

    /**
     * Determines whether a log of the given type should be sampled based on the
     * target sample rate
     * for the current time window.
     *
     * <p>
     * This method is thread-safe and high-performance. It:
     * <ol>
     * <li>Checks if we're in a new window and resets counters if needed
     * <li>Increments the total logs seen counter for this log type
     * <li>Checks if the current sample rate is below the target
     * <li>If below target, increments the sampled counter and returns true
     * <li>If at or above target, returns false
     * </ol>
     *
     * <p>
     * The sample rate is calculated as: sampledCount / totalSeenCount
     *
     * @param logType          the type/category of the log (e.g., "api.request",
     *                         "db.query")
     * @param targetSampleRate the target sample rate (0.0 to 1.0), where 0.1 means
     *                         10% of logs
     *                         should be sampled
     * @return true if this log should be sampled (logged), false otherwise
     * @throws IllegalArgumentException if logType is null or targetSampleRate is
     *                                  not in [0.0, 1.0]
     */
    public boolean shouldSample(String logType, double targetSampleRate) {
        if (logType == null) {
            throw new IllegalArgumentException("logType cannot be null");
        }
        if (targetSampleRate < 0.0 || targetSampleRate > 1.0) {
            throw new IllegalArgumentException(
                    "targetSampleRate must be in [0.0, 1.0], got: " + targetSampleRate);
        }

        long logTypeWindowDurationMs = resolveWindowDurationMs(logType);

        // Get or create window state for this log type
        WindowState state = logTypeStates.computeIfAbsent(
                logType,
                k -> new WindowState(logTypeWindowDurationMs));

        long currentTimeMs = System.currentTimeMillis();
        long currentWindowStart = state.windowStartMs.get();

        // Check if we need to advance to a new window
        if (currentTimeMs >= currentWindowStart + state.windowDurationMs) {
            // Try to reset the window atomically
            long newWindowStart = (currentTimeMs / state.windowDurationMs) * state.windowDurationMs;
            if (state.windowStartMs.compareAndSet(currentWindowStart, newWindowStart)) {
                // Successfully reset window - reset counters
                state.totalSeen.set(0);
                state.totalSampled.set(0);
            }
            // If CAS failed, another thread already reset it, continue with updated state
            currentWindowStart = state.windowStartMs.get();
        }

        // Increment total seen counter
        int totalSeen = state.totalSeen.incrementAndGet();

        // Calculate how many samples we should have at this point
        // We want: sampled / totalSeen ≈ targetRate
        // So: sampled ≈ totalSeen * targetRate
        // We should sample if: sampled < totalSeen * targetRate
        int totalSampled = state.totalSampled.get();
        double expectedSampled = totalSeen * targetSampleRate;

        // Check if we should sample (use floating point comparison with small
        // tolerance)
        if (totalSampled < expectedSampled - 0.5) {
            // Increment sampled counter
            int newSampled = state.totalSampled.incrementAndGet();
            // Double-check: recalculate expected samples with updated count
            // (another thread might have incremented totalSeen in the meantime)
            int currentTotalSeen = state.totalSeen.get();
            double updatedExpected = currentTotalSeen * targetSampleRate;
            return newSampled <= updatedExpected + 0.5; // Allow small rounding tolerance
        }

        return false;
    }

    /**
     * Gets the current sample rate for a specific log type in the current window.
     *
     * <p>
     * This method provides a snapshot of the current state and may be immediately
     * stale in a
     * concurrent environment.
     *
     * @param logType the log type to query
     * @return the current sample rate (0.0 to 1.0), or 0.0 if no logs of this type
     *         have been seen
     */
    public double getCurrentSampleRate(String logType) {
        if (logType == null) {
            return 0.0;
        }
        WindowState state = logTypeStates.get(logType);
        if (state == null) {
            return 0.0;
        }
        int totalSeen = state.totalSeen.get();
        if (totalSeen == 0) {
            return 0.0;
        }
        return (double) state.totalSampled.get() / totalSeen;
    }

    /**
     * Gets statistics for a specific log type in the current window.
     *
     * @param logType the log type to query
     * @return statistics object, or null if no logs of this type have been seen
     */
    public LogTypeStats getStats(String logType) {
        if (logType == null) {
            return null;
        }
        WindowState state = logTypeStates.get(logType);
        if (state == null) {
            return null;
        }
        int totalSeen = state.totalSeen.get();
        int totalSampled = state.totalSampled.get();
        long windowStart = state.windowStartMs.get();
        return new LogTypeStats(logType, windowStart, totalSeen, totalSampled);
    }

    /**
     * Clears all state for all log types. Useful for testing or resetting the
     * sampler.
     */
    public void clear() {
        logTypeStates.clear();
    }

    /**
     * Gets the window duration in milliseconds.
     *
     * @return the window duration
     */
    public long getWindowDurationMs() {
        return windowDurationMs;
    }

    private long resolveWindowDurationMs(String logType) {
        if (!windowDurationsByLogType.isEmpty()) {
            Long duration = windowDurationsByLogType.get(logType);
            if (duration == null) {
                throw new IllegalArgumentException(
                        "No window duration configured for log type: " + logType);
            }
            return duration;
        }
        return windowDurationMs;
    }

    /**
     * Internal state for tracking a single log type's window.
     */
    private static class WindowState {
        final long windowDurationMs;
        final AtomicLong windowStartMs;
        final AtomicInteger totalSeen;
        final AtomicInteger totalSampled;

        WindowState(long windowDurationMs) {
            this.windowDurationMs = windowDurationMs;
            long currentTimeMs = System.currentTimeMillis();
            // Align window start to window boundaries
            this.windowStartMs = new AtomicLong((currentTimeMs / windowDurationMs) * windowDurationMs);
            this.totalSeen = new AtomicInteger(0);
            this.totalSampled = new AtomicInteger(0);
        }
    }

    /**
     * Statistics for a log type in the current window.
     */
    public static class LogTypeStats {
        public final String logType;
        public final long windowStartMs;
        public final int totalSeen;
        public final int totalSampled;
        public final double sampleRate;

        LogTypeStats(String logType, long windowStartMs, int totalSeen, int totalSampled) {
            this.logType = logType;
            this.windowStartMs = windowStartMs;
            this.totalSeen = totalSeen;
            this.totalSampled = totalSampled;
            this.sampleRate = totalSeen > 0 ? (double) totalSampled / totalSeen : 0.0;
        }

        @Override
        public String toString() {
            return "LogTypeStats{"
                    + "logType='"
                    + logType
                    + '\''
                    + ", windowStartMs="
                    + windowStartMs
                    + ", totalSeen="
                    + totalSeen
                    + ", totalSampled="
                    + totalSampled
                    + ", sampleRate="
                    + String.format("%.2f%%", sampleRate * 100)
                    + '}';
        }
    }
}
