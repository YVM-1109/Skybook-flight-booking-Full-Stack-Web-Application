package com.flightbooking.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PassengerRequest {
    @NotNull  private Long seatId;
    @NotBlank private String fullName;
    @NotNull @Positive private Integer age;
    private String passportNo;
}
