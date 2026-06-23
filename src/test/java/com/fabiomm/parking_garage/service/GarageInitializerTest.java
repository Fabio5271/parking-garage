package com.fabiomm.parking_garage.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.web.client.RestTemplate;

import com.fabiomm.parking_garage.dto.GarageConfig;
import com.fabiomm.parking_garage.dto.SectorConfig;
import com.fabiomm.parking_garage.dto.SpotConfig;
import com.fabiomm.parking_garage.entity.ParkingSpot;
import com.fabiomm.parking_garage.entity.Sector;
import com.fabiomm.parking_garage.repository.ParkingSpotRepository;
import com.fabiomm.parking_garage.repository.SectorRepository;

@ExtendWith(MockitoExtension.class)
public class GarageInitializerTest {
    @Mock
    private SectorRepository sectorRepository;
    @Mock
    private ParkingSpotRepository spotRepository;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ApplicationReadyEvent applicationReadyEvent;

    @InjectMocks
    private GarageInitializer garageInitializer;

    private GarageConfig garageConfig;

    @BeforeEach
    void setUp() {
        garageConfig = new GarageConfig();

        SectorConfig sectorConfig = new SectorConfig();
        sectorConfig.setSector("A");
        sectorConfig.setBasePrice(10.0);
        sectorConfig.setCapacity(100);

        SpotConfig spotConfig = new SpotConfig();
        spotConfig.setId(1L);
        spotConfig.setSector("A");
        spotConfig.setLat(-23.561684);
        spotConfig.setLng(-46.655981);

        garageConfig.setGarage(List.of(sectorConfig));
        garageConfig.setSpots(List.of(spotConfig));
    }

    @Test
    void shouldInitializeGarageSuccessfully() {
        when(restTemplate.getForObject("http://localhost:3000/garage", GarageConfig.class))
                .thenReturn(garageConfig);

        when(sectorRepository.findBySector("A")).thenReturn(Optional.empty());
        when(spotRepository.findById(1L)).thenReturn(Optional.empty());

        garageInitializer.initializeGarage();

        verify(restTemplate).getForObject("http://localhost:3000/garage", GarageConfig.class);

        ArgumentCaptor<Sector> sectorCaptor = ArgumentCaptor.forClass(Sector.class);
        verify(sectorRepository).save(sectorCaptor.capture());
        Sector savedSector = sectorCaptor.getValue();
        assertEquals("A", savedSector.getSector());
        assertEquals(new BigDecimal("10.0"), savedSector.getBasePrice());
        assertEquals(100, savedSector.getCapacity());

        ArgumentCaptor<ParkingSpot> spotCaptor = ArgumentCaptor.forClass(ParkingSpot.class);
        verify(spotRepository).save(spotCaptor.capture());
        ParkingSpot savedSpot = spotCaptor.getValue();
        assertEquals(1L, savedSpot.getId());
        assertEquals("A", savedSpot.getSector());
    }

    @Test
    void shouldUpdateExistingSectorInsteadOfCreatingNewOne() {
        when(restTemplate.getForObject(anyString(), eq(GarageConfig.class)))
                .thenReturn(garageConfig);

        Sector existingSector = new Sector();
        existingSector.setId(1L);
        existingSector.setSector("A");
        when(sectorRepository.findBySector("A")).thenReturn(Optional.of(existingSector));

        garageInitializer.initializeGarage();

        ArgumentCaptor<Sector> sectorCaptor = ArgumentCaptor.forClass(Sector.class);
        verify(sectorRepository).save(sectorCaptor.capture());
        assertEquals(1L, sectorCaptor.getValue().getId());
    }

    @Test
    void shouldUpdateExistingSpotInsteadOfCreatingNew() {
        when(restTemplate.getForObject(anyString(), eq(GarageConfig.class)))
                .thenReturn(garageConfig);

        ParkingSpot existingSpot = new ParkingSpot();
        existingSpot.setId(1L);
        when(spotRepository.findById(1L)).thenReturn(Optional.of(existingSpot));

        garageInitializer.initializeGarage();

        verify(spotRepository).save(existingSpot);
    }

    @Test
    void shouldHandleNullResponseFromSimulatorGracefully() {
        when(restTemplate.getForObject(anyString(), eq(GarageConfig.class)))
                .thenReturn(null);

        assertDoesNotThrow(() -> garageInitializer.initializeGarage());

        verify(sectorRepository, never()).save(any());
        verify(spotRepository, never()).save(any());
    }

    @Test
    void shouldLogErrorAndContinue_WhenRestTemplateFails() {
        when(restTemplate.getForObject(anyString(), eq(GarageConfig.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        assertDoesNotThrow(() -> garageInitializer.initializeGarage());

        verify(sectorRepository, never()).save(any());
        verify(spotRepository, never()).save(any());
    }
}
