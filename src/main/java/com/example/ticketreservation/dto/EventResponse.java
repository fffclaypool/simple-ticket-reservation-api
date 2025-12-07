package com.example.ticketreservation.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class EventResponse {
    Long id;
    String name;
    String description;
    String venue;
    LocalDateTime eventDate;
    Integer totalSeats;
    Integer availableSeats;
    Double price;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
