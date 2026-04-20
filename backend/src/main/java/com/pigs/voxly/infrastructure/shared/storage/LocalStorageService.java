package com.pigs.voxly.infrastructure.shared.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.pigs.voxly.application.shared.ports.StorageService;
import com.pigs.voxly.sharedKernel.domain.results.Error;
import com.pigs.voxly.sharedKernel.domain.results.Result;
import com.pigs.voxly.sharedKernel.domain.results.ResultT;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local")
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final Path rootLocation;
    private final String baseUrl;

    public LocalStorageService(StorageProperties properties) {
        this.rootLocation = Paths.get(properties.localPath()).toAbsolutePath().normalize();
        this.baseUrl = properties.baseUrl();
        initializeStorage();
    }

    private void initializeStorage() {
        try {
            Files.createDirectories(rootLocation);
            log.info("Storage initialized at: {}", rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    @Override
    public ResultT<String> store(InputStream inputStream, String fileName, String contentType, String directory) {
        try {
            if (inputStream == null) {
                return ResultT.failure(Error.validation("Storage.NullInput", "Input stream cannot be null"));
            }

            String sanitizedFileName = sanitizeFileName(fileName);
            String uniqueFileName = generateUniqueFileName(sanitizedFileName);

            Path targetDirectory = rootLocation.resolve(directory).normalize();
            if (!targetDirectory.startsWith(rootLocation)) {
                return ResultT.failure(Error.validation("Storage.InvalidPath", "Invalid directory path"));
            }

            Files.createDirectories(targetDirectory);
            Path targetPath = targetDirectory.resolve(uniqueFileName);

            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

            String storagePath = directory + "/" + uniqueFileName;
            log.info("File stored: {}", storagePath);

            return ResultT.success(storagePath);
        } catch (IOException e) {
            log.error("Failed to store file: {}", fileName, e);
            return ResultT.failure(Error.failure("Storage.WriteFailed", "Failed to store file: " + e.getMessage()));
        }
    }

    @Override
    public Optional<InputStream> retrieve(String storagePath) {
        try {
            Path filePath = rootLocation.resolve(storagePath).normalize();
            if (!filePath.startsWith(rootLocation)) {
                log.warn("Attempted path traversal: {}", storagePath);
                return Optional.empty();
            }

            if (Files.exists(filePath) && Files.isReadable(filePath)) {
                return Optional.of(Files.newInputStream(filePath));
            }
            return Optional.empty();
        } catch (IOException e) {
            log.error("Failed to retrieve file: {}", storagePath, e);
            return Optional.empty();
        }
    }

    @Override
    public Result delete(String storagePath) {
        try {
            Path filePath = rootLocation.resolve(storagePath).normalize();
            if (!filePath.startsWith(rootLocation)) {
                return Result.failure(Error.validation("Storage.InvalidPath", "Invalid file path"));
            }

            if (Files.deleteIfExists(filePath)) {
                log.info("File deleted: {}", storagePath);
                return Result.success();
            } else {
                return Result.failure(Error.notFound("Storage.FileNotFound", "File not found: " + storagePath));
            }
        } catch (IOException e) {
            log.error("Failed to delete file: {}", storagePath, e);
            return Result.failure(Error.failure("Storage.DeleteFailed", "Failed to delete file: " + e.getMessage()));
        }
    }

    @Override
    public boolean exists(String storagePath) {
        Path filePath = rootLocation.resolve(storagePath).normalize();
        return filePath.startsWith(rootLocation) && Files.exists(filePath);
    }

    @Override
    public Path getAbsolutePath(String storagePath) {
        return rootLocation.resolve(storagePath).normalize();
    }

    @Override
    public String getPublicUrl(String storagePath) {
        return baseUrl + "/" + storagePath;
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "unnamed";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String generateUniqueFileName(String originalName) {
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalName.substring(dotIndex);
            originalName = originalName.substring(0, dotIndex);
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8);

        return originalName + "_" + timestamp + "_" + uuid + extension;
    }
}
