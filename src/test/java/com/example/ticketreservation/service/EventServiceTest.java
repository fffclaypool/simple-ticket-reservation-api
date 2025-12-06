package com.example.ticketreservation.service;

import com.example.ticketreservation.dto.EventRequest;
import com.example.ticketreservation.dto.EventResponse;
import com.example.ticketreservation.entity.Event;
import com.example.ticketreservation.exception.ResourceNotFoundException;
import com.example.ticketreservation.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    private Event testEvent;
    private EventRequest testEventRequest;

    @BeforeEach
    void setUp() {
        testEvent = Event.builder()
                .id(1L)
                .name("Test Concert")
                .description("A test concert")
                .venue("Tokyo Dome")
                .eventDate(LocalDateTime.of(2025, 12, 25, 19, 0))
                .totalSeats(100)
                .availableSeats(100)
                .price(5000.0)
                .build();

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
    @DisplayName("getAllEvents")
    class GetAllEventsTests {

        @Test
        @DisplayName("should return all events")
        void shouldReturnAllEvents() {
            Event event2 = Event.builder()
                    .id(2L)
                    .name("Another Event")
                    .venue("Osaka Hall")
                    .eventDate(LocalDateTime.of(2025, 12, 31, 20, 0))
                    .totalSeats(50)
                    .availableSeats(50)
                    .price(3000.0)
                    .build();
            when(eventRepository.findAll()).thenReturn(Arrays.asList(testEvent, event2));

            List<EventResponse> result = eventService.getAllEvents();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Test Concert");
            assertThat(result.get(1).getName()).isEqualTo("Another Event");
        }

        @Test
        @DisplayName("should return empty list when no events exist")
        void shouldReturnEmptyListWhenNoEvents() {
            when(eventRepository.findAll()).thenReturn(List.of());

            List<EventResponse> result = eventService.getAllEvents();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getEventById")
    class GetEventByIdTests {

        @Test
        @DisplayName("should return event when found")
        void shouldReturnEventWhenFound() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

            EventResponse result = eventService.getEventById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Test Concert");
            assertThat(result.getVenue()).isEqualTo("Tokyo Dome");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when event not found")
        void shouldThrowExceptionWhenEventNotFound() {
            when(eventRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.getEventById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Event");
        }
    }

    @Nested
    @DisplayName("getAvailableEvents")
    class GetAvailableEventsTests {

        @Test
        @DisplayName("should return available events")
        void shouldReturnAvailableEvents() {
            when(eventRepository.findAvailableEvents(any(LocalDateTime.class)))
                    .thenReturn(List.of(testEvent));

            List<EventResponse> result = eventService.getAvailableEvents();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAvailableSeats()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("searchEventsByName")
    class SearchEventsByNameTests {

        @Test
        @DisplayName("should return events matching search term")
        void shouldReturnEventsMatchingSearchTerm() {
            when(eventRepository.findByNameContainingIgnoreCase("Concert"))
                    .thenReturn(List.of(testEvent));

            List<EventResponse> result = eventService.searchEventsByName("Concert");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).contains("Concert");
        }

        @Test
        @DisplayName("should return empty list when no match")
        void shouldReturnEmptyListWhenNoMatch() {
            when(eventRepository.findByNameContainingIgnoreCase("NonExistent"))
                    .thenReturn(List.of());

            List<EventResponse> result = eventService.searchEventsByName("NonExistent");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createEvent")
    class CreateEventTests {

        @Test
        @DisplayName("should create event successfully")
        void shouldCreateEventSuccessfully() {
            when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

            EventResponse result = eventService.createEvent(testEventRequest);

            assertThat(result.getName()).isEqualTo("Test Concert");
            assertThat(result.getTotalSeats()).isEqualTo(100);
            assertThat(result.getAvailableSeats()).isEqualTo(100);
            verify(eventRepository, times(1)).save(any(Event.class));
        }

        @Test
        @DisplayName("should set availableSeats equal to totalSeats on creation")
        void shouldSetAvailableSeatsEqualToTotalSeats() {
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
                Event saved = invocation.getArgument(0);
                assertThat(saved.getAvailableSeats()).isEqualTo(saved.getTotalSeats());
                return testEvent;
            });

            eventService.createEvent(testEventRequest);

            verify(eventRepository).save(any(Event.class));
        }
    }

    @Nested
    @DisplayName("updateEvent")
    class UpdateEventTests {

        @Test
        @DisplayName("should update event successfully")
        void shouldUpdateEventSuccessfully() {
            EventRequest updateRequest = EventRequest.builder()
                    .name("Updated Concert")
                    .description("Updated description")
                    .venue("Yokohama Arena")
                    .eventDate(LocalDateTime.of(2025, 12, 26, 20, 0))
                    .totalSeats(150)
                    .price(6000.0)
                    .build();

            Event updatedEvent = Event.builder()
                    .id(1L)
                    .name("Updated Concert")
                    .description("Updated description")
                    .venue("Yokohama Arena")
                    .eventDate(LocalDateTime.of(2025, 12, 26, 20, 0))
                    .totalSeats(150)
                    .availableSeats(150)
                    .price(6000.0)
                    .build();

            when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
            when(eventRepository.save(any(Event.class))).thenReturn(updatedEvent);

            EventResponse result = eventService.updateEvent(1L, updateRequest);

            assertThat(result.getName()).isEqualTo("Updated Concert");
            assertThat(result.getVenue()).isEqualTo("Yokohama Arena");
            assertThat(result.getTotalSeats()).isEqualTo(150);
        }

        @Test
        @DisplayName("should adjust availableSeats when totalSeats changes")
        void shouldAdjustAvailableSeatsWhenTotalSeatsChanges() {
            testEvent.setAvailableSeats(80);
            EventRequest updateRequest = EventRequest.builder()
                    .name("Test Concert")
                    .description("A test concert")
                    .venue("Tokyo Dome")
                    .eventDate(LocalDateTime.of(2025, 12, 25, 19, 0))
                    .totalSeats(120)
                    .price(5000.0)
                    .build();

            when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
                Event saved = invocation.getArgument(0);
                assertThat(saved.getAvailableSeats()).isEqualTo(100);
                return saved;
            });

            eventService.updateEvent(1L, updateRequest);

            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when event not found")
        void shouldThrowExceptionWhenEventNotFound() {
            when(eventRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.updateEvent(999L, testEventRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteEvent")
    class DeleteEventTests {

        @Test
        @DisplayName("should delete event successfully")
        void shouldDeleteEventSuccessfully() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
            doNothing().when(eventRepository).delete(testEvent);

            eventService.deleteEvent(1L);

            verify(eventRepository, times(1)).delete(testEvent);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when event not found")
        void shouldThrowExceptionWhenEventNotFound() {
            when(eventRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.deleteEvent(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
