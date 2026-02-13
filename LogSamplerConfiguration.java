public class LogSamplerConfiguration {

    public final String logType;
    public final double sampleRate;
    public final long windowSizeMs;

    public LogSamplerConfiguration(String logType, double sampleRate, long windowSizeMs) {
        if (logType == null || logType.isEmpty()) {
            throw new IllegalArgumentException("logType must be non-empty");
        }
        if (sampleRate < 0.0 || sampleRate > 1.0) {
            throw new IllegalArgumentException(
                    "sampleRate must be in [0.0, 1.0], got: " + sampleRate);
        }
        if (windowSizeMs < LogTypeSampler.MIN_WINDOW_DURATION_MS) {
            throw new IllegalArgumentException(
                    "windowSizeMs must be >= " + LogTypeSampler.MIN_WINDOW_DURATION_MS + " ms");
        }
        this.logType = logType;
        this.sampleRate = sampleRate;
        this.windowSizeMs = windowSizeMs;
    }
}
