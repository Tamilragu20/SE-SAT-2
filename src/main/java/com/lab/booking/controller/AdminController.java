package com.lab.booking.controller;

import com.lab.booking.exception.BusinessRuleException;
import com.lab.booking.exception.UnauthorizedAccessException;
import com.lab.booking.model.*;
import com.lab.booking.service.BookingService;
import com.lab.booking.service.ResourceService;
import com.lab.booking.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final BookingService bookingService;
    private final ResourceService resourceService;
    private final com.lab.booking.config.DataInitializer dataInitializer;

    public AdminController(BookingService bookingService,
                           ResourceService resourceService,
                           com.lab.booking.config.DataInitializer dataInitializer) {
        this.bookingService = bookingService;
        this.resourceService = resourceService;
        this.dataInitializer = dataInitializer;
    }

    private User getAdminUserOrThrow(HttpSession session) {
        User user = SessionUtils.getLoggedInUser(session);
        if (user == null || user.getRole() != Role.ADMINISTRATOR) {
            throw new UnauthorizedAccessException("Unauthorized: Administrator privileges are required to access this resource.");
        }
        return user;
    }

    @GetMapping("/dashboard")
    public String showAdminDashboard(HttpSession session, Model model) {
        User adminUser;
        try {
            adminUser = getAdminUserOrThrow(session);
        } catch (UnauthorizedAccessException e) {
            return "redirect:/login?error=" + e.getMessage();
        }

        List<Reservation> allBookings = bookingService.getAllBookings(adminUser);
        List<Resource> allResources = resourceService.getAllActiveResources();

        long confirmedCount = allBookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        long checkedInCount = allBookings.stream().filter(b -> b.getStatus() == BookingStatus.CHECKED_IN).count();
        long completedCount = allBookings.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        long cancelledCount = allBookings.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();

        BigDecimal totalRevenue = allBookings.stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .map(Reservation::getGrandTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("currentUser", adminUser);
        model.addAttribute("allBookings", allBookings);
        model.addAttribute("allResources", allResources);
        model.addAttribute("totalBookingsCount", allBookings.size());
        model.addAttribute("confirmedCount", confirmedCount);
        model.addAttribute("checkedInCount", checkedInCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("cancelledCount", cancelledCount);
        model.addAttribute("totalRevenue", totalRevenue);

        return "admin/dashboard";
    }

    @GetMapping("/bookings")
    public String showAllBookings(HttpSession session, Model model) {
        User adminUser;
        try {
            adminUser = getAdminUserOrThrow(session);
        } catch (UnauthorizedAccessException e) {
            return "redirect:/login?error=" + e.getMessage();
        }

        List<Reservation> allBookings = bookingService.getAllBookings(adminUser);
        model.addAttribute("currentUser", adminUser);
        model.addAttribute("bookings", allBookings);
        model.addAttribute("allStatuses", BookingStatus.values());

        return "admin/bookings-list";
    }

    @GetMapping("/booking/{id}")
    public String showBookingDetails(@PathVariable("id") Long id,
                                     HttpSession session,
                                     Model model) {
        User adminUser;
        try {
            adminUser = getAdminUserOrThrow(session);
        } catch (UnauthorizedAccessException e) {
            return "redirect:/login?error=" + e.getMessage();
        }

        Reservation reservation = bookingService.getBookingDetails(id, adminUser);
        List<BookingStatusHistory> history = bookingService.getStatusHistory(id);

        model.addAttribute("currentUser", adminUser);
        model.addAttribute("reservation", reservation);
        model.addAttribute("history", history);
        model.addAttribute("nextStatus", reservation.getStatus().getNextAdminStatus());
        model.addAttribute("allStatuses", BookingStatus.values());

        return "admin/booking-details";
    }

    @PostMapping("/booking/{id}/update-status")
    public String handleStatusUpdate(@PathVariable("id") Long id,
                                     @RequestParam("status") BookingStatus requestedStatus,
                                     @RequestParam(value = "redirectPage", defaultValue = "details") String redirectPage,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        User adminUser;
        try {
            adminUser = getAdminUserOrThrow(session);
        } catch (UnauthorizedAccessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }

        try {
            Reservation updated = bookingService.updateStatus(id, requestedStatus, adminUser);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Status successfully updated to " + updated.getStatus().getDisplayName() + " for booking " + updated.getReference());
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        if ("list".equals(redirectPage)) {
            return "redirect:/admin/bookings";
        }
        return "redirect:/admin/booking/" + id;
    }

    @PostMapping("/resource/{id}/update-rate")
    public String handleRateUpdate(@PathVariable("id") Long id,
                                   @RequestParam("rate") BigDecimal newRate,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        try {
            getAdminUserOrThrow(session);
            resourceService.updateResourceRate(id, newRate);
            redirectAttributes.addFlashAttribute("successMessage", "Resource rate updated to Rs. " + newRate);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/resource/{id}/update-availability")
    public String handleAvailabilityUpdate(@PathVariable("id") Long id,
                                           @RequestParam("availability") int newAvailability,
                                           HttpSession session,
                                           RedirectAttributes redirectAttributes) {
        try {
            getAdminUserOrThrow(session);
            resourceService.updateResourceAvailability(id, newAvailability);
            redirectAttributes.addFlashAttribute("successMessage", "Resource availability updated to " + newAvailability);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/reset-demo-data")
    public String handleResetDemoData(HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            getAdminUserOrThrow(session);
            dataInitializer.resetDemonstrationData();
            redirectAttributes.addFlashAttribute("successMessage",
                    "Demonstration resources reset to exact Section 5 initial state: R101(10), R102(5), R103(20), R104(0), R105(3).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }
}
