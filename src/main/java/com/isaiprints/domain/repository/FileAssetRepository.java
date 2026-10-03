package com.isaiprints.domain.repository;

import com.isaiprints.domain.entity.FileAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface FileAssetRepository extends JpaRepository<FileAsset, UUID> {
    Optional<FileAsset> findByStorageKey(String storageKey);
}
