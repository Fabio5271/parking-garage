package com.fabiomm.parking_garage.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class ParkingSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String licensePlate;

    @ManyToOne
    private ParkingSpot spot;

    private Instant entryTime;
    private Instant exitTime;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal chargedAmount = BigDecimal.ZERO;

    private String sector;
}
