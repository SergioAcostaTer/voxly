package com.pigs.voxly.infrastructure.shared.rateLimit;

import com.pigs.voxly.application.shared.RateLimitExceededException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class AnalysisRateLimiter {

    private final AnalysisRateLimitProperties properties;
    private final ConcurrentHashMap<UUID, ConcurrentLinkedQueue<Instant>> requestsByUser = new ConcurrentHashMap<>();

    public AnalysisRateLimiter(AnalysisRateLimitProperties properties) {
        this.properties = properties;
    }

    public void checkAllowed(UUID userId) {
        if (!properties.enabled()) {
            return;
        }

        var now = Instant.now();
        var cutoff = now.minus(Duration.ofMinutes(properties.windowMinutes()));
        var requests = requestsByUser.computeIfAbsent(userId, ignored -> new ConcurrentLinkedQueue<>());

        synchronized (requests) {
            while (!requests.isEmpty() && requests.peek().isBefore(cutoff)) {
                requests.poll();
            }

            if (requests.size() >= properties.maxRequests()) {
                throw new RateLimitExceededException(String.format(
                        "Too many analysis requests. Limit is %d requests per %d minutes.",
                        properties.maxRequests(),
                        properties.windowMinutes()
                ));
            }

            requests.add(now);
        }
    }
}
