package com.lab.booking.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
@Table(name = "resources")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Resource reference cannot be blank")
    @Column(nullable = false, unique = true, length = 50)
    private String reference;

    @NotBlank(message = "Resource name cannot be blank")
    @Column(nullable = false, length = 150)
    private String name;

    @NotBlank(message = "Category cannot be blank")
    @Column(nullable = false, length = 100)
    private String category;

    @NotNull(message = "Rate cannot be null")
    @DecimalMin(value = "0.0", inclusive = true, message = "Rate must be non-negative")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal rate;

    @NotNull(message = "Availability cannot be null")
    @Min(value = 0, message = "Availability cannot be negative")
    @Column(nullable = false)
    private Integer availability;

    @Column(nullable = false)
    private boolean active = true;

    public Resource() {
    }

    public Resource(String reference, String name, String category, BigDecimal rate, Integer availability, boolean active) {
        this.reference = reference;
        this.name = name;
        this.category = category;
        this.rate = rate;
        this.availability = availability;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public Integer getAvailability() {
        return availability;
    }

    public void setAvailability(Integer availability) {
        this.availability = availability;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
