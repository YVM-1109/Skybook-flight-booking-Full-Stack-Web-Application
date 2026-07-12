package com.flightbooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "seats",
    uniqueConstraints = @UniqueConstraint(columnNames = {"flight_id", "seat_number"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(nullable = false, length = 5)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatClass seatClass;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SeatStatus status = SeatStatus.AVAILABLE;

    private LocalDateTime lockedUntil;

    // This is the optimistic locking field.
    // Every time a seat is updated, this number increments.
    // If two users try to update the same seat simultaneously,
    // the second one will fail with OptimisticLockException
    // because the version number will have changed.
    @Version
    private Long version;

    public enum SeatClass {
        ECONOMY, BUSINESS
    }

    public enum SeatStatus {
        AVAILABLE, LOCKED, BOOKED
    }
}
