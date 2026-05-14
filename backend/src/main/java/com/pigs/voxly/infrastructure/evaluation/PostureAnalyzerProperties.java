package com.pigs.voxly.infrastructure.evaluation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "posture.analyzer")
public record PostureAnalyzerProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("http://localhost:8000") String url,
        @DefaultValue("http://localhost:8000") String renderBase) {
}
