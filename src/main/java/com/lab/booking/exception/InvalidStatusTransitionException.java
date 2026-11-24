package com.lab.booking.exception;

public class InvalidStatusTransitionException extends BusinessRuleException {
    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
