package com.lab.booking.repository;

import com.lab.booking.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    Optional<Schedule> findByReference(String reference);
    List<Schedule> findByResourceIdAndActiveTrue(Long resourceId);
    List<Schedule> findByActiveTrue();
}
