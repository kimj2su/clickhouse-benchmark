CREATE DATABASE IF NOT EXISTS benchmark;

CREATE TABLE IF NOT EXISTS benchmark.events (
  id UInt64,
  user_id UInt64,
  event_type LowCardinality(String),
  event_time DateTime,
  value Float64
) ENGINE = MergeTree
ORDER BY (event_type, event_time);
