package com.lab.booking.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AmountCalculation {
    private final BigDecimal baseAmount;
    private final BigDecimal bookingCharge;
    private final BigDecimal grandTotal;

    public AmountCalculation(BigDecimal baseAmount, BigDecimal bookingCharge, BigDecimal grandTotal) {
        this.baseAmount = baseAmount.setScale(2, RoundingMode.HALF_UP);
        this.bookingCharge = bookingCharge.setScale(2, RoundingMode.HALF_UP);
        this.grandTotal = grandTotal.setScale(2, RoundingMode.HALF_UP);
    }

    public static AmountCalculation calculate(BigDecimal rate, int quantity) {
        if (rate == null) {
            throw new IllegalArgumentException("Rate cannot be null");
        }
        BigDecimal base = rate.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal charge;
        if (base.compareTo(BigDecimal.valueOf(1000)) < 0) {
            charge = new BigDecimal("50.00");
        } else {
            charge = new BigDecimal("0.00");
        }
        BigDecimal total = base.add(charge).setScale(2, RoundingMode.HALF_UP);
        return new AmountCalculation(base, charge, total);
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public BigDecimal getBookingCharge() {
        return bookingCharge;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }
}
