package com.example.chbenchmark.service;

import com.example.chbenchmark.dto.BenchmarkQueryResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class ResultStore {

    private final List<BenchmarkQueryResponse> records = new CopyOnWriteArrayList<>();

    public void add(BenchmarkQueryResponse response) {
        records.add(response);
    }

    public List<BenchmarkQueryResponse> all() {
        return List.copyOf(records);
    }
}
