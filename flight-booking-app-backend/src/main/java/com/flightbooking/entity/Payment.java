package com.flightbooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(nullable = false)
    private String razorpayOrderId;

    private String razorpayPaymentId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.CREATED;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime initiatedAt = LocalDateTime.now();

    private LocalDateTime completedAt;

    @Version
    private Long version;

    public enum PaymentStatus {
        CREATED, SUCCESS, FAILED, REFUNDED
    }
}
