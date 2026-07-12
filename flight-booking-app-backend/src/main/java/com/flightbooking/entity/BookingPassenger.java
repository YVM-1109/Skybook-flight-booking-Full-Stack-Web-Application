package com.flightbooking.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "booking_passengers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingPassenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Booking booking;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private Integer age;

    private String passportNo;
}
