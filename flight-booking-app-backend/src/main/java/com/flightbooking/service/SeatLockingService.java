package com.flightbooking.service;

import com.flightbooking.entity.Seat;
import com.flightbooking.exception.ResourceNotFoundException;
import com.flightbooking.exception.SeatUnavailableException;
import com.flightbooking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// This service handles the core concurrency problem of flight booking:
// preventing two users from booking the same seat at the same time.
//
// How it works:
// 1. Each Seat entity has a @Version field (optimistic locking)
// 2. When we try to update a seat's status, Hibernate checks the
//    version number hasn't changed since we read it
// 3. If another transaction modified the seat in between,
//    Hibernate throws OptimisticLockingFailureException automatically
// 4. We catch that and convert it into our own SeatUnavailableException

@Service
@RequiredArgsConstructor
public class SeatLockingService {

    private final SeatRepository seatRepository;

    private static final int LOCK_DURATION_MINUTES = 10;

    @Transactional
    public void lockSeats(List<Long> seatIds) {
        for (Long seatId : seatIds) {
            lockSingleSeat(seatId);
        }
    }

    private void lockSingleSeat(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new ResourceNotFoundException("Seat not found: " + seatId));

        if (seat.getStatus() != Seat.SeatStatus.AVAILABLE) {
            throw new SeatUnavailableException(
                    "Seat " + seat.getSeatNumber() + " is no longer available"
            );
        }

        seat.setStatus(Seat.SeatStatus.LOCKED);
        seat.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));

        try {
            // save() triggers the @Version check against the database.
            // If another request already modified this row since we read it,
            // this throws OptimisticLockingFailureException.
            seatRepository.saveAndFlush(seat);
        } catch (OptimisticLockingFailureException e) {
            throw new SeatUnavailableException(
                    "Seat " + seat.getSeatNumber() + " was just booked by another user. Please choose a different seat."
            );
        }
    }

    @Transactional
    public void confirmSeats(List<Long> seatIds) {
        for (Long seatId : seatIds) {
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new ResourceNotFoundException("Seat not found: " + seatId));
            seat.setStatus(Seat.SeatStatus.BOOKED);
            seat.setLockedUntil(null);
            try {
                seatRepository.saveAndFlush(seat);
            } catch (OptimisticLockingFailureException e) {
                throw new SeatUnavailableException(
                        "Seat " + seat.getSeatNumber() + " was modified concurrently. Please retry."
                );
            }
        }
    }

    @Transactional
    public void releaseSeats(List<Long> seatIds) {
        for (Long seatId : seatIds) {
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new ResourceNotFoundException("Seat not found: " + seatId));
            seat.setStatus(Seat.SeatStatus.AVAILABLE);
            seat.setLockedUntil(null);
            try {
                seatRepository.saveAndFlush(seat);
            } catch (OptimisticLockingFailureException e) {
                // If someone else modified the seat concurrently, just log and continue
                // The seat will be in whatever state the other transaction left it
                throw new SeatUnavailableException(
                        "Seat " + seat.getSeatNumber() + " was modified concurrently. Please retry."
                );
            }
        }
    }
}
