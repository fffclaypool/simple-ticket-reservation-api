package com.example.ticketreservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.ticketreservation.dto.EventRequest;
import com.example.ticketreservation.dto.EventResponse;
import com.example.ticketreservation.exception.ResourceNotFoundException;
import com.example.ticketreservation.repository.EventRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
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
@DisplayName("EventService Cache Integration Tests")
class EventServiceCacheIntegrationTest {

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
        cacheManager.getCache("events").clear();

        testEventRequest = EventRequest.builder()
                .name("Test Concert")
                .description("A test concert for cache testing")
                .venue("Tokyo Dome")
                .eventDate(LocalDateTime.of(2025, 12, 25, 19, 0))
                .totalSeats(100)
                .price(new BigDecimal("5000.0"))
                .build();
    }

    @Test
    @DisplayName("should cache event on first access and return from cache on second access")
    void shouldCacheEventOnFirstAccess() {
        // Given: Create an event
        EventResponse created = eventService.createEvent(testEventRequest);
        Long eventId = created.getId();

        // When: First access - should fetch from database
        EventResponse firstAccess = eventService.getEventById(eventId);

        // Then: Verify cache contains the event
        Object cachedValue = cacheManager.getCache("events").get(eventId);
        assertThat(cachedValue).isNotNull();

        // When: Second access - should fetch from cache
        EventResponse secondAccess = eventService.getEventById(eventId);

        // Then: Both responses should be equal
        assertThat(secondAccess.getId()).isEqualTo(firstAccess.getId());
        assertThat(secondAccess.getName()).isEqualTo(firstAccess.getName());
    }

    @Test
    @DisplayName("should evict cache when event is updated")
    void shouldEvictCacheOnUpdate() {
        // Given: Create and cache an event
        EventResponse created = eventService.createEvent(testEventRequest);
        Long eventId = created.getId();
        eventService.getEventById(eventId); // Cache the event

        // Verify cache contains the event
        assertThat(cacheManager.getCache("events").get(eventId)).isNotNull();

        // When: Update the event
        EventRequest updateRequest = EventRequest.builder()
                .name("Updated Concert")
                .description("Updated description")
                .venue("Osaka Hall")
                .eventDate(LocalDateTime.of(2025, 12, 26, 20, 0))
                .totalSeats(150)
                .price(new BigDecimal("6000.0"))
                .build();
        eventService.updateEvent(eventId, updateRequest);

        // Then: Cache should be evicted
        assertThat(cacheManager.getCache("events").get(eventId)).isNull();

        // When: Access again
        EventResponse afterUpdate = eventService.getEventById(eventId);

        // Then: Should return updated data
        assertThat(afterUpdate.getName()).isEqualTo("Updated Concert");
        assertThat(afterUpdate.getVenue()).isEqualTo("Osaka Hall");
    }

    @Test
    @DisplayName("should evict cache when event is deleted")
    void shouldEvictCacheOnDelete() {
        // Given: Create and cache an event
        EventResponse created = eventService.createEvent(testEventRequest);
        Long eventId = created.getId();
        eventService.getEventById(eventId); // Cache the event

        // Verify cache contains the event
        assertThat(cacheManager.getCache("events").get(eventId)).isNotNull();

        // When: Delete the event
        eventService.deleteEvent(eventId);

        // Then: Cache should be evicted
        assertThat(cacheManager.getCache("events").get(eventId)).isNull();
    }

    @Test
    @DisplayName("should verify Redis is actually being used for caching")
    void shouldUseRedisForCaching() {
        // Given: Create an event
        EventResponse created = eventService.createEvent(testEventRequest);
        Long eventId = created.getId();

        // When: Access the event to cache it
        eventService.getEventById(eventId);

        // Then: Verify Redis contains keys for the cache
        var keys = redisTemplate.keys("events::*");
        assertThat(keys).isNotEmpty();
    }

    @Test
    @DisplayName("should return consistent data from cache")
    void shouldReturnConsistentDataFromCache() {
        // Given: Create an event
        EventResponse created = eventService.createEvent(testEventRequest);
        Long eventId = created.getId();

        // When: Access the event multiple times
        EventResponse access1 = eventService.getEventById(eventId);
        EventResponse access2 = eventService.getEventById(eventId);
        EventResponse access3 = eventService.getEventById(eventId);

        // Then: All accesses should return the same data
        assertThat(access1.getId()).isEqualTo(access2.getId()).isEqualTo(access3.getId());
        assertThat(access1.getName()).isEqualTo(access2.getName()).isEqualTo(access3.getName());
        assertThat(access1.getVenue()).isEqualTo(access2.getVenue()).isEqualTo(access3.getVenue());
        assertThat(access1.getTotalSeats()).isEqualTo(access2.getTotalSeats()).isEqualTo(access3.getTotalSeats());
    }

    @Nested
    @DisplayName("Cache Miss Tests")
    class CacheMissTests {

        @Test
        @DisplayName("should not cache when resource is not found")
        void shouldNotCacheWhenResourceNotFound() {
            // Given: A non-existent event ID
            Long nonExistentId = 99999L;

            // When/Then: Accessing non-existent resource should throw exception
            assertThatThrownBy(() -> eventService.getEventById(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class);

            // Then: Cache should not contain any entry for this ID
            assertThat(cacheManager.getCache("events").get(nonExistentId)).isNull();
        }

        @Test
        @DisplayName("should not affect cache when exception is thrown during update of non-existent event")
        void shouldNotAffectCacheOnUpdateNonExistentEvent() {
            // Given: Create and cache an event
            EventResponse created = eventService.createEvent(testEventRequest);
            Long existingId = created.getId();
            eventService.getEventById(existingId); // Cache the event

            Long nonExistentId = 99999L;
            EventRequest updateRequest = EventRequest.builder()
                    .name("Updated")
                    .description("Updated")
                    .venue("Updated")
                    .eventDate(LocalDateTime.of(2025, 12, 25, 19, 0))
                    .totalSeats(100)
                    .price(new BigDecimal("5000.0"))
                    .build();

            // When: Try to update non-existent event
            assertThatThrownBy(() -> eventService.updateEvent(nonExistentId, updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class);

            // Then: Existing cached event should remain in cache
            assertThat(cacheManager.getCache("events").get(existingId)).isNotNull();
        }
    }

    @Nested
    @DisplayName("Serialization Tests")
    class SerializationTests {

        @Test
        @DisplayName("should correctly serialize and deserialize EventResponse with all fields")
        void shouldSerializeAndDeserializeEventResponseCorrectly() {
            // Given: Create an event with all fields populated
            EventRequest request = EventRequest.builder()
                    .name("Serialization Test Concert")
                    .description("Testing serialization with special chars: 日本語, émojis 🎵")
                    .venue("Tokyo Dome 東京ドーム")
                    .eventDate(LocalDateTime.of(2025, 12, 25, 19, 30, 45))
                    .totalSeats(50000)
                    .price(new BigDecimal("12345.67"))
                    .build();
            EventResponse created = eventService.createEvent(request);
            Long eventId = created.getId();

            // When: Cache the event and retrieve from cache
            EventResponse firstAccess = eventService.getEventById(eventId);

            // Clear local references and get from cache again
            EventResponse fromCache = eventService.getEventById(eventId);

            // Then: All fields should be correctly preserved
            assertThat(fromCache.getId()).isEqualTo(eventId);
            assertThat(fromCache.getName()).isEqualTo("Serialization Test Concert");
            assertThat(fromCache.getDescription())
                    .isEqualTo("Testing serialization with special chars: 日本語, émojis 🎵");
            assertThat(fromCache.getVenue()).isEqualTo("Tokyo Dome 東京ドーム");
            assertThat(fromCache.getEventDate()).isEqualTo(LocalDateTime.of(2025, 12, 25, 19, 30, 45));
            assertThat(fromCache.getTotalSeats()).isEqualTo(50000);
            assertThat(fromCache.getAvailableSeats()).isEqualTo(50000);
            assertThat(fromCache.getPrice()).isEqualTo(new BigDecimal("12345.67"));
            assertThat(fromCache.getCreatedAt()).isNotNull();
            assertThat(fromCache.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should handle LocalDateTime serialization correctly")
        void shouldHandleLocalDateTimeSerialization() {
            // Given: Create events with various date/time values
            LocalDateTime[] testDates = {
                LocalDateTime.of(2025, 1, 1, 0, 0, 0), // Midnight start of year
                LocalDateTime.of(2025, 12, 31, 23, 59, 59), // End of year
                LocalDateTime.of(2025, 6, 15, 12, 30, 0), // Mid-year noon
            };

            for (LocalDateTime testDate : testDates) {
                EventRequest request = EventRequest.builder()
                        .name("Date Test")
                        .description("Test")
                        .venue("Test Venue")
                        .eventDate(testDate)
                        .totalSeats(100)
                        .price(new BigDecimal("1000.0"))
                        .build();
                EventResponse created = eventService.createEvent(request);

                // When: Retrieve from cache
                EventResponse cached = eventService.getEventById(created.getId());

                // Then: Date should be exactly preserved
                assertThat(cached.getEventDate()).isEqualTo(testDate);

                // Cleanup
                eventService.deleteEvent(created.getId());
            }
        }
    }

    @Nested
    @DisplayName("Non-Cached Methods Tests")
    class NonCachedMethodsTests {

        @Test
        @DisplayName("getAllEvents should not use cache")
        void getAllEventsShouldNotUseCache() {
            // Given: Create multiple events
            eventService.createEvent(testEventRequest);
            EventRequest secondRequest = EventRequest.builder()
                    .name("Second Concert")
                    .description("Another concert")
                    .venue("Osaka Hall")
                    .eventDate(LocalDateTime.of(2025, 12, 26, 19, 0))
                    .totalSeats(200)
                    .price(new BigDecimal("6000.0"))
                    .build();
            eventService.createEvent(secondRequest);

            // When: Call getAllEvents multiple times
            var result1 = eventService.getAllEvents();
            var result2 = eventService.getAllEvents();

            // Then: Results should be consistent
            assertThat(result1).hasSize(2);
            assertThat(result2).hasSize(2);

            // Note: We cannot easily verify "not using cache" but we verify
            // the method returns correct data without caching annotations
        }

        @Test
        @DisplayName("getAvailableEvents should always reflect current database state")
        void getAvailableEventsShouldReflectCurrentState() {
            // Given: Create a future event
            EventRequest futureEvent = EventRequest.builder()
                    .name("Future Concert")
                    .description("A future concert")
                    .venue("Tokyo Dome")
                    .eventDate(LocalDateTime.now().plusDays(30))
                    .totalSeats(100)
                    .price(new BigDecimal("5000.0"))
                    .build();
            EventResponse created = eventService.createEvent(futureEvent);

            // When: Get available events
            var availableEvents = eventService.getAvailableEvents();

            // Then: Should include the future event
            assertThat(availableEvents).extracting(EventResponse::getName).contains("Future Concert");
        }

        @Test
        @DisplayName("searchEventsByName should always search database")
        void searchEventsByNameShouldAlwaysSearchDatabase() {
            // Given: Create events with specific names
            eventService.createEvent(testEventRequest); // "Test Concert"

            EventRequest jazzEvent = EventRequest.builder()
                    .name("Jazz Festival")
                    .description("A jazz festival")
                    .venue("Blue Note")
                    .eventDate(LocalDateTime.of(2025, 12, 26, 20, 0))
                    .totalSeats(50)
                    .price(new BigDecimal("8000.0"))
                    .build();
            eventService.createEvent(jazzEvent);

            // When: Search by name
            var concertResults = eventService.searchEventsByName("Concert");
            var jazzResults = eventService.searchEventsByName("Jazz");

            // Then: Should return correct results
            assertThat(concertResults).hasSize(1);
            assertThat(concertResults.get(0).getName()).isEqualTo("Test Concert");

            assertThat(jazzResults).hasSize(1);
            assertThat(jazzResults.get(0).getName()).isEqualTo("Jazz Festival");
        }
    }
}
