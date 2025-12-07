package com.example.ticketreservation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class TicketRequest {

    @NotBlank(message = "Customer name is required")
    @Size(max = 255, message = "Customer name must be at most 255 characters")
    String customerName;

    @NotBlank(message = "Customer email is required")
    @Email(message = "Customer email must be a valid email address")
    @Size(max = 255, message = "Customer email must be at most 255 characters")
    String customerEmail;

    @NotNull(message = "Number of seats is required")
    @Positive(message = "Number of seats must be positive")
    Integer numberOfSeats;
}
