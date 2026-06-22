package com.fabiomm.parking_garage.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

@Data
public class RevenueResponse {
    private double amount;
    private String currency = "BRL";

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC")
    private Instant timestamp = Instant.now();
}
