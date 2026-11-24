package com.lab.booking.repository;

import com.lab.booking.model.BookingStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingStatusHistoryRepository extends JpaRepository<BookingStatusHistory, Long> {
    List<BookingStatusHistory> findByReservationIdOrderByEventAtAsc(Long reservationId);
}
