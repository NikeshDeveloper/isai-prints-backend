package com.isaiprints.config;

import com.razorpay.RazorpayClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RazorpayConfig {
    private final AppProperties appProperties;

    @Bean
    public RazorpayClient razorpayClient() {
        AppProperties.Payment payment = appProperties.getPayment();
        if (payment.getRazorpayKeyId() != null && payment.getRazorpayKeySecret() != null
                && !payment.getRazorpayKeyId().isBlank()) {
            try {
                return new RazorpayClient(payment.getRazorpayKeyId(), payment.getRazorpayKeySecret());
            } catch (Exception e) {
                log.error("[Razorpay] Failed to initialize client: {}", e.getMessage());
            }
        }
        log.warn("[Razorpay] No credentials configured — payment gateway will be in sandbox/mock mode");
        return null;
    }
}
