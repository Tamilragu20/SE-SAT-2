package com.lab.booking.config;

import com.lab.booking.model.*;
import com.lab.booking.repository.ResourceRepository;
import com.lab.booking.repository.ScheduleRepository;
import com.lab.booking.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final ScheduleRepository scheduleRepository;

    public DataInitializer(UserRepository userRepository,
                           ResourceRepository resourceRepository,
                           ScheduleRepository scheduleRepository) {
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        initializeUsers();
        initializeResourcesAndSchedules();
    }

    public void initializeUsers() {
        if (userRepository.count() == 0) {
            userRepository.save(new User(
                    "John Doe",
                    "9876543210",
                    "user1@lab.edu",
                    "password",
                    Role.USER,
                    UserStatus.ACTIVE
            ));

            userRepository.save(new User(
                    "Jane Smith",
                    "9123456780",
                    "user2@lab.edu",
                    "password",
                    Role.USER,
                    UserStatus.ACTIVE
            ));

            userRepository.save(new User(
                    "Lab Administrator",
                    "9988776655",
                    "admin@lab.edu",
                    "admin123",
                    Role.ADMINISTRATOR,
                    UserStatus.ACTIVE
            ));
        }
    }

    public void initializeResourcesAndSchedules() {
        if (resourceRepository.count() == 0) {
            // Section 5 Exact Demonstration Data
            // R101 | Equipment Slot A | Rs.499  | Availability 10
            Resource r101 = resourceRepository.save(new Resource(
                    "R101",
                    "Equipment Slot A",
                    "Centrifuge & Spectroscopy",
                    new BigDecimal("499.00"),
                    10,
                    true
            ));

            // R102 | Equipment Slot B | Rs.799  | Availability 5
            Resource r102 = resourceRepository.save(new Resource(
                    "R102",
                    "Equipment Slot B",
                    "Chromatography",
                    new BigDecimal("799.00"),
                    5,
                    true
            ));

            // R103 | Equipment Slot C | Rs.199  | Availability 20
            Resource r103 = resourceRepository.save(new Resource(
                    "R103",
                    "Equipment Slot C",
                    "Microscopy & Imaging",
                    new BigDecimal("199.00"),
                    20,
                    true
            ));

            // R104 | Equipment Slot D | Rs.1,299 | Availability 0
            Resource r104 = resourceRepository.save(new Resource(
                    "R104",
                    "Equipment Slot D",
                    "Thermal Analysis",
                    new BigDecimal("1299.00"),
                    0,
                    true
            ));

            // R105 | Equipment Slot E | Rs.1,000 | Availability 3
            Resource r105 = resourceRepository.save(new Resource(
                    "R105",
                    "Equipment Slot E",
                    "Nuclear Magnetic Resonance",
                    new BigDecimal("1000.00"),
                    3,
                    true
            ));

            // Preload schedules for resources
            LocalDate today = LocalDate.now();
            scheduleRepository.save(new Schedule("SCH-R101-01", r101, today, LocalTime.of(9, 0), LocalTime.of(11, 0), true));
            scheduleRepository.save(new Schedule("SCH-R101-02", r101, today.plusDays(1), LocalTime.of(14, 0), LocalTime.of(16, 0), true));

            scheduleRepository.save(new Schedule("SCH-R102-01", r102, today, LocalTime.of(10, 0), LocalTime.of(12, 0), true));
            scheduleRepository.save(new Schedule("SCH-R102-02", r102, today.plusDays(1), LocalTime.of(15, 0), LocalTime.of(17, 0), true));

            scheduleRepository.save(new Schedule("SCH-R103-01", r103, today, LocalTime.of(9, 30), LocalTime.of(11, 30), true));
            scheduleRepository.save(new Schedule("SCH-R103-02", r103, today.plusDays(1), LocalTime.of(13, 0), LocalTime.of(15, 0), true));

            scheduleRepository.save(new Schedule("SCH-R104-01", r104, today, LocalTime.of(11, 0), LocalTime.of(13, 0), true));

            scheduleRepository.save(new Schedule("SCH-R105-01", r105, today, LocalTime.of(14, 0), LocalTime.of(16, 0), true));
            scheduleRepository.save(new Schedule("SCH-R105-02", r105, today.plusDays(2), LocalTime.of(10, 0), LocalTime.of(12, 0), true));
        }
    }

    @Transactional
    public void resetDemonstrationData() {
        resourceRepository.findByReference("R101").ifPresent(r -> {
            r.setAvailability(10);
            r.setRate(new BigDecimal("499.00"));
            r.setActive(true);
            resourceRepository.save(r);
        });
        resourceRepository.findByReference("R102").ifPresent(r -> {
            r.setAvailability(5);
            r.setRate(new BigDecimal("799.00"));
            r.setActive(true);
            resourceRepository.save(r);
        });
        resourceRepository.findByReference("R103").ifPresent(r -> {
            r.setAvailability(20);
            r.setRate(new BigDecimal("199.00"));
            r.setActive(true);
            resourceRepository.save(r);
        });
        resourceRepository.findByReference("R104").ifPresent(r -> {
            r.setAvailability(0);
            r.setRate(new BigDecimal("1299.00"));
            r.setActive(true);
            resourceRepository.save(r);
        });
        resourceRepository.findByReference("R105").ifPresent(r -> {
            r.setAvailability(3);
            r.setRate(new BigDecimal("1000.00"));
            r.setActive(true);
            resourceRepository.save(r);
        });
    }
}
