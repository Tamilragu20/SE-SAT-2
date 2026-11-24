package com.lab.booking.repository;

import com.lab.booking.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    Optional<Resource> findByReference(String reference);
    List<Resource> findByActiveTrue();

    @Query("SELECT r FROM Resource r WHERE r.active = true AND " +
           "(LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.reference) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(r.category) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Resource> searchActiveResources(@Param("keyword") String keyword);
}
