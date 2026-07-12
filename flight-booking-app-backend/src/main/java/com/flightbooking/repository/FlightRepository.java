package com.flightbooking.repository;

import com.flightbooking.entity.Airport;
import com.flightbooking.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    @Query("SELECT f FROM Flight f WHERE " +
           "f.originAirport = :origin AND " +
           "f.destinationAirport = :destination AND " +
           "f.departureTime BETWEEN :startOfDay AND :endOfDay AND " +
           "f.status = 'SCHEDULED'")
    List<Flight> searchFlights(
        @Param("origin") Airport origin,
        @Param("destination") Airport destination,
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay
    );

    List<Flight> findByStatus(Flight.FlightStatus status);
}
