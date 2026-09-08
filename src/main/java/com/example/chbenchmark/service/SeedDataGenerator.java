package com.example.chbenchmark.service;

import com.example.chbenchmark.model.EventRow;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class SeedDataGenerator {

    private static final List<String> EVENT_TYPES = List.of(
        "click", "view", "purchase", "login", "logout",
        "signup", "share", "like", "comment", "search"
    );
    private static final int USER_ID_RANGE = 100_000;
    private static final int TIME_RANGE_DAYS = 90;

    public List<EventRow> generate(long count, long startId, long randomSeed, LocalDateTime referenceTime) {
        Random random = new Random(randomSeed);
        List<EventRow> rows = new ArrayList<>();
        for (long i = 0; i < count; i++) {
            long id = startId + i;
            long userId = 1 + random.nextInt(USER_ID_RANGE);
            String eventType = EVENT_TYPES.get(random.nextInt(EVENT_TYPES.size()));
            long secondsAgo = (long) (random.nextDouble() * TIME_RANGE_DAYS * 24 * 3600);
            LocalDateTime eventTime = referenceTime.minusSeconds(secondsAgo);
            double value = Math.round(random.nextDouble() * 1000 * 100) / 100.0;
            rows.add(new EventRow(id, userId, eventType, eventTime, value));
        }
        return rows;
    }
}
