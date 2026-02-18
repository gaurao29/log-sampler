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

## JRuby Demo

Compile the Java classes and run the JRuby stress test:

```zsh
javac LogTypeSampler.java LogSamplerConfiguration.java
jruby jruby_demo.rb 32 20000
```

The script prints per-log-type counts and sample rates.

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

```bash

javac LogTypeSampler.java LogSamplerConfiguration.java LongRunWindowTest.java
 && java LongRunWindowTest

```

Result 
Window size: 10000 ms
Duration: 190000 ms
Threads: 8

Window start: 1771449520000
  api.request: seen=13288900, sampled=1322221, rate=9.95%
  db.query: seen=13288899, sampled=2562367, rate=19.28%
  cache.hit: seen=13288901, sampled=640214, rate=4.82%
  error.log: seen=13288900, sampled=3972091, rate=29.89%

Window start: 1771449530000
  api.request: seen=19760091, sampled=1973448, rate=9.99%
  db.query: seen=19760091, sampled=3841938, rate=19.44%
  cache.hit: seen=19760089, sampled=961553, rate=4.87%
  error.log: seen=19760091, sampled=5919562, rate=29.96%

Window start: 1771449540000
  api.request: seen=19695912, sampled=1968449, rate=9.99%
  db.query: seen=19695911, sampled=3821920, rate=19.40%
  cache.hit: seen=19695912, sampled=957103, rate=4.86%
  error.log: seen=19695911, sampled=5899797, rate=29.95%

Window start: 1771449550000
  api.request: seen=19249962, sampled=1919117, rate=9.97%
  db.query: seen=19249962, sampled=3679122, rate=19.11%
  cache.hit: seen=19249962, sampled=919299, rate=4.78%
  error.log: seen=19249963, sampled=5755154, rate=29.90%

Window start: 1771449560000
  api.request: seen=18480397, sampled=1842368, rate=9.97%
  db.query: seen=18480401, sampled=3530933, rate=19.11%
  cache.hit: seen=18480400, sampled=884023, rate=4.78%
  error.log: seen=18480398, sampled=5525222, rate=29.90%

Window start: 1771449570000
  api.request: seen=18980291, sampled=1896307, rate=9.99%
  db.query: seen=18980289, sampled=3668233, rate=19.33%
  cache.hit: seen=18980288, sampled=918162, rate=4.84%
  error.log: seen=18980290, sampled=5686375, rate=29.96%

Window start: 1771449580000
  api.request: seen=18964700, sampled=1894245, rate=9.99%
  db.query: seen=18964700, sampled=3672737, rate=19.37%
  cache.hit: seen=18964703, sampled=920888, rate=4.86%
  error.log: seen=18964702, sampled=5681264, rate=29.96%

Window start: 1771449590000
  api.request: seen=18240098, sampled=1821224, rate=9.98%
  db.query: seen=18240100, sampled=3532927, rate=19.37%
  cache.hit: seen=18240095, sampled=884588, rate=4.85%
  error.log: seen=18240094, sampled=5463382, rate=29.95%

Window start: 1771449600000
  api.request: seen=18893412, sampled=1887152, rate=9.99%
  db.query: seen=18893410, sampled=3653808, rate=19.34%
  cache.hit: seen=18893414, sampled=922115, rate=4.88%
  error.log: seen=18893414, sampled=5659259, rate=29.95%

Window start: 1771449610000
  api.request: seen=18949405, sampled=1891772, rate=9.98%
  db.query: seen=18949407, sampled=3649864, rate=19.26%
  cache.hit: seen=18949408, sampled=917481, rate=4.84%
  error.log: seen=18949405, sampled=5671284, rate=29.93%

Window start: 1771449620000
  api.request: seen=16949727, sampled=1688606, rate=9.96%
  db.query: seen=16949726, sampled=3261112, rate=19.24%
  cache.hit: seen=16949723, sampled=817455, rate=4.82%
  error.log: seen=16949728, sampled=5069096, rate=29.91%

Window start: 1771449630000
  api.request: seen=16937963, sampled=1683630, rate=9.94%
  db.query: seen=16937963, sampled=3247467, rate=19.17%
  cache.hit: seen=16937963, sampled=810427, rate=4.78%
  error.log: seen=16937960, sampled=5057995, rate=29.86%

Window start: 1771449640000
  api.request: seen=14425899, sampled=1435084, rate=9.95%
  db.query: seen=14425899, sampled=2757695, rate=19.12%
  cache.hit: seen=14425900, sampled=689781, rate=4.78%
  error.log: seen=14425901, sampled=4306639, rate=29.85%

Window start: 1771449650000
  api.request: seen=14893211, sampled=1486360, rate=9.98%
  db.query: seen=14893209, sampled=2856669, rate=19.18%
  cache.hit: seen=14893210, sampled=715157, rate=4.80%
  error.log: seen=14893211, sampled=4459381, rate=29.94%

Window start: 1771449660000
  api.request: seen=16611799, sampled=1653878, rate=9.96%
  db.query: seen=16611801, sampled=3169207, rate=19.08%
  cache.hit: seen=16611799, sampled=791670, rate=4.77%
  error.log: seen=16611799, sampled=4961011, rate=29.86%

Window start: 1771449670000
  api.request: seen=15918039, sampled=1586773, rate=9.97%
  db.query: seen=15918037, sampled=3053764, rate=19.18%
  cache.hit: seen=15918039, sampled=764421, rate=4.80%
  error.log: seen=15918039, sampled=4760700, rate=29.91%

Window start: 1771449680000
  api.request: seen=17283154, sampled=1725978, rate=9.99%
  db.query: seen=17283153, sampled=3336710, rate=19.31%
  cache.hit: seen=17283152, sampled=835998, rate=4.84%
  error.log: seen=17283154, sampled=5177478, rate=29.96%

Window start: 1771449690000
  api.request: seen=18481472, sampled=1845414, rate=9.99%
  db.query: seen=18481475, sampled=3615047, rate=19.56%
  cache.hit: seen=18481474, sampled=905321, rate=4.90%
  error.log: seen=18481473, sampled=5535157, rate=29.95%

Window start: 1771449700000
  api.request: seen=13987300, sampled=1382390, rate=9.88%
  db.query: seen=13987297, sampled=2586297, rate=18.49%
  cache.hit: seen=13987300, sampled=646971, rate=4.63%
  error.log: seen=13987299, sampled=4146470, rate=29.64%

Window start: 1771449710000
  api.request: seen=4191971, sampled=417762, rate=9.97%
  db.query: seen=4191975, sampled=804525, rate=19.19%
  cache.hit: seen=4191971, sampled=201565, rate=4.81%
  error.log: seen=4191972, sampled=1253733, rate=29.91%