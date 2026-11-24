package com.lab.booking.controller;

import com.lab.booking.dto.BookingRequestDto;
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

import java.time.LocalDate;
import java.util.List;

@Controller
public class UserController {

    private final ResourceService resourceService;
    private final BookingService bookingService;

    public UserController(ResourceService resourceService, BookingService bookingService) {
        this.resourceService = resourceService;
        this.bookingService = bookingService;
    }

    private User getAuthenticatedUserOrRedirect(HttpSession session) {
        User user = SessionUtils.getLoggedInUser(session);
        if (user == null) {
            throw new UnauthorizedAccessException("Please log in to continue.");
        }
        return user;
    }

    @GetMapping("/dashboard")
    public String showDashboard(HttpSession session, Model model) {
        User user = SessionUtils.getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }
        if (user.getRole() == Role.ADMINISTRATOR) {
            return "redirect:/admin/dashboard";
        }

        List<Reservation> myBookings = bookingService.getUserBookings(user.getId());
        List<Resource> activeResources = resourceService.getAllActiveResources();

        long confirmedCount = myBookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        long completedCount = myBookings.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();

        model.addAttribute("currentUser", user);
        model.addAttribute("myBookings", myBookings);
        model.addAttribute("activeResources", activeResources);
        model.addAttribute("totalBookingsCount", myBookings.size());
        model.addAttribute("confirmedCount", confirmedCount);
        model.addAttribute("completedCount", completedCount);

        return "user/dashboard";
    }

    @GetMapping("/equipment")
    public String showEquipmentList(@RequestParam(value = "keyword", required = false) String keyword,
                                    HttpSession session,
                                    Model model) {
        User user = SessionUtils.getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        List<Resource> resources = resourceService.searchResources(keyword);
        model.addAttribute("currentUser", user);
        model.addAttribute("resources", resources);
        model.addAttribute("keyword", keyword != null ? keyword : "");

        return "user/equipment-list";
    }

    @GetMapping("/book/{resourceId}")
    public String showBookingForm(@PathVariable("resourceId") Long resourceId,
                                  HttpSession session,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        User user = SessionUtils.getLoggedInUser(session);
        if (user == null) {
            redirectAttributes.addAttribute("error", "Please log in to book equipment.");
            return "redirect:/login";
        }

        Resource resource = resourceService.getResourceById(resourceId);
        if (resource == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Equipment slot not found.");
            return "redirect:/equipment";
        }

        List<Schedule> schedules = resourceService.getSchedulesForResource(resourceId);

        BookingRequestDto bookingForm = new BookingRequestDto();
        bookingForm.setResourceId(resource.getId());
        bookingForm.setContactName(user.getName());
        bookingForm.setContactPhone(user.getPhone());
        bookingForm.setBookingDate(LocalDate.now());
        bookingForm.setQuantityInput("1");

        model.addAttribute("currentUser", user);
        model.addAttribute("resource", resource);
        model.addAttribute("schedules", schedules);
        model.addAttribute("bookingForm", bookingForm);

        return "user/booking-form";
    }

    @PostMapping("/book")
    public String handleBookingSubmit(@ModelAttribute("bookingForm") BookingRequestDto bookingForm,
                                      HttpSession session,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        User user = SessionUtils.getLoggedInUser(session);
        if (user == null) {
            redirectAttributes.addAttribute("error", "Please log in to book equipment.");
            return "redirect:/login";
        }

        try {
            Reservation reservation = bookingService.createBooking(bookingForm, user);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Reservation " + reservation.getReference() + " successfully confirmed!");
            return "redirect:/booking/confirmation/" + reservation.getId();
        } catch (BusinessRuleException e) {
            Resource resource = resourceService.getResourceById(bookingForm.getResourceId());
            List<Schedule> schedules = resource != null ? resourceService.getSchedulesForResource(resource.getId()) : List.of();

            model.addAttribute("currentUser", user);
            model.addAttribute("resource", resource);
            model.addAttribute("schedules", schedules);
            model.addAttribute("bookingForm", bookingForm);
            model.addAttribute("errorMessage", e.getMessage());
            return "user/booking-form";
        }
    }

    @GetMapping("/booking/confirmation/{id}")
    public String showConfirmationPage(@PathVariable("id") Long id,
                                       HttpSession session,
                                       Model model,
                                       RedirectAttributes redirectAttributes) {
        User user = SessionUtils.getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Reservation reservation = bookingService.getBookingDetails(id, user);
            model.addAttribute("currentUser", user);
            model.addAttribute("reservation", reservation);
            return "user/booking-confirmation";
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    @GetMapping("/my-bookings")
    public String showMyBookings(HttpSession session, Model model) {
        User user = SessionUtils.getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        List<Reservation> bookings = bookingService.getUserBookings(user.getId());
        model.addAttribute("currentUser", user);
        model.addAttribute("bookings", bookings);

        return "user/my-bookings";
    }

    @GetMapping("/booking/{id}")
    public String showBookingDetails(@PathVariable("id") Long id,
                                     HttpSession session,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {
        User user = SessionUtils.getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Reservation reservation = bookingService.getBookingDetails(id, user);
            List<BookingStatusHistory> history = bookingService.getStatusHistory(id);

            model.addAttribute("currentUser", user);
            model.addAttribute("reservation", reservation);
            model.addAttribute("history", history);

            return "user/booking-details";
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/my-bookings";
        }
    }

    @PostMapping("/booking/{id}/cancel")
    public String handleCancelBooking(@PathVariable("id") Long id,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        User user = SessionUtils.getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Reservation reservation = bookingService.cancelBooking(id, user);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Reservation " + reservation.getReference() + " cancelled successfully. Capacity has been restored.");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/my-bookings";
    }
}
