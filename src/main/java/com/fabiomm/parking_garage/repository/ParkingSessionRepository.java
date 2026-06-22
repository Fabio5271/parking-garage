package com.fabiomm.parking_garage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fabiomm.parking_garage.entity.ParkingSession;

import java.time.LocalDate;
import java.util.Optional;


public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {
    Optional<ParkingSession> findByLicensePlateAndExitTimeIsNull(String licensePlate);

    Integer countByLicensePlateIsNotNullAndExitTimeIsNullAndSpotIsNull();

    @Query("SELECT SUM(p.chargedAmount) FROM ParkingSession p WHERE p.sector = :sector AND DATE(p.exitTime) = :date")
    Double sumRevenueBySectorAndDate(@Param("sector") String sector, @Param("date") LocalDate date);
}
