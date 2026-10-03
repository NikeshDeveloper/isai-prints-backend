package com.isaiprints.domain.entity;

import com.isaiprints.domain.enums.ColorMode;
import com.isaiprints.domain.enums.PaperSize;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pricing", uniqueConstraints = @UniqueConstraint(columnNames = {"paper_size", "color_mode"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pricing {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "paper_size", nullable = false)
    private PaperSize paperSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "color_mode", nullable = false)
    private ColorMode colorMode;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
