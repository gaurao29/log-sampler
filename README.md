# Log Sampler

Simple Java demo that samples log types at target rates and validates the results.

## Prerequisites

- Java JDK 8+ (compiler and runtime available on `PATH`)

## Run

From the project root:

```zsh
javac Main.java LogTypeSampler.java LogSamplerConfiguration.java
java Main
```

Result
<img width="544" height="74" alt="image" src="https://github.com/user-attachments/assets/e355a4de-7165-4c93-b3fd-232b3d786ea4" />


You should see per-log-type expected vs. actual sampling rates and a final pass message.

Optional CLI args let you set load for stress testing:

```zsh
java Main <threads> <eventsPerThread>
```

Example:

```zsh
java Main 32 20000
```

## Stress Test Results

Run (32 threads, 20,000 events per thread):

```
Log type api.request: expected 10.00%, actual 9.97% (seen=160000, sampled=15946)
Log type db.query: expected 20.00%, actual 19.90% (seen=160000, sampled=31835)
Log type cache.hit: expected 5.00%, actual 4.96% (seen=160000, sampled=7936)
Log type error.log: expected 30.00%, actual 29.47% (seen=160000, sampled=47146)
Sampling validation passed for all log types.
```

## Configuration

Configure per-log-type sample rates and window sizes using
`LogSamplerConfiguration`:

```java
List<LogSamplerConfiguration> configs = new ArrayList<>();
configs.add(new LogSamplerConfiguration("api.request", 0.1, 1000L));
configs.add(new LogSamplerConfiguration("db.query", 0.2, 1000L));

LogTypeSampler sampler = new LogTypeSampler(configs);
if (sampler.shouldSample("api.request")) {
	// log it
}
```
