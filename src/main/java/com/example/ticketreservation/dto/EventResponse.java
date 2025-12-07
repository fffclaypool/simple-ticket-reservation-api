package com.example.ticketreservation.dto;

import java.math.BigDecimal;
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
    BigDecimal price;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
