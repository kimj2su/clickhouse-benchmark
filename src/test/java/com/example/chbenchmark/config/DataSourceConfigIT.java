package com.example.chbenchmark.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DataSourceConfigIT {

    @Autowired
    @Qualifier("mysqlJdbcTemplate")
    private JdbcTemplate mysqlJdbcTemplate;

    @Autowired
    @Qualifier("clickhouseJdbcTemplate")
    private JdbcTemplate clickhouseJdbcTemplate;

    @Test
    void mysqlConnectionIsUsable() {
        Integer result = mysqlJdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void clickhouseConnectionIsUsable() {
        Integer result = clickhouseJdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(result).isEqualTo(1);
    }

    @Test
    void eventsTableExistsInMysql() {
        Integer count = mysqlJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'events'",
            Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void eventsTableExistsInClickhouse() {
        Integer count = clickhouseJdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM system.tables WHERE name = 'events' AND database = 'benchmark'",
            Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
