package com.isaiprints.config;

import com.isaiprints.service.LocalStorageService;
import com.isaiprints.service.R2StorageService;
import com.isaiprints.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import java.net.URI;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class StorageConfig {
    private final AppProperties appProperties;

    @Bean
    public StorageService storageService() {
        AppProperties.Storage storageProps = appProperties.getStorage();
        String provider = storageProps.getProvider();
        log.info("[Storage] Initializing provider: {}", provider);
        if ("r2".equalsIgnoreCase(provider) || "s3".equalsIgnoreCase(provider)) {
            if (storageProps.getEndpoint() != null && storageProps.getAccessKey() != null) {
                S3Client s3Client = S3Client.builder()
                        .endpointOverride(URI.create(storageProps.getEndpoint()))
                        .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(storageProps.getAccessKey(), storageProps.getSecretKey())))
                        .region(Region.of("auto"))
                        .forcePathStyle(true)
                        .build();
                S3Presigner presigner = S3Presigner.builder()
                        .endpointOverride(URI.create(storageProps.getEndpoint()))
                        .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(storageProps.getAccessKey(), storageProps.getSecretKey())))
                        .region(Region.of("auto"))
                        .build();
                return new R2StorageService(s3Client, presigner, storageProps.getBucket(), storageProps.getEndpoint());
            }
        }
        log.warn("[Storage] Falling back to local filesystem storage");
        return new LocalStorageService(storageProps.getLocalUploadDir());
    }
}
