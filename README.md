# Laboratory Equipment Booking System

> **College Software Engineering SAT-II Assessment Web Application**  
> Built with Java 17, Spring Boot 3.3.4, Spring Data JPA / Hibernate, MySQL, and Thymeleaf.

---

## Table of Contents
1. [Project Title](#1-project-title)
2. [Problem Statement](#2-problem-statement)
3. [Features](#3-features)
4. [Actors & Roles](#4-actors--roles)
5. [Functional Requirements (FR1 - FR10)](#5-functional-requirements-fr1---fr10)
6. [Non-Functional Requirements](#6-non-functional-requirements)
7. [Exact Business Rules (BR1 - BR10)](#7-exact-business-rules-br1---br10)
8. [Technology Stack](#8-technology-stack)
9. [Architecture & Design](#9-architecture--design)
10. [Database Setup & Schema](#10-database-setup--schema)
11. [How to Run & Access](#11-how-to-run--access)
12. [Demo Users & Credentials](#12-demo-users--credentials)
13. [Demonstration Workflow Walkthrough](#13-demonstration-workflow-walkthrough)
14. [Automated Test Suite (T1 - T12)](#14-automated-test-suite-t1---t12)
15. [AI-Assisted Development](#15-ai-assisted-development)
16. [Limitations](#16-limitations)
17. [Future Improvements](#17-future-improvements)

---

## 1. Project Title
**Laboratory Equipment Booking System**  
A full-stack, transactional web application developed for academic research laboratory slot reservation and administrative tracking for Software Engineering SAT-II Assessment.

---

## 2. Problem Statement
Academic and research laboratories often encounter scheduling conflicts, overlapping reservations, resource contention, and lack of historical audit trails when sharing high-value equipment (such as centrifuges, spectrometers, chromatographs, and NMR spectrometers).

This system provides a reliable, persistent, role-based platform that guarantees:
- Atomic reservation transactions with strictly enforced availability quotas.
- Immutability of billing and rate snapshots at the moment of reservation.
- Strict sequential booking status progression managed by laboratory administrators.
- Fair, single-restoration user cancellations to prevent inventory leakage.
- Clear role separation and data access protection between researchers and administrators.

---

## 3. Features
- **Equipment Slot Catalog**: Real-time inspection of active laboratory instruments, rates, schedules, and available capacities.
- **Dynamic Cost Calculator**: Instant frontend and authoritative backend calculation of base amount, booking charges, and grand totals.
- **Atomic Booking Pipeline**: Multi-condition validation of user eligibility, inventory, quantities, and contact details with automated rollback on failure.
- **Snapshot Immutability**: Historical preservation of equipment metadata and pricing at the exact time of reservation confirmation.
- **Role-Based Access Control**:
  - Regular researchers (Users) access only their personal bookings and can cancel eligible slots.
  - Lab administrators oversee all transactions and enforce physical workflow stages.
- **Strict Linear Status Flow**: Administrative progression from `CONFIRMED` $\to$ `CHECKED_IN / BOARDED` $\to$ `COMPLETED` with strict prohibition of backward movement or stage skipping.
- **One-Click Quick Login & Switching**: Streamlined persona switching designed specifically for demonstration and evaluator assessments.

---

## 4. Actors & Roles

There are exactly **two** roles in the system:

| Actor | Role Key | Permissions & Capabilities |
| :--- | :--- | :--- |
| **Researcher / Student** | `USER` | - Search and view active equipment slots<br>- Select equipment and input quantities (1 to 5 units)<br>- Compute and review price breakdown<br>- Submit confirmed reservations<br>- View **only** their own bookings and details<br>- Cancel **only** their own eligible `CONFIRMED` bookings |
| **Lab Administrator** | `ADMINISTRATOR` | - Inspect global transaction ledger across all users<br>- View audit history logs and timeline events<br>- Advance booking status strictly sequentially (`CONFIRMED` $\to$ `CHECKED_IN` $\to$ `COMPLETED`)<br>- Update equipment catalog rates and capacities<br>- Reset demonstration data to baseline |

---

## 5. Functional Requirements (FR1 - FR10)

- **FR1: Equipment Slot Search & Display**:
  Display active equipment slots with reference code, category, schedule, unit rate, and live availability.
- **FR2: Slot and Quantity Selection**:
  Allow users to select an active slot and choose a quantity strictly between 1 and 5 (inclusive). Reject 0, negative numbers, decimals, and non-numeric values.
- **FR3: Amount Calculation**:
  - $\text{Base Amount} = \text{Rate} \times \text{Quantity}$
  - $\text{Booking Charge} = \text{Rs. 50}$ when $\text{Base Amount} < \text{Rs. 1,000}$; $\text{Rs. 0}$ when $\text{Base Amount} \ge \text{Rs. 1,000}$.
  - $\text{Grand Total} = \text{Base Amount} + \text{Booking Charge}$.
- **FR4: Pre-Confirmation Validation**:
  Validate booking details, contact parameters, inventory levels, and user status before executing confirmation.
- **FR5: Atomic Booking Confirmation**:
  Confirm booking only when all validation passes. Atomically create reservation, save item snapshot, decrease resource availability, and record status history.
- **FR6: Scoped User View**:
  Logged-in users can only view their own bookings and booking details. Cross-user access is blocked.
- **FR7: Sequential Administrative Status Flow**:
  Administrators can only advance statuses through the sequence: `CONFIRMED` $\to$ `CHECKED_IN / BOARDED` $\to$ `COMPLETED`. Skipping or reversing status is rejected.
- **FR8: User Cancellation & Inventory Restoration**:
  Users can cancel only eligible `CONFIRMED` reservations. On cancellation, status transitions to `CANCELLED` and inventory is restored **exactly once**. Repeated cancellations are rejected.
- **FR9: Clear Error Reporting**:
  Display user-friendly, specific error messages for invalid quantities, malformed phone numbers, insufficient capacity, unauthorized access, and invalid status transitions.
- **FR10: Persistent MySQL Storage**:
  All records (`users`, `resources`, `reservations`, `reservation_items`, `schedules`, `booking_status_history`) persist in MySQL across server restarts.

---

## 6. Non-Functional Requirements
- **Data Integrity & Consistency**: Enforce foreign keys, unique constraint on references, and transactional atomicity (`@Transactional`).
- **Security & Authorization**: Session-based role validation ensuring users cannot modify other users' bookings or perform administrative tasks.
- **Performance & Responsiveness**: Sub-second page loads, lightweight Bootstrap 5 UI, and indexed database queries.
- **Auditability**: Complete historical event tracking for every booking creation, transition, and cancellation.
- **Maintainability**: Clean MVC architecture with clear separation of concerns.

---

## 7. Exact Business Rules (BR1 - BR10)

1. **BR1 - Quantity Limits**: Quantity must be an integer between 1 and 5 (inclusive). Values of 0, negative, >5, decimals (e.g., 1.5), and letters are rejected.
2. **BR2 - Availability Check**: Resource can only be booked if active and current availability $\ge$ requested quantity. Checked again at confirmation.
3. **BR3 - Fare Snapshot**: The unit rate at the moment of confirmation is frozen into the reservation item snapshot. Subsequent changes to resource prices do not alter past bookings.
4. **BR4 - Price & Charge Threshold**: Base total = rate $\times$ quantity. Booking fee is Rs. 50 if base total < Rs. 1,000, and Rs. 0 if base total $\ge$ Rs. 1,000.
5. **BR5 - Contact Validation**: Contact name cannot be blank. Phone number must consist of exactly 10 digits (`^\d{10}$`). Booking date must be valid.
6. **BR6 - All-or-Nothing Confirmation**: Any validation failure results in total rollback; no partial record or capacity change occurs.
7. **BR7 - Transactional Success**: Successful booking creates reservation, items, snapshots, deducts capacity, and writes history log in a single transaction.
8. **BR8 - Status Progression**: Only `CONFIRMED` $\to$ `CHECKED_IN` $\to$ `COMPLETED`. No skipping (e.g., `CONFIRMED` $\to$ `COMPLETED` is forbidden).
9. **BR9 - Single-Time Inventory Restoration**: Cancellation sets status to `CANCELLED` and restores inventory once. Re-cancellation is strictly blocked.
10. **BR10 - Strict Access Control**: Users can only see/cancel their own reservations. Administrators handle operations across all bookings.

---

## 8. Technology Stack

- **Backend Framework**: Spring Boot 3.3.4 (Java 17 LTS)
- **Web Layer**: Spring MVC (`@Controller`, `@GetMapping`, `@PostMapping`)
- **Data Persistence**: Spring Data JPA with Hibernate ORM 6.5.3
- **Database**: MySQL Server 8.0 (persistent local storage on port 3306)
- **Template Engine**: Thymeleaf 3
- **Frontend Assets**: HTML5, Bootstrap 5.3, Bootstrap Icons, JavaScript
- **Build & Dependency Management**: Apache Maven 3.9
- **Automated Testing**: JUnit 5, Spring Boot Test (`@SpringBootTest`)

---

## 9. Architecture & Design

The project strictly follows a clean **MVC Layered Architecture**:

```
                       [ Browser Client / Evaluator ]
                                     |
                                     v
                       +----------------------------+
                       |    Presentation Layer      |
                       | Thymeleaf Views (.html)    |
                       +----------------------------+
                                     |
                                     v
+-------------------------------------------------------------------------+
| Controller Layer                                                        |
| - AuthController   : Login, Logout, Persona Quick-Switch                |
| - UserController   : Dashboard, Equipment Catalog, Booking, Cancel      |
| - AdminController  : Dashboard, Ledger, Status Updates, Reset           |
+-------------------------------------------------------------------------+
                                     |
                                     v
+-------------------------------------------------------------------------+
| Service Layer (Business Logic & Transactions)                           |
| - BookingService       : Orchestrates booking, cancellation, transitions|
| - AvailabilityService  : Capacity check, deduction, and restoration     |
| - ResourceService      : Equipment queries and catalog maintenance      |
| - UserService          : User verification and session management       |
+-------------------------------------------------------------------------+
                                     |
                                     v
+-------------------------------------------------------------------------+
| Repository Layer (Spring Data JPA)                                      |
| - UserRepository, ResourceRepository, ReservationRepository             |
| - ReservationItemRepository, ScheduleRepository, HistoryRepository      |
+-------------------------------------------------------------------------+
                                     |
                                     v
+-------------------------------------------------------------------------+
| Database Layer (MySQL 8.0 - Persistent Storage)                         |
| users | resources | reservations | reservation_items | schedules | ...  |
+-------------------------------------------------------------------------+
```

---

## 10. Database Setup & Schema

### MySQL Configuration (`src/main/resources/application.properties`)
```properties
server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3306/lab_equipment_booking_system?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=tamilragu@123
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

### Demonstration Initial Data (Preloaded Automatically)
As specified in **Section 5 Exact Demonstration Data**:

| Ref Code | Equipment Name | Category | Rate | Initial Availability | Status |
| :---: | :--- | :--- | :---: | :---: | :---: |
| **R101** | Equipment Slot A | Centrifuge & Spectroscopy | Rs. 499.00 | 10 | ACTIVE |
| **R102** | Equipment Slot B | Chromatography | Rs. 799.00 | 5 | ACTIVE |
| **R103** | Equipment Slot C | Microscopy & Imaging | Rs. 199.00 | 20 | ACTIVE |
| **R104** | Equipment Slot D | Thermal Analysis | Rs. 1,299.00 | 0 *(Unavailable)* | ACTIVE |
| **R105** | Equipment Slot E | Nuclear Magnetic Resonance | Rs. 1,000.00 | 3 | ACTIVE |

---

## 11. How to Run & Access

### Prerequisites
- Java Development Kit (JDK 17 LTS) installed.
- MySQL Server 8.0 running locally on port 3306.
- Apache Maven installed (or use `./mvnw`).

### Build & Package
```powershell
mvn clean package -DskipTests
```

### Launch Application
```powershell
java -jar target/lab-equipment-booking-1.0.0.jar
```
Or directly using Spring Boot Maven plugin:
```powershell
mvn spring-boot:run
```

The application starts on: **`http://localhost:8080`**

---

## 12. Demo Users & Credentials

To enable rapid demonstration, both form-based authentication and 1-click Quick Login buttons are provided on the login page:

| Role | Name | Email | Password | Quick Login Link |
| :--- | :--- | :--- | :--- | :--- |
| **USER 1** | John Doe | `user1@lab.edu` | `password` | [Login as User 1](http://localhost:8080/quick-login?email=user1@lab.edu) |
| **USER 2** | Jane Smith | `user2@lab.edu` | `password` | [Login as User 2](http://localhost:8080/quick-login?email=user2@lab.edu) |
| **ADMIN** | Lab Administrator | `admin@lab.edu` | `admin123` | [Login as Admin](http://localhost:8080/quick-login?email=admin@lab.edu) |

---

## 13. Demonstration Workflow Walkthrough

1. **Login & Role Recognition**:
   - Navigate to `http://localhost:8080/login`.
   - Click **"Login as User 1 (John Doe)"**.
   - User is redirected to `http://localhost:8080/dashboard`.
2. **Search & Check Availability**:
   - Click **"Browse & Book Equipment"** or navigate to `http://localhost:8080/equipment`.
   - Observe live cards for R101 (10 available), R102 (5 available), R104 (0 available - disabled button).
3. **Select Equipment & Form Filling**:
   - Click **"Book Slot"** on **R101 (Equipment Slot A)**.
   - Test frontend dynamic calculator: Change quantity to `2`.
     - Base Amount: $2 \times 499 = \text{Rs. } 998.00$
     - Booking Charge: $\text{Rs. } 50.00$ (since base amount < Rs. 1,000)
     - Grand Total: $\text{Rs. } 1,048.00$
4. **Confirm Reservation**:
   - Enter contact phone `9876543210` and submit booking.
   - Redirects to **Booking Confirmation** page (`/booking/confirmation/{id}`) showing unique reference (e.g., `RES-20261008...`), rate snapshot, and status `CONFIRMED`.
   - Equipment catalog now reflects R101 availability reduced from $10 \to 8$.
5. **Inspect User Bookings**:
   - Navigate to `http://localhost:8080/my-bookings`.
   - Confirm only John Doe's bookings appear.
6. **Admin Status Progression**:
   - Switch persona by clicking **Admin (Lab Administrator)** quick login.
   - Open **Admin Dashboard** (`/admin/dashboard`) or **Transactions** (`/admin/bookings`).
   - Click **"Manage"** on John Doe's booking.
   - Notice the system offers only the next valid status: **CHECKED_IN / BOARDED**.
   - Click **"Advance Status to CHECKED_IN / BOARDED"**. Status updates.
   - Next, advance status to **COMPLETED**.
7. **User Cancellation Test**:
   - Log back in as `user1@lab.edu`.
   - Book 1 unit of **R105 (Equipment Slot E)** (availability drops $3 \to 2$).
   - In **My Bookings**, click **"Cancel"** on the newly created `CONFIRMED` booking.
   - Confirm status transitions to `CANCELLED`.
   - Check equipment catalog: R105 availability is restored back to $3$.
   - Confirm that the **"Cancel"** button is now disabled, preventing repeated cancellation.

---

## 14. Automated Test Suite (T1 - T12)

All 12 mandatory assessment test cases are implemented in [`LaboratoryEquipmentBookingTests.java`](file:///c:/Users/hp/Desktop/SE/SE%20SAT%202/src/test/java/com/lab/booking/LaboratoryEquipmentBookingTests.java):

```powershell
mvn test
```

### Execution Results:
```
[INFO] Running com.lab.booking.LaboratoryEquipmentBookingTests
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 9.384 s
[INFO] BUILD SUCCESS
```

### Test Case Verification Summary:

| Test ID | Test Case Description | Test Method | Outcome |
| :---: | :--- | :--- | :---: |
| **T1** | Select same resource twice | `testT1_SelectSameResourceTwice()` | **PASSED** |
| **T2** | Validate quantities: 0, 1, 5, 6, 1.5 | `testT2_QuantityValidation()` | **PASSED** |
| **T3** | Try booking unavailable resource (R104) | `testT3_TryUnavailableResource()` | **PASSED** |
| **T4** | Test boundary amounts: Rs.999, Rs.1000, Rs.1001 | `testT4_AmountCalculationBoundaries()` | **PASSED** |
| **T5** | Complete one valid reservation | `testT5_CompleteValidReservation()` | **PASSED** |
| **T6** | Confirm with invalid/missing details | `testT6_ConfirmWithInvalidOrMissingDetails()` | **PASSED** |
| **T7** | Change availability before confirmation | `testT7_ChangeAvailabilityBeforeConfirmation()` | **PASSED** |
| **T8** | Unauthorized access to another user's booking | `testT8_UnauthorizedAccessToAnotherUsersBooking()` | **PASSED** |
| **T9** | Admin status progression and skipping rejection | `testT9_AdminStatusTransitionsAndSkipping()` | **PASSED** |
| **T10** | Cancel eligible booking and prevent double cancellation | `testT10_CancelEligibleBookingAndRepeatCancellation()` | **PASSED** |
| **T11** | Verify MySQL persistence across operations | `testT11_PersistenceVerification()` | **PASSED** |
| **T12** | Verify rate snapshot immutability after price update | `testT12_ChangeResourceRateAfterBooking()` | **PASSED** |

---

## 15. AI-Assisted Development

### AI Tool Used
- **Antigravity** (Powered by Google DeepMind's Advanced Agentic Coding architecture).

### Representative Prompts Used
1. *"Translate the college Software Engineering SAT-II requirements for a Laboratory Equipment Booking System into an MVC architecture with Spring Boot 3, Spring Data JPA, and MySQL."*
2. *"Define JPA entities for Reservation, ReservationItem, and BookingStatusHistory ensuring rate snapshots and immutable audit trails are recorded."*
3. *"Implement strict status transition logic in BookingService so that administrators can only advance CONFIRMED $\to$ CHECKED_IN $\to$ COMPLETED and skipping is rejected."*
4. *"Create a comprehensive JUnit 5 test suite validating test cases T1 through T12 against a live MySQL database."*

### What AI Suggested
- Layered package structure separating web controllers, service transactions, domain entities, and data repositories.
- Atomic execution of reservation creation using `@Transactional` to ensure simultaneous capacity decrement, snapshot recording, and history logging.
- BigDecimal financial arithmetic with `RoundingMode.HALF_UP` for precision in fee calculations.
- Clean Thymeleaf UI layouts with Bootstrap 5 cards and status badges.

### What Was Reviewed and Corrected Manually
- **Decimal Quantity Validation**: Initial AI suggestions relied solely on standard integer parsing which would truncate decimal strings (e.g., `"1.5"` to `1` or fail generically). This was manually updated to explicitly check for decimals (`trimmed.contains(".")`) and throw `InvalidQuantityException`.
- **Status Skipping Enforcement**: Added explicit checks in `BookingService.updateStatus` comparing the target status against `currentStatus.getNextAdminStatus()`, ensuring skipping directly from `CONFIRMED` to `COMPLETED` is strictly blocked with `InvalidStatusTransitionException`.
- **Cancellation Idempotency**: Verified that `restoreAvailability` is executed strictly once and subsequent attempts on `CANCELLED` bookings throw clear business exceptions.
- **Rate Snapshot Independence**: Ensured `ReservationItem` persists `rateSnapshot` independently of `Resource.rate` so subsequent catalog price updates never alter historical bookings.

> **Note**: AI assistance was utilized as an engineering accelerator for architectural scaffolding and boilerplate generation; all business rules, boundary conditions, and test assertions were reviewed and verified against the assessment specification.

---

## 16. Limitations
- **Local Scope**: Built for single-institution local deployment; does not integrate external cloud storage or distributed message queues.
- **Synchronous Locking**: Inventory availability relies on transactional database isolation rather than distributed Redis locks.
- **Simplified Authentication**: Utilizes session-based role attribution tailored for rapid examiner evaluation rather than multi-factor SSO or OAuth2.

---

## 17. Future Improvements
- Multi-factor authentication (MFA) and Single Sign-On (SSO) integration with university LDAP systems.
- Automated email/SMS notifications upon booking confirmation and status changes.
- Integration of QR-code generation on booking confirmations for contactless equipment check-in.
- Advanced conflict resolution and recurring reservation scheduling for semester-long research projects.
