package com.example.chbenchmark.repository;

import com.example.chbenchmark.dto.CountResult;
import com.example.chbenchmark.dto.GroupByResult;
import com.example.chbenchmark.model.EventRow;
import com.example.chbenchmark.service.SeedDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MysqlBenchmarkRepositoryIT {

    @Autowired
    private MysqlBenchmarkRepository repository;

    @Autowired
    private SeedDataGenerator generator;

    @BeforeEach
    void cleanTable() {
        repository.truncate();
    }

    @Test
    void insertBatchAndCountRoundTrip() {
        List<EventRow> rows = generator.generate(500, 1, 1L, LocalDateTime.now());
        repository.insertBatch(rows);
        assertThat(repository.count()).isEqualTo(500);
    }

    @Test
    void groupByEventTypeCoversAllInsertedRows() {
        List<EventRow> rows = generator.generate(1000, 1, 2L, LocalDateTime.now());
        repository.insertBatch(rows);
        List<GroupByResult> results = repository.groupByEventType(90);
        assertThat(results).isNotEmpty();
        assertThat(results.stream().mapToLong(GroupByResult::count).sum()).isEqualTo(1000);
    }

    @Test
    void countByDaysExcludesRowsOlderThanRange() {
        LocalDateTime oldReferenceTime = LocalDateTime.now().minusDays(200);
        List<EventRow> oldRows = generator.generate(10, 1, 3L, oldReferenceTime);
        repository.insertBatch(oldRows);
        CountResult result = repository.countByDays(90);
        assertThat(result.count()).isEqualTo(0);
    }
}
