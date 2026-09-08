package com.example.chbenchmark.controller;

import com.example.chbenchmark.repository.ClickHouseBenchmarkRepository;
import com.example.chbenchmark.repository.MysqlBenchmarkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BenchmarkControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MysqlBenchmarkRepository mysqlRepository;

    @Autowired
    private ClickHouseBenchmarkRepository clickHouseRepository;

    @BeforeEach
    void cleanTables() {
        mysqlRepository.truncate();
        clickHouseRepository.truncate();
    }

    @Test
    void seedThenQueryReturnsAggregateResultWithTimings() throws Exception {
        mockMvc.perform(post("/benchmark/seed").param("target", "mysql").param("count", "1000"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/benchmark/query")
                .param("target", "mysql")
                .param("type", "groupby"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.target").value("MYSQL"))
            .andExpect(jsonPath("$.rowCountInTable").value(1000))
            .andExpect(jsonPath("$.dbElapsedMs").exists())
            .andExpect(jsonPath("$.apiElapsedMs").exists());
    }

    @Test
    void resultsEndpointAccumulatesPastQueries() throws Exception {
        mockMvc.perform(post("/benchmark/seed").param("target", "clickhouse").param("count", "200"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/benchmark/query").param("target", "clickhouse").param("type", "count"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/benchmark/results"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.target=='CLICKHOUSE')]").exists());
    }

    @Test
    void resetTruncatesTable() throws Exception {
        mockMvc.perform(post("/benchmark/seed").param("target", "mysql").param("count", "50"))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/benchmark/reset").param("target", "mysql"))
            .andExpect(status().isOk());

        assertThat(mysqlRepository.count()).isEqualTo(0);
    }
}
