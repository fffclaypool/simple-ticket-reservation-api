package com.example.ticketreservation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ticketreservation.dto.EventRequest;
import com.example.ticketreservation.dto.EventResponse;
import com.example.ticketreservation.repository.EventRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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

        submitReadTasks(futures, executor, startLatch, doneLatch, eventId, readThreadCount);
        submitWriteTasks(futures, executor, startLatch, doneLatch, eventId, updateCounter, writeThreadCount);

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        int[] results = countSuccessesAndFailures(futures);
        assertThat(results[0]).isGreaterThan(0);
        assertThat(results[0] + results[1]).isEqualTo(readThreadCount + writeThreadCount);

        cacheManager.getCache("events").clear();
        EventResponse finalState = eventService.getEventById(eventId);
        assertThat(finalState).isNotNull();
        assertThat(finalState.getName()).startsWith("Updated Concert");
    }

    private void submitReadTasks(
            List<Future<?>> futures,
            ExecutorService executor,
            CountDownLatch startLatch,
            CountDownLatch doneLatch,
            Long eventId,
            int count) {
        for (int i = 0; i < count; i++) {
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
    }

    private void submitWriteTasks(
            List<Future<?>> futures,
            ExecutorService executor,
            CountDownLatch startLatch,
            CountDownLatch doneLatch,
            Long eventId,
            AtomicInteger updateCounter,
            int count) {
        for (int i = 0; i < count; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    int num = updateCounter.incrementAndGet();
                    EventRequest updateRequest = createUpdateRequest("Updated Concert " + num);
                    eventService.updateEvent(eventId, updateRequest);
                    return null;
                } finally {
                    doneLatch.countDown();
                }
            }));
        }
    }

    private int[] countSuccessesAndFailures(List<Future<?>> futures) throws Exception {
        int successCount = 0;
        int optimisticLockFailures = 0;
        for (Future<?> future : futures) {
            try {
                future.get();
                successCount++;
            } catch (ExecutionException e) {
                if (e.getCause() instanceof ObjectOptimisticLockingFailureException) {
                    optimisticLockFailures++;
                } else {
                    throw e;
                }
            }
        }
        return new int[] {successCount, optimisticLockFailures};
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
