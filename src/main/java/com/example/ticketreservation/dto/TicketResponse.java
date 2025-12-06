package com.example.ticketreservation.dto;

import com.example.ticketreservation.entity.TicketStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {
    private Long id;
    private String ticketCode;
    private Long eventId;
    private String eventName;
    private String customerName;
    private String customerEmail;
    private Integer numberOfSeats;
    private Double totalAmount;
    private TicketStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
