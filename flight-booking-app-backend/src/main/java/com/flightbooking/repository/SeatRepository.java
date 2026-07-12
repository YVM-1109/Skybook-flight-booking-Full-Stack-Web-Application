package com.flightbooking.repository;

import com.flightbooking.entity.Flight;
import com.flightbooking.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByFlight(Flight flight);

    Optional<Seat> findByFlightAndSeatNumber(Flight flight, String seatNumber);

    @Query("SELECT COUNT(s) FROM Seat s WHERE s.flight = :flight AND s.status = 'AVAILABLE' AND s.seatClass = :seatClass")
    long countAvailableSeats(@Param("flight") Flight flight, @Param("seatClass") Seat.SeatClass seatClass);

    // Releases expired seat locks back to AVAILABLE
    @Modifying
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.lockedUntil = NULL " +
           "WHERE s.status = 'LOCKED' AND s.lockedUntil < :now")
    int releaseExpiredLocks(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.lockedUntil = NULL WHERE s.id IN :seatIds")
    int updateSeatStatusToAvailable(@Param("seatIds") List<Long> seatIds);
}
