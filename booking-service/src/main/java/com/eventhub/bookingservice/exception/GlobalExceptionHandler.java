package com.eventhub.bookingservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    // Event Service unavailable -> 503
    @ExceptionHandler(EventServiceUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleEventServiceUnavailable(
            EventServiceUnavailableException ex) {

        ProblemDetail problemDetail =
                ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);

        problemDetail.setTitle("Event Service Unavailable");
        problemDetail.setDetail(ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(problemDetail);

    }

    // Booking with the given id does not exist -> 404
    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleBookingNotFound(
            BookingNotFoundException ex) {

        ProblemDetail problemDetail =
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND);

        problemDetail.setTitle("Booking Not Found");
        problemDetail.setDetail(ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problemDetail);
    }

    // Event Service is running, but Event does not exist -> 404
    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEventNotFound(
            EventNotFoundException ex) {

        ProblemDetail problemDetail =
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND);

        problemDetail.setTitle("Event Not Found");
        problemDetail.setDetail(ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problemDetail);
    }

    // Circuit Breaker is OPEN -> 503
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ProblemDetail> handleCircuitBreakerOpen(
            CallNotPermittedException ex) {

        System.out.println(
                "Circuit Breaker OPEN: Event Service calls are temporarily blocked."
        );

        ProblemDetail problemDetail =
                ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);

        problemDetail.setTitle("Event Service Temporarily Unavailable");

        problemDetail.setDetail(
                "The Event Service is currently unavailable. "
                        + "The circuit breaker is OPEN and is temporarily blocking requests."
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(problemDetail);
    }
}
