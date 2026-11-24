package com.lab.booking.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "reservation_items")
public class ReservationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Column(name = "resource_ref_snapshot", nullable = false, length = 50)
    private String resourceRefSnapshot;

    @Column(name = "resource_name_snapshot", nullable = false, length = 150)
    private String resourceNameSnapshot;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "rate_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal rateSnapshot;

    @Column(name = "base_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "booking_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal bookingCharge;

    @Column(name = "item_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal itemTotal;

    public ReservationItem() {
    }

    public ReservationItem(Reservation reservation, Resource resource, Integer quantity,
                           BigDecimal rateSnapshot, BigDecimal baseAmount, BigDecimal bookingCharge,
                           BigDecimal itemTotal) {
        this.reservation = reservation;
        this.resource = resource;
        this.resourceRefSnapshot = resource.getReference();
        this.resourceNameSnapshot = resource.getName();
        this.quantity = quantity;
        this.rateSnapshot = rateSnapshot;
        this.baseAmount = baseAmount;
        this.bookingCharge = bookingCharge;
        this.itemTotal = itemTotal;
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

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public String getResourceRefSnapshot() {
        return resourceRefSnapshot;
    }

    public void setResourceRefSnapshot(String resourceRefSnapshot) {
        this.resourceRefSnapshot = resourceRefSnapshot;
    }

    public String getResourceNameSnapshot() {
        return resourceNameSnapshot;
    }

    public void setResourceNameSnapshot(String resourceNameSnapshot) {
        this.resourceNameSnapshot = resourceNameSnapshot;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getRateSnapshot() {
        return rateSnapshot;
    }

    public void setRateSnapshot(BigDecimal rateSnapshot) {
        this.rateSnapshot = rateSnapshot;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(BigDecimal baseAmount) {
        this.baseAmount = baseAmount;
    }

    public BigDecimal getBookingCharge() {
        return bookingCharge;
    }

    public void setBookingCharge(BigDecimal bookingCharge) {
        this.bookingCharge = bookingCharge;
    }

    public BigDecimal getItemTotal() {
        return itemTotal;
    }

    public void setItemTotal(BigDecimal itemTotal) {
        this.itemTotal = itemTotal;
    }
}
