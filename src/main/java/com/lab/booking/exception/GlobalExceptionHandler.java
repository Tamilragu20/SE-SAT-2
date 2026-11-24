package com.lab.booking.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessRuleException.class)
    public String handleBusinessRuleException(BusinessRuleException ex, Model model) {
        model.addAttribute("errorTitle", "Business Rule Violation");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public String handleUnauthorizedException(UnauthorizedAccessException ex, Model model) {
        model.addAttribute("errorTitle", "Access Denied");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, Model model) {
        model.addAttribute("errorTitle", "Application Error");
        model.addAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred.");
        return "error";
    }
}
