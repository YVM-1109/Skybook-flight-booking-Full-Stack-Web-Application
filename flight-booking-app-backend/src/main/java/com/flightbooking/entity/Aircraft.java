package com.flightbooking.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "aircraft")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Aircraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private Integer totalSeats;

    @Column(nullable = false)
    private Integer economySeats;

    @Column(nullable = false)
    private Integer businessSeats;
}
