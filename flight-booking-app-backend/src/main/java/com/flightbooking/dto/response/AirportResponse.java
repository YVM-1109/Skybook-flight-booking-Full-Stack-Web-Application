package com.flightbooking.dto.response;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AirportResponse {
    private Long id;
    private String code;
    private String name;
    private String city;
    private String country;
}
