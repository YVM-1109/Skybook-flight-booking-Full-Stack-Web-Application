package com.flightbooking.repository;

import com.flightbooking.entity.Booking;
import com.flightbooking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingRef(String bookingRef);
    List<Booking> findByUserOrderByBookedAtDesc(User user);
    List<Booking> findAllByOrderByBookedAtDesc();

    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.bookedAt < :cutoff")
    List<Booking> findByStatusAndBookedAtBefore(@Param("status") Booking.BookingStatus status, @Param("cutoff") LocalDateTime cutoff);
}
