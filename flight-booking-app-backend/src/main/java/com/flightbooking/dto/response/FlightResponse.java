package com.flightbooking.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightResponse {

    private Long id;

    private String flightNumber;

    // Airline
    private Long airlineId;
    private String airlineName;
    private String airlineCode;
    private String airlineLogo;

    // Airports
    private String originCode;
    private String originCity;

    private String destinationCode;
    private String destinationCity;

    // Schedule
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private LocalDateTime boardingTime;

    // Airport Information
    private String terminal;
    private String gate;

    // Flight Details
    private Integer durationMinutes;
    private Integer stops;

    // Prices
    private BigDecimal basePriceEconomy;
    private BigDecimal basePriceBusiness;

    // Status
    private String status;

    // Aircraft
    private String aircraftModel;

    // Seat Availability
    private long availableEconomySeats;
    private long availableBusinessSeats;
}