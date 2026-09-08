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
        // referenceTime must track real "now": countByDays() below filters against the actual
        // wall-clock time inside each repository, not this reference point. A fixed historical
        // date would eventually drift more than 30 days into the past and make that assertion
        // vacuously true (0 == 0) instead of exercising a real time boundary.
        LocalDateTime referenceTime = LocalDateTime.now();
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

        // MySQL binds a Timestamp against `event_time >= ?` (wall-clock semantics); ClickHouse
        // binds an epoch-second long against `event_time >= toDateTime(?)` (absolute-instant
        // semantics). groupByEventType(9999) above is effectively unbounded and can't detect a
        // skew between those two translations. days=30 uses a real mid-range boundary that only
        // part of the 90-day seeded spread falls inside of, so a systematic skew would surface here.
        long mysqlCount30 = mysqlRepository.countByDays(30).count();
        long clickHouseCount30 = clickHouseRepository.countByDays(30).count();
        // Guard against a vacuous 0 == 0 (or 5000 == 5000) pass: the 30-day window must actually
        // split the 90-day seeded spread, or this assertion proves nothing about the boundary.
        assertThat(mysqlCount30).isNotZero().isLessThan(5000);
        assertThat(clickHouseCount30).isEqualTo(mysqlCount30);
    }
}
