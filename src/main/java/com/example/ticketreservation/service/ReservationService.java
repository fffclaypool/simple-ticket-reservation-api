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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;

    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ReservationResponse getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", id));
        return mapToResponse(reservation);
    }

    public ReservationResponse getReservationByCode(String code) {
        Reservation reservation = reservationRepository.findByReservationCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "code", code));
        return mapToResponse(reservation);
    }

    public List<ReservationResponse> getReservationsByEmail(String email) {
        return reservationRepository.findByCustomerEmail(email).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ReservationResponse> getReservationsByEventId(Long eventId) {
        return reservationRepository.findByEventId(eventId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", request.getEventId()));

        if (event.getAvailableSeats() < request.getNumberOfSeats()) {
            throw new InsufficientSeatsException(request.getNumberOfSeats(), event.getAvailableSeats());
        }

        event.setAvailableSeats(event.getAvailableSeats() - request.getNumberOfSeats());
        eventRepository.save(event);

        Reservation reservation = Reservation.builder()
                .reservationCode(generateReservationCode())
                .event(event)
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .numberOfSeats(request.getNumberOfSeats())
                .totalAmount(event.getPrice() * request.getNumberOfSeats())
                .status(ReservationStatus.CONFIRMED)
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);
        return mapToResponse(savedReservation);
    }

    @Transactional
    public ReservationResponse cancelReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", id));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Reservation is already cancelled");
        }

        Event event = reservation.getEvent();
        event.setAvailableSeats(event.getAvailableSeats() + reservation.getNumberOfSeats());
        eventRepository.save(event);

        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation cancelledReservation = reservationRepository.save(reservation);
        return mapToResponse(cancelledReservation);
    }

    private String generateReservationCode() {
        return "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private ReservationResponse mapToResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .reservationCode(reservation.getReservationCode())
                .eventId(reservation.getEvent().getId())
                .eventName(reservation.getEvent().getName())
                .customerName(reservation.getCustomerName())
                .customerEmail(reservation.getCustomerEmail())
                .numberOfSeats(reservation.getNumberOfSeats())
                .totalAmount(reservation.getTotalAmount())
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .build();
    }
}
