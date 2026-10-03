package com.isaiprints.service;

public interface StorageService {
    String upload(byte[] data, String key, String contentType);
    byte[] download(String key);
    String getDownloadUrl(String key);
    void delete(String key);
    String getProviderName();
}
