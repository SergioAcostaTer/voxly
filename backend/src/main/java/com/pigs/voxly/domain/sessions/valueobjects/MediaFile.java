package com.pigs.voxly.domain.sessions.valueobjects;

import com.pigs.voxly.sharedKernel.domain.ddd.ValueObject;

import java.util.ArrayList;
import java.util.List;

public final class MediaFile extends ValueObject {

    private final String storagePath;
    private final String originalFileName;
    private final String contentType;
    private final long sizeBytes;
    private final Double durationSeconds;

    private MediaFile(String storagePath, String originalFileName, String contentType, long sizeBytes, Double durationSeconds) {
        this.storagePath = storagePath;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.durationSeconds = durationSeconds;
    }

    public static MediaFile create(String storagePath, String originalFileName, String contentType, long sizeBytes) {
        return new MediaFile(storagePath, originalFileName, contentType, sizeBytes, null);
    }

    public static MediaFile reconstitute(String storagePath, String originalFileName, String contentType, long sizeBytes, Double durationSeconds) {
        return new MediaFile(storagePath, originalFileName, contentType, sizeBytes, durationSeconds);
    }

    public MediaFile withDuration(double durationSeconds) {
        return new MediaFile(this.storagePath, this.originalFileName, this.contentType, this.sizeBytes, durationSeconds);
    }

    public String getStoragePath() {
        return storagePath;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public Double getDurationSeconds() {
        return durationSeconds;
    }

    public double getSizeMB() {
        return sizeBytes / 1048576.0;
    }

    @Override
    protected List<Object> equalityComponents() {
        List<Object> components = new ArrayList<>();
        components.add(storagePath);
        components.add(originalFileName);
        components.add(contentType);
        components.add(sizeBytes);
        components.add(durationSeconds);
        return components;
    }
}
