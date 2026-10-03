package com.isaiprints.dto.request;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdatePricingRequest {
    private String paperSize;
    private String colorMode;
    private BigDecimal unitPrice;
}
