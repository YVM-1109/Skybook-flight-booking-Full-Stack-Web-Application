package com.flightbooking.dto.response;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AirlineResponse {
    private Long id;
    private String name;
    private String code;
    private String logoUrl;
    private String country;
}
