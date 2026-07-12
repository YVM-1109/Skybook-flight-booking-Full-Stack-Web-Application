package com.flightbooking.controller;

import com.flightbooking.dto.request.FlightSearchRequest;
import com.flightbooking.dto.response.ApiResponse;
import com.flightbooking.dto.response.FlightResponse;
import com.flightbooking.dto.response.SeatResponse;
import com.flightbooking.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    @GetMapping("/search")
    public ApiResponse<List<FlightResponse>> search(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") int passengers,
            @RequestParam(defaultValue = "ECONOMY") String seatClass
    ) {
        FlightSearchRequest request = new FlightSearchRequest();
        request.setOriginCode(origin);
        request.setDestinationCode(destination);
        request.setTravelDate(date);
        request.setPassengers(passengers);
        request.setSeatClass(seatClass);

        List<FlightResponse> results = flightService.searchFlights(request);
        return ApiResponse.success(results, "Flights retrieved successfully");
    }

    @GetMapping("/{id}")
    public ApiResponse<FlightResponse> getFlight(@PathVariable Long id) {
        return ApiResponse.success(flightService.getFlightById(id), "Flight retrieved");
    }

    @GetMapping("/{id}/seats")
    public ApiResponse<List<SeatResponse>> getSeatMap(@PathVariable Long id) {
        return ApiResponse.success(flightService.getSeatMap(id), "Seat map retrieved");
    }
}
