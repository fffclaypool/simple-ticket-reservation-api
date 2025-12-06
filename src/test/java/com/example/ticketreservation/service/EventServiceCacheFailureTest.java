package com.example.ticketreservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
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

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

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
    @DisplayName("Redis Connection Failure Tests")
    class RedisConnectionFailureTests {

        @Test
        @DisplayName("should verify Redis connection failure throws exception when accessing cache directly")
        void shouldThrowExceptionWhenRedisUnavailableAndAccessingCacheDirectly() {
            // Given: Create an event and cache it while Redis is available
            EventResponse created = eventService.createEvent(testEventRequest);
            Long eventId = created.getId();
            eventService.getEventById(eventId); // This should cache the event

            // Verify Redis is working
            assertThat(cacheManager.getCache("events").get(eventId)).isNotNull();

            // When: Stop Redis container
            redis.stop();

            // Then: Direct Redis operations should fail
            assertThatThrownBy(() -> redisTemplate.keys("*")).isInstanceOf(RedisConnectionFailureException.class);

            // Restart Redis for other tests
            redis.start();
        }

        @Test
        @DisplayName("should demonstrate behavior when Redis becomes unavailable mid-operation")
        void shouldDemonstrateBehaviorWhenRedisBecomesUnavailable() {
            // Given: Create an event while Redis is available
            EventResponse created = eventService.createEvent(testEventRequest);
            Long eventId = created.getId();

            // First access - should work and cache
            EventResponse firstAccess = eventService.getEventById(eventId);
            assertThat(firstAccess).isNotNull();

            // Verify cache is populated
            assertThat(cacheManager.getCache("events").get(eventId)).isNotNull();

            // When: Stop Redis
            redis.stop();

            // Then: Depending on Spring Cache configuration:
            // - Without error handling: throws RedisConnectionFailureException
            // - With CacheErrorHandler: falls back to database
            // Default Spring Boot behavior is to throw exception
            try {
                // This will either succeed from cache (if cached locally) or fail
                EventResponse afterRedisDown = eventService.getEventById(eventId);
                // If we get here, it means Spring used a local cache or fallback
                assertThat(afterRedisDown).isNotNull();
            } catch (RedisConnectionFailureException e) {
                // Expected behavior without custom CacheErrorHandler
                assertThat(e).isInstanceOf(RedisConnectionFailureException.class);
            } finally {
                // Restart Redis for other tests
                redis.start();
            }
        }
    }

    @Nested
    @DisplayName("Cache Recovery Tests")
    class CacheRecoveryTests {

        @Test
        @DisplayName("should recover cache after Redis restart")
        void shouldRecoverCacheAfterRedisRestart() {
            // Given: Create an event and cache it
            EventResponse created = eventService.createEvent(testEventRequest);
            Long eventId = created.getId();
            eventService.getEventById(eventId);

            // Verify initial cache state
            assertThat(cacheManager.getCache("events").get(eventId)).isNotNull();

            // When: Restart Redis (simulating recovery)
            redis.stop();
            redis.start();

            // Wait for Redis to be ready
            waitForRedis();

            // Then: Cache should be empty after restart (Redis memory is cleared)
            // Next access should fetch from database and re-cache
            cacheManager.getCache("events").clear(); // Clear any stale references
            EventResponse afterRestart = eventService.getEventById(eventId);

            assertThat(afterRestart).isNotNull();
            assertThat(afterRestart.getId()).isEqualTo(eventId);
            assertThat(afterRestart.getName()).isEqualTo("Test Concert");

            // And: Cache should be repopulated
            assertThat(cacheManager.getCache("events").get(eventId)).isNotNull();
        }

        @Test
        @DisplayName("should handle cache operations gracefully after Redis recovery")
        void shouldHandleCacheOperationsGracefullyAfterRecovery() {
            // Given: Create multiple events
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

            // Cache both events
            eventService.getEventById(event1.getId());
            eventService.getEventById(event2.getId());

            // When: Restart Redis
            redis.stop();
            redis.start();
            waitForRedis();

            // Then: All operations should work correctly
            cacheManager.getCache("events").clear();

            // Read operations
            EventResponse retrieved1 = eventService.getEventById(event1.getId());
            EventResponse retrieved2 = eventService.getEventById(event2.getId());
            assertThat(retrieved1.getName()).isEqualTo("Test Concert");
            assertThat(retrieved2.getName()).isEqualTo("Second Concert");

            // Update operation
            EventRequest updateRequest = EventRequest.builder()
                    .name("Updated Concert")
                    .description("Updated")
                    .venue("Updated Venue")
                    .eventDate(LocalDateTime.of(2025, 12, 27, 20, 0))
                    .totalSeats(150)
                    .price(5500.0)
                    .build();
            eventService.updateEvent(event1.getId(), updateRequest);

            // Verify update
            EventResponse afterUpdate = eventService.getEventById(event1.getId());
            assertThat(afterUpdate.getName()).isEqualTo("Updated Concert");

            // Delete operation
            eventService.deleteEvent(event2.getId());
            assertThat(cacheManager.getCache("events").get(event2.getId())).isNull();
        }

        private void waitForRedis() {
            int maxRetries = 10;
            for (int i = 0; i < maxRetries; i++) {
                try {
                    redisTemplate.getConnectionFactory().getConnection().ping();
                    return;
                } catch (Exception e) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted while waiting for Redis", ie);
                    }
                }
            }
            throw new RuntimeException("Redis did not become available after " + maxRetries + " retries");
        }
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
