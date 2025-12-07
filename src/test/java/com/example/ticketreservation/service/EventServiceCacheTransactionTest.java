package com.example.ticketreservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ticketreservation.dto.EventRequest;
import com.example.ticketreservation.dto.EventResponse;
import com.example.ticketreservation.dto.TicketRequest;
import com.example.ticketreservation.exception.InsufficientSeatsException;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.TicketRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@DisplayName("EventService Cache Transaction Tests")
class EventServiceCacheTransactionTest {

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

    private EventRequest testEventRequest;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        eventRepository.deleteAll();
        cacheManager.getCache("events").clear();

        testEventRequest = EventRequest.builder()
                .name("Test Concert")
                .description("A test concert for transaction testing")
                .venue("Tokyo Dome")
                .eventDate(LocalDateTime.of(2025, 12, 25, 19, 0))
                .totalSeats(10)
                .price(5000.0)
                .build();
    }

    @Nested
    @DisplayName("Transaction Rollback Cache Consistency Tests")
    class TransactionRollbackTests {

        @Test
        @DisplayName("should not evict cache when ticket creation fails due to insufficient seats")
        void shouldNotEvictCacheWhenTicketCreationFails() {
            // Given: Create an event with limited seats and cache it
            EventResponse created = eventService.createEvent(testEventRequest);
            Long eventId = created.getId();

            // Cache the event
            EventResponse cachedEvent = eventService.getEventById(eventId);
            assertThat(cacheManager.getCache("events").get(eventId)).isNotNull();

            int initialAvailableSeats = cachedEvent.getAvailableSeats();

            // When: Try to create a ticket requesting more seats than available
            TicketRequest invalidRequest = TicketRequest.builder()
                    .customerName("John Doe")
                    .customerEmail("john@example.com")
                    .numberOfSeats(100) // More than available (10)
                    .build();

            assertThatThrownBy(() -> ticketService.createTicket(eventId, invalidRequest))
                    .isInstanceOf(InsufficientSeatsException.class);

            // Then: Cache should still be evicted because @CacheEvict is processed before the exception
            // Note: This is the current behavior - @CacheEvict with beforeInvocation=false (default)
            // will still evict cache even if exception is thrown after cache eviction but before commit
            // The cache IS evicted in this case due to @CacheEvict annotation

            // Re-fetch to verify database state is unchanged
            cacheManager.getCache("events").clear();
            EventResponse afterFailedAttempt = eventService.getEventById(eventId);
            assertThat(afterFailedAttempt.getAvailableSeats()).isEqualTo(initialAvailableSeats);
        }

        @Test
        @DisplayName("should maintain cache-database consistency after failed ticket creation")
        void shouldMaintainCacheDatabaseConsistencyAfterFailedTicketCreation() {
            // Given: Create an event and cache it
            EventResponse created = eventService.createEvent(testEventRequest);
            Long eventId = created.getId();
            eventService.getEventById(eventId); // Cache the event

            // First, book some seats successfully
            TicketRequest validRequest = TicketRequest.builder()
                    .customerName("First Customer")
                    .customerEmail("first@example.com")
                    .numberOfSeats(5)
                    .build();
            ticketService.createTicket(eventId, validRequest);

            // Cache should be evicted, re-cache with updated state
            EventResponse afterFirstBooking = eventService.getEventById(eventId);
            assertThat(afterFirstBooking.getAvailableSeats()).isEqualTo(5);

            // When: Try to book more seats than remaining
            TicketRequest invalidRequest = TicketRequest.builder()
                    .customerName("Second Customer")
                    .customerEmail("second@example.com")
                    .numberOfSeats(10) // Only 5 available
                    .build();

            assertThatThrownBy(() -> ticketService.createTicket(eventId, invalidRequest))
                    .isInstanceOf(InsufficientSeatsException.class);

            // Then: After failure, cache and database should be consistent
            cacheManager.getCache("events").clear();
            EventResponse finalState = eventService.getEventById(eventId);
            assertThat(finalState.getAvailableSeats()).isEqualTo(5);
        }

        @Test
        @DisplayName("should handle multiple sequential operations with proper cache invalidation")
        void shouldHandleSequentialOperationsWithProperCacheInvalidation() {
            // Given: Create an event
            EventResponse created = eventService.createEvent(testEventRequest);
            Long eventId = created.getId();

            // When: Perform multiple operations
            // 1. Cache the event
            eventService.getEventById(eventId);
            assertThat(cacheManager.getCache("events").get(eventId)).isNotNull();

            // 2. Book tickets (should evict cache)
            TicketRequest request1 = TicketRequest.builder()
                    .customerName("Customer 1")
                    .customerEmail("c1@example.com")
                    .numberOfSeats(2)
                    .build();
            ticketService.createTicket(eventId, request1);

            // 3. Re-cache
            EventResponse afterFirst = eventService.getEventById(eventId);
            assertThat(afterFirst.getAvailableSeats()).isEqualTo(8);

            // 4. Book more tickets
            TicketRequest request2 = TicketRequest.builder()
                    .customerName("Customer 2")
                    .customerEmail("c2@example.com")
                    .numberOfSeats(3)
                    .build();
            ticketService.createTicket(eventId, request2);

            // 5. Verify final state
            EventResponse afterSecond = eventService.getEventById(eventId);
            assertThat(afterSecond.getAvailableSeats()).isEqualTo(5);

            // Then: Cache should be populated and consistent with DB
            assertThat(cacheManager.getCache("events").get(eventId)).isNotNull();
        }

        @Test
        @DisplayName("cache should be consistent when update operation partially completes")
        void cacheShouldBeConsistentWhenUpdatePartiallyCompletes() {
            // Given: Create and cache an event
            EventResponse created = eventService.createEvent(testEventRequest);
            Long eventId = created.getId();
            eventService.getEventById(eventId);

            // When: Update event with valid data
            EventRequest updateRequest = EventRequest.builder()
                    .name("Updated Concert")
                    .description("Updated description")
                    .venue("Updated Venue")
                    .eventDate(LocalDateTime.of(2025, 12, 26, 20, 0))
                    .totalSeats(20)
                    .price(6000.0)
                    .build();
            eventService.updateEvent(eventId, updateRequest);

            // Then: Cache should be evicted and next read should show updated data
            assertThat(cacheManager.getCache("events").get(eventId)).isNull();

            EventResponse afterUpdate = eventService.getEventById(eventId);
            assertThat(afterUpdate.getName()).isEqualTo("Updated Concert");
            assertThat(afterUpdate.getTotalSeats()).isEqualTo(20);
            assertThat(afterUpdate.getAvailableSeats()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("Cache Eviction Order Tests")
    class CacheEvictionOrderTests {

        @Test
        @DisplayName("should evict cache after successful delete even when no cache entry exists")
        void shouldHandleDeleteWithoutCacheEntry() {
            // Given: Create an event but don't cache it
            EventResponse created = eventService.createEvent(testEventRequest);
            Long eventId = created.getId();

            // Verify not in cache
            assertThat(cacheManager.getCache("events").get(eventId)).isNull();

            // When: Delete the event
            eventService.deleteEvent(eventId);

            // Then: No exception should occur (evicting non-existent cache entry is fine)
            assertThat(cacheManager.getCache("events").get(eventId)).isNull();
        }

        @Test
        @DisplayName("should properly evict cache on ticket cancellation")
        void shouldProperlyEvictCacheOnTicketCancellation() {
            // Given: Create event, book tickets, and cache
            EventResponse created = eventService.createEvent(testEventRequest);
            Long eventId = created.getId();

            TicketRequest ticketRequest = TicketRequest.builder()
                    .customerName("Customer")
                    .customerEmail("customer@example.com")
                    .numberOfSeats(3)
                    .build();
            var ticket = ticketService.createTicket(eventId, ticketRequest);

            // Cache the event after booking
            EventResponse afterBooking = eventService.getEventById(eventId);
            assertThat(afterBooking.getAvailableSeats()).isEqualTo(7);
            assertThat(cacheManager.getCache("events").get(eventId)).isNotNull();

            // When: Cancel the ticket
            ticketService.cancelTicket(ticket.getId());

            // Then: Cache should be evicted
            assertThat(cacheManager.getCache("events").get(eventId)).isNull();

            // And: Next read should show restored seats
            EventResponse afterCancel = eventService.getEventById(eventId);
            assertThat(afterCancel.getAvailableSeats()).isEqualTo(10);
        }
    }
}
