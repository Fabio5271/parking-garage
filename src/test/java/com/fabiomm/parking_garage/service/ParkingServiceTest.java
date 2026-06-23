package com.fabiomm.parking_garage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {
    @Mock
    private ParkingSessionRepository sessionRepository;

    @Mock
    private ParkingSpotRepository spotRepository;

    @Mock
    private SectorRepository sectorRepository;

    @InjectMocks
    private ParkingService parkingService;

    // private ParkingSession sessionAfterEntry;
    private ParkingSpot spot;
    private Sector sectorA;
    private Event entryEvent;
    private Event parkedEvent;
    private Event exitEvent;
    private RevenueRequest revenueRequest;

    @BeforeEach
    void setUp() {
        sectorA = new Sector();
        sectorA.setId(1L);
        sectorA.setSector("A");
        sectorA.setBasePrice(BigDecimal.valueOf(10.0));
        sectorA.setCapacity(100);
        sectorA.setOccupiedCount(40);

        spot = new ParkingSpot();
        spot.setId(1L);
        spot.setSector("A");
        spot.setLat(-23.561684);
        spot.setLng(-46.655981);
        spot.setOccupied(false);

        entryEvent = new Event();
        entryEvent.setLicensePlate("ZUL0001");
        entryEvent.setEventType("ENTRY");
        entryEvent.setEntryTime(Instant.parse("2026-06-20T10:00:00.000Z"));

        parkedEvent = new Event();
        parkedEvent.setLicensePlate("ZUL0001");
        parkedEvent.setEventType("PARKED");
        parkedEvent.setLat(-23.561684);
        parkedEvent.setLng(-46.655981);

        exitEvent = new Event();
        exitEvent.setLicensePlate("ZUL0001");
        exitEvent.setEventType("EXIT");
        exitEvent.setExitTime(Instant.parse("2026-06-20T11:45:00.000Z"));

        revenueRequest = new RevenueRequest();
        revenueRequest.setSector("A");
        revenueRequest.setDate(LocalDate.of(2026, 6, 20));
    }

    // ==================== ENTRY ====================

    @Test
    void shouldCreateSessionOnEntry_WhenThereIsSpace() {
        when(sectorRepository.findAll()).thenReturn(List.of(sectorA));
        when(sessionRepository.countByLicensePlateIsNotNullAndExitTimeIsNullAndSpotIsNull()).thenReturn(0);

        parkingService.handleEntry(entryEvent);

        verify(sessionRepository, times(1)).save(any(ParkingSession.class));
    }

    @Test
    void shouldThrowOccupancyException_WhenNoSpotsAvail() {
        sectorA.setOccupiedCount(100);
        when(sectorRepository.findAll()).thenReturn(List.of(sectorA));

        assertThrows(OccupancyException.class, () -> parkingService.handleEntry(entryEvent));
    }

    @Test
    void shouldThrowOccupancyException_WhenGarageIsFull() {
        sectorA.setOccupiedCount(90);
        when(sectorRepository.findAll()).thenReturn(List.of(sectorA));
        when(sessionRepository.countByLicensePlateIsNotNullAndExitTimeIsNullAndSpotIsNull()).thenReturn(10);

        assertThrows(OccupancyException.class, () -> parkingService.handleEntry(entryEvent));
    }

    // ==================== PARKED ====================

    @Test
    void shouldParkVehicleSuccessfully() {
        when(spotRepository.findByCoordinates(anyDouble(), anyDouble())).thenReturn(Optional.of(spot));
        when(sessionRepository.findByLicensePlateAndExitTimeIsNull(anyString())).thenReturn(Optional.of(new ParkingSession()));
        when(sectorRepository.findBySector("A")).thenReturn(Optional.of(sectorA));

        parkingService.handleParked(parkedEvent);

        assertTrue(spot.isOccupied());
        assertEquals(41, sectorA.getOccupiedCount());

        verify(spotRepository).save(spot);
        verify(sectorRepository).save(sectorA);
    }

    @Test
    void shouldThrowNotFoundException_WhenSpotNotFound() {
        when(spotRepository.findByCoordinates(anyDouble(), anyDouble())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> parkingService.handleParked(parkedEvent));
    }

    @Test
    void shouldThrowOccupancyException_WhenSpotOccupied() {
        spot.setOccupied(true);
        when(spotRepository.findByCoordinates(anyDouble(), anyDouble())).thenReturn(Optional.of(spot));

        assertThrows(OccupancyException.class, () -> parkingService.handleParked(parkedEvent));
    }

    @Test
    void shouldThrowNotFoundException_WhenNoSessionFound() {
        when(spotRepository.findByCoordinates(anyDouble(), anyDouble())).thenReturn(Optional.of(spot));
        when(sessionRepository.findByLicensePlateAndExitTimeIsNull(anyString())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> parkingService.handleParked(parkedEvent));
    }

    @Test
    void shouldThrowMissingDataException_WhenSectorNotFound() {
        when(spotRepository.findByCoordinates(anyDouble(), anyDouble())).thenReturn(Optional.of(spot));
        when(sessionRepository.findByLicensePlateAndExitTimeIsNull(anyString())).thenReturn(Optional.of(new ParkingSession()));
        when(sectorRepository.findBySector(anyString())).thenReturn(Optional.empty());

        assertThrows(MissingDataException.class, () -> parkingService.handleParked(parkedEvent));
    }

    @Test
    void shouldThrowOccupancyException_WhenSectorFull() {
        when(spotRepository.findByCoordinates(anyDouble(), anyDouble())).thenReturn(Optional.of(spot));
        when(sessionRepository.findByLicensePlateAndExitTimeIsNull(anyString())).thenReturn(Optional.of(new ParkingSession()));
        sectorA.setOccupiedCount(100);
        when(sectorRepository.findBySector(anyString())).thenReturn(Optional.of(sectorA));

        assertThrows(OccupancyException.class, () -> parkingService.handleParked(parkedEvent));
    }

    // ==================== EXIT ====================

    @Test
    void shouldCalculatePriceAndFreeSpotOnExit() {
        ParkingSession session = new ParkingSession();
        session.setEntryTime(Instant.parse("2025-06-20T10:00:00.000Z"));
        session.setSector("A");
        session.setSpot(spot);
        spot.setOccupied(true);
        sectorA.setOccupiedCount(41);

        when(sessionRepository.findByLicensePlateAndExitTimeIsNull(anyString()))
                .thenReturn(Optional.of(session));
        
        when(sectorRepository.findBySector("A")).thenReturn(Optional.of(sectorA));

        parkingService.handleExit(exitEvent);

        assertFalse(spot.isOccupied());
        assertEquals(40, sectorA.getOccupiedCount());
        assertNotNull(session.getExitTime());
        assertNotNull(session.getChargedAmount());
        assertTrue(session.getChargedAmount().compareTo(BigDecimal.ZERO) > 0);

        verify(spotRepository).save(spot);
        verify(sectorRepository).save(sectorA);
    }

    @Test
    void shouldThrowNotFoundException_WhenNoSessionFoundOnExit() {
        when(sessionRepository.findByLicensePlateAndExitTimeIsNull(anyString())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> parkingService.handleExit(exitEvent));
    }

    @Test
    void shouldThrowException_WhenExitTimeIsBeforeEntryTime() {
        ParkingSession session = new ParkingSession();
        session.setEntryTime(Instant.parse("2026-06-20T12:00:00.000Z"));

        when(sessionRepository.findByLicensePlateAndExitTimeIsNull(anyString())).thenReturn(Optional.of(session));

        exitEvent.setExitTime(Instant.parse("2026-06-20T11:00:00.000Z"));

        assertThrows(InvalidClientDataException.class, () -> parkingService.handleExit(exitEvent));
    }

    @Test
    void shouldThrowMissingDataException_WhenSectorNotFoundOnExit() {
        ParkingSession session = new ParkingSession();
        session.setEntryTime(Instant.parse("2026-06-20T10:00:00.000Z"));
        session.setSector("Z");
        spot.setSector("Z");
        session.setSpot(spot);

        when(sessionRepository.findByLicensePlateAndExitTimeIsNull(anyString())).thenReturn(Optional.of(session));
        when(sectorRepository.findBySector(anyString())).thenReturn(Optional.empty());

        assertThrows(MissingDataException.class, () -> parkingService.handleExit(exitEvent));
    }

    // ==================== REVENUE ====================

    @Test
    void shouldThrowException_WhenNotFound() {
        when(sessionRepository.sumRevenueBySectorAndDate(anyString(), any(LocalDate.class))).thenReturn(null);
        assertThrows(NotFoundException.class, () -> parkingService.getRevenue(revenueRequest));
    }

    @Test
    void shouldReturnRevenue() {

        when(sessionRepository.sumRevenueBySectorAndDate(anyString(), any(LocalDate.class))).thenReturn(30.0);

        RevenueResponse response = parkingService.getRevenue(revenueRequest);

        // Then
        assertNotNull(response);
        assertEquals(30.0, response.getAmount());
        assertEquals("BRL", response.getCurrency());
    }

    // ==================== PRICE CALCULATION ====================
    @Test
    void shouldThrowException_WhenMissingTimes() {
        ParkingSession session = new ParkingSession();

        assertThrows(MissingDataException.class, () -> parkingService.calculatePriceForTest(session));

        session.setEntryTime(Instant.parse("2025-06-20T10:00:00.000Z"));

        assertThrows(MissingDataException.class, () -> parkingService.calculatePriceForTest(session));

        session.setEntryTime(null);
        session.setExitTime(Instant.parse("2025-06-20T10:25:00.000Z"));

        assertThrows(MissingDataException.class, () -> parkingService.calculatePriceForTest(session));
    }

    @Test
    void shouldThrowException_WhenSectorNotFound() {
        ParkingSession session = createSession("2025-06-20T10:00:00.000Z", "2025-06-20T11:00:00.000Z");
        session.setSector("Z");

        assertThrows(MissingDataException.class, () -> parkingService.calculatePriceForTest(session));
    }

    @Test
    void shouldBeFreeForFirst30Minutes() {
        ParkingSession session = createSession("2025-06-20T10:00:00.000Z", "2025-06-20T10:25:00.000Z");

        BigDecimal price = parkingService.calculatePriceForTest(session);

        assertEquals(BigDecimal.ZERO, price);
    }

    @Test
    void shouldChargeCorrectlyFor1HourAnd15Minutes() {
        ParkingSession session = createSession("2025-06-20T10:00:00.000Z", "2025-06-20T11:15:00.000Z");

        when(sectorRepository.findBySector("A")).thenReturn(Optional.of(sectorA));

        BigDecimal price = parkingService.calculatePriceForTest(session);

        assertTrue(price.compareTo(BigDecimal.valueOf(20.0)) >= 0); // 2 horas * 10
    }

    @Test
    void shouldApplyDynamicMultiplier_LowOccupancy() {
        sectorA.setOccupiedCount(15); // 15% → 10% desconto
        double multiplier = parkingService.getDynamicMultiplierForTest(sectorA);
        assertEquals(0.9, multiplier);
    }

    @Test
    void shouldApplyDynamicMultiplier_HighOccupancy() {
        sectorA.setOccupiedCount(70); // 70% → +10%
        double multiplier = parkingService.getDynamicMultiplierForTest(sectorA);
        assertEquals(1.1, multiplier);
    }

    @Test
    void shouldApplyDynamicMultiplier_VeryHighOccupancy() {
        sectorA.setOccupiedCount(85); // 85% → +25%
        double multiplier = parkingService.getDynamicMultiplierForTest(sectorA);
        assertEquals(1.25, multiplier);
    }

    // ==================== HELPER METHODS ====================

    private ParkingSession createSession(String entry, String exit) {
        ParkingSession session = new ParkingSession();
        session.setEntryTime(Instant.parse(entry));
        session.setExitTime(Instant.parse(exit));
        session.setSector("A");
        return session;
    }
}