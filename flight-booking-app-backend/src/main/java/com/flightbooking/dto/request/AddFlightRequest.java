package com.flightbooking.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AddFlightRequest {

    @NotBlank
    private String flightNumber;

    @NotBlank
    @Size(min = 3, max = 3)
    private String originCode;

    @NotBlank
    @Size(min = 3, max = 3)
    private String destinationCode;

    @NotNull
    private Long aircraftId;

    @NotNull
    private Long airlineId;

    @NotNull
    @Future
    private LocalDateTime departureTime;

    @NotNull
    private LocalDateTime arrivalTime;

    @NotNull
    private LocalDateTime boardingTime;

    @NotBlank
    private String terminal;

    @NotBlank
    private String gate;

    private Integer stops = 0;

    @NotNull
    @Positive
    private BigDecimal basePriceEconomy;

    @NotNull
    @Positive
    private BigDecimal basePriceBusiness;
}
