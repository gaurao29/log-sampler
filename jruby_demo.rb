require "java"

$CLASSPATH << File.expand_path(".")

java_import "LogTypeSampler"
java_import "LogSamplerConfiguration"

configs = java.util.ArrayList.new
configs.add(LogSamplerConfiguration.new("api.request", 0.1, 1000))
configs.add(LogSamplerConfiguration.new("db.query", 0.05, 1000))

sampler = LogTypeSampler.new(configs)

thread_count = (ARGV[0] || 8).to_i
events_per_thread = (ARGV[1] || 500).to_i

log_types = ["api.request", "db.query"]
seen = Hash.new(0)
sampled = Hash.new(0)

threads = Array.new(thread_count) do |thread_id|
  Thread.new do
    events_per_thread.times do |i|
      log_type = log_types[(thread_id + i) % log_types.length]
      seen[log_type] += 1
      sampled[log_type] += 1 if sampler.shouldSample(log_type)
    end
  end
end

threads.each(&:join)

log_types.each do |log_type|
  actual_rate = seen[log_type] > 0 ? (sampled[log_type].to_f / seen[log_type]) : 0.0
  puts "#{log_type}: seen=#{seen[log_type]}, sampled=#{sampled[log_type]}, rate=#{(actual_rate * 100).round(2)}%"
end
