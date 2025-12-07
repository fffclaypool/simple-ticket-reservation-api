package com.example.ticketreservation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ticketreservation.dto.EventRequest;
import com.example.ticketreservation.dto.EventResponse;
import com.example.ticketreservation.dto.TicketRequest;
import com.example.ticketreservation.dto.TicketResponse;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.TicketRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
@ActiveProfiles("redis-test")
@DisplayName("TicketService Cache Integration Tests")
class TicketServiceCacheIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7")).withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private EventService eventService;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CacheManager cacheManager;

    private Long eventId;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        eventRepository.deleteAll();
        cacheManager.getCache("events").clear();

        // Create a test event
        EventRequest eventRequest = EventRequest.builder()
                .name("Test Concert")
                .description("A test concert")
                .venue("Tokyo Dome")
                .eventDate(LocalDateTime.of(2025, 12, 25, 19, 0))
                .totalSeats(100)
                .price(new BigDecimal("5000.0"))
                .build();
        EventResponse created = eventService.createEvent(eventRequest);
        eventId = created.getId();
    }

    @Test
    @DisplayName("should evict event cache when ticket is created")
    void shouldEvictEventCacheOnTicketCreation() {
        // Given: Cache the event
        eventService.getEventById(eventId);
        assertThat(cacheManager.getCache("events").get(eventId)).isNotNull();

        // When: Create a ticket
        TicketRequest ticketRequest = TicketRequest.builder()
                .customerName("John Doe")
                .customerEmail("john@example.com")
                .numberOfSeats(2)
                .build();
        ticketService.createTicket(eventId, ticketRequest);

        // Then: Event cache should be evicted (because available seats changed)
        assertThat(cacheManager.getCache("events").get(eventId)).isNull();
    }

    @Test
    @DisplayName("should reflect updated available seats after cache eviction")
    void shouldReflectUpdatedAvailableSeatsAfterCacheEviction() {
        // Given: Cache the event and note initial available seats
        EventResponse initialEvent = eventService.getEventById(eventId);
        int initialAvailableSeats = initialEvent.getAvailableSeats();

        // When: Create a ticket for 2 seats
        TicketRequest ticketRequest = TicketRequest.builder()
                .customerName("John Doe")
                .customerEmail("john@example.com")
                .numberOfSeats(2)
                .build();
        ticketService.createTicket(eventId, ticketRequest);

        // Then: Next access should show updated available seats
        EventResponse updatedEvent = eventService.getEventById(eventId);
        assertThat(updatedEvent.getAvailableSeats()).isEqualTo(initialAvailableSeats - 2);
    }

    @Test
    @DisplayName("should evict event cache when ticket is cancelled")
    void shouldEvictEventCacheOnTicketCancellation() {
        // Given: Create a ticket and cache the event
        TicketRequest ticketRequest = TicketRequest.builder()
                .customerName("John Doe")
                .customerEmail("john@example.com")
                .numberOfSeats(2)
                .build();
        TicketResponse ticket = ticketService.createTicket(eventId, ticketRequest);

        // Cache the event after ticket creation
        eventService.getEventById(eventId);
        assertThat(cacheManager.getCache("events").get(eventId)).isNotNull();

        // When: Cancel the ticket
        ticketService.cancelTicket(ticket.getId());

        // Then: Event cache should be evicted
        assertThat(cacheManager.getCache("events").get(eventId)).isNull();
    }

    @Test
    @DisplayName("should restore available seats after ticket cancellation")
    void shouldRestoreAvailableSeatsAfterCancellation() {
        // Given: Create a ticket
        TicketRequest ticketRequest = TicketRequest.builder()
                .customerName("John Doe")
                .customerEmail("john@example.com")
                .numberOfSeats(2)
                .build();
        TicketResponse ticket = ticketService.createTicket(eventId, ticketRequest);

        EventResponse afterBooking = eventService.getEventById(eventId);
        int seatsAfterBooking = afterBooking.getAvailableSeats();

        // When: Cancel the ticket
        ticketService.cancelTicket(ticket.getId());

        // Then: Available seats should be restored
        EventResponse afterCancellation = eventService.getEventById(eventId);
        assertThat(afterCancellation.getAvailableSeats()).isEqualTo(seatsAfterBooking + 2);
    }

    @Test
    @DisplayName("should maintain cache consistency across multiple ticket operations")
    void shouldMaintainCacheConsistencyAcrossOperations() {
        // Given: Initial state
        EventResponse initial = eventService.getEventById(eventId);
        int initialSeats = initial.getAvailableSeats();

        // When: Create multiple tickets
        for (int i = 0; i < 3; i++) {
            TicketRequest ticketRequest = TicketRequest.builder()
                    .customerName("Customer " + i)
                    .customerEmail("customer" + i + "@example.com")
                    .numberOfSeats(1)
                    .build();
            ticketService.createTicket(eventId, ticketRequest);
        }

        // Then: Available seats should be correctly reduced
        EventResponse afterBookings = eventService.getEventById(eventId);
        assertThat(afterBookings.getAvailableSeats()).isEqualTo(initialSeats - 3);
    }
}
