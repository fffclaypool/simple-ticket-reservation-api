package com.example.ticketreservation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ticketreservation.dto.EventRequest;
import com.example.ticketreservation.dto.EventResponse;
import com.example.ticketreservation.repository.EventRepository;
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
@DisplayName("EventService Cache Failure Tests")
class EventServiceCacheFailureTest {

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
    private EventRepository eventRepository;

    @Autowired
    private CacheManager cacheManager;

    private EventRequest testEventRequest;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        try {
            cacheManager.getCache("events").clear();
        } catch (Exception e) {
            // Ignore if Redis is not available
        }

        testEventRequest = EventRequest.builder()
                .name("Test Concert")
                .description("A test concert")
                .venue("Tokyo Dome")
                .eventDate(LocalDateTime.of(2025, 12, 25, 19, 0))
                .totalSeats(100)
                .price(5000.0)
                .build();
    }

    @Nested
    @DisplayName("Cache Clear Tests")
    class CacheClearTests {

        @Test
        @DisplayName("should handle full cache clear correctly")
        void shouldHandleFullCacheClearCorrectly() {
            // Given: Create and cache multiple events
            EventResponse event1 = eventService.createEvent(testEventRequest);
            EventRequest secondRequest = EventRequest.builder()
                    .name("Second Concert")
                    .description("Another concert")
                    .venue("Osaka Hall")
                    .eventDate(LocalDateTime.of(2025, 12, 26, 19, 0))
                    .totalSeats(200)
                    .price(6000.0)
                    .build();
            EventResponse event2 = eventService.createEvent(secondRequest);

            eventService.getEventById(event1.getId());
            eventService.getEventById(event2.getId());

            // Verify both are cached
            assertThat(cacheManager.getCache("events").get(event1.getId())).isNotNull();
            assertThat(cacheManager.getCache("events").get(event2.getId())).isNotNull();

            // When: Clear entire cache
            cacheManager.getCache("events").clear();

            // Then: Both entries should be gone
            assertThat(cacheManager.getCache("events").get(event1.getId())).isNull();
            assertThat(cacheManager.getCache("events").get(event2.getId())).isNull();

            // And: Next access should repopulate from database
            EventResponse refetched1 = eventService.getEventById(event1.getId());
            EventResponse refetched2 = eventService.getEventById(event2.getId());

            assertThat(refetched1.getName()).isEqualTo("Test Concert");
            assertThat(refetched2.getName()).isEqualTo("Second Concert");

            // And: Cache should be repopulated
            assertThat(cacheManager.getCache("events").get(event1.getId())).isNotNull();
            assertThat(cacheManager.getCache("events").get(event2.getId())).isNotNull();
        }

        @Test
        @DisplayName("should isolate cache entries by key")
        void shouldIsolateCacheEntriesByKey() {
            // Given: Create two events
            EventResponse event1 = eventService.createEvent(testEventRequest);
            EventRequest secondRequest = EventRequest.builder()
                    .name("Second Concert")
                    .description("Another concert")
                    .venue("Osaka Hall")
                    .eventDate(LocalDateTime.of(2025, 12, 26, 19, 0))
                    .totalSeats(200)
                    .price(6000.0)
                    .build();
            EventResponse event2 = eventService.createEvent(secondRequest);

            // Cache both
            eventService.getEventById(event1.getId());
            eventService.getEventById(event2.getId());

            // When: Evict only event1 via update
            EventRequest updateRequest = EventRequest.builder()
                    .name("Updated Concert")
                    .description("Updated")
                    .venue("Updated Venue")
                    .eventDate(LocalDateTime.of(2025, 12, 27, 20, 0))
                    .totalSeats(150)
                    .price(5500.0)
                    .build();
            eventService.updateEvent(event1.getId(), updateRequest);

            // Then: Only event1 cache should be evicted
            assertThat(cacheManager.getCache("events").get(event1.getId())).isNull();
            assertThat(cacheManager.getCache("events").get(event2.getId())).isNotNull();
        }
    }
}
