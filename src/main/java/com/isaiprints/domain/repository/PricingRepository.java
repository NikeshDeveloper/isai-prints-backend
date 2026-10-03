package com.isaiprints.domain.repository;

import com.isaiprints.domain.entity.Pricing;
import com.isaiprints.domain.enums.ColorMode;
import com.isaiprints.domain.enums.PaperSize;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PricingRepository extends JpaRepository<Pricing, UUID> {
    Optional<Pricing> findByPaperSizeAndColorMode(PaperSize paperSize, ColorMode colorMode);
}
