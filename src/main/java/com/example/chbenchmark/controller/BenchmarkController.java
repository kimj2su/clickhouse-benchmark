package com.example.chbenchmark.controller;

import com.example.chbenchmark.dto.BenchmarkQueryResponse;
import com.example.chbenchmark.service.BenchmarkService;
import com.example.chbenchmark.service.BenchmarkTarget;
import com.example.chbenchmark.service.QueryType;
import com.example.chbenchmark.service.ResultStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/benchmark")
public class BenchmarkController {

    private final BenchmarkService benchmarkService;
    private final ResultStore resultStore;

    public BenchmarkController(BenchmarkService benchmarkService, ResultStore resultStore) {
        this.benchmarkService = benchmarkService;
        this.resultStore = resultStore;
    }

    @PostMapping("/seed")
    public Map<String, Object> seed(@RequestParam String target, @RequestParam long count) {
        BenchmarkTarget benchmarkTarget = BenchmarkTarget.valueOf(target.toUpperCase());
        long totalRows = benchmarkService.seed(benchmarkTarget, count);
        return Map.of("target", benchmarkTarget, "insertedCount", count, "totalRows", totalRows);
    }

    @GetMapping("/query")
    public BenchmarkQueryResponse query(
            @RequestParam String target,
            @RequestParam String type,
            @RequestParam(defaultValue = "90") int days) {
        BenchmarkTarget benchmarkTarget = BenchmarkTarget.valueOf(target.toUpperCase());
        QueryType queryType = QueryType.valueOf(type.toUpperCase());

        long apiStart = System.nanoTime();
        BenchmarkQueryResponse partial = benchmarkService.query(benchmarkTarget, queryType, days);
        long apiElapsedMs = (System.nanoTime() - apiStart) / 1_000_000;

        BenchmarkQueryResponse full = partial.withApiElapsedMs(apiElapsedMs);
        resultStore.add(full);
        return full;
    }

    @DeleteMapping("/reset")
    public Map<String, Object> reset(@RequestParam String target) {
        BenchmarkTarget benchmarkTarget = BenchmarkTarget.valueOf(target.toUpperCase());
        benchmarkService.reset(benchmarkTarget);
        return Map.of("target", benchmarkTarget, "status", "reset");
    }

    @GetMapping("/results")
    public List<BenchmarkQueryResponse> results() {
        return resultStore.all();
    }
}
