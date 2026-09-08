package com.example.chbenchmark.dto;

import com.example.chbenchmark.service.BenchmarkTarget;
import com.example.chbenchmark.service.QueryType;

public record BenchmarkQueryResponse(
    BenchmarkTarget target,
    QueryType type,
    long rowCountInTable,
    long apiElapsedMs,
    long dbElapsedMs,
    Object result
) {
    public BenchmarkQueryResponse withApiElapsedMs(long apiElapsedMs) {
        return new BenchmarkQueryResponse(target, type, rowCountInTable, apiElapsedMs, dbElapsedMs, result);
    }
}
