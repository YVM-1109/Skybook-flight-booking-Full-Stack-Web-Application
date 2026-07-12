package com.flightbooking.service;

import com.flightbooking.dto.request.FlightSearchRequest;
import com.flightbooking.dto.response.FlightResponse;
import com.flightbooking.dto.response.SeatResponse;
import com.flightbooking.entity.Airport;
import com.flightbooking.entity.Flight;
import com.flightbooking.entity.Seat;
import com.flightbooking.exception.ResourceNotFoundException;
import com.flightbooking.repository.AirportRepository;
import com.flightbooking.repository.FlightRepository;
import com.flightbooking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightRepository flightRepository;
    private final AirportRepository airportRepository;
    private final SeatRepository seatRepository;

    @Transactional(readOnly = true)
    public List<FlightResponse> searchFlights(FlightSearchRequest request) {
        Airport origin = airportRepository.findByCode(request.getOriginCode().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Origin airport not found: " + request.getOriginCode()));

        Airport destination = airportRepository.findByCode(request.getDestinationCode().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Destination airport not found: " + request.getDestinationCode()));

        LocalDateTime startOfDay = request.getTravelDate().atStartOfDay();
        LocalDateTime endOfDay = request.getTravelDate().atTime(23, 59, 59);

        List<Flight> flights = flightRepository.searchFlights(origin, destination, startOfDay, endOfDay);

        return flights.stream()
                .map(this::toFlightResponse)
                .filter(f -> hasEnoughSeats(f, request))
                .toList();
    }

    private boolean hasEnoughSeats(FlightResponse flight, FlightSearchRequest request) {
        String seatClass = request.getSeatClass() != null
                ? request.getSeatClass().toUpperCase()
                : "ECONOMY";
        int needed = request.getPassengers();

        return switch (seatClass) {
            case "BUSINESS" -> flight.getAvailableBusinessSeats() >= needed;
            case "ECONOMY" -> flight.getAvailableEconomySeats() >= needed;
            default -> flight.getAvailableEconomySeats() >= needed;
        };
    }

    @Transactional(readOnly = true)
    public FlightResponse getFlightById(Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));
        return toFlightResponse(flight);
    }

    @Transactional(readOnly = true)
    public List<FlightResponse> getAllFlights() {
        return flightRepository.findAll().stream()
                .map(this::toFlightResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatMap(Long flightId) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found"));

        List<Seat> seats = seatRepository.findByFlight(flight);

        return seats.stream()
                .map(seat -> SeatResponse.builder()
                        .id(seat.getId())
                        .seatNumber(seat.getSeatNumber())
                        .seatClass(seat.getSeatClass().name())
                        .status(seat.getStatus().name())
                        .build())
                .toList();
    }

    private FlightResponse toFlightResponse(Flight flight) {

        long availableEconomy =
                seatRepository.countAvailableSeats(flight, Seat.SeatClass.ECONOMY);
    
        long availableBusiness =
                seatRepository.countAvailableSeats(flight, Seat.SeatClass.BUSINESS);
    
        return FlightResponse.builder()
                .id(flight.getId())
    
                .flightNumber(flight.getFlightNumber())
    
                // Airline
                .airlineId(flight.getAirline() != null ? flight.getAirline().getId() : null)
                .airlineName(flight.getAirline() != null ? flight.getAirline().getName() : null)
                .airlineCode(flight.getAirline() != null ? flight.getAirline().getCode() : null)
                .airlineLogo(flight.getAirline() != null ? flight.getAirline().getLogoUrl() : null)
                // Airports
                .originCode(flight.getOriginAirport().getCode())
                .originCity(flight.getOriginAirport().getCity())
    
                .destinationCode(flight.getDestinationAirport().getCode())
                .destinationCity(flight.getDestinationAirport().getCity())
    
                // Schedule
                .departureTime(flight.getDepartureTime())
                .arrivalTime(flight.getArrivalTime())
                .boardingTime(flight.getBoardingTime())
    
                // Airport Information
                .terminal(flight.getTerminal())
                .gate(flight.getGate())
    
                // Flight Information
                .durationMinutes(flight.getDurationMinutes())
                .stops(flight.getStops())
    
                // Pricing
                .basePriceEconomy(flight.getBasePriceEconomy())
                .basePriceBusiness(flight.getBasePriceBusiness())
    
                // Status
                .status(flight.getStatus().name())
    
                // Aircraft
                .aircraftModel(flight.getAircraft().getModel())
    
                // Seats
                .availableEconomySeats(availableEconomy)
                .availableBusinessSeats(availableBusiness)
    
                .build();
    }
}
