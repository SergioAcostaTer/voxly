package com.pigs.voxly.infrastructure.shared.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        @DefaultValue("mock") String provider,
        OpenAiProperties openai
) {
    public record OpenAiProperties(
            String apiKey,
            @DefaultValue("https://api.openai.com/v1") String baseUrl,
            @DefaultValue("gpt-4") String model,
            @DefaultValue("whisper-1") String whisperModel
    ) {}
}
