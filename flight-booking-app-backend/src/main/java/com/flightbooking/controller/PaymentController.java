package com.flightbooking.controller;

import com.flightbooking.dto.response.ApiResponse;
import com.flightbooking.dto.response.PaymentOrderResponse;
import com.flightbooking.entity.User;
import com.flightbooking.exception.PaymentVerificationException;
import com.flightbooking.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    // Called by the frontend right before opening the Razorpay Checkout widget.
    @PostMapping("/initiate/{bookingRef}")
    public ApiResponse<PaymentOrderResponse> initiate(
            @AuthenticationPrincipal User user,
            @PathVariable String bookingRef
    ) {
        PaymentOrderResponse order = paymentService.createOrder(bookingRef, user.getId());
        return ApiResponse.success(order, "Payment order created");
    }

    // Called by Razorpay's servers, not the browser — must stay public
    // (see SecurityConfig) and must verify the signature itself since there's
    // no JWT on this request. We read the raw body manually rather than
    // letting Spring deserialize it, because the HMAC signature is computed
    // over the exact raw bytes Razorpay sent.
    // IMPORTANT: Always return 200 OK to acknowledge receipt and stop Razorpay
    // from retrying. Invalid signatures are logged but not processed.
    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(
            HttpServletRequest request,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature
    ) throws IOException {
        String payload = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        try {
            paymentService.handleWebhook(payload, signature);
        } catch (PaymentVerificationException e) {
            log.warn("Invalid webhook signature or payload: {}", e.getMessage());
            // Return 200 to acknowledge receipt and stop retries, but don't process
        } catch (Exception e) {
            log.error("Webhook processing error: {}", e.getMessage(), e);
            // Return 200 to acknowledge receipt and stop retries
        }
        return ResponseEntity.ok("ok");
    }
}
