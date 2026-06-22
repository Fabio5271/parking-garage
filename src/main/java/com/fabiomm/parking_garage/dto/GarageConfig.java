package com.fabiomm.parking_garage.dto;

import java.util.List;

import lombok.Data;

@Data
public class GarageConfig {
    private List<SectorConfig> garage;
    private List<SpotConfig> spots;
}
