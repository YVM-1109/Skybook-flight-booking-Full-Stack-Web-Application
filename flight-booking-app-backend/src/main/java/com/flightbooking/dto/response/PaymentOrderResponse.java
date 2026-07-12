package com.flightbooking.dto.response;

import lombok.*;

// Everything the React frontend needs to open the Razorpay Checkout widget.
// amountInPaise is intentional — Razorpay's API works in the smallest currency
// unit (paise for INR), never in rupees.
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentOrderResponse {
    private String razorpayOrderId;
    private String razorpayKeyId;
    private Long amountInPaise;
    private String currency;
    private String bookingRef;
    private String description;
}
