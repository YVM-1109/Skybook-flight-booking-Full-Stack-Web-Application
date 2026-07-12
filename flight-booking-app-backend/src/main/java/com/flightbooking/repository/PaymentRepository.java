package com.flightbooking.repository;

import com.flightbooking.entity.Booking;
import com.flightbooking.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByBooking(Booking booking);
    Optional<Payment> findByRazorpayOrderId(String orderId);
}
