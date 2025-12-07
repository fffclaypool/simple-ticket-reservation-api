package com.example.ticketreservation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.ticketreservation.dto.TicketRequest;
import com.example.ticketreservation.dto.TicketResponse;
import com.example.ticketreservation.entity.TicketStatus;
import com.example.ticketreservation.exception.InsufficientSeatsException;
import com.example.ticketreservation.exception.ResourceNotFoundException;
import com.example.ticketreservation.service.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TicketService ticketService;

    private TicketResponse testResponse;
    private TicketRequest testRequest;

    @BeforeEach
    void setUp() {
        testResponse = TicketResponse.builder()
                .id(1L)
                .ticketCode("TKT-12345678")
                .eventId(1L)
                .eventName("Test Event")
                .customerName("John Doe")
                .customerEmail("john@example.com")
                .numberOfSeats(2)
                .totalAmount(new BigDecimal("2000.0"))
                .status(TicketStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testRequest = TicketRequest.builder()
                .customerName("John Doe")
                .customerEmail("john@example.com")
                .numberOfSeats(2)
                .build();
    }

    @Nested
    @DisplayName("GET /api/tickets")
    class GetAllTicketsTests {

        @Test
        @DisplayName("should return all tickets")
        void shouldReturnAllTickets() throws Exception {
            when(ticketService.getAllTickets()).thenReturn(List.of(testResponse));

            mockMvc.perform(get("/api/tickets"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(1))
                    .andExpect(jsonPath("$[0].ticketCode").value("TKT-12345678"));
        }

        @Test
        @DisplayName("should return empty list when no tickets")
        void shouldReturnEmptyList() throws Exception {
            when(ticketService.getAllTickets()).thenReturn(List.of());

            mockMvc.perform(get("/api/tickets"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/tickets/{id}")
    class GetTicketByIdTests {

        @Test
        @DisplayName("should return ticket when found")
        void shouldReturnTicketWhenFound() throws Exception {
            when(ticketService.getTicketById(1L)).thenReturn(testResponse);

            mockMvc.perform(get("/api/tickets/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.customerName").value("John Doe"));
        }

        @Test
        @DisplayName("should return 404 when not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(ticketService.getTicketById(999L)).thenThrow(new ResourceNotFoundException("Ticket", "id", 999L));

            mockMvc.perform(get("/api/tickets/999")).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/tickets/code/{code}")
    class GetTicketByCodeTests {

        @Test
        @DisplayName("should return ticket when found by code")
        void shouldReturnTicketWhenFoundByCode() throws Exception {
            when(ticketService.getTicketByCode("TKT-12345678")).thenReturn(testResponse);

            mockMvc.perform(get("/api/tickets/code/TKT-12345678"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ticketCode").value("TKT-12345678"));
        }
    }

    @Nested
    @DisplayName("GET /api/tickets/email/{email}")
    class GetTicketsByEmailTests {

        @Test
        @DisplayName("should return tickets for email")
        void shouldReturnTicketsForEmail() throws Exception {
            when(ticketService.getTicketsByEmail("john@example.com")).thenReturn(List.of(testResponse));

            mockMvc.perform(get("/api/tickets/email/john@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].customerEmail").value("john@example.com"));
        }
    }

    @Nested
    @DisplayName("GET /api/events/{eventId}/tickets")
    class GetTicketsByEventIdTests {

        @Test
        @DisplayName("should return tickets for event")
        void shouldReturnTicketsForEvent() throws Exception {
            when(ticketService.getTicketsByEventId(1L)).thenReturn(List.of(testResponse));

            mockMvc.perform(get("/api/events/1/tickets"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].eventId").value(1));
        }
    }

    @Nested
    @DisplayName("POST /api/events/{eventId}/tickets")
    class CreateTicketTests {

        @Test
        @DisplayName("should create ticket successfully")
        void shouldCreateTicketSuccessfully() throws Exception {
            when(ticketService.createTicket(eq(1L), any(TicketRequest.class))).thenReturn(testResponse);

            mockMvc.perform(post("/api/events/1/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.ticketCode").value("TKT-12345678"));
        }

        @Test
        @DisplayName("should return 404 when event not found")
        void shouldReturn404WhenEventNotFound() throws Exception {
            when(ticketService.createTicket(eq(999L), any(TicketRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Event", "id", 999L));

            mockMvc.perform(post("/api/events/999/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 400 when insufficient seats")
        void shouldReturn400WhenInsufficientSeats() throws Exception {
            when(ticketService.createTicket(eq(1L), any(TicketRequest.class)))
                    .thenThrow(new InsufficientSeatsException(10, 5));

            mockMvc.perform(post("/api/events/1/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /api/tickets/{id}/cancel")
    class CancelTicketTests {

        @Test
        @DisplayName("should cancel ticket successfully")
        void shouldCancelTicketSuccessfully() throws Exception {
            TicketResponse cancelledResponse = TicketResponse.builder()
                    .id(testResponse.getId())
                    .ticketCode(testResponse.getTicketCode())
                    .eventId(testResponse.getEventId())
                    .eventName(testResponse.getEventName())
                    .customerName(testResponse.getCustomerName())
                    .customerEmail(testResponse.getCustomerEmail())
                    .numberOfSeats(testResponse.getNumberOfSeats())
                    .totalAmount(testResponse.getTotalAmount())
                    .status(TicketStatus.CANCELLED)
                    .createdAt(testResponse.getCreatedAt())
                    .updatedAt(testResponse.getUpdatedAt())
                    .build();
            when(ticketService.cancelTicket(1L)).thenReturn(cancelledResponse);

            mockMvc.perform(patch("/api/tickets/1/cancel"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("should return 404 when ticket not found")
        void shouldReturn404WhenTicketNotFound() throws Exception {
            when(ticketService.cancelTicket(999L)).thenThrow(new ResourceNotFoundException("Ticket", "id", 999L));

            mockMvc.perform(patch("/api/tickets/999/cancel")).andExpect(status().isNotFound());
        }
    }
}
