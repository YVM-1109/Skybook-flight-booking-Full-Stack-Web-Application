package com.flightbooking.controller;

import com.flightbooking.dto.response.AircraftResponse;
import com.flightbooking.dto.response.AirlineResponse;
import com.flightbooking.dto.response.AirportResponse;
import com.flightbooking.dto.response.ApiResponse;
import com.flightbooking.repository.AircraftRepository;
import com.flightbooking.repository.AirlineRepository;
import com.flightbooking.repository.AirportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Read-only public lookup data. Needed by the frontend to populate airport
// pickers (search form) and the admin "add flight" form (airline/aircraft
// dropdowns), and to render airline names/logos on flight cards without the
// frontend having to hardcode them.
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReferenceDataController {

    private final AirportRepository airportRepository;
    private final AirlineRepository airlineRepository;
    private final AircraftRepository aircraftRepository;

    @GetMapping("/airports")
    public ApiResponse<List<AirportResponse>> listAirports() {
        List<AirportResponse> airports = airportRepository.findAll().stream()
                .map(a -> AirportResponse.builder()
                        .id(a.getId())
                        .code(a.getCode())
                        .name(a.getName())
                        .city(a.getCity())
                        .country(a.getCountry())
                        .build())
                .toList();
        return ApiResponse.success(airports, "Airports retrieved");
    }

    @GetMapping("/airlines")
    public ApiResponse<List<AirlineResponse>> listAirlines() {
        List<AirlineResponse> airlines = airlineRepository.findAll().stream()
                .map(a -> AirlineResponse.builder()
                        .id(a.getId())
                        .name(a.getName())
                        .code(a.getCode())
                        .logoUrl(a.getLogoUrl())
                        .country(a.getCountry())
                        .build())
                .toList();
        return ApiResponse.success(airlines, "Airlines retrieved");
    }

    @GetMapping("/aircraft")
    public ApiResponse<List<AircraftResponse>> listAircraft() {
        List<AircraftResponse> aircraft = aircraftRepository.findAll().stream()
                .map(a -> AircraftResponse.builder()
                        .id(a.getId())
                        .model(a.getModel())
                        .totalSeats(a.getTotalSeats())
                        .economySeats(a.getEconomySeats())
                        .businessSeats(a.getBusinessSeats())
                        .build())
                .toList();
        return ApiResponse.success(aircraft, "Aircraft retrieved");
    }
}
