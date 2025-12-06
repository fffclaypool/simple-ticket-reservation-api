package com.example.ticketreservation.repository;

import com.example.ticketreservation.entity.Event;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByEventDateAfter(LocalDateTime date);

    List<Event> findByAvailableSeatsGreaterThan(Integer seats);

    @Query("SELECT e FROM Event e WHERE e.eventDate > :now AND e.availableSeats > 0 ORDER BY e.eventDate ASC")
    List<Event> findAvailableEvents(@Param("now") LocalDateTime now);

    List<Event> findByNameContainingIgnoreCase(String name);

    List<Event> findByVenueContainingIgnoreCase(String venue);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdWithLock(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Event e SET e.availableSeats = e.availableSeats - :seats, e.version = e.version + 1 "
            + "WHERE e.id = :id AND e.availableSeats >= :seats")
    int decreaseAvailableSeats(@Param("id") Long id, @Param("seats") int seats);
}
