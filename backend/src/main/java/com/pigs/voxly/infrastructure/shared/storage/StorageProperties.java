package com.pigs.voxly.infrastructure.shared.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String type,
        String localPath,
        String baseUrl,
        long maxFileSizeBytes
) {
    public StorageProperties {
        if (type == null) type = "local";
        if (localPath == null) localPath = "./uploads";
        if (baseUrl == null) baseUrl = "/api/v1/files";
        if (maxFileSizeBytes == 0) maxFileSizeBytes = 104857600L; // 100MB default
    }
}
