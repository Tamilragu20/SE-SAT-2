package com.lab.booking.service;

import com.lab.booking.dto.AmountCalculation;
import com.lab.booking.dto.BookingRequestDto;
import com.lab.booking.exception.*;
import com.lab.booking.model.*;
import com.lab.booking.repository.BookingStatusHistoryRepository;
import com.lab.booking.repository.ReservationItemRepository;
import com.lab.booking.repository.ReservationRepository;
import com.lab.booking.repository.ScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private final ReservationRepository reservationRepository;
    private final ReservationItemRepository reservationItemRepository;
    private final BookingStatusHistoryRepository historyRepository;
    private final ScheduleRepository scheduleRepository;
    private final AvailabilityService availabilityService;

    public BookingService(ReservationRepository reservationRepository,
                          ReservationItemRepository reservationItemRepository,
                          BookingStatusHistoryRepository historyRepository,
                          ScheduleRepository scheduleRepository,
                          AvailabilityService availabilityService) {
        this.reservationRepository = reservationRepository;
        this.reservationItemRepository = reservationItemRepository;
        this.historyRepository = historyRepository;
        this.scheduleRepository = scheduleRepository;
        this.availabilityService = availabilityService;
    }

    /**
     * Parse and validate quantity according to BR1:
     * - Integer between 1 and 5 (inclusive)
     * - Zero rejected
     * - Negative rejected
     * - Values above 5 rejected
     * - Decimal values rejected (e.g. "1.5")
     * - Non-numeric values rejected
     */
    public int validateAndParseQuantity(String quantityInput, Integer quantityObj) {
        if (quantityInput != null && !quantityInput.trim().isEmpty()) {
            String trimmed = quantityInput.trim();
            // Reject decimal inputs like "1.5"
            if (trimmed.contains(".")) {
                throw new InvalidQuantityException("Invalid quantity: decimal values are not allowed. Must be an integer between 1 and 5.");
            }
            try {
                int q = Integer.parseInt(trimmed);
                return validateQuantityRange(q);
            } catch (NumberFormatException e) {
                throw new InvalidQuantityException("Invalid quantity: must be a numeric integer between 1 and 5.");
            }
        } else if (quantityObj != null) {
            return validateQuantityRange(quantityObj);
        } else {
            throw new InvalidQuantityException("Quantity is required and must be between 1 and 5.");
        }
    }

    private int validateQuantityRange(int q) {
        if (q <= 0) {
            throw new InvalidQuantityException("Invalid quantity: must be greater than 0. Requested: " + q);
        }
        if (q > 5) {
            throw new InvalidQuantityException("Invalid quantity: maximum allowed is 5 units per booking. Requested: " + q);
        }
        return q;
    }

    /**
     * Validates booking details according to BR5
     */
    public void validateBookingDetails(String contactName, String contactPhone, LocalDate bookingDate) {
        if (contactName == null || contactName.trim().isEmpty()) {
            throw new BusinessRuleException("Name cannot be blank.");
        }
        if (contactPhone == null || !contactPhone.trim().matches("^\\d{10}$")) {
            throw new BusinessRuleException("Phone must contain exactly 10 digits.");
        }
        if (bookingDate == null) {
            throw new BusinessRuleException("Booking date is required.");
        }
    }

    /**
     * T1: Select same resource twice rule enforcement.
     * Duplicate resource selection in the same booking is strictly prohibited.
     */
    public void validateNoDuplicateResources(List<Long> resourceIds) {
        if (resourceIds != null && !resourceIds.isEmpty()) {
            java.util.Set<Long> uniqueIds = new java.util.HashSet<>();
            for (Long id : resourceIds) {
                if (!uniqueIds.add(id)) {
                    throw new BusinessRuleException("Duplicate resource selection rejected: Resource ID " + id +
                            " selected more than once. Each resource slot may be selected at most once per booking.");
                }
            }
        }
    }

    /**
     * FR4, FR5, BR1-BR7: Create a confirmed booking atomically.
     */
    @Transactional
    public Reservation createBooking(BookingRequestDto request, User user) {
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("User is not eligible to create bookings.");
        }

        if (request.getResourceId() == null) {
            throw new BusinessRuleException("Resource selection is required.");
        }

        // 1. Validate quantity (BR1)
        int quantity = validateAndParseQuantity(request.getQuantityInput(), request.getQuantity());

        // 2. Validate details (BR5)
        validateBookingDetails(request.getContactName(), request.getContactPhone(), request.getBookingDate());

        // 3. Check current availability at confirmation (BR2)
        Resource resource = availabilityService.getAvailableResourceOrThrow(request.getResourceId(), quantity);

        // 4. Calculate amounts (BR3, BR4)
        BigDecimal currentRate = resource.getRate();
        AmountCalculation calc = AmountCalculation.calculate(currentRate, quantity);

        // 5. Generate unique reference
        String timestampPart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String bookingRef = "RES-" + timestampPart + "-" + randomPart;

        // 6. Create Reservation entity
        Reservation reservation = new Reservation(
                bookingRef,
                user,
                request.getContactName().trim(),
                request.getContactPhone().trim(),
                request.getBookingDate(),
                BookingStatus.CONFIRMED,
                calc.getBaseAmount(),
                calc.getBookingCharge(),
                calc.getGrandTotal(),
                LocalDateTime.now()
        );

        // 7. Create ReservationItem with rate and resource snapshot
        ReservationItem item = new ReservationItem(
                reservation,
                resource,
                quantity,
                currentRate, // Snapshot of current rate
                calc.getBaseAmount(),
                calc.getBookingCharge(),
                calc.getGrandTotal()
        );
        reservation.addItem(item);

        // Save reservation (cascades to item)
        Reservation savedReservation = reservationRepository.save(reservation);

        // 8. Deduct availability (BR7)
        availabilityService.updateAvailability(resource.getId(), quantity);

        // 9. Record initial status history (BR7)
        BookingStatusHistory history = new BookingStatusHistory(
                savedReservation,
                null,
                BookingStatus.CONFIRMED,
                "USER (" + user.getName() + ")",
                LocalDateTime.now()
        );
        historyRepository.save(history);

        return savedReservation;
    }

    /**
     * FR8, BR9: User cancellation of eligible CONFIRMED booking.
     */
    @Transactional
    public Reservation cancelBooking(Long reservationId, User actor) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessRuleException("Reservation not found with id: " + reservationId));

        // Authorization check (BR10)
        if (actor.getRole() != Role.ADMINISTRATOR && !reservation.getUser().getId().equals(actor.getId())) {
            throw new UnauthorizedAccessException("Unauthorized: You can view/cancel only your own bookings.");
        }

        // Status check (FR8, BR9)
        if (reservation.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleException("Repeated cancellation is rejected. This booking has already been cancelled.");
        }

        if (reservation.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessRuleException("Cannot cancel booking. Current status is " +
                    reservation.getStatus().getDisplayName() + ". Only CONFIRMED bookings are eligible for cancellation.");
        }

        BookingStatus oldStatus = reservation.getStatus();
        reservation.setStatus(BookingStatus.CANCELLED);
        Reservation updated = reservationRepository.save(reservation);

        // Restore availability for each item exactly ONCE
        for (ReservationItem item : reservation.getItems()) {
            availabilityService.restoreAvailability(item.getResource().getId(), item.getQuantity());
        }

        // Record history
        BookingStatusHistory history = new BookingStatusHistory(
                updated,
                oldStatus,
                BookingStatus.CANCELLED,
                actor.getRole() == Role.ADMINISTRATOR ? "ADMINISTRATOR (" + actor.getName() + ")" : "USER (" + actor.getName() + ")",
                LocalDateTime.now()
        );
        historyRepository.save(history);

        return updated;
    }

    /**
     * FR7, BR8: Administrator updates booking status strictly to next valid status.
     * CONFIRMED -> CHECKED_IN -> COMPLETED
     */
    @Transactional
    public Reservation updateStatus(Long reservationId, BookingStatus requestedStatus, User adminActor) {
        if (adminActor == null || adminActor.getRole() != Role.ADMINISTRATOR) {
            throw new UnauthorizedAccessException("Unauthorized: Only administrators can update booking status.");
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessRuleException("Reservation not found with id: " + reservationId));

        BookingStatus currentStatus = reservation.getStatus();
        BookingStatus expectedNext = currentStatus.getNextAdminStatus();

        if (expectedNext == null) {
            throw new InvalidStatusTransitionException("Invalid status transition: Cannot transition from terminal status " +
                    currentStatus.getDisplayName());
        }

        if (requestedStatus != expectedNext) {
            throw new InvalidStatusTransitionException("Invalid status transition: Cannot change status from " +
                    currentStatus.getDisplayName() + " to " + requestedStatus.getDisplayName() +
                    ". Next valid status must be: " + expectedNext.getDisplayName() + ". Skipping or backward transitions are forbidden.");
        }

        reservation.setStatus(requestedStatus);
        Reservation updated = reservationRepository.save(reservation);

        // Record history
        BookingStatusHistory history = new BookingStatusHistory(
                updated,
                currentStatus,
                requestedStatus,
                "ADMINISTRATOR (" + adminActor.getName() + ")",
                LocalDateTime.now()
        );
        historyRepository.save(history);

        return updated;
    }

    /**
     * FR6: View only own bookings
     */
    public List<Reservation> getUserBookings(Long userId) {
        return reservationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Administrator: view all bookings/transactions
     */
    public List<Reservation> getAllBookings(User adminActor) {
        if (adminActor == null || adminActor.getRole() != Role.ADMINISTRATOR) {
            throw new UnauthorizedAccessException("Unauthorized: Only administrators can view all bookings.");
        }
        return reservationRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * FR6, BR10: View specific booking details with ownership check
     */
    public Reservation getBookingDetails(Long reservationId, User requestingUser) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessRuleException("Reservation not found with id: " + reservationId));

        if (requestingUser == null) {
            throw new UnauthorizedAccessException("Authentication required.");
        }

        if (requestingUser.getRole() != Role.ADMINISTRATOR && !reservation.getUser().getId().equals(requestingUser.getId())) {
            throw new UnauthorizedAccessException("Unauthorized access: You can view only your own bookings.");
        }

        return reservation;
    }

    public List<BookingStatusHistory> getStatusHistory(Long reservationId) {
        return historyRepository.findByReservationIdOrderByEventAtAsc(reservationId);
    }
}
