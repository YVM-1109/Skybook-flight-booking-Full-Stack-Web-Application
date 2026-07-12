package com.flightbooking.config;

import com.razorpay.RazorpayClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Wires up a single RazorpayClient bean using the key id/secret from
// application.properties (app.razorpay.key-id / app.razorpay.key-secret).
// The secret NEVER goes to the frontend — only the key id does (it's public
// by design, needed to open the Razorpay Checkout widget in the browser).

@Configuration
public class RazorpayConfig {

    @Value("${app.razorpay.key-id}")
    private String keyId;

    @Value("${app.razorpay.key-secret}")
    private String keySecret;

    @PostConstruct
    public void validateConfig() {
        if (keyId == null || keyId.isBlank()) {
            throw new IllegalStateException("Razorpay key-id (app.razorpay.key-id) is not configured");
        }
        if (keySecret == null || keySecret.isBlank()) {
            throw new IllegalStateException("Razorpay key-secret (app.razorpay.key-secret) is not configured");
        }
    }

    @Bean
    public RazorpayClient razorpayClient() throws Exception {
        return new RazorpayClient(keyId, keySecret);
    }
}
