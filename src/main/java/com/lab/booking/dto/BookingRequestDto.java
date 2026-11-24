package com.lab.booking.dto;

import java.time.LocalDate;

public class BookingRequestDto {
    private Long resourceId;
    private String quantityInput;
    private Integer quantity;
    private String contactName;
    private String contactPhone;
    private LocalDate bookingDate;
    private Long scheduleId;

    public BookingRequestDto() {
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getQuantityInput() {
        return quantityInput;
    }

    public void setQuantityInput(String quantityInput) {
        this.quantityInput = quantityInput;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDate bookingDate) {
        this.bookingDate = bookingDate;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }
}
