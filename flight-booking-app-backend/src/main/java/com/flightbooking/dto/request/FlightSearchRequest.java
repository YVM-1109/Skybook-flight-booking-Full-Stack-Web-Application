package com.flightbooking.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class FlightSearchRequest {
    @NotBlank private String originCode;
    @NotBlank private String destinationCode;
    @NotNull  private LocalDate travelDate;
    @Positive private int passengers = 1;
    private String seatClass = "ECONOMY";
}
