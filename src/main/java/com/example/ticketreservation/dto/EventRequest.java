package com.example.ticketreservation.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class EventRequest {
    String name;
    String description;
    String venue;
    LocalDateTime eventDate;
    Integer totalSeats;
    Double price;
}
