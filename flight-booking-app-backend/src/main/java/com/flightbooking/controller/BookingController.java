package com.flightbooking.controller;

import com.flightbooking.dto.request.BookingRequest;
import com.flightbooking.dto.response.ApiResponse;
import com.flightbooking.dto.response.BookingResponse;
import com.flightbooking.entity.User;
import com.flightbooking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/initiate")
    public ApiResponse<BookingResponse> initiate(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BookingRequest request
    ) {
        BookingResponse response = bookingService.initiateBooking(user.getId(), request);
        return ApiResponse.success(response, "Booking initiated — proceed to payment");
    }

    @GetMapping
    public ApiResponse<List<BookingResponse>> myBookings(@AuthenticationPrincipal User user) {
        return ApiResponse.success(bookingService.getUserBookings(user.getId()), "Bookings retrieved");
    }

    @GetMapping("/{ref}")
    public ApiResponse<BookingResponse> getBooking(
            @AuthenticationPrincipal User user,
            @PathVariable String ref
    ) {
        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        return ApiResponse.success(
                bookingService.getBookingByRef(ref, user.getId(), isAdmin),
                "Booking retrieved"
        );
    }

    @DeleteMapping("/{ref}/cancel")
    public ApiResponse<Void> cancelBooking(
            @AuthenticationPrincipal User user,
            @PathVariable String ref
    ) {
        boolean isAdmin = user.getRole() == User.Role.ADMIN;
        bookingService.cancelBooking(ref, user.getId(), isAdmin);
        return ApiResponse.success(null, "Booking cancelled successfully");
    }
}
