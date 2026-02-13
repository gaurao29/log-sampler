# Log Sampler

Simple Java demo that samples log types at target rates and validates the results.

## Prerequisites

- Java JDK 8+ (compiler and runtime available on `PATH`)

## Run

From the project root:

```zsh
javac Main.java LogTypeSampler.java
java Main
```

You should see per-log-type expected vs. actual sampling rates and a final pass message.
