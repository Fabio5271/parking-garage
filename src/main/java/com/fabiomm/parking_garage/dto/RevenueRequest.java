package com.fabiomm.parking_garage.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class RevenueRequest {
    private LocalDate date;
    private String sector;
}
