package com.example.ticketreservation.dto;

import com.example.ticketreservation.entity.TicketStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class TicketResponse {
    Long id;
    String ticketCode;
    Long eventId;
    String eventName;
    String customerName;
    String customerEmail;
    Integer numberOfSeats;
    Double totalAmount;
    TicketStatus status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
