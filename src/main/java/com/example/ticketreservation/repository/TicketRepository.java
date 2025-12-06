package com.example.ticketreservation.repository;

import com.example.ticketreservation.entity.Ticket;
import com.example.ticketreservation.entity.TicketStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketCode(String ticketCode);

    List<Ticket> findByCustomerEmail(String customerEmail);

    List<Ticket> findByEventId(Long eventId);

    List<Ticket> findByStatus(TicketStatus status);

    List<Ticket> findByCustomerEmailAndStatus(String customerEmail, TicketStatus status);

    long countByEventId(Long eventId);
}
