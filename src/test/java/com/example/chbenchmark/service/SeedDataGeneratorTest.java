package com.example.chbenchmark.service;

import com.example.chbenchmark.model.EventRow;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SeedDataGeneratorTest {

    private final SeedDataGenerator generator = new SeedDataGenerator();

    @Test
    void generatesRequestedNumberOfRows() {
        List<EventRow> rows = generator.generate(100, 1, 42L, LocalDateTime.now());
        assertThat(rows).hasSize(100);
    }

    @Test
    void idsAreSequentialStartingFromGivenStartId() {
        List<EventRow> rows = generator.generate(5, 1000, 42L, LocalDateTime.now());
        assertThat(rows.stream().map(EventRow::id).toList())
            .containsExactly(1000L, 1001L, 1002L, 1003L, 1004L);
    }

    @Test
    void sameSeedAndReferenceTimeProduceIdenticalRows() {
        LocalDateTime referenceTime = LocalDateTime.of(2026, 1, 1, 0, 0);
        List<EventRow> first = generator.generate(50, 1, 42L, referenceTime);
        List<EventRow> second = generator.generate(50, 1, 42L, referenceTime);
        assertThat(first).isEqualTo(second);
    }

    @Test
    void userIdIsWithinExpectedRange() {
        List<EventRow> rows = generator.generate(1000, 1, 42L, LocalDateTime.now());
        assertThat(rows).allSatisfy(row ->
            assertThat(row.userId()).isBetween(1L, 100_000L));
    }

    @Test
    void eventTypeIsFromFixedSetOfTen() {
        Set<String> allowed = Set.of("click", "view", "purchase", "login", "logout",
            "signup", "share", "like", "comment", "search");
        List<EventRow> rows = generator.generate(1000, 1, 42L, LocalDateTime.now());
        assertThat(rows).allSatisfy(row -> assertThat(allowed).contains(row.eventType()));
    }

    @Test
    void eventTimeIsWithinNinetyDaysBeforeReferenceTime() {
        LocalDateTime referenceTime = LocalDateTime.of(2026, 6, 1, 0, 0);
        List<EventRow> rows = generator.generate(1000, 1, 42L, referenceTime);
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.eventTime()).isBeforeOrEqualTo(referenceTime);
            assertThat(row.eventTime()).isAfterOrEqualTo(referenceTime.minusDays(90));
        });
    }
}
