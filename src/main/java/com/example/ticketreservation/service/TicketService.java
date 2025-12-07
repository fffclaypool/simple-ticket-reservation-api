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
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TicketService {

    private static final String EVENTS_CACHE = "events";

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final CacheManager cacheManager;

    // === Public methods (orchestration with side effects) ===

    public List<TicketResponse> getAllTickets() {
        return ticketRepository.findAll().stream()
                .map(TicketService::toResponse)
                .toList();
    }

    public TicketResponse getTicketById(Long id) {
        Ticket ticket = findTicketOrThrow(id);
        return toResponse(ticket);
    }

    public TicketResponse getTicketByCode(String code) {
        Ticket ticket = ticketRepository
                .findByTicketCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "code", code));
        return toResponse(ticket);
    }

    public List<TicketResponse> getTicketsByEmail(String email) {
        return ticketRepository.findByCustomerEmail(email).stream()
                .map(TicketService::toResponse)
                .toList();
    }

    public List<TicketResponse> getTicketsByEventId(Long eventId) {
        return ticketRepository.findByEventId(eventId).stream()
                .map(TicketService::toResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "events", key = "#eventId")
    public TicketResponse createTicket(Long eventId, TicketRequest request) {
        log.info(
                "Creating ticket for eventId={}, customerEmail={}, seats={}",
                eventId,
                request.getCustomerEmail(),
                request.getNumberOfSeats());

        Event event = findEventWithLockOrThrow(eventId);
        simulateProcessingDelay();
        validateSeatAvailability(event, request.getNumberOfSeats());

        int newAvailableSeats = calculateSeatsAfterBooking(event.getAvailableSeats(), request.getNumberOfSeats());
        event.setAvailableSeats(newAvailableSeats);
        eventRepository.save(event);

        Ticket ticket = toNewEntity(event, request);
        Ticket savedTicket = ticketRepository.save(ticket);

        log.info(
                "Ticket created successfully: ticketId={}, ticketCode={}, remainingSeats={}",
                savedTicket.getId(),
                savedTicket.getTicketCode(),
                event.getAvailableSeats());

        return toResponse(savedTicket);
    }

    @Transactional
    public TicketResponse cancelTicket(Long id) {
        Ticket ticket = findTicketOrThrow(id);
        validateNotAlreadyCancelled(ticket);

        Event event = ticket.getEvent();
        int newAvailableSeats = calculateSeatsAfterCancellation(event.getAvailableSeats(), ticket.getNumberOfSeats());
        event.setAvailableSeats(newAvailableSeats);
        eventRepository.save(event);

        evictEventCache(event.getId());

        ticket.setStatus(TicketStatus.CANCELLED);
        Ticket cancelledTicket = ticketRepository.save(ticket);
        return toResponse(cancelledTicket);
    }

    // === Private methods with side effects ===

    private Ticket findTicketOrThrow(Long id) {
        return ticketRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));
    }

    private Event findEventWithLockOrThrow(Long eventId) {
        return eventRepository
                .findByIdWithLock(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));
    }

    private void simulateProcessingDelay() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Ticket creation interrupted", e);
        }
    }

    private void evictEventCache(Long eventId) {
        Optional.ofNullable(cacheManager.getCache(EVENTS_CACHE)).ifPresent(cache -> {
            cache.evict(eventId);
            log.info("Evicted event cache: eventId={}", eventId);
        });
    }

    // === Pure functions (no side effects, static) ===

    static void validateSeatAvailability(Event event, int requestedSeats) {
        if (!hasEnoughSeats(event.getAvailableSeats(), requestedSeats)) {
            throw new InsufficientSeatsException(requestedSeats, event.getAvailableSeats());
        }
    }

    static boolean hasEnoughSeats(int availableSeats, int requestedSeats) {
        return availableSeats >= requestedSeats;
    }

    static void validateNotAlreadyCancelled(Ticket ticket) {
        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Ticket is already cancelled");
        }
    }

    static int calculateSeatsAfterBooking(int currentSeats, int bookedSeats) {
        return currentSeats - bookedSeats;
    }

    static int calculateSeatsAfterCancellation(int currentSeats, int cancelledSeats) {
        return currentSeats + cancelledSeats;
    }

    static double calculateTotalAmount(double pricePerSeat, int numberOfSeats) {
        return pricePerSeat * numberOfSeats;
    }

    private static String generateTicketCode() {
        return "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static Ticket toNewEntity(Event event, TicketRequest request) {
        return Ticket.builder()
                .ticketCode(generateTicketCode())
                .event(event)
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .numberOfSeats(request.getNumberOfSeats())
                .totalAmount(calculateTotalAmount(event.getPrice(), request.getNumberOfSeats()))
                .status(TicketStatus.CONFIRMED)
                .build();
    }

    private static TicketResponse toResponse(Ticket ticket) {
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
