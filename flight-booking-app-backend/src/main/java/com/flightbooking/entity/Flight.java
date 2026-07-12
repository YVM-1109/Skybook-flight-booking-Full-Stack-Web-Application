package com.flightbooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "flights")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String flightNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "origin_airport_id", nullable = false)
    private Airport originAirport;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dest_airport_id", nullable = false)
    private Airport destinationAirport;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "aircraft_id", nullable = false)
    private Aircraft aircraft;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "airline_id")
    private Airline airline;

    @Column(nullable = false)
    private LocalDateTime departureTime;

    @Column(nullable = false)
    private LocalDateTime arrivalTime;

    @Column
    private LocalDateTime boardingTime;

    @Column(length = 20)
    private String terminal;

    @Column(length = 20)
    private String gate;

    @Column
    private Integer durationMinutes;

    @Column
    @Builder.Default
    private Integer stops = 0;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePriceEconomy;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePriceBusiness;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FlightStatus status = FlightStatus.SCHEDULED;

    public enum FlightStatus {
        SCHEDULED, DELAYED, CANCELLED, COMPLETED
    }
}
