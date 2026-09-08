package com.example.chbenchmark.service;

import com.example.chbenchmark.dto.BenchmarkQueryResponse;
import com.example.chbenchmark.model.EventRow;
import com.example.chbenchmark.repository.BenchmarkRepository;
import com.example.chbenchmark.repository.ClickHouseBenchmarkRepository;
import com.example.chbenchmark.repository.MysqlBenchmarkRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class BenchmarkService {

    private final Map<BenchmarkTarget, BenchmarkRepository> repositories;
    private final SeedDataGenerator seedDataGenerator;

    public BenchmarkService(
            MysqlBenchmarkRepository mysqlRepository,
            ClickHouseBenchmarkRepository clickHouseRepository,
            SeedDataGenerator seedDataGenerator) {
        this.repositories = Map.of(
            BenchmarkTarget.MYSQL, mysqlRepository,
            BenchmarkTarget.CLICKHOUSE, clickHouseRepository
        );
        this.seedDataGenerator = seedDataGenerator;
    }

    public long seed(BenchmarkTarget target, long count) {
        BenchmarkRepository repository = repositories.get(target);
        long startId = repository.count() + 1;
        long randomSeed = System.nanoTime();
        List<EventRow> rows = seedDataGenerator.generate(count, startId, randomSeed, LocalDateTime.now());
        repository.insertBatch(rows);
        return repository.count();
    }

    public BenchmarkQueryResponse query(BenchmarkTarget target, QueryType type, int days) {
        BenchmarkRepository repository = repositories.get(target);

        long dbStart = System.nanoTime();
        Object result = switch (type) {
            case COUNT -> repository.countByDays(days);
            case GROUPBY -> repository.groupByEventType(days);
        };
        long dbElapsedMs = (System.nanoTime() - dbStart) / 1_000_000;

        return new BenchmarkQueryResponse(target, type, repository.count(), 0L, dbElapsedMs, result);
    }

    public void reset(BenchmarkTarget target) {
        repositories.get(target).truncate();
    }
}
