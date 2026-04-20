package com.pigs.voxly.sharedKernel.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.pigs.voxly.sharedKernel.domain.results.Error;
import com.pigs.voxly.sharedKernel.domain.results.Result;

/**
 * Utility class for validating uploaded files.
 */
public final class FileValidator {

    private FileValidator() {
    }

    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
            "video/mp4");

    private static final Set<String> ALLOWED_VIDEO_EXTENSIONS = Set.of(
            ".mp4");

    private static final Set<String> ALLOWED_AUDIO_TYPES = Set.of(
            "audio/webm",
            "audio/mp4",
            "audio/mpeg",
            "audio/wav",
            "audio/x-wav",
            "audio/ogg");

    private static final Set<String> ALLOWED_AUDIO_EXTENSIONS = Set.of(
            ".webm",
            ".m4a",
            ".mp4",
            ".mp3",
            ".wav",
            ".ogg");

    private static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-powerpoint");

    private static final Set<String> ALLOWED_DOCUMENT_EXTENSIONS = Set.of(
            ".pdf", ".pptx", ".ppt");

    private static final long DEFAULT_MAX_VIDEO_SIZE = 104857600L; // 100 MB
    private static final long DEFAULT_MAX_DOCUMENT_SIZE = 52428800L; // 50 MB

    /**
     * Validates a video file.
     */
    public static Result validateVideo(String fileName, String contentType, long fileSize) {
        return validateVideo(fileName, contentType, fileSize, DEFAULT_MAX_VIDEO_SIZE);
    }

    /**
     * Validates a video file with custom max size.
     */
    public static Result validateVideo(String fileName, String contentType, long fileSize, long maxSize) {
        List<Error> errors = new ArrayList<>();

        if (fileName == null || fileName.isBlank()) {
            errors.add(Error.validation("File.NoName", "File name is required"));
        } else {
            String extension = getExtension(fileName).toLowerCase();
            if (!ALLOWED_VIDEO_EXTENSIONS.contains(extension)) {
                errors.add(Error.validation("File.InvalidVideoExtension",
                        "Invalid video file extension. Allowed: " + ALLOWED_VIDEO_EXTENSIONS));
            }
        }

        if (contentType == null || !ALLOWED_VIDEO_TYPES.contains(contentType.toLowerCase())) {
            errors.add(Error.validation("File.InvalidVideoType",
                    "Invalid video content type. Allowed: " + ALLOWED_VIDEO_TYPES));
        }

        if (fileSize <= 0) {
            errors.add(Error.validation("File.Empty", "File is empty"));
        } else if (fileSize > maxSize) {
            errors.add(Error.validation("File.TooLarge",
                    String.format("File size (%d MB) exceeds maximum allowed (%d MB)",
                            fileSize / 1048576, maxSize / 1048576)));
        }

        if (!errors.isEmpty()) {
            return Result.failure(errors.toArray(new Error[0]));
        }

        return Result.success();
    }

    /**
     * Validates an audio file.
     */
    public static Result validateAudio(String fileName, String contentType, long fileSize) {
        return validateAudio(fileName, contentType, fileSize, DEFAULT_MAX_VIDEO_SIZE);
    }

    /**
     * Validates an audio file with custom max size.
     */
    public static Result validateAudio(String fileName, String contentType, long fileSize, long maxSize) {
        List<Error> errors = new ArrayList<>();

        if (fileName == null || fileName.isBlank()) {
            errors.add(Error.validation("File.NoName", "File name is required"));
        } else {
            String extension = getExtension(fileName).toLowerCase();
            if (!ALLOWED_AUDIO_EXTENSIONS.contains(extension)) {
                errors.add(Error.validation("File.InvalidAudioExtension",
                        "Invalid audio file extension. Allowed: " + ALLOWED_AUDIO_EXTENSIONS));
            }
        }

        if (contentType == null || !ALLOWED_AUDIO_TYPES.contains(contentType.toLowerCase())) {
            errors.add(Error.validation("File.InvalidAudioType",
                    "Invalid audio content type. Allowed: " + ALLOWED_AUDIO_TYPES));
        }

        if (fileSize <= 0) {
            errors.add(Error.validation("File.Empty", "File is empty"));
        } else if (fileSize > maxSize) {
            errors.add(Error.validation("File.TooLarge",
                    String.format("File size (%d MB) exceeds maximum allowed (%d MB)",
                            fileSize / 1048576, maxSize / 1048576)));
        }

        if (!errors.isEmpty()) {
            return Result.failure(errors.toArray(new Error[0]));
        }

        return Result.success();
    }

    /**
     * Validates a document file (PDF, PPTX).
     */
    public static Result validateDocument(String fileName, String contentType, long fileSize) {
        return validateDocument(fileName, contentType, fileSize, DEFAULT_MAX_DOCUMENT_SIZE);
    }

    /**
     * Validates a document file with custom max size.
     */
    public static Result validateDocument(String fileName, String contentType, long fileSize, long maxSize) {
        List<Error> errors = new ArrayList<>();

        if (fileName == null || fileName.isBlank()) {
            errors.add(Error.validation("File.NoName", "File name is required"));
        } else {
            String extension = getExtension(fileName).toLowerCase();
            if (!ALLOWED_DOCUMENT_EXTENSIONS.contains(extension)) {
                errors.add(Error.validation("File.InvalidDocumentExtension",
                        "Invalid document file extension. Allowed: " + ALLOWED_DOCUMENT_EXTENSIONS));
            }
        }

        if (contentType != null && !ALLOWED_DOCUMENT_TYPES.contains(contentType.toLowerCase())) {
            errors.add(Error.validation("File.InvalidDocumentType",
                    "Invalid document content type. Allowed: PDF, PPTX, PPT"));
        }

        if (fileSize <= 0) {
            errors.add(Error.validation("File.Empty", "File is empty"));
        } else if (fileSize > maxSize) {
            errors.add(Error.validation("File.TooLarge",
                    String.format("File size (%d MB) exceeds maximum allowed (%d MB)",
                            fileSize / 1048576, maxSize / 1048576)));
        }

        if (!errors.isEmpty()) {
            return Result.failure(errors.toArray(new Error[0]));
        }

        return Result.success();
    }

    /**
     * Sanitizes a file name by removing potentially dangerous characters.
     */
    public static String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "unnamed";
        }

        // Remove path components
        fileName = fileName.replace("\\", "/");
        int lastSlash = fileName.lastIndexOf('/');
        if (lastSlash >= 0) {
            fileName = fileName.substring(lastSlash + 1);
        }

        // Replace dangerous characters
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Gets the file extension including the dot.
     */
    public static String getExtension(String fileName) {
        if (fileName == null)
            return "";
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(dotIndex) : "";
    }

    /**
     * Gets the file name without extension.
     */
    public static String getNameWithoutExtension(String fileName) {
        if (fileName == null)
            return "";
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    /**
     * Checks if the content type indicates a video file.
     */
    public static boolean isVideoContentType(String contentType) {
        return contentType != null && ALLOWED_VIDEO_TYPES.contains(contentType.toLowerCase());
    }

    /**
     * Checks if the content type indicates an audio file.
     */
    public static boolean isAudioContentType(String contentType) {
        return contentType != null && ALLOWED_AUDIO_TYPES.contains(contentType.toLowerCase());
    }

    /**
     * Checks if the content type indicates a document file.
     */
    public static boolean isDocumentContentType(String contentType) {
        return contentType != null && ALLOWED_DOCUMENT_TYPES.contains(contentType.toLowerCase());
    }
}
