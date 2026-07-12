package com.flightbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FlightBookingApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlightBookingApiApplication.class, args);
    }
}
