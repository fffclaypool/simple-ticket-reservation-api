package com.example.ticketreservation.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.ticketreservation.dto.TicketRequest;
import com.example.ticketreservation.dto.TicketResponse;
import com.example.ticketreservation.entity.Event;
import com.example.ticketreservation.entity.Ticket;
import com.example.ticketreservation.entity.TicketStatus;
import com.example.ticketreservation.exception.InsufficientSeatsException;
import com.example.ticketreservation.exception.ResourceNotFoundException;
import com.example.ticketreservation.repository.EventRepository;
import com.example.ticketreservation.repository.TicketRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private TicketService ticketService;

    private Event testEvent;
    private Ticket testTicket;
    private TicketRequest testRequest;

    @BeforeEach
    void setUp() {
        testEvent = Event.builder()
                .id(1L)
                .name("Test Event")
                .description("Test Description")
                .venue("Test Venue")
                .eventDate(LocalDateTime.now().plusDays(30))
                .totalSeats(100)
                .availableSeats(50)
                .price(new BigDecimal("1000.0"))
                .build();

        testTicket = Ticket.builder()
                .id(1L)
                .ticketCode("TKT-12345678")
                .event(testEvent)
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
    @DisplayName("getAllTickets")
    class GetAllTicketsTests {

        @Test
        @DisplayName("should return all tickets")
        void shouldReturnAllTickets() {
            Ticket ticket2 = Ticket.builder()
                    .id(2L)
                    .ticketCode("TKT-87654321")
                    .event(testEvent)
                    .customerName("Jane Doe")
                    .customerEmail("jane@example.com")
                    .numberOfSeats(1)
                    .totalAmount(new BigDecimal("1000.0"))
                    .status(TicketStatus.CONFIRMED)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(ticketRepository.findAll()).thenReturn(Arrays.asList(testTicket, ticket2));

            List<TicketResponse> result = ticketService.getAllTickets();

            assertThat(result).hasSize(2);
            verify(ticketRepository).findAll();
        }

        @Test
        @DisplayName("should return empty list when no tickets exist")
        void shouldReturnEmptyListWhenNoTicketsExist() {
            when(ticketRepository.findAll()).thenReturn(List.of());

            List<TicketResponse> result = ticketService.getAllTickets();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getTicketById")
    class GetTicketByIdTests {

        @Test
        @DisplayName("should return ticket when found")
        void shouldReturnTicketWhenFound() {
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));

            TicketResponse result = ticketService.getTicketById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTicketCode()).isEqualTo("TKT-12345678");
            assertThat(result.getCustomerName()).isEqualTo("John Doe");
        }

        @Test
        @DisplayName("should throw exception when not found")
        void shouldThrowExceptionWhenNotFound() {
            when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ticketService.getTicketById(999L)).isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getTicketByCode")
    class GetTicketByCodeTests {

        @Test
        @DisplayName("should return ticket when found by code")
        void shouldReturnTicketWhenFoundByCode() {
            when(ticketRepository.findByTicketCode("TKT-12345678")).thenReturn(Optional.of(testTicket));

            TicketResponse result = ticketService.getTicketByCode("TKT-12345678");

            assertThat(result.getTicketCode()).isEqualTo("TKT-12345678");
        }

        @Test
        @DisplayName("should throw exception when code not found")
        void shouldThrowExceptionWhenCodeNotFound() {
            when(ticketRepository.findByTicketCode("INVALID")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ticketService.getTicketByCode("INVALID"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getTicketsByEmail")
    class GetTicketsByEmailTests {

        @Test
        @DisplayName("should return tickets for email")
        void shouldReturnTicketsForEmail() {
            when(ticketRepository.findByCustomerEmail("john@example.com")).thenReturn(List.of(testTicket));

            List<TicketResponse> result = ticketService.getTicketsByEmail("john@example.com");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCustomerEmail()).isEqualTo("john@example.com");
        }
    }

    @Nested
    @DisplayName("getTicketsByEventId")
    class GetTicketsByEventIdTests {

        @Test
        @DisplayName("should return tickets for event")
        void shouldReturnTicketsForEvent() {
            when(ticketRepository.findByEventId(1L)).thenReturn(List.of(testTicket));

            List<TicketResponse> result = ticketService.getTicketsByEventId(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEventId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("createTicket")
    class CreateTicketTests {

        @Test
        @DisplayName("should create ticket successfully")
        void shouldCreateTicketSuccessfully() {
            when(eventRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testEvent));
            when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
                Ticket ticket = invocation.getArgument(0);
                ticket.setId(1L);
                return ticket;
            });

            TicketResponse result = ticketService.createTicket(1L, testRequest);

            assertThat(result.getCustomerName()).isEqualTo("John Doe");
            assertThat(result.getNumberOfSeats()).isEqualTo(2);
            verify(eventRepository).findByIdWithLock(1L);
            verify(eventRepository).save(any(Event.class));
            verify(ticketRepository).save(any(Ticket.class));
        }

        @Test
        @DisplayName("should throw exception when event not found")
        void shouldThrowExceptionWhenEventNotFound() {
            when(eventRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ticketService.createTicket(999L, testRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw exception when insufficient seats")
        void shouldThrowExceptionWhenInsufficientSeats() {
            testEvent.setAvailableSeats(1);
            when(eventRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testEvent));

            assertThatThrownBy(() -> ticketService.createTicket(1L, testRequest))
                    .isInstanceOf(InsufficientSeatsException.class);
        }

        @Test
        @DisplayName("should decrease available seats after booking")
        void shouldDecreaseAvailableSeatsAfterBooking() {
            int initialSeats = testEvent.getAvailableSeats();
            when(eventRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testEvent));
            when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
                Ticket ticket = invocation.getArgument(0);
                ticket.setId(1L);
                return ticket;
            });

            ticketService.createTicket(1L, testRequest);

            assertThat(testEvent.getAvailableSeats()).isEqualTo(initialSeats - 2);
            verify(eventRepository).save(testEvent);
        }
    }

    @Nested
    @DisplayName("cancelTicket")
    class CancelTicketTests {

        @Test
        @DisplayName("should cancel ticket successfully")
        void shouldCancelTicketSuccessfully() {
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));
            when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);
            when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
            when(cacheManager.getCache("events")).thenReturn(cache);

            TicketResponse result = ticketService.cancelTicket(1L);

            assertThat(result.getStatus()).isEqualTo(TicketStatus.CANCELLED);
            verify(ticketRepository).save(any(Ticket.class));
            verify(cache).evict(testEvent.getId());
        }

        @Test
        @DisplayName("should throw exception when ticket not found")
        void shouldThrowExceptionWhenTicketNotFound() {
            when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ticketService.cancelTicket(999L)).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw exception when already cancelled")
        void shouldThrowExceptionWhenAlreadyCancelled() {
            testTicket.setStatus(TicketStatus.CANCELLED);
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));

            assertThatThrownBy(() -> ticketService.cancelTicket(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already cancelled");
        }

        @Test
        @DisplayName("should restore seats after cancellation")
        void shouldRestoreSeatsAfterCancellation() {
            int initialSeats = testEvent.getAvailableSeats();
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));
            when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);
            when(eventRepository.save(any(Event.class))).thenReturn(testEvent);
            when(cacheManager.getCache("events")).thenReturn(cache);

            ticketService.cancelTicket(1L);

            assertThat(testEvent.getAvailableSeats()).isEqualTo(initialSeats + testTicket.getNumberOfSeats());
        }
    }
}
