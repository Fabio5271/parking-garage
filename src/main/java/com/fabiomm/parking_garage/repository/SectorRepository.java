package com.fabiomm.parking_garage.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fabiomm.parking_garage.entity.Sector;
import java.util.Optional;


public interface SectorRepository extends JpaRepository<Sector, Long> {
    Optional<Sector> findBySector(String sector);
}
