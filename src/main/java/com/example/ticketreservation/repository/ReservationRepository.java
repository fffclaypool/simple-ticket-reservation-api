package com.example.ticketreservation.repository;

import com.example.ticketreservation.entity.Reservation;
import com.example.ticketreservation.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByReservationCode(String reservationCode);

    List<Reservation> findByCustomerEmail(String customerEmail);

    List<Reservation> findByEventId(Long eventId);

    List<Reservation> findByStatus(ReservationStatus status);

    List<Reservation> findByCustomerEmailAndStatus(String customerEmail, ReservationStatus status);
}
