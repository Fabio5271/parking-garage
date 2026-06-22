package com.fabiomm.parking_garage.dto;

import lombok.Data;

@Data
public class SpotConfig {
    private Long id;
    private String sector;
    private double lat;
    private double lng;
}
