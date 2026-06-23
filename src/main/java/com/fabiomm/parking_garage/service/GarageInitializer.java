package com.fabiomm.parking_garage.service;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fabiomm.parking_garage.dto.GarageConfig;
import com.fabiomm.parking_garage.entity.ParkingSpot;
import com.fabiomm.parking_garage.entity.Sector;
import com.fabiomm.parking_garage.repository.ParkingSpotRepository;
import com.fabiomm.parking_garage.repository.SectorRepository;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Component
public class GarageInitializer {
    private final SectorRepository sectorRepository;
    private final ParkingSpotRepository spotRepository;
    private final RestTemplate restTemplate;

    private static final Logger log = LoggerFactory.getLogger(GarageInitializer.class);

    @EventListener(ApplicationReadyEvent.class)
    public void initializeGarage() {
        try {
            GarageConfig config = restTemplate.getForObject("http://localhost:3000/garage", GarageConfig.class);
            if (config != null) {
                for (var sectorConfig : config.getGarage()) {
                    Sector sector = sectorRepository.findBySector(sectorConfig.getSector()).orElse(null);
                    if (sector == null) {
                        sector = new Sector();
                        sector.setSector(sectorConfig.getSector());
                    }
                    sector.setBasePrice(BigDecimal.valueOf(sectorConfig.getBasePrice()));
                    sector.setCapacity(sectorConfig.getCapacity());
                    sectorRepository.save(sector);
                }
                for (var spotConfig : config.getSpots()) {
                    ParkingSpot spot = spotRepository.findById(spotConfig.getId()).orElse(null);
                    if (spot == null){
                        spot = new ParkingSpot();
                        spot.setId(spotConfig.getId());
                    }
                    spot.setSector(spotConfig.getSector());
                    spot.setLat(spotConfig.getLat());
                    spot.setLng(spotConfig.getLng());
                    spotRepository.save(spot);
                }
            }
        } catch (Exception e) {
            log.error("Failed to get garage configuration: " + e.getMessage());
            log.warn("Proceeding using previous database data (may be empty)");
        }
    }
}
