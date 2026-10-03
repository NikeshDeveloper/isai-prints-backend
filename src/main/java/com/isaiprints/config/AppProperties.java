package com.isaiprints.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    private String frontendUrl = "http://localhost:5173";
    private List<String> adminEmails = List.of("lvnikesh03@gmail.com", "nikeshrahul99@gmail.com");
    private String printAgentToken = "prt_agent_sec_token_987654321";
    private Jwt jwt = new Jwt();
    private Storage storage = new Storage();
    private Payment payment = new Payment();
    private Vapid vapid = new Vapid();

    @Data
    public static class Jwt {
        private String secret = "change-this-in-production-must-be-at-least-256-bits-long-random-string";
        private long expiryHours = 24;
    }

    @Data
    public static class Storage {
        private String provider = "local";
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucket = "isai-prints-files";
        private String localUploadDir = "./uploads";
    }

    @Data
    public static class Payment {
        private String razorpayKeyId;
        private String razorpayKeySecret;
        private String razorpayWebhookSecret;
    }

    @Data
    public static class Vapid {
        private String publicKey = "";
        private String privateKey = "";
        private String subject = "mailto:admin@isaiprints.com";
    }
}
