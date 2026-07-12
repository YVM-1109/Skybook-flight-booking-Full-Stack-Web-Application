package com.flightbooking.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BookingResponse {
    private Long id;
    private String bookingRef;
    private String flightNumber;
    private String originCity;
    private String destinationCity;
    private LocalDateTime departureTime;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime bookedAt;
    private List<PassengerResponse> passengers;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PassengerResponse {
        private String fullName;
        private Integer age;
        private String seatNumber;
        private String seatClass;
    }
}
