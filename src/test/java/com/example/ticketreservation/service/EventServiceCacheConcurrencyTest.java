package com.example.ticketreservation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ticketreservation.dto.EventRequest;
import com.example.ticketreservation.dto.EventResponse;
import com.example.ticketreservation.repository.EventRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
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
@DisplayName("EventService Cache Concurrency Tests")
class EventServiceCacheConcurrencyTest {

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
        cacheManager.getCache("events").clear();

        testEventRequest = EventRequest.builder()
                .name("Test Concert")
                .description("A test concert for concurrency testing")
                .venue("Tokyo Dome")
                .eventDate(LocalDateTime.of(2025, 12, 25, 19, 0))
                .totalSeats(100)
                .price(5000.0)
                .build();
    }

    @Test
    @DisplayName("should handle concurrent reads safely")
    void shouldHandleConcurrentReadsSafely() throws Exception {
        EventResponse created = eventService.createEvent(testEventRequest);
        Long eventId = created.getId();

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Future<EventResponse>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    return eventService.getEventById(eventId);
                } finally {
                    doneLatch.countDown();
                }
            }));
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        List<EventResponse> results = new ArrayList<>();
        for (Future<EventResponse> future : futures) {
            results.add(future.get());
        }

        EventResponse first = results.get(0);
        for (EventResponse result : results) {
            assertThat(result.getId()).isEqualTo(first.getId());
            assertThat(result.getName()).isEqualTo(first.getName());
            assertThat(result.getAvailableSeats()).isEqualTo(first.getAvailableSeats());
        }

        assertThat(cacheManager.getCache("events").get(eventId)).isNotNull();
    }

    @Test
    @DisplayName("should handle concurrent read and write safely")
    void shouldHandleConcurrentReadAndWriteSafely() throws Exception {
        EventResponse created = eventService.createEvent(testEventRequest);
        Long eventId = created.getId();

        int readThreadCount = 5;
        int writeThreadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(readThreadCount + writeThreadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(readThreadCount + writeThreadCount);
        AtomicInteger updateCounter = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        // Read threads
        for (int i = 0; i < readThreadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 5; j++) {
                        eventService.getEventById(eventId);
                        Thread.sleep(10);
                    }
                    return null;
                } finally {
                    doneLatch.countDown();
                }
            }));
        }

        // Write threads
        for (int i = 0; i < writeThreadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    int count = updateCounter.incrementAndGet();
                    EventRequest updateRequest = createUpdateRequest("Updated Concert " + count);
                    eventService.updateEvent(eventId, updateRequest);
                    return null;
                } finally {
                    doneLatch.countDown();
                }
            }));
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        for (Future<?> future : futures) {
            future.get();
        }

        cacheManager.getCache("events").clear();
        EventResponse finalState = eventService.getEventById(eventId);
        assertThat(finalState).isNotNull();
        assertThat(finalState.getName()).startsWith("Updated Concert");
    }

    private EventRequest createUpdateRequest(String name) {
        return EventRequest.builder()
                .name(name)
                .description("Updated description")
                .venue("Updated Venue")
                .eventDate(LocalDateTime.of(2025, 12, 25, 19, 0))
                .totalSeats(100)
                .price(5000.0)
                .build();
    }
}
