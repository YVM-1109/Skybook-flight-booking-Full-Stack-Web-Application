package com.flightbooking.service;

import com.flightbooking.dto.request.FlightSearchRequest;
import com.flightbooking.dto.response.FlightResponse;
import com.flightbooking.entity.Aircraft;
import com.flightbooking.entity.Airport;
import com.flightbooking.entity.Flight;
import com.flightbooking.repository.AirportRepository;
import com.flightbooking.repository.FlightRepository;
import com.flightbooking.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlightServiceTest {

    @Mock
    FlightRepository flightRepository;

    @Mock
    AirportRepository airportRepository;

    @Mock
    SeatRepository seatRepository;

    @InjectMocks
    FlightService flightService;

    private Airport del;
    private Airport hyd;
    private Flight flight;

    @BeforeEach
    void setUp() {
        del = Airport.builder().code("DEL").city("Delhi").build();
        hyd = Airport.builder().code("HYD").city("Hyderabad").build();
        Aircraft aircraft = Aircraft.builder().model("Boeing 737").build();
        flight = Flight.builder()
                .id(1L)
                .flightNumber("AI101")
                .originAirport(del)
                .destinationAirport(hyd)
                .aircraft(aircraft)
                .departureTime(LocalDateTime.of(2026, 8, 15, 10, 0))
                .arrivalTime(LocalDateTime.of(2026, 8, 15, 12, 30))
                .basePriceEconomy(BigDecimal.valueOf(5000))
                .basePriceBusiness(BigDecimal.valueOf(12000))
                .status(Flight.FlightStatus.SCHEDULED)
                .build();
    }

    @Test
    void searchFlights_filtersOutFlightsWithoutEnoughEconomySeats() {
        FlightSearchRequest request = new FlightSearchRequest();
        request.setOriginCode("DEL");
        request.setDestinationCode("HYD");
        request.setTravelDate(LocalDate.of(2026, 8, 15));
        request.setPassengers(3);
        request.setSeatClass("ECONOMY");

        when(airportRepository.findByCode("DEL")).thenReturn(Optional.of(del));
        when(airportRepository.findByCode("HYD")).thenReturn(Optional.of(hyd));
        when(flightRepository.searchFlights(eq(del), eq(hyd), any(), any()))
                .thenReturn(List.of(flight));
        when(seatRepository.countAvailableSeats(flight, com.flightbooking.entity.Seat.SeatClass.ECONOMY))
                .thenReturn(2L);
        when(seatRepository.countAvailableSeats(flight, com.flightbooking.entity.Seat.SeatClass.BUSINESS))
                .thenReturn(10L);

        List<FlightResponse> results = flightService.searchFlights(request);

        assertEquals(0, results.size());
    }

    @Test
    void searchFlights_includesFlightsWithEnoughBusinessSeats() {
        FlightSearchRequest request = new FlightSearchRequest();
        request.setOriginCode("DEL");
        request.setDestinationCode("HYD");
        request.setTravelDate(LocalDate.of(2026, 8, 15));
        request.setPassengers(2);
        request.setSeatClass("BUSINESS");

        when(airportRepository.findByCode("DEL")).thenReturn(Optional.of(del));
        when(airportRepository.findByCode("HYD")).thenReturn(Optional.of(hyd));
        when(flightRepository.searchFlights(eq(del), eq(hyd), any(), any()))
                .thenReturn(List.of(flight));
        when(seatRepository.countAvailableSeats(flight, com.flightbooking.entity.Seat.SeatClass.ECONOMY))
                .thenReturn(50L);
        when(seatRepository.countAvailableSeats(flight, com.flightbooking.entity.Seat.SeatClass.BUSINESS))
                .thenReturn(4L);

        List<FlightResponse> results = flightService.searchFlights(request);

        assertEquals(1, results.size());
        assertEquals("AI101", results.get(0).getFlightNumber());
    }
}
