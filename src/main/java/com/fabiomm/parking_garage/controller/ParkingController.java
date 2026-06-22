package com.fabiomm.parking_garage.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fabiomm.parking_garage.dto.Event;
import com.fabiomm.parking_garage.dto.RevenueRequest;
import com.fabiomm.parking_garage.dto.RevenueResponse;
import com.fabiomm.parking_garage.exception.InvalidEventTypeException;
import com.fabiomm.parking_garage.service.ParkingService;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;

@AllArgsConstructor
@RestController
public class ParkingController {
    private final ParkingService parkingService;

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody Event event) {
        switch (event.getEventType()) {
            case "ENTRY":
                parkingService.handleEntry(event);
                break;
            case "PARKED":
                parkingService.handleParked(event);
                break;
            case "EXIT":
                parkingService.handleExit(event);
                break;
            default:
                throw new InvalidEventTypeException("Invalid event type: " + event.getEventType());
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/revenue")
    public ResponseEntity<RevenueResponse> getRevenue(@RequestBody RevenueRequest request) {
        RevenueResponse response = parkingService.getRevenue(request);
        return ResponseEntity.ok(response);
    }

}
