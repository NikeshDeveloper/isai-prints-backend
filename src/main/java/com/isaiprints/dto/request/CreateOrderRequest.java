package com.isaiprints.dto.request;

import com.isaiprints.domain.enums.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class CreateOrderRequest {
    @NotNull
    private PaymentMethod paymentMethod;

    @NotEmpty
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {
        @NotNull private String storageKey;
        @NotNull private String originalFilename;
        private String mimeType;
        private String fileType;
        private Long fileSize;
        private Integer pageCount;
        private Integer originalWidth;
        private Integer originalHeight;
        @NotNull private String paperSize;
        @NotNull private String colorMode;
        private String orientation;
        private int copies = 1;
        private Map<String, Object> cropData;
    }
}
