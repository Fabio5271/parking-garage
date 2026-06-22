package com.fabiomm.parking_garage.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "sector")
@Data
public class Sector {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 10)
    private String sector;

    @Column(precision = 10, scale = 2)
    private BigDecimal basePrice;

    private int capacity;

    private int occupiedCount = 0;

    public void increaseOccupied(){
        ++this.occupiedCount;
    }

    public void decreaseOccupied(){
        --this.occupiedCount;
    }
}
