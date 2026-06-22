package com.fabiomm.parking_garage.api;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fabiomm.parking_garage.dto.ErrorResponse;
import com.fabiomm.parking_garage.exception.InvalidClientDataException;
import com.fabiomm.parking_garage.exception.InvalidEventTypeException;
import com.fabiomm.parking_garage.exception.MissingDataException;
import com.fabiomm.parking_garage.exception.NotFoundException;
import com.fabiomm.parking_garage.exception.OccupancyException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidEventTypeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEventTypeException(InvalidEventTypeException ex) {
        ErrorResponse errorResponse = buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
        log.info(ex.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(InvalidClientDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidClientDataException(InvalidClientDataException ex) {
        ErrorResponse errorResponse = buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
        log.info(ex.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MissingDataException.class)
    public ResponseEntity<ErrorResponse> handleMissingDataException(MissingDataException ex) {
        ErrorResponse errorResponse = buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        log.error(ex.getMessage());
        return ResponseEntity.internalServerError().body(errorResponse);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException ex) {
        ErrorResponse errorResponse = buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
        log.warn(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(OccupancyException.class)
    public ResponseEntity<ErrorResponse> handleOccupancyException(OccupancyException ex) {
        ErrorResponse errorResponse = buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
        log.info(ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        ErrorResponse errorResponse = buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An internal server error occured");
        log.error(ex.getMessage());
        return ResponseEntity.internalServerError().body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse errorResponse = buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An internal server error occured");
        log.error(ex.getMessage());
        return ResponseEntity.internalServerError().body(errorResponse);
    }

    private ErrorResponse buildErrorResponse(HttpStatusCode status, String message) {
        return new ErrorResponse(status.value(), message, Instant.now());
    }
}
