CREATE TABLE IF NOT EXISTS events (
  id BIGINT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  event_type VARCHAR(32) NOT NULL,
  event_time DATETIME NOT NULL,
  value DOUBLE NOT NULL,
  INDEX idx_event_time (event_time)
) ENGINE=InnoDB;
