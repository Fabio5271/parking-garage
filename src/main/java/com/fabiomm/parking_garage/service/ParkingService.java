package com.fabiomm.parking_garage.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fabiomm.parking_garage.dto.Event;
import com.fabiomm.parking_garage.dto.RevenueRequest;
import com.fabiomm.parking_garage.dto.RevenueResponse;
import com.fabiomm.parking_garage.entity.ParkingSession;
import com.fabiomm.parking_garage.entity.ParkingSpot;
import com.fabiomm.parking_garage.entity.Sector;
import com.fabiomm.parking_garage.exception.InvalidClientDataException;
import com.fabiomm.parking_garage.exception.MissingDataException;
import com.fabiomm.parking_garage.exception.NotFoundException;
import com.fabiomm.parking_garage.exception.OccupancyException;
import com.fabiomm.parking_garage.repository.ParkingSessionRepository;
import com.fabiomm.parking_garage.repository.ParkingSpotRepository;
import com.fabiomm.parking_garage.repository.SectorRepository;

import lombok.AllArgsConstructor;

@AllArgsConstructor
@Service
public class ParkingService {
    private final ParkingSessionRepository sessionRepository;
    private final ParkingSpotRepository spotRepository;
    private final SectorRepository sectorRepository;

    private static final Logger log = LoggerFactory.getLogger(ParkingService.class);

    @Transactional
    public void handleEntry(Event event) {
        log.info("Recieved ENTRY event | plate: {}", event.getLicensePlate());

        List<Sector> freeSectors = sectorRepository.findAll().stream()
                .filter(s -> s.getOccupiedCount() < s.getCapacity())
                .toList();

        if (freeSectors.isEmpty()) {
            throw new OccupancyException("Parking full. No spots available in any sectors.");
        }

        int inButNotParked = sessionRepository.countByLicensePlateIsNotNullAndExitTimeIsNullAndSpotIsNull();
        int freeSpots = 0;
        for (Sector sector : freeSectors) {
            freeSpots += sector.getCapacity() - sector.getOccupiedCount();
        }
        if (freeSpots <= inButNotParked) {
            throw new OccupancyException("Parking full. Please wait until another user leaves the garage.");
        }

        ParkingSession session = new ParkingSession();
        session.setLicensePlate(event.getLicensePlate());
        session.setEntryTime(event.getEntryTime());
        sessionRepository.save(session);
    }

    @Transactional
    public void handleParked(Event event) {
        log.info("Recieved PARKED event | plate: {} | lat: {} | lng: {}", event.getLicensePlate(), event.getLat(),
                event.getLng());

        ParkingSpot spot = spotRepository.findByCoordinates(event.getLat(), event.getLng())
                .orElseThrow(() -> new NotFoundException("Unable to park: Parking spot not found on location"));

        if (spot.isOccupied()) {
            throw new OccupancyException("Unable to park: Spot is already occupied");
        }

        ParkingSession session = sessionRepository.findByLicensePlateAndExitTimeIsNull(event.getLicensePlate())
                .orElseThrow(() -> new NotFoundException(
                        "Unable to park: No active parking session for license plate " + event.getLicensePlate()));

        String sectorName = spot.getSector();
        Sector sector = sectorRepository.findBySector(sectorName)
                .orElseThrow(() -> new MissingDataException(
                        "Unable to park: Sector '" + sectorName + "' related to this parking spot does not exist"));

        if (sector.getOccupiedCount() >= sector.getCapacity()) {
            throw new OccupancyException("Unable to park: Sector '" + sectorName + "' is already full");
        }

        spot.setOccupied(true);
        spotRepository.save(spot);

        sector.increaseOccupied();
        sectorRepository.save(sector);

        session.setSpot(spot);
        session.setSector(sectorName);
        sessionRepository.save(session);

    }

    @Transactional
    public void handleExit(Event event) {
        log.info("Recieved EXIT event | plate: {} | exit_time: {}", event.getLicensePlate(), event.getExitTime());

        ParkingSession session = sessionRepository
                .findByLicensePlateAndExitTimeIsNull(event.getLicensePlate())
                .orElseThrow(() -> new NotFoundException(
                        "Error exiting garage: No active parking session for license plate '" + event.getLicensePlate() + "'"));

        if (event.getExitTime().isBefore(session.getEntryTime())) {
            throw new InvalidClientDataException("Error exiting garage: Cannot exit at a time/date earlier than the entry time");
        }
        session.setExitTime(event.getExitTime());

        if (session.getSpot() != null) {
            ParkingSpot spot = session.getSpot();
            spot.setOccupied(false);
            spotRepository.save(spot);

            Sector sector = sectorRepository.findBySector(session.getSector())
                    .orElseThrow(() -> new MissingDataException(
                            "Error exiting garage: Sector " + session.getSector() + " not found"));

            session.setChargedAmount(calculatePrice(session)); // Depends on sector

            sector.decreaseOccupied();
            sectorRepository.save(sector);
        }

        sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public RevenueResponse getRevenue(RevenueRequest request) {
        Double amount = sessionRepository.sumRevenueBySectorAndDate(request.getSector(), request.getDate());
        if (amount == null) {
            throw new NotFoundException("No revenue data found for the specified sector and date");
        }
        RevenueResponse response = new RevenueResponse();
        response.setAmount(amount);
        return response;
    }

    private BigDecimal calculatePrice(ParkingSession session) {
        if (session.getEntryTime() == null || session.getExitTime() == null) {
            throw new MissingDataException("Could not calculate price: Missing entry/exit times");
        }

        Duration duration = Duration.between(session.getEntryTime(), session.getExitTime());
        long minutes = duration.toMinutes();

        if (minutes < 30) {
            return BigDecimal.ZERO;
        }

        Sector sector = sectorRepository.findBySector(session.getSector())
                .orElseThrow(() -> new MissingDataException(
                        "Could not calculate price: Sector " + session.getSector() + " not found"));
        double basePrice = sector.getBasePrice().doubleValue();

        double multiplier = getDynamicMultiplier(sector);

        long hours = (long) Math.ceil(minutes / 60.0); // Considera a primeira hora completa no total
        BigDecimal price = BigDecimal.valueOf(hours * basePrice * multiplier);
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private double getDynamicMultiplier(Sector sector) {
        int totalSpots = sector.getCapacity();
        int occupied = sector.getOccupiedCount();
        double occupancy = (double) occupied / totalSpots * 100;

        if (occupancy < 25) {
            return 0.9;
        } else if (occupancy <= 50) {
            return 1.0;
        } else if (occupancy <= 75) {
            return 1.1;
        } else {
            return 1.25;
        }
    };

    // ==================== FOR TESTS ====================

    BigDecimal calculatePriceForTest(ParkingSession session) {
        return calculatePrice(session);
    }

    double getDynamicMultiplierForTest(Sector sector) {
        return getDynamicMultiplier(sector);
    }
}
