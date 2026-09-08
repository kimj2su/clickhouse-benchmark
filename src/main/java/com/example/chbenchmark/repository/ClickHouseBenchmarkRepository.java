package com.example.chbenchmark.repository;

import com.example.chbenchmark.dto.CountResult;
import com.example.chbenchmark.dto.GroupByResult;
import com.example.chbenchmark.model.EventRow;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Repository
public class ClickHouseBenchmarkRepository implements BenchmarkRepository {

    private final JdbcTemplate jdbcTemplate;

    public ClickHouseBenchmarkRepository(@Qualifier("clickhouseJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insertBatch(List<EventRow> rows) {
        String sql = "INSERT INTO events (id, user_id, event_type, event_time, value) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, rows, 2000, (ps, row) -> {
            ps.setLong(1, row.id());
            ps.setLong(2, row.userId());
            ps.setString(3, row.eventType());
            ps.setTimestamp(4, Timestamp.valueOf(row.eventTime()));
            ps.setDouble(5, row.value());
        });
    }

    @Override
    public long count() {
        Long result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events", Long.class);
        return result == null ? 0 : result;
    }

    @Override
    public void truncate() {
        jdbcTemplate.execute("TRUNCATE TABLE events");
    }

    @Override
    public CountResult countByDays(int days) {
        LocalDateTime from = LocalDateTime.now().minusDays(days);
        Long result = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM events WHERE event_time >= toDateTime(?)", Long.class, epochSecond(from));
        return new CountResult(result == null ? 0 : result);
    }

    @Override
    public List<GroupByResult> groupByEventType(int days) {
        LocalDateTime from = LocalDateTime.now().minusDays(days);
        String sql = """
            SELECT event_type, COUNT(*) AS cnt, SUM(value) AS total, AVG(value) AS average
            FROM events
            WHERE event_time >= toDateTime(?)
            GROUP BY event_type
            ORDER BY event_type
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new GroupByResult(
            rs.getString("event_type"),
            rs.getLong("cnt"),
            rs.getDouble("total"),
            rs.getDouble("average")
        ), epochSecond(from));
    }

    private long epochSecond(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
    }
}
