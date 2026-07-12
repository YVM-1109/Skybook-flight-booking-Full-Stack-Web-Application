package com.flightbooking.dto.response;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AircraftResponse {
    private Long id;
    private String model;
    private Integer totalSeats;
    private Integer economySeats;
    private Integer businessSeats;
}
