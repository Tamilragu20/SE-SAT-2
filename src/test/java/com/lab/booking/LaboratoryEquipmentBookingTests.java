package com.lab.booking;

import com.lab.booking.dto.AmountCalculation;
import com.lab.booking.dto.BookingRequestDto;
import com.lab.booking.exception.*;
import com.lab.booking.model.*;
import com.lab.booking.repository.*;
import com.lab.booking.service.AvailabilityService;
import com.lab.booking.service.BookingService;
import com.lab.booking.service.ResourceService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class LaboratoryEquipmentBookingTests {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private AvailabilityService availabilityService;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationItemRepository reservationItemRepository;

    @Autowired
    private BookingStatusHistoryRepository historyRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private User testUser1;
    private User testUser2;
    private User adminUser;
    private Resource resA;
    private Resource resB;
    private Resource resC;
    private Resource resD_Unavailable;
    private Resource resE;

    @BeforeEach
    void setUp() {
        // Fetch demo users preloaded by DataInitializer
        testUser1 = userRepository.findByEmail("user1@lab.edu").orElseThrow();
        testUser2 = userRepository.findByEmail("user2@lab.edu").orElseThrow();
        adminUser = userRepository.findByEmail("admin@lab.edu").orElseThrow();

        // Fetch demo resources preloaded by DataInitializer
        resA = resourceRepository.findByReference("R101").orElseThrow(); // Rate 499, Avail 10
        resB = resourceRepository.findByReference("R102").orElseThrow(); // Rate 799, Avail 5
        resC = resourceRepository.findByReference("R103").orElseThrow(); // Rate 199, Avail 20
        resD_Unavailable = resourceRepository.findByReference("R104").orElseThrow(); // Rate 1299, Avail 0
        resE = resourceRepository.findByReference("R105").orElseThrow(); // Rate 1000, Avail 3
    }

    /**
     * T1: Select same resource twice.
     * Expected: No duplicate/incorrect record; rule enforced.
     */
    @Test
    @DisplayName("T1: Select same resource twice - duplicate rule enforced")
    void testT1_SelectSameResourceTwice() {
        // Attempting to select duplicate resource IDs in same booking request
        List<Long> duplicateSelection = List.of(resA.getId(), resA.getId());

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> {
            bookingService.validateNoDuplicateResources(duplicateSelection);
        });

        assertTrue(ex.getMessage().contains("Duplicate resource selection rejected"),
                "Exception message should mention duplicate rejection");
    }

    /**
     * T2: Try quantities: 0, 1, 5, 6, 1.5.
     * Expected: Only 1 and 5 are valid.
     */
    @Test
    @DisplayName("T2: Try quantities: 0, 1, 5, 6, 1.5 - only 1 and 5 are valid")
    void testT2_QuantityValidation() {
        // 0 -> reject
        assertThrows(InvalidQuantityException.class, () -> {
            bookingService.validateAndParseQuantity("0", null);
        }, "Quantity 0 must be rejected");

        // 6 -> reject
        assertThrows(InvalidQuantityException.class, () -> {
            bookingService.validateAndParseQuantity("6", null);
        }, "Quantity 6 must be rejected");

        // 1.5 -> reject (decimal)
        assertThrows(InvalidQuantityException.class, () -> {
            bookingService.validateAndParseQuantity("1.5", null);
        }, "Quantity 1.5 must be rejected");

        // Negative -> reject
        assertThrows(InvalidQuantityException.class, () -> {
            bookingService.validateAndParseQuantity("-2", null);
        }, "Negative quantity must be rejected");

        // Non-numeric -> reject
        assertThrows(InvalidQuantityException.class, () -> {
            bookingService.validateAndParseQuantity("abc", null);
        }, "Non-numeric quantity must be rejected");

        // 1 -> valid
        int q1 = bookingService.validateAndParseQuantity("1", null);
        assertEquals(1, q1, "Quantity 1 must be accepted");

        // 5 -> valid
        int q5 = bookingService.validateAndParseQuantity("5", null);
        assertEquals(5, q5, "Quantity 5 must be accepted");
    }

    /**
     * T3: Try unavailable resource.
     * Expected: Rejected; state unchanged.
     */
    @Test
    @DisplayName("T3: Try unavailable resource - rejected and state unchanged")
    void testT3_TryUnavailableResource() {
        // R104 has availability 0
        Resource r104Before = resourceRepository.findByReference("R104").orElseThrow();
        assertEquals(0, r104Before.getAvailability(), "Precondition: R104 availability is 0");
        long initialReservationCount = reservationRepository.count();

        BookingRequestDto dto = new BookingRequestDto();
        dto.setResourceId(r104Before.getId());
        dto.setQuantityInput("1");
        dto.setContactName("Test Candidate");
        dto.setContactPhone("9876543210");
        dto.setBookingDate(LocalDate.now());

        assertThrows(ResourceUnavailableException.class, () -> {
            bookingService.createBooking(dto, testUser1);
        }, "Booking unavailable resource must throw ResourceUnavailableException");

        // Verify state is unchanged
        Resource r104After = resourceRepository.findByReference("R104").orElseThrow();
        assertEquals(0, r104After.getAvailability(), "R104 availability must remain 0");
        assertEquals(initialReservationCount, reservationRepository.count(), "No reservation record must be created");
    }

    /**
     * T4: Test amounts: Rs.999, Rs.1,000, Rs.1,001.
     * Expected:
     * Rs.999 → booking charge Rs.50
     * Rs.1,000 → booking charge Rs.0
     * Rs.1,001 → booking charge Rs.0
     */
    @Test
    @DisplayName("T4: Amount calculation boundary test - Rs.999, Rs.1000, Rs.1001")
    void testT4_AmountCalculationBoundaries() {
        // Rs. 999 boundary (< 1000)
        AmountCalculation c999 = AmountCalculation.calculate(new BigDecimal("999.00"), 1);
        assertEquals(new BigDecimal("999.00"), c999.getBaseAmount());
        assertEquals(new BigDecimal("50.00"), c999.getBookingCharge());
        assertEquals(new BigDecimal("1049.00"), c999.getGrandTotal());

        // Rs. 1,000 boundary (== 1000)
        AmountCalculation c1000 = AmountCalculation.calculate(new BigDecimal("1000.00"), 1);
        assertEquals(new BigDecimal("1000.00"), c1000.getBaseAmount());
        assertEquals(new BigDecimal("0.00"), c1000.getBookingCharge());
        assertEquals(new BigDecimal("1000.00"), c1000.getGrandTotal());

        // Rs. 1,001 boundary (> 1000)
        AmountCalculation c1001 = AmountCalculation.calculate(new BigDecimal("1001.00"), 1);
        assertEquals(new BigDecimal("1001.00"), c1001.getBaseAmount());
        assertEquals(new BigDecimal("0.00"), c1001.getBookingCharge());
        assertEquals(new BigDecimal("1001.00"), c1001.getGrandTotal());
    }

    /**
     * T5: Complete one valid reservation.
     * Expected: Successful record created and availability reduced.
     */
    @Test
    @DisplayName("T5: Complete one valid reservation - record created and availability reduced")
    void testT5_CompleteValidReservation() {
        Resource r101Before = resourceRepository.findByReference("R101").orElseThrow();
        int initialAvail = r101Before.getAvailability();
        assertTrue(initialAvail >= 2, "Precondition: sufficient availability");

        BookingRequestDto dto = new BookingRequestDto();
        dto.setResourceId(r101Before.getId());
        dto.setQuantityInput("2");
        dto.setContactName("John Doe");
        dto.setContactPhone("9876543210");
        dto.setBookingDate(LocalDate.now());

        Reservation created = bookingService.createBooking(dto, testUser1);

        assertNotNull(created.getId());
        assertNotNull(created.getReference());
        assertEquals(BookingStatus.CONFIRMED, created.getStatus());
        assertEquals("John Doe", created.getContactName());
        assertEquals("9876543210", created.getContactPhone());

        // Calculations check: Rate = 499, Qty = 2 -> Base = 998, Charge = 50, Total = 1048
        assertEquals(new BigDecimal("998.00"), created.getBaseAmount());
        assertEquals(new BigDecimal("50.00"), created.getBookingCharge());
        assertEquals(new BigDecimal("1048.00"), created.getGrandTotal());

        // Check availability reduced by 2
        Resource r101After = resourceRepository.findByReference("R101").orElseThrow();
        assertEquals(initialAvail - 2, r101After.getAvailability());

        // Check audit history
        List<BookingStatusHistory> history = historyRepository.findByReservationIdOrderByEventAtAsc(created.getId());
        assertFalse(history.isEmpty());
        assertEquals(BookingStatus.CONFIRMED, history.get(0).getNewStatus());
    }

    /**
     * T6: Confirm with invalid/missing details.
     * Expected: Rejected; no successful record and no partial update.
     */
    @Test
    @DisplayName("T6: Confirm with invalid/missing details - rejected and no partial update")
    void testT6_ConfirmWithInvalidOrMissingDetails() {
        Resource r102 = resourceRepository.findByReference("R102").orElseThrow();
        int initialAvail = r102.getAvailability();
        long initialReservationCount = reservationRepository.count();

        // 1. Blank name
        BookingRequestDto badNameDto = new BookingRequestDto();
        badNameDto.setResourceId(r102.getId());
        badNameDto.setQuantityInput("1");
        badNameDto.setContactName("   ");
        badNameDto.setContactPhone("9876543210");
        badNameDto.setBookingDate(LocalDate.now());

        assertThrows(BusinessRuleException.class, () -> {
            bookingService.createBooking(badNameDto, testUser1);
        }, "Blank name must be rejected");

        // 2. Phone with less than 10 digits
        BookingRequestDto shortPhoneDto = new BookingRequestDto();
        shortPhoneDto.setResourceId(r102.getId());
        shortPhoneDto.setQuantityInput("1");
        shortPhoneDto.setContactName("Alice");
        shortPhoneDto.setContactPhone("12345");
        shortPhoneDto.setBookingDate(LocalDate.now());

        assertThrows(BusinessRuleException.class, () -> {
            bookingService.createBooking(shortPhoneDto, testUser1);
        }, "Short phone must be rejected");

        // 3. Phone with letters
        BookingRequestDto letterPhoneDto = new BookingRequestDto();
        letterPhoneDto.setResourceId(r102.getId());
        letterPhoneDto.setQuantityInput("1");
        letterPhoneDto.setContactName("Alice");
        letterPhoneDto.setContactPhone("98765abcde");
        letterPhoneDto.setBookingDate(LocalDate.now());

        assertThrows(BusinessRuleException.class, () -> {
            bookingService.createBooking(letterPhoneDto, testUser1);
        }, "Phone with letters must be rejected");

        // 4. Missing booking date
        BookingRequestDto nullDateDto = new BookingRequestDto();
        nullDateDto.setResourceId(r102.getId());
        nullDateDto.setQuantityInput("1");
        nullDateDto.setContactName("Alice");
        nullDateDto.setContactPhone("9876543210");
        nullDateDto.setBookingDate(null);

        assertThrows(BusinessRuleException.class, () -> {
            bookingService.createBooking(nullDateDto, testUser1);
        }, "Missing date must be rejected");

        // Verify zero state changes
        Resource r102After = resourceRepository.findByReference("R102").orElseThrow();
        assertEquals(initialAvail, r102After.getAvailability(), "Availability must remain completely unchanged");
        assertEquals(initialReservationCount, reservationRepository.count(), "No partial reservations created");
    }

    /**
     * T7: Change availability before confirmation.
     * Expected: Transaction/request rejected if no longer available.
     */
    @Test
    @DisplayName("T7: Change availability before confirmation - rejected when no longer available")
    void testT7_ChangeAvailabilityBeforeConfirmation() {
        Resource r105 = resourceRepository.findByReference("R105").orElseThrow();
        int originalAvail = r105.getAvailability();

        // Simulate availability change to 0 by concurrent process/admin
        r105.setAvailability(0);
        resourceRepository.save(r105);

        BookingRequestDto dto = new BookingRequestDto();
        dto.setResourceId(r105.getId());
        dto.setQuantityInput("1");
        dto.setContactName("Late Booker");
        dto.setContactPhone("9876543210");
        dto.setBookingDate(LocalDate.now());

        assertThrows(ResourceUnavailableException.class, () -> {
            bookingService.createBooking(dto, testUser1);
        }, "Must fail because slot became unavailable before confirmation");

        // Restore original availability for test clean-up
        r105.setAvailability(originalAvail);
        resourceRepository.save(r105);
    }

    /**
     * T8: Attempt to access another user's booking/status update.
     * Expected: Access denied; no data/state change.
     */
    @Test
    @DisplayName("T8: Attempt to access another user's booking - access denied and no data change")
    void testT8_UnauthorizedAccessToAnotherUsersBooking() {
        // User 1 creates a booking
        BookingRequestDto dto = new BookingRequestDto();
        dto.setResourceId(resC.getId());
        dto.setQuantityInput("1");
        dto.setContactName("John Doe");
        dto.setContactPhone("9876543210");
        dto.setBookingDate(LocalDate.now());

        Reservation user1Booking = bookingService.createBooking(dto, testUser1);

        // 1. User 2 tries to view User 1's booking details
        assertThrows(UnauthorizedAccessException.class, () -> {
            bookingService.getBookingDetails(user1Booking.getId(), testUser2);
        }, "User 2 cannot view User 1's booking");

        // 2. User 2 tries to cancel User 1's booking
        assertThrows(UnauthorizedAccessException.class, () -> {
            bookingService.cancelBooking(user1Booking.getId(), testUser2);
        }, "User 2 cannot cancel User 1's booking");

        // 3. Regular user tries to update status (Admin operation)
        assertThrows(UnauthorizedAccessException.class, () -> {
            bookingService.updateStatus(user1Booking.getId(), BookingStatus.CHECKED_IN, testUser1);
        }, "Regular user cannot execute admin status update");

        // Verify status is unchanged
        Reservation reloaded = reservationRepository.findById(user1Booking.getId()).orElseThrow();
        assertEquals(BookingStatus.CONFIRMED, reloaded.getStatus(), "Status must remain unchanged");
    }

    /**
     * T9: Admin advances valid status and tries skipping.
     * Expected: Valid sequence works; skipping/invalid action fails.
     */
    @Test
    @DisplayName("T9: Admin advances valid status and tries skipping - skipping rejected, valid works")
    void testT9_AdminStatusTransitionsAndSkipping() {
        // Create booking at CONFIRMED
        BookingRequestDto dto = new BookingRequestDto();
        dto.setResourceId(resC.getId());
        dto.setQuantityInput("1");
        dto.setContactName("Sequence Test");
        dto.setContactPhone("9876543210");
        dto.setBookingDate(LocalDate.now());

        Reservation booking = bookingService.createBooking(dto, testUser1);
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());

        // 1. Try SKIPPING directly from CONFIRMED -> COMPLETED (Forbidden!)
        assertThrows(InvalidStatusTransitionException.class, () -> {
            bookingService.updateStatus(booking.getId(), BookingStatus.COMPLETED, adminUser);
        }, "Skipping from CONFIRMED directly to COMPLETED must fail");

        // Status remains CONFIRMED
        Reservation check1 = reservationRepository.findById(booking.getId()).orElseThrow();
        assertEquals(BookingStatus.CONFIRMED, check1.getStatus());

        // 2. Advance validly: CONFIRMED -> CHECKED_IN
        Reservation checkIn = bookingService.updateStatus(booking.getId(), BookingStatus.CHECKED_IN, adminUser);
        assertEquals(BookingStatus.CHECKED_IN, checkIn.getStatus());

        // 3. Try BACKWARD transition: CHECKED_IN -> CONFIRMED (Forbidden!)
        assertThrows(InvalidStatusTransitionException.class, () -> {
            bookingService.updateStatus(booking.getId(), BookingStatus.CONFIRMED, adminUser);
        }, "Backward transition from CHECKED_IN to CONFIRMED must fail");

        // 4. Advance validly: CHECKED_IN -> COMPLETED
        Reservation completed = bookingService.updateStatus(booking.getId(), BookingStatus.COMPLETED, adminUser);
        assertEquals(BookingStatus.COMPLETED, completed.getStatus());

        // 5. Try moving past terminal COMPLETED status
        assertThrows(InvalidStatusTransitionException.class, () -> {
            bookingService.updateStatus(booking.getId(), BookingStatus.CONFIRMED, adminUser);
        }, "Cannot transition out of terminal COMPLETED status");
    }

    /**
     * T10: Cancel eligible booking and try cancellation again.
     * Expected: First cancellation restores availability; second cancellation rejected.
     */
    @Test
    @DisplayName("T10: Cancel eligible booking and try cancellation again - restores once, second fails")
    void testT10_CancelEligibleBookingAndRepeatCancellation() {
        Resource r103 = resourceRepository.findByReference("R103").orElseThrow();
        int initialAvail = r103.getAvailability();

        // Book 3 units
        BookingRequestDto dto = new BookingRequestDto();
        dto.setResourceId(r103.getId());
        dto.setQuantityInput("3");
        dto.setContactName("Cancel Test User");
        dto.setContactPhone("9876543210");
        dto.setBookingDate(LocalDate.now());

        Reservation booking = bookingService.createBooking(dto, testUser1);

        // Availability decreased by 3
        Resource afterBooking = resourceRepository.findByReference("R103").orElseThrow();
        assertEquals(initialAvail - 3, afterBooking.getAvailability());

        // First cancellation -> Success
        Reservation cancelled = bookingService.cancelBooking(booking.getId(), testUser1);
        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());

        // Capacity restored exactly once
        Resource afterCancel = resourceRepository.findByReference("R103").orElseThrow();
        assertEquals(initialAvail, afterCancel.getAvailability());

        // Second cancellation -> REJECTED
        assertThrows(BusinessRuleException.class, () -> {
            bookingService.cancelBooking(booking.getId(), testUser1);
        }, "Repeated cancellation must be rejected");

        // Ensure capacity was NOT restored a second time
        Resource afterSecondAttempt = resourceRepository.findByReference("R103").orElseThrow();
        assertEquals(initialAvail, afterSecondAttempt.getAvailability(), "Capacity must not be restored twice");
    }

    /**
     * T11: Restart application after successful booking.
     * Expected: Created booking and changed state still exist persistently in MySQL.
     */
    @Test
    @DisplayName("T11: Persistence verification - booking and state exist in MySQL")
    void testT11_PersistenceVerification() {
        Resource r101 = resourceRepository.findByReference("R101").orElseThrow();
        int initialAvail = r101.getAvailability();

        BookingRequestDto dto = new BookingRequestDto();
        dto.setResourceId(r101.getId());
        dto.setQuantityInput("1");
        dto.setContactName("Persistence Test");
        dto.setContactPhone("9876543210");
        dto.setBookingDate(LocalDate.now());

        Reservation created = bookingService.createBooking(dto, testUser1);
        Long reservationId = created.getId();
        String bookingRef = created.getReference();

        // Verify retrieval directly from repository
        Reservation reloaded = reservationRepository.findById(reservationId).orElse(null);
        assertNotNull(reloaded, "Reservation must persist in database");
        assertEquals(bookingRef, reloaded.getReference());
        assertEquals("Persistence Test", reloaded.getContactName());
        assertEquals(BookingStatus.CONFIRMED, reloaded.getStatus());

        // Verify items and snapshots persisted
        assertFalse(reloaded.getItems().isEmpty());
        assertEquals(1, reloaded.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("499.00"), reloaded.getItems().get(0).getRateSnapshot());

        // Verify resource availability reduction persisted
        Resource r101Reloaded = resourceRepository.findByReference("R101").orElseThrow();
        assertEquals(initialAvail - 1, r101Reloaded.getAvailability());
    }

    /**
     * T12: Change resource rate after booking.
     * Expected: Old booking retains original rate snapshot.
     */
    @Test
    @DisplayName("T12: Change resource rate after booking - old booking retains original rate snapshot")
    void testT12_ChangeResourceRateAfterBooking() {
        Resource r101 = resourceRepository.findByReference("R101").orElseThrow();
        BigDecimal originalRate = r101.getRate(); // Rs. 499.00

        BookingRequestDto dto = new BookingRequestDto();
        dto.setResourceId(r101.getId());
        dto.setQuantityInput("1");
        dto.setContactName("Rate Snapshot Test");
        dto.setContactPhone("9876543210");
        dto.setBookingDate(LocalDate.now());

        Reservation booking = bookingService.createBooking(dto, testUser1);
        Long bookingId = booking.getId();

        // Update resource rate to Rs. 999.00 in database
        BigDecimal newRate = new BigDecimal("999.00");
        resourceService.updateResourceRate(r101.getId(), newRate);

        // Verify resource rate was modified
        Resource updatedResource = resourceRepository.findById(r101.getId()).orElseThrow();
        assertEquals(new BigDecimal("999.00"), updatedResource.getRate());

        // Reload existing booking from database
        Reservation reloadedBooking = reservationRepository.findById(bookingId).orElseThrow();
        ReservationItem reloadedItem = reloadedBooking.getItems().get(0);

        // Old booking MUST retain original rate snapshot (Rs. 499.00)
        assertEquals(originalRate, reloadedItem.getRateSnapshot(),
                "Old booking must retain original rate snapshot of Rs. 499.00 even after resource rate was modified");
        assertEquals(new BigDecimal("499.00"), reloadedBooking.getBaseAmount());
        assertEquals(new BigDecimal("549.00"), reloadedBooking.getGrandTotal());

        // Restore resource rate back for test hygiene
        resourceService.updateResourceRate(r101.getId(), originalRate);
    }
}
