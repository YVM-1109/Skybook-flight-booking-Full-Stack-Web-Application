package com.flightbooking.dto.response;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SeatResponse {
    private Long id;
    private String seatNumber;
    private String seatClass;
    private String status;
}
