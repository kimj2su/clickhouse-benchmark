package com.example.chbenchmark.model;

import java.time.LocalDateTime;

public record EventRow(long id, long userId, String eventType, LocalDateTime eventTime, double value) {
}
