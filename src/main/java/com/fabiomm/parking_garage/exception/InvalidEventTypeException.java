package com.fabiomm.parking_garage.exception;

public class InvalidEventTypeException extends RuntimeException {
    public InvalidEventTypeException(String message) {
        super(message);
    }
}
