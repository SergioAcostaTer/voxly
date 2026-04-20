package com.pigs.voxly.application.sessions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RecordingUploadService {

    private final Path tempDir;
    private final ConcurrentHashMap<UUID, UploadState> uploads = new ConcurrentHashMap<>();

    public RecordingUploadService(@Value("${storage.temp-dir:/tmp/voxly-temp}") String tempDirPath) throws IOException {
        this.tempDir = Path.of(tempDirPath).toAbsolutePath().normalize().resolve("recording-uploads");
        Files.createDirectories(this.tempDir);
    }

    public UploadState createUpload(UUID userId, String originalFileName, String contentType) throws IOException {
        String safeFileName = sanitizeFileName(originalFileName == null || originalFileName.isBlank()
                ? "voxly-recording.webm"
                : originalFileName);
        String extension = extractExtension(safeFileName);
        Path tempFile = Files.createTempFile(tempDir, "recording-", extension);

        UploadState state = new UploadState(
                UUID.randomUUID(),
                userId,
                safeFileName,
                contentType == null || contentType.isBlank() ? "audio/webm" : contentType,
                tempFile
        );
        uploads.put(state.uploadId(), state);
        return state;
    }

    public UploadState appendChunk(UUID userId, UUID uploadId, MultipartFile chunk, int sequence, boolean isLastChunk)
            throws IOException {
        UploadState state = getOwnedUpload(userId, uploadId);

        synchronized (state) {
            if (state.completed()) {
                throw new IllegalStateException("Recording upload is already completed");
            }
            if (sequence != state.nextSequence()) {
                throw new IllegalStateException("Unexpected chunk sequence");
            }

            try (InputStream inputStream = chunk.getInputStream()) {
                Files.write(state.path(), inputStream.readAllBytes(),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }

            state.advance(chunk.getSize(), isLastChunk);
            return state;
        }
    }

    public UploadState getOwnedCompletedUpload(UUID userId, UUID uploadId) {
        UploadState state = getOwnedUpload(userId, uploadId);
        if (!state.completed()) {
            throw new IllegalStateException("Recording upload is not complete yet");
        }
        return state;
    }

    public void deleteUpload(UUID userId, UUID uploadId) {
        UploadState state = getOwnedUpload(userId, uploadId);
        uploads.remove(uploadId);
        try {
            Files.deleteIfExists(state.path());
        } catch (IOException ignored) {
        }
    }

    public void consumeUpload(UUID userId, UUID uploadId) {
        deleteUpload(userId, uploadId);
    }

    private UploadState getOwnedUpload(UUID userId, UUID uploadId) {
        UploadState state = uploads.get(uploadId);
        if (state == null || !state.userId().equals(userId)) {
            throw new IllegalStateException("Recording upload not found");
        }
        return state;
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            return ".webm";
        }
        return fileName.substring(dotIndex);
    }

    public static final class UploadState {
        private final UUID uploadId;
        private final UUID userId;
        private final String originalFileName;
        private final String contentType;
        private final Path path;
        private int nextSequence;
        private long sizeBytes;
        private boolean completed;
        private Instant modifiedAt;

        private UploadState(UUID uploadId, UUID userId, String originalFileName, String contentType, Path path) {
            this.uploadId = uploadId;
            this.userId = userId;
            this.originalFileName = originalFileName;
            this.contentType = contentType;
            this.path = path;
            this.modifiedAt = Instant.now();
        }

        public UUID uploadId() { return uploadId; }
        public UUID userId() { return userId; }
        public String originalFileName() { return originalFileName; }
        public String contentType() { return contentType; }
        public Path path() { return path; }
        public int nextSequence() { return nextSequence; }
        public long sizeBytes() { return sizeBytes; }
        public boolean completed() { return completed; }
        public Instant modifiedAt() { return modifiedAt; }

        private void advance(long chunkSize, boolean isLastChunk) {
            nextSequence++;
            sizeBytes += chunkSize;
            completed = isLastChunk;
            modifiedAt = Instant.now();
        }
    }
}
