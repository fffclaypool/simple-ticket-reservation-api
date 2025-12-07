package com.example.ticketreservation.controller;

import com.example.ticketreservation.dto.TicketRequest;
import com.example.ticketreservation.dto.TicketResponse;
import com.example.ticketreservation.service.TicketService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/tickets")
    public ResponseEntity<List<TicketResponse>> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @GetMapping("/tickets/code/{code}")
    public ResponseEntity<TicketResponse> getTicketByCode(@PathVariable String code) {
        return ResponseEntity.ok(ticketService.getTicketByCode(code));
    }

    @GetMapping("/tickets/email/{email}")
    public ResponseEntity<List<TicketResponse>> getTicketsByEmail(@PathVariable String email) {
        return ResponseEntity.ok(ticketService.getTicketsByEmail(email));
    }

    @GetMapping("/events/{eventId}/tickets")
    public ResponseEntity<List<TicketResponse>> getTicketsByEventId(@PathVariable Long eventId) {
        return ResponseEntity.ok(ticketService.getTicketsByEventId(eventId));
    }

    @PostMapping("/events/{eventId}/tickets")
    public ResponseEntity<TicketResponse> createTicket(
            @PathVariable Long eventId, @Valid @RequestBody TicketRequest request) {
        TicketResponse createdTicket = ticketService.createTicket(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTicket);
    }

    @PatchMapping("/tickets/{id}/cancel")
    public ResponseEntity<TicketResponse> cancelTicket(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.cancelTicket(id));
    }
}
