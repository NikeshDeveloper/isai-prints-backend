package com.isaiprints.dto.request;

import lombok.Data;

@Data
public class VerifyPaymentRequest {
    private String orderNumber;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
    private String paymentId;
    private String utr;
    private String reference;
}
