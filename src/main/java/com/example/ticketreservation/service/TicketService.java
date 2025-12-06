package com.example.ticketreservation.service;

import com.example.ticketreservation.dto.TicketRequest;
import com.example.ticketreservation.dto.TicketResponse;
import com.example.ticketreservation.entity.Event;
import com.example.ticketreservation.entity.Ticket;
import com.example.ticketreservation.entity.TicketStatus;
import com.example.ticketreservation.exception.InsufficientSeatsException;
import com.example.ticketreservation.exception.ResourceNotFoundException;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.TicketRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TicketService {

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;

    public List<TicketResponse> getAllTickets() {
        return ticketRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public TicketResponse getTicketById(Long id) {
        Ticket ticket =
                ticketRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));
        return mapToResponse(ticket);
    }

    public TicketResponse getTicketByCode(String code) {
        Ticket ticket = ticketRepository
                .findByTicketCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "code", code));
        return mapToResponse(ticket);
    }

    public List<TicketResponse> getTicketsByEmail(String email) {
        return ticketRepository.findByCustomerEmail(email).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TicketResponse> getTicketsByEventId(Long eventId) {
        return ticketRepository.findByEventId(eventId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TicketResponse createTicket(Long eventId, TicketRequest request) {
        log.info(
                "Creating ticket for eventId={}, customerEmail={}, seats={}",
                eventId,
                request.getCustomerEmail(),
                request.getNumberOfSeats());

        // Acquire pessimistic lock on the event row - this blocks other transactions
        Event event = eventRepository
                .findByIdWithLock(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        log.info("Acquired lock for eventId={}, availableSeats={}", eventId, event.getAvailableSeats());

        // Check if enough seats are available
        if (event.getAvailableSeats() < request.getNumberOfSeats()) {
            throw new InsufficientSeatsException(request.getNumberOfSeats(), event.getAvailableSeats());
        }

        // Intentional delay for load testing (simulates processing time)
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Ticket creation interrupted", e);
        }

        // Decrease available seats and flush immediately to ensure lock is released with updated data
        event.setAvailableSeats(event.getAvailableSeats() - request.getNumberOfSeats());
        eventRepository.saveAndFlush(event);

        // Create and save ticket
        Ticket ticket = Ticket.builder()
                .ticketCode(generateTicketCode())
                .event(event)
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .numberOfSeats(request.getNumberOfSeats())
                .totalAmount(event.getPrice() * request.getNumberOfSeats())
                .status(TicketStatus.CONFIRMED)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);
        log.info(
                "Ticket created successfully: ticketId={}, ticketCode={}, remainingSeats={}",
                savedTicket.getId(),
                savedTicket.getTicketCode(),
                event.getAvailableSeats());

        return mapToResponse(savedTicket);
    }

    @Transactional
    public TicketResponse cancelTicket(Long id) {
        Ticket ticket =
                ticketRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Ticket is already cancelled");
        }

        Event event = ticket.getEvent();
        event.setAvailableSeats(event.getAvailableSeats() + ticket.getNumberOfSeats());
        eventRepository.save(event);

        ticket.setStatus(TicketStatus.CANCELLED);
        Ticket cancelledTicket = ticketRepository.save(ticket);
        return mapToResponse(cancelledTicket);
    }

    private String generateTicketCode() {
        return "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private TicketResponse mapToResponse(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .ticketCode(ticket.getTicketCode())
                .eventId(ticket.getEvent().getId())
                .eventName(ticket.getEvent().getName())
                .customerName(ticket.getCustomerName())
                .customerEmail(ticket.getCustomerEmail())
                .numberOfSeats(ticket.getNumberOfSeats())
                .totalAmount(ticket.getTotalAmount())
                .status(ticket.getStatus())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
