package com.isaiprints.controller;

import com.isaiprints.domain.entity.Pricing;
import com.isaiprints.domain.enums.ColorMode;
import com.isaiprints.domain.enums.PaperSize;
import com.isaiprints.dto.request.UpdatePricingRequest;
import com.isaiprints.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PricingController {
    private final PricingService pricingService;

    @GetMapping("/pricing")
    public ResponseEntity<Map<String, Object>> getPricing() {
        List<Pricing> all = pricingService.getAllPricing();
        List<Map<String, Object>> list = all.stream().map(p -> Map.<String, Object>of(
            "id", p.getId().toString(),
            "paperSize", p.getPaperSize().name(),
            "colorMode", p.getColorMode().name(),
            "unitPrice", p.getUnitPrice()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("pricing", list));
    }

    @PutMapping("/admin/pricing")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updatePricing(@RequestBody UpdatePricingRequest req) {
        Pricing updated = pricingService.updatePricing(
            PaperSize.valueOf(req.getPaperSize()),
            ColorMode.valueOf(req.getColorMode()),
            req.getUnitPrice()
        );
        return ResponseEntity.ok(Map.of(
            "success", true,
            "paperSize", updated.getPaperSize().name(),
            "colorMode", updated.getColorMode().name(),
            "unitPrice", updated.getUnitPrice()
        ));
    }
}
