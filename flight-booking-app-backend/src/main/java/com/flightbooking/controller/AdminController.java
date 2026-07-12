package com.flightbooking.controller;

import com.flightbooking.dto.request.AddFlightRequest;
import com.flightbooking.dto.response.ApiResponse;
import com.flightbooking.dto.response.BookingResponse;
import com.flightbooking.dto.response.FlightResponse;
import com.flightbooking.service.AdminFlightService;
import com.flightbooking.service.BookingService;
import com.flightbooking.service.FlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminFlightService adminFlightService;
    private final BookingService bookingService;
    private final FlightService flightService;

    @PostMapping("/flights")
    public ApiResponse<FlightResponse> addFlight(@Valid @RequestBody AddFlightRequest req) {
        FlightResponse flight = adminFlightService.createFlight(req);
        return ApiResponse.success(flight, "Flight created with seat map generated");
    }

    // No such listing endpoint existed anywhere (public search requires
    // origin/destination/date). Without this, PATCH /flights/{id}/status
    // has no way to be used, since the admin has no way to discover ids.
    @GetMapping("/flights")
    public ApiResponse<List<FlightResponse>> allFlights() {
        return ApiResponse.success(flightService.getAllFlights(), "All flights");
    }

    @GetMapping("/bookings")
    public ApiResponse<List<BookingResponse>> allBookings() {
        return ApiResponse.success(bookingService.getAllBookings(), "All bookings");
    }

    @PatchMapping("/flights/{id}/status")
    public ApiResponse<FlightResponse> updateStatus(@PathVariable Long id, @RequestParam String status) {
        FlightResponse flight = adminFlightService.updateFlightStatus(id, status);
        return ApiResponse.success(flight, "Flight status updated");
    }
}
