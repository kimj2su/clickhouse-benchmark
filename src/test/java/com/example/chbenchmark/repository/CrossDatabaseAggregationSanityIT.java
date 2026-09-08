package com.example.chbenchmark.repository;

import com.example.chbenchmark.dto.GroupByResult;
import com.example.chbenchmark.model.EventRow;
import com.example.chbenchmark.service.SeedDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
class CrossDatabaseAggregationSanityIT {

    @Autowired
    private MysqlBenchmarkRepository mysqlRepository;

    @Autowired
    private ClickHouseBenchmarkRepository clickHouseRepository;

    @Autowired
    private SeedDataGenerator generator;

    @BeforeEach
    void cleanTables() {
        mysqlRepository.truncate();
        clickHouseRepository.truncate();
    }

    @Test
    void bothDatabasesProduceEquivalentAggregatesForIdenticalSeededData() {
        LocalDateTime referenceTime = LocalDateTime.of(2026, 6, 1, 12, 0);
        List<EventRow> rows = generator.generate(5000, 1, 777L, referenceTime);

        mysqlRepository.insertBatch(rows);
        clickHouseRepository.insertBatch(rows);

        List<GroupByResult> mysqlResults = mysqlRepository.groupByEventType(9999).stream()
            .sorted(Comparator.comparing(GroupByResult::eventType)).toList();
        List<GroupByResult> clickHouseResults = clickHouseRepository.groupByEventType(9999).stream()
            .sorted(Comparator.comparing(GroupByResult::eventType)).toList();

        assertThat(clickHouseResults).hasSameSizeAs(mysqlResults);
        for (int i = 0; i < mysqlResults.size(); i++) {
            GroupByResult mysqlRow = mysqlResults.get(i);
            GroupByResult clickHouseRow = clickHouseResults.get(i);
            assertThat(clickHouseRow.eventType()).isEqualTo(mysqlRow.eventType());
            assertThat(clickHouseRow.count()).isEqualTo(mysqlRow.count());
            assertThat(clickHouseRow.sum()).isCloseTo(mysqlRow.sum(), within(0.01));
            assertThat(clickHouseRow.avg()).isCloseTo(mysqlRow.avg(), within(0.01));
        }
    }
}
