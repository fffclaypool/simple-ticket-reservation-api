package com.example.ticketreservation.repository;

import com.example.ticketreservation.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByEventDateAfter(LocalDateTime date);

    List<Event> findByAvailableSeatsGreaterThan(Integer seats);

    @Query("SELECT e FROM Event e WHERE e.eventDate > :now AND e.availableSeats > 0 ORDER BY e.eventDate ASC")
    List<Event> findAvailableEvents(@Param("now") LocalDateTime now);

    List<Event> findByNameContainingIgnoreCase(String name);

    List<Event> findByVenueContainingIgnoreCase(String venue);
}
