package com.example.ticketreservation.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ticketreservation.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

@WebMvcTest(EventController.class)
@DisplayName("EventController Validation Tests")
class EventControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Nested
    @DisplayName("POST /api/events validation")
    class CreateEventValidationTests {

        @Test
        @DisplayName("should return 400 when name is blank")
        void shouldReturn400WhenNameIsBlank() throws Exception {
            Map<String, Object> request = validEventRequest();
            request.put("name", "");

            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.errors", hasItem(containsString("name"))));
        }

        @Test
        @DisplayName("should return 400 when name is null")
        void shouldReturn400WhenNameIsNull() throws Exception {
            Map<String, Object> request = validEventRequest();
            request.remove("name");

            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem(containsString("name"))));
        }

        @Test
        @DisplayName("should return 400 when venue is blank")
        void shouldReturn400WhenVenueIsBlank() throws Exception {
            Map<String, Object> request = validEventRequest();
            request.put("venue", "");

            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem(containsString("venue"))));
        }

        @Test
        @DisplayName("should return 400 when eventDate is null")
        void shouldReturn400WhenEventDateIsNull() throws Exception {
            Map<String, Object> request = validEventRequest();
            request.remove("eventDate");

            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem(containsString("eventDate"))));
        }

        @Test
        @DisplayName("should return 400 when eventDate is in the past")
        void shouldReturn400WhenEventDateIsInPast() throws Exception {
            Map<String, Object> request = validEventRequest();
            request.put("eventDate", LocalDateTime.now().minusDays(1));

            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem(containsString("eventDate"))));
        }

        @Test
        @DisplayName("should return 400 when totalSeats is null")
        void shouldReturn400WhenTotalSeatsIsNull() throws Exception {
            Map<String, Object> request = validEventRequest();
            request.remove("totalSeats");

            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem(containsString("totalSeats"))));
        }

        @Test
        @DisplayName("should return 400 when totalSeats is zero or negative")
        void shouldReturn400WhenTotalSeatsIsZeroOrNegative() throws Exception {
            Map<String, Object> request = validEventRequest();
            request.put("totalSeats", 0);

            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem(containsString("totalSeats"))));
        }

        @Test
        @DisplayName("should return 400 when price is null")
        void shouldReturn400WhenPriceIsNull() throws Exception {
            Map<String, Object> request = validEventRequest();
            request.remove("price");

            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem(containsString("price"))));
        }

        @Test
        @DisplayName("should return 400 when price is zero or negative")
        void shouldReturn400WhenPriceIsZeroOrNegative() throws Exception {
            Map<String, Object> request = validEventRequest();
            request.put("price", new BigDecimal("0.00"));

            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem(containsString("price"))));
        }

        @Test
        @DisplayName("should return 400 with multiple errors when multiple fields are invalid")
        void shouldReturn400WithMultipleErrors() throws Exception {
            Map<String, Object> request = new HashMap<>();
            request.put("description", "Test");

            mockMvc.perform(post("/api/events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors").isArray());
        }
    }

    @Nested
    @DisplayName("PUT /api/events/{id} validation")
    class UpdateEventValidationTests {

        @Test
        @DisplayName("should return 400 when name is blank on update")
        void shouldReturn400WhenNameIsBlankOnUpdate() throws Exception {
            Map<String, Object> request = validEventRequest();
            request.put("name", "");

            mockMvc.perform(put("/api/events/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasItem(containsString("name"))));
        }
    }

    private Map<String, Object> validEventRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("name", "Test Concert");
        request.put("description", "A test concert");
        request.put("venue", "Tokyo Dome");
        request.put("eventDate", LocalDateTime.now().plusDays(30));
        request.put("totalSeats", 100);
        request.put("price", new BigDecimal("5000.00"));
        return request;
    }
}
