package com.pigs.voxly.infrastructure.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record AppMailProperties(
        String fromEmail,
        String fromName,
        String baseUrl
) {

    public AppMailProperties {
        if (fromEmail == null) fromEmail = "noreply@voxly.com";
        if (fromName == null) fromName = "VoxLy";
        if (baseUrl == null) baseUrl = "http://localhost:8080";
    }
}
