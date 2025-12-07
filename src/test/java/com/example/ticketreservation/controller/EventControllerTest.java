package com.example.ticketreservation.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.ticketreservation.dto.EventRequest;
import com.example.ticketreservation.dto.EventResponse;
import com.example.ticketreservation.exception.ResourceNotFoundException;
import com.example.ticketreservation.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    private ObjectMapper objectMapper;
    private EventResponse testEventResponse;
    private EventRequest testEventRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        testEventResponse = EventResponse.builder()
                .id(1L)
                .name("Test Concert")
                .description("A test concert")
                .venue("Tokyo Dome")
                .eventDate(LocalDateTime.of(2025, 12, 25, 19, 0))
                .totalSeats(100)
                .availableSeats(100)
                .price(new BigDecimal("5000.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testEventRequest = EventRequest.builder()
                .name("Test Concert")
                .description("A test concert")
                .venue("Tokyo Dome")
                .eventDate(LocalDateTime.of(2025, 12, 25, 19, 0))
                .totalSeats(100)
                .price(new BigDecimal("5000.00"))
                .build();
    }

    @Nested
    @DisplayName("GET /api/events")
    class GetAllEventsTests {

        @Test
        @DisplayName("should return all events")
        void shouldReturnAllEvents() throws Exception {
            EventResponse event2 = EventResponse.builder()
                    .id(2L)
                    .name("Another Event")
                    .venue("Osaka Hall")
                    .eventDate(LocalDateTime.of(2025, 12, 31, 20, 0))
                    .totalSeats(50)
                    .availableSeats(50)
                    .price(new BigDecimal("3000.00"))
                    .build();
            when(eventService.getAllEvents()).thenReturn(List.of(testEventResponse, event2));

            mockMvc.perform(get("/api/events"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].name", is("Test Concert")))
                    .andExpect(jsonPath("$[1].name", is("Another Event")));
        }

        @Test
        @DisplayName("should return empty list when no events")
        void shouldReturnEmptyListWhenNoEvents() throws Exception {
            when(eventService.getAllEvents()).thenReturn(List.of());

            mockMvc.perform(get("/api/events")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/events/{id}")
    class GetEventByIdTests {

        @Test
        @DisplayName("should return event when found")
        void shouldReturnEventWhenFound() throws Exception {
            when(eventService.getEventById(1L)).thenReturn(testEventResponse);

            mockMvc.perform(get("/api/events/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Test Concert")))
                    .andExpect(jsonPath("$.venue", is("Tokyo Dome")))
                    .andExpect(jsonPath("$.totalSeats", is(100)))
                    .andExpect(jsonPath("$.price", is(5000.0)));
        }

        @Test
        @DisplayName("should return 404 when event not found")
        void shouldReturn404WhenEventNotFound() throws Exception {
            when(eventService.getEventById(999L)).thenThrow(new ResourceNotFoundException("Event", "id", 999L));

            mockMvc.perform(get("/api/events/999")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/events/available")
    class GetAvailableEventsTests {

        @Test
        @DisplayName("should return available events")
        void shouldReturnAvailableEvents() throws Exception {
            when(eventService.getAvailableEvents()).thenReturn(List.of(testEventResponse));

            mockMvc.perform(get("/api/events/available"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].availableSeats", greaterThan(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/events/search")
    class SearchEventsTests {

        @Test
        @DisplayName("should return events matching search term")
        void shouldReturnEventsMatchingSearchTerm() throws Exception {
            when(eventService.searchEventsByName("Concert")).thenReturn(List.of(testEventResponse));

            mockMvc.perform(get("/api/events/search").param("name", "Concert"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name", containsString("Concert")));
        }
    }

    @Nested
    @DisplayName("POST /api/events")
    class CreateEventTests {

        @Test
        @DisplayName("should create event and return 201")
        void shouldCreateEventAndReturn201() throws Exception {
            when(eventService.createEvent(any(EventRequest.class))).thenReturn(testEventResponse);

            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testEventRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Test Concert")));

            verify(eventService, times(1)).createEvent(any(EventRequest.class));
        }
    }

    @Nested
    @DisplayName("PUT /api/events/{id}")
    class UpdateEventTests {

        @Test
        @DisplayName("should update event successfully")
        void shouldUpdateEventSuccessfully() throws Exception {
            EventResponse updatedResponse = EventResponse.builder()
                    .id(1L)
                    .name("Updated Concert")
                    .description("Updated description")
                    .venue("Yokohama Arena")
                    .eventDate(LocalDateTime.of(2025, 12, 26, 20, 0))
                    .totalSeats(150)
                    .availableSeats(150)
                    .price(new BigDecimal("6000.00"))
                    .build();

            EventRequest updateRequest = EventRequest.builder()
                    .name("Updated Concert")
                    .description("Updated description")
                    .venue("Yokohama Arena")
                    .eventDate(LocalDateTime.of(2025, 12, 26, 20, 0))
                    .totalSeats(150)
                    .price(new BigDecimal("6000.00"))
                    .build();

            when(eventService.updateEvent(eq(1L), any(EventRequest.class))).thenReturn(updatedResponse);

            mockMvc.perform(put("/api/events/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Updated Concert")))
                    .andExpect(jsonPath("$.venue", is("Yokohama Arena")));
        }

        @Test
        @DisplayName("should return 404 when updating non-existent event")
        void shouldReturn404WhenUpdatingNonExistentEvent() throws Exception {
            when(eventService.updateEvent(eq(999L), any(EventRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Event", "id", 999L));

            mockMvc.perform(put("/api/events/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testEventRequest)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/events/{id}")
    class DeleteEventTests {

        @Test
        @DisplayName("should delete event and return 204")
        void shouldDeleteEventAndReturn204() throws Exception {
            doNothing().when(eventService).deleteEvent(1L);

            mockMvc.perform(delete("/api/events/1")).andExpect(status().isNoContent());

            verify(eventService, times(1)).deleteEvent(1L);
        }

        @Test
        @DisplayName("should return 404 when deleting non-existent event")
        void shouldReturn404WhenDeletingNonExistentEvent() throws Exception {
            doThrow(new ResourceNotFoundException("Event", "id", 999L))
                    .when(eventService)
                    .deleteEvent(999L);

            mockMvc.perform(delete("/api/events/999")).andExpect(status().isNotFound());
        }
    }
}
