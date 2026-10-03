package com.isaiprints.domain.entity;

import com.isaiprints.domain.enums.ColorMode;
import com.isaiprints.domain.enums.PaperSize;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "order_items", indexes = @Index(name = "idx_order_items_order", columnList = "order_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_asset_id")
    private FileAsset fileAsset;

    @Column(name = "print_ready_key", length = 500)
    private String printReadyKey;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "storage_key", length = 500)
    private String storageKey;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "page_count")
    @Builder.Default
    private Integer pageCount = 1;

    @Column(name = "original_width")
    private Integer originalWidth;

    @Column(name = "original_height")
    private Integer originalHeight;

    @Column(name = "print_width")
    private Integer printWidth;

    @Column(name = "print_height")
    private Integer printHeight;

    @Enumerated(EnumType.STRING)
    @Column(name = "paper_size", nullable = false)
    private PaperSize paperSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "color_mode", nullable = false)
    private ColorMode colorMode;

    @Column(nullable = false)
    @Builder.Default
    private String orientation = "AUTO";

    @Column(nullable = false)
    @Builder.Default
    private Integer copies = 1;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "crop_data", columnDefinition = "jsonb")
    private Map<String, Object> cropData;

    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private java.util.List<PrintJob> printJobs = new java.util.ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
