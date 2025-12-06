package com.example.ticketreservation.dto;

import com.example.ticketreservation.entity.ReservationStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {
    private Long id;
    private String reservationCode;
    private Long eventId;
    private String eventName;
    private String customerName;
    private String customerEmail;
    private Integer numberOfSeats;
    private Double totalAmount;
    private ReservationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
