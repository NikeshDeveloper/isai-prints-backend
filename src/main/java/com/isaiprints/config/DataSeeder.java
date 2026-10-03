package com.isaiprints.config;

import com.isaiprints.domain.entity.Pricing;
import com.isaiprints.domain.enums.ColorMode;
import com.isaiprints.domain.enums.PaperSize;
import com.isaiprints.domain.repository.PricingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {
    private final PricingRepository pricingRepository;

    @Override
    public void run(String... args) {
        if (pricingRepository.count() == 0) {
            log.info("Seeding default pricing rules into database...");
            pricingRepository.saveAll(List.of(
                Pricing.builder().paperSize(PaperSize.A4).colorMode(ColorMode.BW).unitPrice(new BigDecimal("5.00")).build(),
                Pricing.builder().paperSize(PaperSize.A4).colorMode(ColorMode.COLOR).unitPrice(new BigDecimal("10.00")).build(),
                Pricing.builder().paperSize(PaperSize.A5).colorMode(ColorMode.BW).unitPrice(new BigDecimal("3.00")).build(),
                Pricing.builder().paperSize(PaperSize.A5).colorMode(ColorMode.COLOR).unitPrice(new BigDecimal("8.00")).build()
            ));
            log.info("Default pricing rules seeded successfully.");
        }
    }
}
