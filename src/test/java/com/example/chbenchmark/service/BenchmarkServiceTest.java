package com.example.chbenchmark.service;

import com.example.chbenchmark.dto.BenchmarkQueryResponse;
import com.example.chbenchmark.dto.GroupByResult;
import com.example.chbenchmark.model.EventRow;
import com.example.chbenchmark.repository.ClickHouseBenchmarkRepository;
import com.example.chbenchmark.repository.MysqlBenchmarkRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class BenchmarkServiceTest {

    @Test
    void queryRoutesToCorrectRepositoryAndMeasuresDbTime() {
        MysqlBenchmarkRepository mysqlRepo = mock(MysqlBenchmarkRepository.class);
        ClickHouseBenchmarkRepository chRepo = mock(ClickHouseBenchmarkRepository.class);
        when(mysqlRepo.groupByEventType(90))
            .thenReturn(List.of(new GroupByResult("click", 10, 100.0, 10.0)));

        BenchmarkService service = new BenchmarkService(mysqlRepo, chRepo, new SeedDataGenerator());

        BenchmarkQueryResponse response = service.query(BenchmarkTarget.MYSQL, QueryType.GROUPBY, 90);

        assertThat(response.target()).isEqualTo(BenchmarkTarget.MYSQL);
        // No seed() was called on this fresh service, so the cached row count starts at 0 -
        // and query() must read the cache rather than issuing a live SELECT COUNT(*).
        assertThat(response.rowCountInTable()).isEqualTo(0L);
        assertThat(response.dbElapsedMs()).isGreaterThanOrEqualTo(0);
        verify(mysqlRepo).groupByEventType(90);
        verify(chRepo, never()).groupByEventType(anyInt());
        verify(mysqlRepo, never()).count();
    }

    @Test
    void seedUpdatesCachedRowCountAndSubsequentQueryReflectsIt() {
        MysqlBenchmarkRepository mysqlRepo = mock(MysqlBenchmarkRepository.class);
        ClickHouseBenchmarkRepository chRepo = mock(ClickHouseBenchmarkRepository.class);
        when(mysqlRepo.count()).thenReturn(0L).thenReturn(42L);
        when(mysqlRepo.groupByEventType(90)).thenReturn(List.of());

        BenchmarkService service = new BenchmarkService(mysqlRepo, chRepo, new SeedDataGenerator());

        long total = service.seed(BenchmarkTarget.MYSQL, 42);
        assertThat(total).isEqualTo(42L);

        BenchmarkQueryResponse response = service.query(BenchmarkTarget.MYSQL, QueryType.GROUPBY, 90);

        assertThat(response.rowCountInTable()).isEqualTo(42L);
        // Both count() invocations came from seed() (startId lookup + post-insert total);
        // query() must not trigger a third one.
        verify(mysqlRepo, times(2)).count();
    }

    @Test
    void seedGeneratesRowsStartingAfterExistingRowCount() {
        MysqlBenchmarkRepository mysqlRepo = mock(MysqlBenchmarkRepository.class);
        ClickHouseBenchmarkRepository chRepo = mock(ClickHouseBenchmarkRepository.class);
        when(mysqlRepo.count()).thenReturn(100L).thenReturn(200L);

        BenchmarkService service = new BenchmarkService(mysqlRepo, chRepo, new SeedDataGenerator());

        long total = service.seed(BenchmarkTarget.MYSQL, 100);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EventRow>> captor = ArgumentCaptor.forClass(List.class);
        verify(mysqlRepo).insertBatch(captor.capture());
        assertThat(captor.getValue()).hasSize(100);
        assertThat(captor.getValue().get(0).id()).isEqualTo(101L);
        assertThat(total).isEqualTo(200L);
    }

    @Test
    void resetCallsTruncateOnlyOnCorrectRepository() {
        MysqlBenchmarkRepository mysqlRepo = mock(MysqlBenchmarkRepository.class);
        ClickHouseBenchmarkRepository chRepo = mock(ClickHouseBenchmarkRepository.class);

        BenchmarkService service = new BenchmarkService(mysqlRepo, chRepo, new SeedDataGenerator());

        service.reset(BenchmarkTarget.CLICKHOUSE);

        verify(chRepo).truncate();
        verify(mysqlRepo, never()).truncate();
    }
}
