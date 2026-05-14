package com.pigs.voxly.infrastructure.shared.rateLimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.analysis.rate-limit")
public record AnalysisRateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("20") int maxRequests,
        @DefaultValue("60") long windowMinutes
) {
}
