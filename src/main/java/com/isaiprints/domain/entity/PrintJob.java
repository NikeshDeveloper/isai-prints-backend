package com.isaiprints.domain.entity;

import com.isaiprints.domain.enums.PrintJobStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "print_jobs", indexes = {
    @Index(name = "idx_print_jobs_status", columnList = "status"),
    @Index(name = "idx_print_jobs_order", columnList = "order_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrintJob {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PrintJobStatus status = PrintJobStatus.QUEUED;

    @Column(name = "attempt_count")
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "agent_id")
    private String agentId;

    @Column(name = "claim_token")
    private String claimToken;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "printer_name")
    private String printerName;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private Long version;
}
