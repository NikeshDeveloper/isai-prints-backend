package com.isaiprints.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "file_assets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "storage_key", unique = true, nullable = false, length = 500)
    private String storageKey;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "file_type", nullable = false)
    private String fileType; // IMAGE or PDF

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "original_width")
    private Integer originalWidth;

    @Column(name = "original_height")
    private Integer originalHeight;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
