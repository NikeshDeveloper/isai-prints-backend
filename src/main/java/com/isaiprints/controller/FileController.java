package com.isaiprints.controller;

import com.isaiprints.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {
    private final StorageService storageService;

    @GetMapping("/**")
    public ResponseEntity<byte[]> getFile(HttpServletRequest request) {
        try {
            String path = request.getRequestURI();
            String key = path.substring("/api/files/".length());
            if (key.startsWith("/")) key = key.substring(1);

            byte[] data = storageService.download(key);
            String contentType = guessContentType(key);
            return ResponseEntity.ok()
                .header("Content-Type", contentType)
                .header("Cache-Control", "max-age=3600")
                .body(data);
        } catch (Exception e) {
            log.warn("File not found error: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    private String guessContentType(String key) {
        if (key.endsWith(".pdf")) return "application/pdf";
        if (key.endsWith(".jpg") || key.endsWith(".jpeg")) return "image/jpeg";
        if (key.endsWith(".png")) return "image/png";
        if (key.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }
}
