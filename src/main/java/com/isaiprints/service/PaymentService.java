package com.isaiprints.service;

import com.isaiprints.config.AppProperties;
import com.isaiprints.domain.enums.PaymentStatus;
import com.isaiprints.domain.repository.OrderRepository;
import com.isaiprints.exception.PaymentVerificationException;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

@Service
@Slf4j
public class PaymentService {
    private final AppProperties appProperties;
    private final RazorpayClient razorpayClient;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final AuditLogService auditLogService;

    public PaymentService(AppProperties appProperties,
                          ObjectProvider<RazorpayClient> razorpayClientProvider,
                          OrderRepository orderRepository,
                          OrderService orderService,
                          AuditLogService auditLogService) {
        this.appProperties = appProperties;
        this.razorpayClient = razorpayClientProvider.getIfAvailable();
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.auditLogService = auditLogService;
    }

    public Map<String, Object> createPaymentOrder(String orderNumber) throws RazorpayException {
        var order = orderService.findByOrderNumber(orderNumber);
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Order is already paid");
        }

        long amountInPaise = order.getTotalAmount().multiply(java.math.BigDecimal.valueOf(100)).longValue();
        String gatewayOrderId;

        if (razorpayClient != null) {
            JSONObject opts = new JSONObject();
            opts.put("amount", amountInPaise);
            opts.put("currency", "INR");
            opts.put("receipt", orderNumber);
            JSONObject notes = new JSONObject();
            notes.put("orderNumber", orderNumber);
            opts.put("notes", notes);
            com.razorpay.Order rzpOrder = razorpayClient.orders.create(opts);
            gatewayOrderId = rzpOrder.get("id");
        } else {
            gatewayOrderId = "order_sandbox_" + java.util.UUID.randomUUID().toString().replace("-","").substring(0, 16);
        }

        var o = orderService.findByOrderNumber(orderNumber);
        o.setGatewayOrderId(gatewayOrderId);
        o.setGatewayProvider("RAZORPAY");
        o.setPaymentStatus(PaymentStatus.PROCESSING);
        orderRepository.save(o);

        log.info("[Payment] Created gateway order {} for {}", gatewayOrderId, orderNumber);
        return Map.of(
            "success", true,
            "orderNumber", orderNumber,
            "gatewayOrderId", gatewayOrderId,
            "amount", amountInPaise,
            "displayAmount", order.getTotalAmount(),
            "currency", "INR",
            "key", appProperties.getPayment().getRazorpayKeyId() != null && !appProperties.getPayment().getRazorpayKeyId().isBlank()
                  ? appProperties.getPayment().getRazorpayKeyId() : "rzp_test_sandbox",
            "provider", "RAZORPAY",
            "shopName", "ISAI PRINTS",
            "description", "Print Order " + orderNumber
        );
    }

    public void verifyPaymentSignature(String razorpayOrderId, String razorpayPaymentId, String signature)
            throws PaymentVerificationException {
        String keySecret = appProperties.getPayment().getRazorpayKeySecret();
        if (keySecret == null || keySecret.isBlank()) {
            if (signature != null && (signature.startsWith("sig_") || signature.length() == 64)) return;
            throw new PaymentVerificationException("Payment signature invalid (sandbox mode)");
        }
        String payload = razorpayOrderId + "|" + razorpayPaymentId;
        String expected = hmacSha256(payload, keySecret);
        if (!expected.equals(signature)) {
            throw new PaymentVerificationException("Payment signature verification failed");
        }
    }

    public void verifyWebhookSignature(String rawBody, String signature) throws PaymentVerificationException {
        String webhookSecret = appProperties.getPayment().getRazorpayWebhookSecret();
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("[Payment] Webhook secret not configured — skipping signature verification");
            return;
        }
        String expected = hmacSha256(rawBody, webhookSecret);
        if (!expected.equals(signature)) {
            throw new PaymentVerificationException("Webhook signature invalid");
        }
    }

    @Transactional
    public void processWebhookPayment(String rawBody) {
        try {
            JSONObject body = new JSONObject(rawBody);
            String event = body.optString("event");

            if ("payment.captured".equals(event) || "order.paid".equals(event)) {
                JSONObject payload = body.optJSONObject("payload");
                if (payload == null) return;
                JSONObject paymentObj = payload.optJSONObject("payment");
                if (paymentObj == null) return;
                JSONObject paymentEntity = paymentObj.optJSONObject("entity");
                if (paymentEntity == null) return;

                String paymentId = paymentEntity.optString("id");
                String gatewayOrderId = paymentEntity.optString("order_id");
                JSONObject notes = paymentEntity.optJSONObject("notes");
                String orderNumber = notes != null ? notes.optString("orderNumber") : null;

                if (orderRepository.existsByGatewayPaymentId(paymentId)) {
                    log.info("[Webhook] Payment {} already processed", paymentId);
                    return;
                }

                if (orderNumber == null || orderNumber.isBlank()) {
                    final String goid = gatewayOrderId;
                    orderNumber = orderRepository.findAll().stream()
                        .filter(o -> goid.equals(o.getGatewayOrderId()))
                        .findFirst()
                        .map(o -> o.getOrderNumber())
                        .orElse(null);
                }

                if (orderNumber != null && !orderNumber.isBlank()) {
                    orderService.markPaid(orderNumber, paymentId, gatewayOrderId, paymentId, "RAZORPAY");
                    auditLogService.log("RAZORPAY", "SYSTEM", "PAYMENT_CAPTURED", "Order", orderNumber, "SUCCESS",
                        "Payment " + paymentId + " captured via webhook");
                }
            }
        } catch (Exception e) {
            log.error("[Webhook] Processing error: {}", e.getMessage(), e);
        }
    }

    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC error", e);
        }
    }
}
