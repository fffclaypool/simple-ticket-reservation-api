package com.example.ticketreservation.service;

import com.example.ticketreservation.dto.ReservationRequest;
import com.example.ticketreservation.dto.ReservationResponse;
import com.example.ticketreservation.entity.Event;
import com.example.ticketreservation.entity.Reservation;
import com.example.ticketreservation.entity.ReservationStatus;
import com.example.ticketreservation.exception.InsufficientSeatsException;
import com.example.ticketreservation.exception.ResourceNotFoundException;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private ReservationService reservationService;

    private Event testEvent;
    private Reservation testReservation;
    private ReservationRequest testReservationRequest;

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

        testReservation = Reservation.builder()
                .id(1L)
                .reservationCode("RES-12345678")
                .event(testEvent)
                .customerName("Test User")
                .customerEmail("test@example.com")
                .numberOfSeats(2)
                .totalAmount(10000.0)
                .status(ReservationStatus.CONFIRMED)
                .build();

        testReservationRequest = ReservationRequest.builder()
                .eventId(1L)
                .customerName("Test User")
                .customerEmail("test@example.com")
                .numberOfSeats(2)
                .build();
    }

    @Nested
    @DisplayName("getAllReservations")
    class GetAllReservationsTests {

        @Test
        @DisplayName("should return all reservations")
        void shouldReturnAllReservations() {
            Reservation reservation2 = Reservation.builder()
                    .id(2L)
                    .reservationCode("RES-87654321")
                    .event(testEvent)
                    .customerName("Another User")
                    .customerEmail("another@example.com")
                    .numberOfSeats(3)
                    .totalAmount(15000.0)
                    .status(ReservationStatus.CONFIRMED)
                    .build();
            when(reservationRepository.findAll()).thenReturn(List.of(testReservation, reservation2));

            List<ReservationResponse> result = reservationService.getAllReservations();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getCustomerName()).isEqualTo("Test User");
            assertThat(result.get(1).getCustomerName()).isEqualTo("Another User");
        }

        @Test
        @DisplayName("should return empty list when no reservations exist")
        void shouldReturnEmptyListWhenNoReservations() {
            when(reservationRepository.findAll()).thenReturn(List.of());

            List<ReservationResponse> result = reservationService.getAllReservations();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getReservationById")
    class GetReservationByIdTests {

        @Test
        @DisplayName("should return reservation when found")
        void shouldReturnReservationWhenFound() {
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));

            ReservationResponse result = reservationService.getReservationById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getCustomerName()).isEqualTo("Test User");
            assertThat(result.getReservationCode()).isEqualTo("RES-12345678");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when reservation not found")
        void shouldThrowExceptionWhenReservationNotFound() {
            when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.getReservationById(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Reservation");
        }
    }

    @Nested
    @DisplayName("getReservationByCode")
    class GetReservationByCodeTests {

        @Test
        @DisplayName("should return reservation when code found")
        void shouldReturnReservationWhenCodeFound() {
            when(reservationRepository.findByReservationCode("RES-12345678"))
                    .thenReturn(Optional.of(testReservation));

            ReservationResponse result = reservationService.getReservationByCode("RES-12345678");

            assertThat(result.getReservationCode()).isEqualTo("RES-12345678");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when code not found")
        void shouldThrowExceptionWhenCodeNotFound() {
            when(reservationRepository.findByReservationCode("INVALID"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.getReservationByCode("INVALID"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getReservationsByEmail")
    class GetReservationsByEmailTests {

        @Test
        @DisplayName("should return reservations for email")
        void shouldReturnReservationsForEmail() {
            when(reservationRepository.findByCustomerEmail("test@example.com"))
                    .thenReturn(List.of(testReservation));

            List<ReservationResponse> result = reservationService.getReservationsByEmail("test@example.com");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCustomerEmail()).isEqualTo("test@example.com");
        }
    }

    @Nested
    @DisplayName("getReservationsByEventId")
    class GetReservationsByEventIdTests {

        @Test
        @DisplayName("should return reservations for event")
        void shouldReturnReservationsForEvent() {
            when(reservationRepository.findByEventId(1L)).thenReturn(List.of(testReservation));

            List<ReservationResponse> result = reservationService.getReservationsByEventId(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEventId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("createReservation")
    class CreateReservationTests {

        @Test
        @DisplayName("should create reservation successfully")
        void shouldCreateReservationSuccessfully() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
            when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
            when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

            ReservationResponse result = reservationService.createReservation(testReservationRequest);

            assertThat(result.getCustomerName()).isEqualTo("Test User");
            assertThat(result.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
            verify(eventRepository, times(1)).save(any(Event.class));
            verify(reservationRepository, times(1)).save(any(Reservation.class));
        }

        @Test
        @DisplayName("should calculate total amount correctly")
        void shouldCalculateTotalAmountCorrectly() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
            when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
                Reservation saved = invocation.getArgument(0);
                assertThat(saved.getTotalAmount()).isEqualTo(10000.0);
                return testReservation;
            });

            reservationService.createReservation(testReservationRequest);

            verify(reservationRepository).save(any(Reservation.class));
        }

        @Test
        @DisplayName("should decrease available seats")
        void shouldDecreaseAvailableSeats() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
                Event saved = invocation.getArgument(0);
                assertThat(saved.getAvailableSeats()).isEqualTo(98);
                return saved;
            });
            when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

            reservationService.createReservation(testReservationRequest);

            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("should throw InsufficientSeatsException when not enough seats")
        void shouldThrowExceptionWhenNotEnoughSeats() {
            testEvent.setAvailableSeats(1);
            when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

            assertThatThrownBy(() -> reservationService.createReservation(testReservationRequest))
                    .isInstanceOf(InsufficientSeatsException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when event not found")
        void shouldThrowExceptionWhenEventNotFound() {
            when(eventRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.createReservation(testReservationRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should generate unique reservation code")
        void shouldGenerateUniqueReservationCode() {
            when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
            when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
            when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
                Reservation saved = invocation.getArgument(0);
                assertThat(saved.getReservationCode()).startsWith("RES-");
                assertThat(saved.getReservationCode()).hasSize(12);
                return testReservation;
            });

            reservationService.createReservation(testReservationRequest);

            verify(reservationRepository).save(any(Reservation.class));
        }
    }

    @Nested
    @DisplayName("cancelReservation")
    class CancelReservationTests {

        @Test
        @DisplayName("should cancel reservation successfully")
        void shouldCancelReservationSuccessfully() {
            testEvent.setAvailableSeats(98);
            Reservation cancelledReservation = Reservation.builder()
                    .id(1L)
                    .reservationCode("RES-12345678")
                    .event(testEvent)
                    .customerName("Test User")
                    .customerEmail("test@example.com")
                    .numberOfSeats(2)
                    .totalAmount(10000.0)
                    .status(ReservationStatus.CANCELLED)
                    .build();

            when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
            when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
            when(reservationRepository.save(any(Reservation.class))).thenReturn(cancelledReservation);

            ReservationResponse result = reservationService.cancelReservation(1L);

            assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }

        @Test
        @DisplayName("should restore available seats on cancellation")
        void shouldRestoreAvailableSeatsOnCancellation() {
            testEvent.setAvailableSeats(98);
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
                Event saved = invocation.getArgument(0);
                assertThat(saved.getAvailableSeats()).isEqualTo(100);
                return saved;
            });
            when(reservationRepository.save(any(Reservation.class))).thenReturn(testReservation);

            reservationService.cancelReservation(1L);

            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("should throw IllegalStateException when already cancelled")
        void shouldThrowExceptionWhenAlreadyCancelled() {
            testReservation.setStatus(ReservationStatus.CANCELLED);
            when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));

            assertThatThrownBy(() -> reservationService.cancelReservation(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already cancelled");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when reservation not found")
        void shouldThrowExceptionWhenReservationNotFound() {
            when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reservationService.cancelReservation(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
