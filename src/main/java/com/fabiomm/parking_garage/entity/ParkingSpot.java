package com.fabiomm.parking_garage.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class ParkingSpot {
    @Id
    private long id;
    private String sector;
    private double lat;
    private double lng;
    private boolean occupied = false;
}
