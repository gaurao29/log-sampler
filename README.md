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

Result
<img width="544" height="74" alt="image" src="https://github.com/user-attachments/assets/e355a4de-7165-4c93-b3fd-232b3d786ea4" />


You should see per-log-type expected vs. actual sampling rates and a final pass message.
