package com.fabiomm.parking_garage.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fabiomm.parking_garage.api.ApiExceptionHandler;
import com.fabiomm.parking_garage.dto.Event;
import com.fabiomm.parking_garage.dto.RevenueRequest;
import com.fabiomm.parking_garage.dto.RevenueResponse;
import com.fabiomm.parking_garage.service.ParkingService;

@ExtendWith(MockitoExtension.class)
public class ParkingControllerTest {
    @Mock
    private ParkingService parkingService;

    @InjectMocks
    private ParkingController parkingController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(parkingController)
        .setControllerAdvice(new ApiExceptionHandler())
        .build();
    }

    @Test
    void shouldHandleEntryEventSuccessfully() throws Exception {

        mockMvc.perform(post("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "license_plate": "ZUL0001",
                        "entry_time": "2025-06-20T10:00:00",
                        "event_type": "ENTRY"
                    }
                    """))
                .andExpect(status().isOk());

        verify(parkingService).handleEntry(any(Event.class));
    }

    @Test
    void shouldHandleParkedEventSuccessfully() throws Exception {

        mockMvc.perform(post("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "license_plate": "ZUL0001",
                        "lat": -23.561684,
                        "lng": -46.655981,
                        "event_type": "PARKED"
                    }
                    """))
                .andExpect(status().isOk());

        verify(parkingService).handleParked(any(Event.class));
    }

    @Test
    void shouldHandleExitEventSuccessfully() throws Exception {

        mockMvc.perform(post("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "license_plate": "ZUL0001",
                        "exit_time": "2025-06-20T11:45:00",
                        "event_type": "EXIT"
                    }
                    """))
                .andExpect(status().isOk());

        verify(parkingService).handleExit(any(Event.class));
    }

    @Test
    void shouldReturnBadRequest_WhenInvalidEventType() throws Exception {
        mockMvc.perform(post("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "license_plate": "ZUL0001",
                        "event_type": "INVALID"
                    }
                    """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturnRevenueSuccessfully() throws Exception {
        RevenueResponse response = new RevenueResponse();
        response.setAmount(1250.75);

        when(parkingService.getRevenue(any(RevenueRequest.class))).thenReturn(response);

        mockMvc.perform(get("/revenue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "date": "2025-06-20",
                        "sector": "A"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(1250.75))
                .andExpect(jsonPath("$.currency").value("BRL"));
    }

    @Test
    void shouldPropagateException_WhenServiceThrows() throws Exception {
        when(parkingService.getRevenue(any(RevenueRequest.class)))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/revenue")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "date": "2025-06-20",
                        "sector": "A"
                    }
                    """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
