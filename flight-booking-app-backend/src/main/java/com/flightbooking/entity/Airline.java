package com.flightbooking.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "airline")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Airline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 3)
    private String code;

    @Column
    private String logoUrl;

    @Column
    private String country;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
}