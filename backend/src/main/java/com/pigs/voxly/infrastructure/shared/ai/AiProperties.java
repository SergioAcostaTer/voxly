package com.pigs.voxly.infrastructure.shared.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "openai")
public record AiProperties(
        String apiKey,
        @DefaultValue("https://api.openai.com/v1") String baseUrl,
        @DefaultValue OpenAiWhisperProperties whisper,
        @DefaultValue OpenAiGptProperties gpt
) {
    public record OpenAiWhisperProperties(
            @DefaultValue("whisper-1") String model
    ) {}

    public record OpenAiGptProperties(
            @DefaultValue("gpt-4o-mini") String model
    ) {}
}
