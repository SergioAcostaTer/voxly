package com.pigs.voxly.infrastructure.shared.storage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
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
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3", matchIfMissing = true)
public class R2StorageService implements StorageService {
    private static final Logger logger = LoggerFactory.getLogger(R2StorageService.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final Duration presignedUrlDuration;
    private final Path tempDir;

    public R2StorageService(
            @Value("${cloudflare.r2.endpoint}") String endpoint,
            @Value("${cloudflare.r2.accessKeyId}") String accessKeyId,
            @Value("${cloudflare.r2.accessKeySecret}") String accessKeySecret,
            @Value("${cloudflare.r2.bucketName}") String bucketName,
            @Value("${cloudflare.r2.region:auto}") String region,
            @Value("${cloudflare.r2.presigned-url-expiration-seconds:3600}") long presignedUrlExpirationSeconds,
            @Value("${storage.temp-dir:/tmp/voxly-temp}") String tempDirPath) {

        this.bucketName = bucketName;
        this.presignedUrlDuration = Duration.ofSeconds(Math.max(1, presignedUrlExpirationSeconds));
        this.tempDir = Path.of(tempDirPath).toAbsolutePath().normalize();

        // Initialize S3 client with R2 endpoint
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, accessKeySecret);
        Region awsRegion = Region.of(region);

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(awsRegion)
                .build();

        this.s3Presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(awsRegion)
                .build();

        try {
            Files.createDirectories(this.tempDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize temp directory for media processing", e);
        }

        ensureBucketExists();
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

            putObjectEnsuringBucketExists(putRequest, fileBytes);

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
            Path tempFile = Files.createTempFile(tempDir, "voxly-media-", extractExtension(storagePath));

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
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(storagePath)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(presignedUrlDuration)
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
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

    private String extractExtension(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return ".bin";
        }

        String fileName = Path.of(storagePath).getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return ".bin";
        }

        return fileName.substring(dotIndex);
    }

    private void putObjectEnsuringBucketExists(PutObjectRequest putRequest, byte[] fileBytes) {
        try {
            s3Client.putObject(putRequest, RequestBody.fromBytes(fileBytes));
        } catch (S3Exception e) {
            if (e.statusCode() == 404 || isBucketMissingError(e)) {
                logger.warn("Bucket {} was missing during upload. Creating it and retrying once.", bucketName);
                ensureBucketExists();
                s3Client.putObject(putRequest, RequestBody.fromBytes(fileBytes));
                return;
            }
            throw e;
        }
    }

    private void ensureBucketExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        } catch (S3Exception e) {
            if (e.statusCode() != 404 && !isBucketMissingError(e)) {
                throw e;
            }

            logger.info("Bucket {} does not exist. Creating it.", bucketName);
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        }
    }

    private boolean isBucketMissingError(S3Exception e) {
        String errorCode = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : null;
        return "NoSuchBucket".equals(errorCode);
    }
}
