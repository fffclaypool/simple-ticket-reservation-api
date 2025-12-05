package com.example.ticketreservation.exception;

public class InsufficientSeatsException extends RuntimeException {

    public InsufficientSeatsException(String message) {
        super(message);
    }

    public InsufficientSeatsException(int requested, int available) {
        super(String.format("Requested %d seats but only %d available", requested, available));
    }
}
