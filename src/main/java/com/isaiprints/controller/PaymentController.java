package com.isaiprints.controller;

import com.isaiprints.dto.request.VerifyPaymentRequest;
import com.isaiprints.exception.PaymentVerificationException;
import com.isaiprints.service.OrderService;
import com.isaiprints.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;
    private final OrderService orderService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createPaymentOrder(@RequestBody Map<String, String> body) {
        String orderNumber = body.get("orderNumber");
        if (orderNumber == null) return ResponseEntity.badRequest().body(Map.of("error", "orderNumber required"));
        try {
            Map<String, Object> result = paymentService.createPaymentOrder(orderNumber);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[Payment] Create order error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPayment(@RequestBody VerifyPaymentRequest req) {
        String orderNumber = req.getOrderNumber();
        if (orderNumber == null) return ResponseEntity.badRequest().body(Map.of("error", "orderNumber required"));

        try {
            if (req.getRazorpayOrderId() != null && req.getRazorpayPaymentId() != null && req.getRazorpaySignature() != null) {
                paymentService.verifyPaymentSignature(req.getRazorpayOrderId(), req.getRazorpayPaymentId(), req.getRazorpaySignature());
            }

            String paymentRef = req.getRazorpayPaymentId() != null ? req.getRazorpayPaymentId()
                : (req.getUtr() != null ? req.getUtr() : (req.getReference() != null ? req.getReference() : req.getPaymentId()));

            var order = orderService.markPaid(orderNumber, paymentRef, req.getRazorpayOrderId(),
                req.getRazorpayPaymentId(), "RAZORPAY");

            return ResponseEntity.ok(Map.of(
                "success", true,
                "orderNumber", orderNumber,
                "status", order.getStatus().name(),
                "paymentStatus", order.getPaymentStatus().name(),
                "jobsCreated", order.getPrintJobs().size()
            ));
        } catch (PaymentVerificationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("[Payment] Verify error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/failed")
    public ResponseEntity<Map<String, Object>> paymentFailed(@RequestBody Map<String, String> body) {
        String orderNumber = body.get("orderNumber");
        if (orderNumber == null) return ResponseEntity.badRequest().body(Map.of("error", "orderNumber required"));
        var order = orderService.findByOrderNumber(orderNumber);
        order.setStatus(com.isaiprints.domain.enums.OrderStatus.PAYMENT_FAILED);
        order.setPaymentStatus(com.isaiprints.domain.enums.PaymentStatus.FAILED);
        order.setPaymentFailureReason(body.getOrDefault("reason", "Payment declined"));
        return ResponseEntity.ok(Map.of("success", true, "status", "PAYMENT_FAILED"));
    }

    @PostMapping("/cancelled")
    public ResponseEntity<Map<String, Object>> paymentCancelled(@RequestBody Map<String, String> body) {
        String orderNumber = body.get("orderNumber");
        if (orderNumber == null) return ResponseEntity.badRequest().body(Map.of("error", "orderNumber required"));
        return ResponseEntity.ok(Map.of("success", true, "status", "PAYMENT_PENDING"));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(@RequestBody String rawBody,
                                         @RequestHeader(value = "X-Razorpay-Signature", required = false) String sig) {
        try {
            if (sig != null && !sig.isBlank()) {
                paymentService.verifyWebhookSignature(rawBody, sig);
            }
            paymentService.processWebhookPayment(rawBody);
        } catch (PaymentVerificationException e) {
            log.warn("[Webhook] Signature invalid: {}", e.getMessage());
            return ResponseEntity.status(400).build();
        } catch (Exception e) {
            log.error("[Webhook] Error: {}", e.getMessage(), e);
        }
        return ResponseEntity.ok().build();
    }
}
