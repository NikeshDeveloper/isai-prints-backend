package com.isaiprints.service;

import lombok.extern.slf4j.Slf4j;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class LocalStorageService implements StorageService {
    private final Path uploadDir;

    public LocalStorageService(String uploadDirPath) {
        this.uploadDir = Paths.get(uploadDirPath);
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            log.error("Could not create upload directory: {}", e.getMessage());
        }
    }

    @Override
    public String upload(byte[] data, String key, String contentType) {
        try {
            Path target = uploadDir.resolve(key);
            Files.createDirectories(target.getParent());
            Files.write(target, data);
            log.info("[LocalStorage] Stored: {}", key);
            return key;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + key, e);
        }
    }

    @Override
    public byte[] download(String key) {
        try {
            return Files.readAllBytes(uploadDir.resolve(key));
        } catch (IOException e) {
            throw new RuntimeException("File not found: " + key, e);
        }
    }

    @Override
    public String getDownloadUrl(String key) {
        return "/api/files/" + key;
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(uploadDir.resolve(key));
        } catch (IOException e) {
            log.warn("Could not delete file: {}", key);
        }
    }

    @Override
    public String getProviderName() {
        return "local";
    }
}
