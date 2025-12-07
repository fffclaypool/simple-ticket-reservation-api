package com.example.ticketreservation.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class TicketRequest {
    String customerName;
    String customerEmail;
    Integer numberOfSeats;
}
