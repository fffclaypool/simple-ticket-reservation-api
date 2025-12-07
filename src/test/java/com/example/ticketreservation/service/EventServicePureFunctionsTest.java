package com.example.ticketreservation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EventService Pure Functions Tests")
class EventServicePureFunctionsTest {

    @Nested
    @DisplayName("calculateNewAvailableSeats")
    class CalculateNewAvailableSeatsTests {

        @Test
        @DisplayName("should return same available seats when total unchanged")
        void shouldReturnSameWhenTotalUnchanged() {
            int result = EventService.calculateNewAvailableSeats(50, 100, 100);
            assertThat(result).isEqualTo(50);
        }

        @Test
        @DisplayName("should increase available seats when total increased")
        void shouldIncreaseWhenTotalIncreased() {
            int result = EventService.calculateNewAvailableSeats(50, 100, 150);
            assertThat(result).isEqualTo(100);
        }

        @Test
        @DisplayName("should decrease available seats when total decreased within limit")
        void shouldDecreaseWhenTotalDecreasedWithinLimit() {
            // 100 total, 50 available = 50 sold
            // Reducing to 80 total is valid (50 sold + 30 available)
            int result = EventService.calculateNewAvailableSeats(50, 100, 80);
            assertThat(result).isEqualTo(30);
        }

        @Test
        @DisplayName("should allow reducing to exactly sold count")
        void shouldAllowReducingToExactlySoldCount() {
            // 100 total, 50 available = 50 sold
            // Reducing to 50 total means 0 available
            int result = EventService.calculateNewAvailableSeats(50, 100, 50);
            assertThat(result).isEqualTo(0);
        }

        @Test
        @DisplayName("should throw exception when reducing below sold count")
        void shouldThrowWhenReducingBelowSoldCount() {
            // 100 total, 50 available = 50 sold
            // Reducing to 40 total is invalid (40 < 50 sold)
            assertThatThrownBy(() -> EventService.calculateNewAvailableSeats(50, 100, 40))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot reduce total seats to 40")
                    .hasMessageContaining("50 seats already sold");
        }

        @Test
        @DisplayName("should handle all seats sold scenario")
        void shouldHandleAllSeatsSold() {
            // 100 total, 0 available = 100 sold
            // Can increase but not decrease
            int result = EventService.calculateNewAvailableSeats(0, 100, 150);
            assertThat(result).isEqualTo(50);

            assertThatThrownBy(() -> EventService.calculateNewAvailableSeats(0, 100, 99))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("100 seats already sold");
        }

        @Test
        @DisplayName("should handle no seats sold scenario")
        void shouldHandleNoSeatsSold() {
            // 100 total, 100 available = 0 sold
            // Can reduce to any value >= 0
            int result = EventService.calculateNewAvailableSeats(100, 100, 50);
            assertThat(result).isEqualTo(50);
        }
    }
}
