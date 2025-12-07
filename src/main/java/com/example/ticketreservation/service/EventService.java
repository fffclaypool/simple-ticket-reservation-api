package com.example.ticketreservation.service;

import com.example.ticketreservation.dto.EventRequest;
import com.example.ticketreservation.dto.EventResponse;
import com.example.ticketreservation.entity.Event;
import com.example.ticketreservation.exception.ResourceNotFoundException;
import com.example.ticketreservation.repository.EventRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EventService {

    private static final String CACHE_NAME = "events";

    private final EventRepository eventRepository;

    // === Public methods (orchestration with side effects) ===

    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll().stream().map(EventService::toResponse).toList();
    }

    @Cacheable(value = CACHE_NAME, key = "#id")
    public EventResponse getEventById(Long id) {
        log.info("Fetching event from database: id={}", id);
        Event event = findEventOrThrow(id);
        return toResponse(event);
    }

    public List<EventResponse> getAvailableEvents() {
        return eventRepository.findAvailableEvents(LocalDateTime.now()).stream()
                .map(EventService::toResponse)
                .toList();
    }

    public List<EventResponse> searchEventsByName(String name) {
        return eventRepository.findByNameContainingIgnoreCase(name).stream()
                .map(EventService::toResponse)
                .toList();
    }

    @Transactional
    public EventResponse createEvent(EventRequest request) {
        Event event = toNewEntity(request);
        Event savedEvent = eventRepository.save(event);
        return toResponse(savedEvent);
    }

    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#id")
    public EventResponse updateEvent(Long id, EventRequest request) {
        log.info("Updating event and evicting cache: id={}", id);
        Event event = findEventOrThrow(id);
        applyUpdate(event, request);
        Event updatedEvent = eventRepository.save(event);
        return toResponse(updatedEvent);
    }

    @Transactional
    @CacheEvict(value = CACHE_NAME, key = "#id")
    public void deleteEvent(Long id) {
        log.info("Deleting event and evicting cache: id={}", id);
        Event event = findEventOrThrow(id);
        eventRepository.delete(event);
    }

    // === Private methods with side effects ===

    private Event findEventOrThrow(Long id) {
        return eventRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Event", "id", id));
    }

    // === Pure functions (no side effects, static) ===

    private static Event toNewEntity(EventRequest request) {
        return Event.builder()
                .name(request.getName())
                .description(request.getDescription())
                .venue(request.getVenue())
                .eventDate(request.getEventDate())
                .totalSeats(request.getTotalSeats())
                .availableSeats(request.getTotalSeats())
                .price(request.getPrice())
                .build();
    }

    private static void applyUpdate(Event event, EventRequest request) {
        int newAvailableSeats =
                calculateNewAvailableSeats(event.getAvailableSeats(), event.getTotalSeats(), request.getTotalSeats());

        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setVenue(request.getVenue());
        event.setEventDate(request.getEventDate());
        event.setTotalSeats(request.getTotalSeats());
        event.setAvailableSeats(newAvailableSeats);
        event.setPrice(request.getPrice());
    }

    static int calculateNewAvailableSeats(int currentAvailable, int oldTotal, int newTotal) {
        return currentAvailable + (newTotal - oldTotal);
    }

    private static EventResponse toResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getDescription())
                .venue(event.getVenue())
                .eventDate(event.getEventDate())
                .totalSeats(event.getTotalSeats())
                .availableSeats(event.getAvailableSeats())
                .price(event.getPrice())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}
