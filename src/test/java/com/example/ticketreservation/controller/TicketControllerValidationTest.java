package com.example.ticketreservation.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ticketreservation.service.TicketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
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
@DisplayName("TicketController Validation Tests")
class TicketControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TicketService ticketService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("POST /api/events/{eventId}/tickets validation")
    class CreateTicketValidationTests {

        @Test
        @DisplayName("should return 400 when customerName is blank")
        void shouldReturn400WhenCustomerNameIsBlank() throws Exception {
            Map<String, Object> request = validTicketRequest();
            request.put("customerName", "");

            mockMvc.perform(post("/api/events/1/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.errors", hasItem(containsString("customerName"))));
        }

        @Test
        @DisplayName("should return 400 when customerName is null")
        void shouldReturn400WhenCustomerNameIsNull() throws Exception {
            Map<String, Object> request = validTicketRequest();
            request.remove("customerName");

            mockMvc.perform(post("/api/events/1/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem(containsString("customerName"))));
        }

        @Test
        @DisplayName("should return 400 when customerEmail is blank")
        void shouldReturn400WhenCustomerEmailIsBlank() throws Exception {
            Map<String, Object> request = validTicketRequest();
            request.put("customerEmail", "");

            mockMvc.perform(post("/api/events/1/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem(containsString("customerEmail"))));
        }

        @Test
        @DisplayName("should return 400 when customerEmail is invalid format")
        void shouldReturn400WhenCustomerEmailIsInvalidFormat() throws Exception {
            Map<String, Object> request = validTicketRequest();
            request.put("customerEmail", "invalid-email");

            mockMvc.perform(post("/api/events/1/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem(containsString("customerEmail"))));
        }

        @Test
        @DisplayName("should return 400 when numberOfSeats is null")
        void shouldReturn400WhenNumberOfSeatsIsNull() throws Exception {
            Map<String, Object> request = validTicketRequest();
            request.remove("numberOfSeats");

            mockMvc.perform(post("/api/events/1/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem(containsString("numberOfSeats"))));
        }

        @Test
        @DisplayName("should return 400 when numberOfSeats is zero")
        void shouldReturn400WhenNumberOfSeatsIsZero() throws Exception {
            Map<String, Object> request = validTicketRequest();
            request.put("numberOfSeats", 0);

            mockMvc.perform(post("/api/events/1/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem(containsString("numberOfSeats"))));
        }

        @Test
        @DisplayName("should return 400 when numberOfSeats is negative")
        void shouldReturn400WhenNumberOfSeatsIsNegative() throws Exception {
            Map<String, Object> request = validTicketRequest();
            request.put("numberOfSeats", -1);

            mockMvc.perform(post("/api/events/1/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem(containsString("numberOfSeats"))));
        }

        @Test
        @DisplayName("should return 400 with multiple errors when multiple fields are invalid")
        void shouldReturn400WithMultipleErrors() throws Exception {
            Map<String, Object> request = new HashMap<>();

            mockMvc.perform(post("/api/events/1/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors").isArray());
        }
    }

    private Map<String, Object> validTicketRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("customerName", "Test User");
        request.put("customerEmail", "test@example.com");
        request.put("numberOfSeats", 2);
        return request;
    }
}
