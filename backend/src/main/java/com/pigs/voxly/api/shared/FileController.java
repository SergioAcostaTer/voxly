package com.pigs.voxly.api.shared;

import java.io.InputStream;
import java.net.URLConnection;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pigs.voxly.application.shared.ports.StorageService;

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
            @PathVariable String filename) {
        return serve(directory + "/" + filename, filename);
    }

    @GetMapping("/{directory}/{subdirectory}/{filename}")
    public ResponseEntity<Resource> serveNestedFile(
            @PathVariable String directory,
            @PathVariable String subdirectory,
            @PathVariable String filename) {
        return serve(directory + "/" + subdirectory + "/" + filename, filename);
    }

    private ResponseEntity<Resource> serve(String storagePath, String filename) {
        var fileStreamOpt = storageService.retrieve(storagePath);
        if (fileStreamOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = guessContentType(filename);
        InputStream inputStream = fileStreamOpt.get();
        Resource resource = new InputStreamResource(inputStream);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
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
