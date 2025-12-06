package com.example.ticketreservation.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.ticketreservation.dto.ReservationRequest;
import com.example.ticketreservation.dto.ReservationResponse;
import com.example.ticketreservation.entity.ReservationStatus;
import com.example.ticketreservation.exception.InsufficientSeatsException;
import com.example.ticketreservation.exception.ResourceNotFoundException;
import com.example.ticketreservation.service.ReservationService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@WebMvcTest(ReservationController.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReservationService reservationService;

    private ObjectMapper objectMapper;
    private ReservationResponse testReservationResponse;
    private ReservationRequest testReservationRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        testReservationResponse = ReservationResponse.builder()
                .id(1L)
                .reservationCode("RES-12345678")
                .eventId(1L)
                .eventName("Test Concert")
                .customerName("Test User")
                .customerEmail("test@example.com")
                .numberOfSeats(2)
                .totalAmount(10000.0)
                .status(ReservationStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testReservationRequest = ReservationRequest.builder()
                .eventId(1L)
                .customerName("Test User")
                .customerEmail("test@example.com")
                .numberOfSeats(2)
                .build();
    }

    @Nested
    @DisplayName("GET /api/reservations")
    class GetAllReservationsTests {

        @Test
        @DisplayName("should return all reservations")
        void shouldReturnAllReservations() throws Exception {
            ReservationResponse reservation2 = ReservationResponse.builder()
                    .id(2L)
                    .reservationCode("RES-87654321")
                    .eventId(1L)
                    .eventName("Test Concert")
                    .customerName("Another User")
                    .customerEmail("another@example.com")
                    .numberOfSeats(3)
                    .totalAmount(15000.0)
                    .status(ReservationStatus.CONFIRMED)
                    .build();
            when(reservationService.getAllReservations()).thenReturn(List.of(testReservationResponse, reservation2));

            mockMvc.perform(get("/api/reservations"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].customerName", is("Test User")))
                    .andExpect(jsonPath("$[1].customerName", is("Another User")));
        }

        @Test
        @DisplayName("should return empty list when no reservations")
        void shouldReturnEmptyListWhenNoReservations() throws Exception {
            when(reservationService.getAllReservations()).thenReturn(List.of());

            mockMvc.perform(get("/api/reservations")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/reservations/{id}")
    class GetReservationByIdTests {

        @Test
        @DisplayName("should return reservation when found")
        void shouldReturnReservationWhenFound() throws Exception {
            when(reservationService.getReservationById(1L)).thenReturn(testReservationResponse);

            mockMvc.perform(get("/api/reservations/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.reservationCode", is("RES-12345678")))
                    .andExpect(jsonPath("$.customerName", is("Test User")))
                    .andExpect(jsonPath("$.numberOfSeats", is(2)))
                    .andExpect(jsonPath("$.totalAmount", is(10000.0)));
        }

        @Test
        @DisplayName("should return 404 when reservation not found")
        void shouldReturn404WhenReservationNotFound() throws Exception {
            when(reservationService.getReservationById(999L))
                    .thenThrow(new ResourceNotFoundException("Reservation", "id", 999L));

            mockMvc.perform(get("/api/reservations/999")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/reservations/code/{code}")
    class GetReservationByCodeTests {

        @Test
        @DisplayName("should return reservation when code found")
        void shouldReturnReservationWhenCodeFound() throws Exception {
            when(reservationService.getReservationByCode("RES-12345678")).thenReturn(testReservationResponse);

            mockMvc.perform(get("/api/reservations/code/RES-12345678"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reservationCode", is("RES-12345678")));
        }

        @Test
        @DisplayName("should return 404 when code not found")
        void shouldReturn404WhenCodeNotFound() throws Exception {
            when(reservationService.getReservationByCode("INVALID"))
                    .thenThrow(new ResourceNotFoundException("Reservation", "code", "INVALID"));

            mockMvc.perform(get("/api/reservations/code/INVALID")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/reservations/email/{email}")
    class GetReservationsByEmailTests {

        @Test
        @DisplayName("should return reservations for email")
        void shouldReturnReservationsForEmail() throws Exception {
            when(reservationService.getReservationsByEmail("test@example.com"))
                    .thenReturn(List.of(testReservationResponse));

            mockMvc.perform(get("/api/reservations/email/test@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].customerEmail", is("test@example.com")));
        }

        @Test
        @DisplayName("should return empty list for unknown email")
        void shouldReturnEmptyListForUnknownEmail() throws Exception {
            when(reservationService.getReservationsByEmail("unknown@example.com"))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/reservations/email/unknown@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/reservations/event/{eventId}")
    class GetReservationsByEventIdTests {

        @Test
        @DisplayName("should return reservations for event")
        void shouldReturnReservationsForEvent() throws Exception {
            when(reservationService.getReservationsByEventId(1L)).thenReturn(List.of(testReservationResponse));

            mockMvc.perform(get("/api/reservations/event/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].eventId", is(1)));
        }
    }

    @Nested
    @DisplayName("POST /api/reservations")
    class CreateReservationTests {

        @Test
        @DisplayName("should create reservation and return 201")
        void shouldCreateReservationAndReturn201() throws Exception {
            when(reservationService.createReservation(any(ReservationRequest.class)))
                    .thenReturn(testReservationResponse);

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testReservationRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.reservationCode", org.hamcrest.Matchers.startsWith("RES-")))
                    .andExpect(jsonPath("$.status", is("CONFIRMED")));

            verify(reservationService, times(1)).createReservation(any(ReservationRequest.class));
        }

        @Test
        @DisplayName("should return 404 when event not found")
        void shouldReturn404WhenEventNotFound() throws Exception {
            when(reservationService.createReservation(any(ReservationRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Event", "id", 999L));

            ReservationRequest invalidRequest = ReservationRequest.builder()
                    .eventId(999L)
                    .customerName("Test User")
                    .customerEmail("test@example.com")
                    .numberOfSeats(2)
                    .build();

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when insufficient seats")
        void shouldReturn400WhenInsufficientSeats() throws Exception {
            when(reservationService.createReservation(any(ReservationRequest.class)))
                    .thenThrow(new InsufficientSeatsException(10, 5));

            ReservationRequest tooManySeatsRequest = ReservationRequest.builder()
                    .eventId(1L)
                    .customerName("Test User")
                    .customerEmail("test@example.com")
                    .numberOfSeats(10)
                    .build();

            mockMvc.perform(post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(tooManySeatsRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /api/reservations/{id}/cancel")
    class CancelReservationTests {

        @Test
        @DisplayName("should cancel reservation successfully")
        void shouldCancelReservationSuccessfully() throws Exception {
            ReservationResponse cancelledResponse = ReservationResponse.builder()
                    .id(1L)
                    .reservationCode("RES-12345678")
                    .eventId(1L)
                    .eventName("Test Concert")
                    .customerName("Test User")
                    .customerEmail("test@example.com")
                    .numberOfSeats(2)
                    .totalAmount(10000.0)
                    .status(ReservationStatus.CANCELLED)
                    .build();

            when(reservationService.cancelReservation(1L)).thenReturn(cancelledResponse);

            mockMvc.perform(patch("/api/reservations/1/cancel"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("CANCELLED")));
        }

        @Test
        @DisplayName("should return 404 when reservation not found")
        void shouldReturn404WhenReservationNotFound() throws Exception {
            when(reservationService.cancelReservation(999L))
                    .thenThrow(new ResourceNotFoundException("Reservation", "id", 999L));

            mockMvc.perform(patch("/api/reservations/999/cancel")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when already cancelled")
        void shouldReturn400WhenAlreadyCancelled() throws Exception {
            when(reservationService.cancelReservation(1L))
                    .thenThrow(new IllegalStateException("Reservation is already cancelled"));

            mockMvc.perform(patch("/api/reservations/1/cancel")).andExpect(status().isBadRequest());
        }
    }
}
