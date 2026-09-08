package com.example.chbenchmark.repository;

import com.example.chbenchmark.dto.CountResult;
import com.example.chbenchmark.dto.GroupByResult;
import com.example.chbenchmark.model.EventRow;

import java.util.List;

public interface BenchmarkRepository {
    void insertBatch(List<EventRow> rows);
    long count();
    void truncate();
    CountResult countByDays(int days);
    List<GroupByResult> groupByEventType(int days);
}
