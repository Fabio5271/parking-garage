package com.fabiomm.parking_garage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class SectorConfig {
    private String sector;

    @JsonProperty("base_price")
    private double basePrice;

    @JsonProperty("max_capacity")
    private int capacity;
}
