package com.isaiprints.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import java.time.Duration;

@RequiredArgsConstructor
@Slf4j
public class R2StorageService implements StorageService {
    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final String bucket;
    private final String endpoint;

    @Override
    public String upload(byte[] data, String key, String contentType) {
        s3Client.putObject(
            PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
            RequestBody.fromBytes(data)
        );
        log.info("[R2Storage] Uploaded: {}", key);
        return key;
    }

    @Override
    public byte[] download(String key) {
        return s3Client.getObjectAsBytes(
            GetObjectRequest.builder().bucket(bucket).key(key).build()
        ).asByteArray();
    }

    @Override
    public String getDownloadUrl(String key) {
        var presignedRequest = presigner.presignGetObject(GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofHours(1))
            .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
            .build());
        return presignedRequest.url().toString();
    }

    @Override
    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    @Override
    public String getProviderName() {
        return "r2";
    }
}
