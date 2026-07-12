package com.flightbooking.repository;

import com.flightbooking.entity.BookingPassenger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingPassengerRepository extends JpaRepository<BookingPassenger, Long> {}
