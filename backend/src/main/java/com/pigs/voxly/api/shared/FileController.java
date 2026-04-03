package com.pigs.voxly.api.shared;

import com.pigs.voxly.application.shared.ports.StorageService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.URLConnection;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/{directory}/{filename}")
    public ResponseEntity<Resource> serveFile(
            @PathVariable String directory,
            @PathVariable String filename
    ) {
        String storagePath = directory + "/" + filename;

        return storageService.retrieve(storagePath)
                .map(inputStream -> {
                    String contentType = guessContentType(filename);

                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                            .contentType(MediaType.parseMediaType(contentType))
                            .body((Resource) new InputStreamResource(inputStream));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{directory}/{subdirectory}/{filename}")
    public ResponseEntity<Resource> serveNestedFile(
            @PathVariable String directory,
            @PathVariable String subdirectory,
            @PathVariable String filename
    ) {
        String storagePath = directory + "/" + subdirectory + "/" + filename;

        return storageService.retrieve(storagePath)
                .map(inputStream -> {
                    String contentType = guessContentType(filename);

                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                            .contentType(MediaType.parseMediaType(contentType))
                            .body((Resource) new InputStreamResource(inputStream));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private String guessContentType(String filename) {
        String contentType = URLConnection.guessContentTypeFromName(filename);
        if (contentType == null) {
            String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
            return switch (ext) {
                case "mp4" -> "video/mp4";
                case "webm" -> "video/webm";
                case "pdf" -> "application/pdf";
                case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
                case "ppt" -> "application/vnd.ms-powerpoint";
                default -> "application/octet-stream";
            };
        }
        return contentType;
    }
}
