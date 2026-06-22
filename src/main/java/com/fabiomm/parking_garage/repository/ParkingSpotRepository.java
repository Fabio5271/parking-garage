package com.fabiomm.parking_garage.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fabiomm.parking_garage.entity.ParkingSpot;

public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {
    @Query("SELECT s FROM ParkingSpot s WHERE ABS(s.lat - :lat) < 0.000005 AND ABS(s.lng - :lng) < 0.000005")
    Optional<ParkingSpot> findByCoordinates(@Param("lat") double lat, @Param("lng") double lng);
}
