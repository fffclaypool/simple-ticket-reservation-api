package com.example.ticketreservation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationRequest {
    private Long eventId;
    private String customerName;
    private String customerEmail;
    private Integer numberOfSeats;
}
