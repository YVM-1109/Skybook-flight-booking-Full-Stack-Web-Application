package com.flightbooking.service;

import com.flightbooking.entity.Booking;
import com.flightbooking.entity.Payment;
import com.flightbooking.repository.BookingRepository;
import com.flightbooking.repository.PaymentRepository;
import com.flightbooking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// Background job that runs automatically.
// If a user locks seats but abandons the booking (closes the tab,
// payment fails, etc.), those seats stay LOCKED forever unless
// something releases them. This job runs every 2 minutes and
// releases any seat whose lock has expired.
//
// Also cleans up stale PENDING bookings that have exceeded their
// lock window (no payment initiated, or payment failed/abandoned).

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTaskService {

    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    // Runs every 2 minutes (120000 ms)
    @Scheduled(fixedRate = 120000)
    @Transactional
    public void releaseExpiredSeatLocks() {
        int releasedCount = seatRepository.releaseExpiredLocks(LocalDateTime.now());
        if (releasedCount > 0) {
            log.info("Released {} expired seat locks", releasedCount);
        }
    }

    // Runs every 5 minutes - cleans up stale PENDING bookings
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanupStalePendingBookings() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15); // 15 min grace period
        var staleBookings = bookingRepository.findByStatusAndBookedAtBefore(
                Booking.BookingStatus.PENDING, cutoff);

        int cleaned = 0;
        for (Booking booking : staleBookings) {
            // Check if there's a payment that might still be processing
            boolean paymentPending = paymentRepository.findByBooking(booking)
                    .map(p -> p.getStatus() == Payment.PaymentStatus.CREATED)
                    .orElse(false);

            if (!paymentPending) {
                List<Long> seatIds = booking.getPassengers().stream()
                        .map(p -> p.getSeat().getId())
                        .toList();
                seatRepository.updateSeatStatusToAvailable(seatIds);

                booking.setStatus(Booking.BookingStatus.CANCELLED);
                booking.setCancelledAt(LocalDateTime.now());
                bookingRepository.save(booking);
                cleaned++;
            }
        }

        if (cleaned > 0) {
            log.info("Cleaned up {} stale PENDING bookings", cleaned);
        }
    }
}
