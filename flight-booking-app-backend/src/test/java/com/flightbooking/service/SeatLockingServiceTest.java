package com.flightbooking.service;

import com.flightbooking.entity.Seat;
import com.flightbooking.exception.SeatUnavailableException;
import com.flightbooking.repository.SeatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatLockingServiceTest {

    @Mock
    SeatRepository seatRepository;

    @InjectMocks
    SeatLockingService seatLockingService;

    @Test
    void lockSeat_throwsWhenNotAvailable() {
        Seat seat = Seat.builder()
                .id(1L)
                .seatNumber("1A")
                .status(Seat.SeatStatus.LOCKED)
                .build();
        when(seatRepository.findById(1L)).thenReturn(Optional.of(seat));

        assertThrows(SeatUnavailableException.class,
                () -> seatLockingService.lockSeats(List.of(1L)));
    }

    @Test
    void lockSeat_throwsWhenSeatNotFound() {
        when(seatRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(com.flightbooking.exception.ResourceNotFoundException.class,
                () -> seatLockingService.lockSeats(List.of(99L)));
    }
}
