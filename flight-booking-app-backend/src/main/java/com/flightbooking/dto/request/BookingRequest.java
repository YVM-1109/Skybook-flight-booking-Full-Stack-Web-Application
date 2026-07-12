package com.flightbooking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class BookingRequest {
    @NotNull private Long flightId;
    @Valid @NotNull @Size(min = 1, max = 6, message = "Booking limited to 6 passengers per request")
    private List<PassengerRequest> passengers;
}
