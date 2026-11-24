package com.lab.booking.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_status_history")
public class BookingStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 30)
    private BookingStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private BookingStatus newStatus;

    @Column(nullable = false, length = 100)
    private String actor;

    @Column(name = "event_at", nullable = false)
    private LocalDateTime eventAt;

    public BookingStatusHistory() {
    }

    public BookingStatusHistory(Reservation reservation, BookingStatus oldStatus, BookingStatus newStatus, String actor, LocalDateTime eventAt) {
        this.reservation = reservation;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.actor = actor;
        this.eventAt = eventAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public BookingStatus getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(BookingStatus oldStatus) {
        this.oldStatus = oldStatus;
    }

    public BookingStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(BookingStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public LocalDateTime getEventAt() {
        return eventAt;
    }

    public void setEventAt(LocalDateTime eventAt) {
        this.eventAt = eventAt;
    }
}
