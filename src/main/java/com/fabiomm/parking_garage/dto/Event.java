package com.fabiomm.parking_garage.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Event {
    @JsonProperty("license_plate")
    private String licensePlate;

    @JsonProperty("entry_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private Instant entryTime;
    
    @JsonProperty("exit_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private Instant exitTime;
    
    private double lat;
    private double lng;

    @JsonProperty("event_type")
    private String eventType;
}
