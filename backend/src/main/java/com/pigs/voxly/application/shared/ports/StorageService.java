package com.pigs.voxly.application.shared.ports;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

import com.pigs.voxly.sharedKernel.domain.results.Result;
import com.pigs.voxly.sharedKernel.domain.results.ResultT;

/**
 * Port interface for file storage operations.
 * Implementations can use local filesystem, S3, Azure Blob, etc.
 */
public interface StorageService {

    /**
     * Stores a file and returns the storage path/key.
     *
     * @param inputStream the file content
     * @param fileName    the original file name
     * @param contentType the MIME type
     * @param directory   the target directory/prefix
     * @return ResultT containing the storage path on success, or error on failure
     */
    ResultT<String> store(InputStream inputStream, String fileName, String contentType, String directory);

    /**
     * Retrieves a file by its storage path.
     *
     * @param storagePath the path returned by store()
     * @return Optional containing the file input stream, or empty if not found
     */
    Optional<InputStream> retrieve(String storagePath);

    /**
     * Deletes a file by its storage path.
     *
     * @param storagePath the path to delete
     * @return Result indicating success or failure
     */
    Result delete(String storagePath);

    /**
     * Checks if a file exists at the given path.
     *
     * @param storagePath the path to check
     * @return true if the file exists
     */
    boolean exists(String storagePath);

    /**
     * Gets the absolute path for serving files (for local storage).
     *
     * @param storagePath the storage path
     * @return the absolute file path
     */
    Path getAbsolutePath(String storagePath);

    /**
     * Gets the public URL for accessing the file (for cloud storage).
     *
     * @param storagePath the storage path
     * @return the public URL or local path
     */
    String getPublicUrl(String storagePath);

    /**
     * Cleans up temporary files created during processing (if any).
     * Cloud-backed storage implementations can download objects to temp files
     * and remove them after transcription/analysis completes.
     */
    default void cleanupTemporaryFile(Path filePath) {
        // No-op by default.
    }
}
