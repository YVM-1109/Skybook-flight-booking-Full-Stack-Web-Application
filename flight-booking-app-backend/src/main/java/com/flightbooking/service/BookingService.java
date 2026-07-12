package com.flightbooking.service;

import com.flightbooking.dto.request.BookingRequest;
import com.flightbooking.dto.request.PassengerRequest;
import com.flightbooking.dto.response.BookingResponse;
import com.flightbooking.entity.*;
import com.flightbooking.exception.ResourceNotFoundException;
import com.flightbooking.repository.BookingRepository;
import com.flightbooking.repository.FlightRepository;
import com.flightbooking.repository.PaymentRepository;
import com.flightbooking.repository.SeatRepository;
import com.flightbooking.repository.UserRepository;
import com.flightbooking.util.BookingRefGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final SeatLockingService seatLockingService;
    private final PaymentRepository paymentRepository;

    // STEP 1 of the booking flow: lock seats and create a PENDING booking.
    // The booking only becomes CONFIRMED after payment succeeds (see PaymentService).
    @Transactional
    public BookingResponse initiateBooking(Long userId, BookingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));

        List<Long> seatIds = request.getPassengers().stream()
                .map(PassengerRequest::getSeatId)
                .toList();

        // This throws SeatUnavailableException if any seat is taken —
        // the whole transaction rolls back, no partial locking happens
        seatLockingService.lockSeats(seatIds);

        BigDecimal totalAmount = calculateTotalAmount(flight, request.getPassengers().size(), seatIds);

        Booking booking = Booking.builder()
                .bookingRef(generateUniqueBookingRef())
                .user(user)
                .flight(flight)
                .totalAmount(totalAmount)
                .status(Booking.BookingStatus.PENDING)
                .build();

        booking = bookingRepository.save(booking);

        for (PassengerRequest passengerReq : request.getPassengers()) {
            Seat seat = seatRepository.findById(passengerReq.getSeatId())
                    .orElseThrow(() -> new ResourceNotFoundException("Seat not found"));

            BookingPassenger passenger = BookingPassenger.builder()
                    .booking(booking)
                    .seat(seat)
                    .fullName(passengerReq.getFullName())
                    .age(passengerReq.getAge())
                    .passportNo(passengerReq.getPassportNo())
                    .build();

            booking.getPassengers().add(passenger);
        }

        booking = bookingRepository.save(booking);

        return toBookingResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAllByOrderByBookedAtDesc().stream()
                .map(this::toBookingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return bookingRepository.findByUserOrderByBookedAtDesc(user).stream()
                .map(this::toBookingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingByRef(String bookingRef, Long requestingUserId, boolean isAdmin) {
        Booking booking = bookingRepository.findByBookingRef(bookingRef)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        // IDOR protection: a passenger can only view their own bookings
        if (!isAdmin && !booking.getUser().getId().equals(requestingUserId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have access to this booking"
            );
        }

        return toBookingResponse(booking);
    }

    @Transactional
    public void cancelBooking(String bookingRef, Long requestingUserId, boolean isAdmin) {
        Booking booking = bookingRepository.findByBookingRef(bookingRef)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!isAdmin && !booking.getUser().getId().equals(requestingUserId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have access to this booking"
            );
        }

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }

        // If booking was CONFIRMED (paid), we should also cancel the payment/refund
        if (booking.getStatus() == Booking.BookingStatus.CONFIRMED) {
            paymentRepository.findByBooking(booking).ifPresent(payment -> {
                payment.setStatus(Payment.PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
            });
        }

        List<Long> seatIds = booking.getPassengers().stream()
                .map(p -> p.getSeat().getId())
                .toList();

        seatLockingService.releaseSeats(seatIds);

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);
    }

    private BigDecimal calculateTotalAmount(Flight flight, int passengerCount, List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            return flight.getBasePriceEconomy().multiply(BigDecimal.valueOf(passengerCount));
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Long seatId : seatIds) {
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new ResourceNotFoundException("Seat not found: " + seatId));
            if (seat.getSeatClass() == Seat.SeatClass.BUSINESS) {
                total = total.add(flight.getBasePriceBusiness());
            } else {
                total = total.add(flight.getBasePriceEconomy());
            }
        }
        return total;
    }

    private String generateUniqueBookingRef() {
        int maxRetries = 100;
        String ref;
        int attempts = 0;
        do {
            ref = BookingRefGenerator.generate();
            attempts++;
            if (attempts >= maxRetries) {
                throw new IllegalStateException("Failed to generate unique booking reference after " + maxRetries + " attempts");
            }
        } while (bookingRepository.findByBookingRef(ref).isPresent());
        return ref;
    }

    private BookingResponse toBookingResponse(Booking booking) {
        List<BookingResponse.PassengerResponse> passengers = booking.getPassengers().stream()
                .map(p -> BookingResponse.PassengerResponse.builder()
                        .fullName(p.getFullName())
                        .age(p.getAge())
                        .seatNumber(p.getSeat().getSeatNumber())
                        .seatClass(p.getSeat().getSeatClass().name())
                        .build())
                .toList();

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingRef(booking.getBookingRef())
                .flightNumber(booking.getFlight().getFlightNumber())
                .originCity(booking.getFlight().getOriginAirport().getCity())
                .destinationCity(booking.getFlight().getDestinationAirport().getCity())
                .departureTime(booking.getFlight().getDepartureTime())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus().name())
                .bookedAt(booking.getBookedAt())
                .passengers(passengers)
                .build();
    }
}
