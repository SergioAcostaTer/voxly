package com.pigs.voxly.infrastructure.shared.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.pigs.voxly.application.shared.ports.StorageService;
import com.pigs.voxly.sharedKernel.domain.results.Error;
import com.pigs.voxly.sharedKernel.domain.results.Result;
import com.pigs.voxly.sharedKernel.domain.results.ResultT;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3", matchIfMissing = true)
public class R2StorageService implements StorageService {
    private static final Logger logger = LoggerFactory.getLogger(R2StorageService.class);

    private final S3Client s3Client;
    private final String bucketName;
    private final String publicUrl;
    private final Path tempDir;

    public R2StorageService(
            @Value("${cloudflare.r2.endpoint}") String endpoint,
            @Value("${cloudflare.r2.accessKeyId}") String accessKeyId,
            @Value("${cloudflare.r2.accessKeySecret}") String accessKeySecret,
            @Value("${cloudflare.r2.bucketName}") String bucketName,
            @Value("${cloudflare.r2.publicUrl}") String publicUrl,
            @Value("${storage.temp-dir:/tmp/voxly-temp}") String tempDirPath) {

        this.bucketName = bucketName;
        this.publicUrl = publicUrl;
        this.tempDir = Path.of(tempDirPath).toAbsolutePath().normalize();

        // Initialize S3 client with R2 endpoint
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, accessKeySecret);

        this.s3Client = S3Client.builder()
                .region(Region.US_EAST_1)
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();

        try {
            Files.createDirectories(this.tempDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize temp directory for media processing", e);
        }

        logger.info("R2StorageService initialized with bucket: {}", bucketName);
    }

    @Override
    public ResultT<String> store(InputStream inputStream, String fileName, String contentType, String directory) {
        try {
            if (inputStream == null) {
                return ResultT.failure(Error.validation("Storage.NullInput", "Input stream cannot be null"));
            }

            String key = generateStorageKey(directory, fileName);
            byte[] fileBytes = inputStream.readAllBytes();

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .acl(ObjectCannedACL.PRIVATE)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .contentLength((long) fileBytes.length)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(fileBytes));

            logger.info("File uploaded successfully to R2: {}", key);
            return ResultT.success(key);

        } catch (Exception e) {
            logger.error("Failed to upload file to R2: {}", e.getMessage(), e);
            return ResultT.failure(Error.failure("Storage.UploadFailed", "Upload failed: " + e.getMessage()));
        }
    }

    @Override
    public java.util.Optional<InputStream> retrieve(String storagePath) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(storagePath)
                    .build();

            return java.util.Optional.of(s3Client.getObject(getRequest));
        } catch (Exception e) {
            logger.error("Failed to retrieve file from R2: {}", storagePath, e);
            return java.util.Optional.empty();
        }
    }

    @Override
    public Result delete(String storagePath) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(storagePath)
                    .build();

            s3Client.deleteObject(deleteRequest);
            logger.info("File deleted from R2: {}", storagePath);
            return Result.success();
        } catch (Exception e) {
            logger.error("Failed to delete file: {}", e.getMessage());
            return Result.failure(Error.failure("Storage.DeleteFailed", "Delete failed: " + e.getMessage()));
        }
    }

    @Override
    public boolean exists(String storagePath) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(storagePath)
                    .build();
            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            return e.statusCode() != 404;
        }
    }

    @Override
    public Path getAbsolutePath(String storagePath) {
        try {
            Files.createDirectories(tempDir);
            Path tempFile = Files.createTempFile(tempDir, "voxly-media-", ".bin");

            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(storagePath)
                    .build();

            try (InputStream inputStream = s3Client.getObject(getRequest)) {
                Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            return tempFile;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to stage object for processing: " + storagePath, e);
        }
    }

    @Override
    public String getPublicUrl(String storagePath) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return "/api/v1/files/" + storagePath;
        }
        return publicUrl.endsWith("/") ? (publicUrl + storagePath) : (publicUrl + "/" + storagePath);
    }

    @Override
    public void cleanupTemporaryFile(Path filePath) {
        try {
            if (filePath != null && filePath.startsWith(tempDir)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            logger.warn("Failed to cleanup temporary file: {}", filePath, e);
        }
    }

    private String generateStorageKey(String directory, String fileName) {
        String safeDirectory = directory == null ? "media" : directory.replace("\\", "/").replaceAll("/+$", "");
        String sanitizedFileName = sanitizeFileName(fileName == null ? "unnamed" : fileName);
        return safeDirectory + "/" + UUID.randomUUID() + "_" + sanitizedFileName;
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
