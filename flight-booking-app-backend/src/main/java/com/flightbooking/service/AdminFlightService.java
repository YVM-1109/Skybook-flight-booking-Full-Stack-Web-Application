package com.flightbooking.service;

import com.flightbooking.entity.Airline;
import com.flightbooking.repository.AirlineRepository;
import java.time.Duration;
import com.flightbooking.dto.request.AddFlightRequest;
import com.flightbooking.dto.response.FlightResponse;
import com.flightbooking.entity.Aircraft;
import com.flightbooking.entity.Airport;
import com.flightbooking.entity.Flight;
import com.flightbooking.entity.Seat;
import com.flightbooking.exception.ResourceNotFoundException;
import com.flightbooking.repository.AircraftRepository;
import com.flightbooking.repository.AirportRepository;
import com.flightbooking.repository.FlightRepository;
import com.flightbooking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminFlightService {

    private final FlightRepository flightRepository;
    private final AirportRepository airportRepository;
    private final AircraftRepository aircraftRepository;
    private final AirlineRepository airlineRepository;
    private final SeatRepository seatRepository;
    private final FlightService flightService;

    @Transactional
    public FlightResponse createFlight(AddFlightRequest req) {
        Airport origin = airportRepository.findByCode(req.getOriginCode().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Origin airport not found"));
        Airport dest = airportRepository.findByCode(req.getDestinationCode().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Destination airport not found"));
        Aircraft aircraft = aircraftRepository.findById(req.getAircraftId())
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft not found"));
        Airline airline = airlineRepository.findById(req.getAirlineId())
                .orElseThrow(() -> new ResourceNotFoundException("Airline not found"));

        if (origin.getId().equals(dest.getId())) {
            throw new IllegalArgumentException("Origin and destination airports must be different");
        }

        if (!req.getBoardingTime().isBefore(req.getDepartureTime())) {
            throw new IllegalArgumentException(
                    "Boarding time must be before departure time");
        }

        // Arrival must be strictly after departure — a flight that "arrives"
        // before or at the moment it departs is a data-entry error, not a
        // valid schedule. (@Future on departureTime already guarantees the
        // flight itself isn't being created in the past.)
        if (!req.getArrivalTime().isAfter(req.getDepartureTime())) {
            throw new IllegalArgumentException(
                    "Arrival time must be after departure time");
        }

        int durationMinutes = (int) Duration.between(req.getDepartureTime(), req.getArrivalTime()).toMinutes();

        Flight flight = Flight.builder()
                .flightNumber(req.getFlightNumber())
                .originAirport(origin)
                .destinationAirport(dest)
                .aircraft(aircraft)
                .airline(airline)
                .departureTime(req.getDepartureTime())
                .arrivalTime(req.getArrivalTime())
                .boardingTime(req.getBoardingTime())
                .terminal(req.getTerminal())
                .gate(req.getGate())
                .durationMinutes(durationMinutes)
                .stops(req.getStops())
                .basePriceEconomy(req.getBasePriceEconomy())
                .basePriceBusiness(req.getBasePriceBusiness())
                .status(Flight.FlightStatus.SCHEDULED)
                .build();

        flight = flightRepository.save(flight);
        generateSeatsForFlight(flight, aircraft);

        return flightService.getFlightById(flight.getId());
    }

    @Transactional
    public FlightResponse updateFlightStatus(Long id, String status) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));
        flight.setStatus(Flight.FlightStatus.valueOf(status.toUpperCase()));
        flightRepository.save(flight);
        return flightService.getFlightById(id);
    }

    private void generateSeatsForFlight(Flight flight, Aircraft aircraft) {
        int economyRows = aircraft.getEconomySeats() / 6;
        int businessRows = aircraft.getBusinessSeats() / 4;
        char[] economyLetters = {'A', 'B', 'C', 'D', 'E', 'F'};
        char[] businessLetters = {'A', 'B', 'C', 'D'};

        for (int row = 1; row <= businessRows; row++) {
            for (char letter : businessLetters) {
                Seat seat = Seat.builder()
                        .flight(flight)
                        .seatNumber(row + String.valueOf(letter))
                        .seatClass(Seat.SeatClass.BUSINESS)
                        .status(Seat.SeatStatus.AVAILABLE)
                        .build();
                seatRepository.save(seat);
            }
        }
        for (int row = businessRows + 1; row <= businessRows + economyRows; row++) {
            for (char letter : economyLetters) {
                Seat seat = Seat.builder()
                        .flight(flight)
                        .seatNumber(row + String.valueOf(letter))
                        .seatClass(Seat.SeatClass.ECONOMY)
                        .status(Seat.SeatStatus.AVAILABLE)
                        .build();
                seatRepository.save(seat);
            }
        }
    }
}
