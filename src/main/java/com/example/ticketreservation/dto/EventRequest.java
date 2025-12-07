package com.example.ticketreservation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class EventRequest {

    @NotBlank(message = "Event name is required")
    @Size(max = 255, message = "Event name must be at most 255 characters")
    String name;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    String description;

    @NotBlank(message = "Venue is required")
    @Size(max = 255, message = "Venue must be at most 255 characters")
    String venue;

    @NotNull(message = "Event date is required")
    @Future(message = "Event date must be in the future")
    LocalDateTime eventDate;

    @NotNull(message = "Total seats is required")
    @Positive(message = "Total seats must be positive")
    Integer totalSeats;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be positive")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer digits and 2 decimal places")
    BigDecimal price;
}
