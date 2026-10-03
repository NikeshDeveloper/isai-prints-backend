package com.isaiprints.service;

import com.isaiprints.domain.entity.Pricing;
import com.isaiprints.domain.enums.ColorMode;
import com.isaiprints.domain.enums.PaperSize;
import com.isaiprints.domain.repository.PricingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PricingService {
    private final PricingRepository pricingRepository;

    public List<Pricing> getAllPricing() {
        return pricingRepository.findAll();
    }

    public BigDecimal getUnitPrice(PaperSize paperSize, ColorMode colorMode) {
        return pricingRepository.findByPaperSizeAndColorMode(paperSize, colorMode)
            .map(Pricing::getUnitPrice)
            .orElseGet(() -> colorMode == ColorMode.COLOR ? BigDecimal.valueOf(10) : BigDecimal.valueOf(5));
    }

    public BigDecimal calculateItemPrice(PaperSize paperSize, ColorMode colorMode, int copies, int pageCount) {
        BigDecimal unit = getUnitPrice(paperSize, colorMode);
        return unit.multiply(BigDecimal.valueOf(copies)).multiply(BigDecimal.valueOf(pageCount));
    }

    @Transactional
    public Pricing updatePricing(PaperSize paperSize, ColorMode colorMode, BigDecimal newPrice) {
        Pricing pricing = pricingRepository.findByPaperSizeAndColorMode(paperSize, colorMode)
            .orElse(Pricing.builder().paperSize(paperSize).colorMode(colorMode).build());
        pricing.setUnitPrice(newPrice);
        return pricingRepository.save(pricing);
    }
}
