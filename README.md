# ParkZone

ParkZone is a Spring Boot parking management system that allows users to manage vehicles, create parking reservations, pay reservation invoices, and track their reservation history.

The system is built as two separate Spring Boot applications:

- Main application: [ParkZone](https://github.com/dimitardimitrov1996/ParkZone)
- Billing microservice: [ParkZoneBillingService](https://github.com/dimitardimitrov1996/ParkZoneBillingService)

The main application provides the web interface, user management, reservation logic, vehicle management, administration panel, scheduling, validation, and security. The billing microservice is responsible for invoice creation, invoice updates, payments, cancellations, and refund status handling.

## Tech Stack

- Java 21
- Spring Boot 3.4.0
- Spring MVC
- Spring Security
- Spring Data JPA
- Hibernate
- Thymeleaf
- MySQL
- H2 Database for tests
- OpenFeign
- Maven
- Lombok
- JUnit 5
- Mockito
- MockMvc

## Application Overview

ParkZone supports two user roles:

- `USER`
- `ADMIN`

Regular users can register, log in, manage their profile, add vehicles, create reservations, pay invoices, and view their reservation history.

Administrators can manage users, roles, vehicles, reservations, parking lots, and parking spots through a dedicated admin panel.

## Main Features

### User Features

- User registration
- Login and logout
- Profile view and profile update
- Vehicle creation, editing, and deletion
- Reservation creation
- Reservation editing according to reservation status
- Reservation cancellation
- Invoice payment
- Reservation history

### Admin Features

- Admin dashboard
- View all users
- Activate and deactivate users
- Change user roles
- Prevent admins from changing their own role
- View all vehicles
- Delete and reactivate vehicles
- View all reservations
- Cancel active or pending reservations
- View parking lots
- Manage parking spots
- Change parking spot type
- Activate and deactivate parking spots

### Billing Features

The billing functionality is handled by the separate billing microservice.

Supported invoice operations:

- Create invoice
- Get invoice by reservation ID
- Update pending invoice
- Pay invoice
- Cancel invoice
- Mark paid cancelled invoice as refunded

Billing microservice repository:

[ParkZoneBillingService](https://github.com/dimitardimitrov1996/ParkZoneBillingService)

## Reservation Rules

ParkZone contains business rules for safe and consistent reservation management.

A user can create a reservation only when:

- The selected vehicle belongs to the user
- The selected vehicle is active
- The selected parking spot is active
- The parking spot belongs to the selected parking lot
- The vehicle does not already have an overlapping active or pending reservation
- The parking spot does not already have an overlapping active or pending reservation

Additional rules:

- Vans cannot reserve indoor parking spots.
- Only electric vehicles can reserve electric charging spots.
- Disabled parking spots can be reserved only by vehicles marked as requiring disabled parking.
- Daily reservations must be at least one full day.
- Monthly reservations must be exactly one full month.
- Yearly reservations must be exactly one full year.

## Reservation Editing Rules

Reservation editing depends on the reservation status.

### Pending Payment Reservations

Pending payment reservations can be fully edited.

The user can change:

- Vehicle
- Parking lot
- Parking spot
- Reservation type
- Start date
- End date

When a pending payment reservation is edited, ParkZone recalculates the price and updates the existing invoice in the billing microservice.

### Active Reservations Before Start Time

Paid active reservations can be edited only before their start time.

The user can change:

- Vehicle
- Parking spot in the same parking lot

The user cannot change:

- Parking lot
- Reservation type
- Start date
- End date

This prevents price mismatches after the invoice has already been paid.

### Started Reservations

Started active reservations cannot be edited.

### Cancelled and Completed Reservations

Cancelled and completed reservations cannot be edited.

## Payment Flow

When a reservation is created, it starts with status:

```text
PENDING_PAYMENT
```

ParkZone sends a request to the billing microservice and creates an invoice.

When the user pays the invoice:

1. ParkZone validates the payment form.
2. ParkZone calls the billing microservice.
3. The billing microservice marks the invoice as paid.
4. ParkZone changes the reservation status to `ACTIVE`.

If a paid reservation is cancelled, the invoice is marked as refunded in the billing microservice.

## Microservice Communication

ParkZone communicates with ParkZoneBillingService through OpenFeign.

Billing service base URL example:

```properties
billing.service.base.url=http://localhost:8081/api/v1/invoices
```

The billing service is protected with an API key. ParkZone sends the API key with every billing request using this header:

```http
X-API-Key
```

Both applications must use the same API key value.

## Security

The application uses Spring Security.

Security features:

- Role-based access control
- Protected user pages
- Protected admin pages
- CSRF protection
- Password hashing
- Disabled user login prevention
- Custom authenticated principal
- Admin-only access to `/admin/**`

Public pages:

- Home page
- Login page
- Register page

Protected user pages:

- Profile
- Vehicles
- Reservations
- Payments

Protected admin pages:

- Admin dashboard
- User management
- Vehicle management
- Reservation management
- Parking lot management
- Parking spot management

## Validation and Error Handling

ParkZone uses both DTO validation and custom business validation.

Validation examples:

- Required fields
- Valid email format
- Valid vehicle registration number format
- Valid reservation dates
- Valid reservation period
- Valid card number
- Valid card expiration date
- Valid CVV
- Valid profile data

The application includes centralized exception handling through `GlobalExceptionHandler`.

Handled cases include:

- Business rule violations
- Application exceptions
- Validation errors
- Missing resources
- Billing service communication problems

## Scheduling

The application includes scheduled reservation maintenance.

Scheduled jobs:

- Complete expired active reservations
- Cancel expired pending payment reservations

This keeps reservation statuses consistent over time.

## Caching

ParkZone uses caching for frequently accessed parking-related data to reduce unnecessary database calls and improve performance.

## Logging

The main business operations include logging.

Logged operations include:

- User registration
- Profile update
- Vehicle creation, update, deletion, and activation
- Reservation creation
- Reservation editing
- Reservation cancellation
- Expired reservation completion
- Expired pending payment cancellation
- Payment processing
- Billing service communication failures
- Admin operations

## Database

ParkZone uses MySQL for development/runtime and H2 for tests.

Example MySQL configuration:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/parkzone?createDatabaseIfNotExist=true
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:1234}
```

Example test database configuration:

```properties
spring.datasource.url=jdbc:h2:mem:parkzone_test;MODE=MYSQL
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=create-drop
```

## Environment Variables

ParkZone supports the following environment variables:

```properties
DB_USERNAME=root
DB_PASSWORD=1234
BILLING_API_KEY=your-api-key
```

The billing microservice must use the same billing API key.

Example:

```properties
billing.service.api.key=${BILLING_API_KEY}
```

## Running the Project

The full system requires both applications to be running.

### 1. Start MySQL

Make sure MySQL is running locally.

### 2. Start ParkZoneBillingService

Billing microservice repository:

[ParkZoneBillingService](https://github.com/dimitardimitrov1996/ParkZoneBillingService)

The billing microservice should run on port `8081`.

### 3. Start ParkZone

Main application repository:

[ParkZone](https://github.com/dimitardimitrov1996/ParkZone)

Run the application with Maven:

```bash
mvn spring-boot:run
```

Default application URL:

```text
http://localhost:8080
```

## Running Tests

Run all tests with:

```bash
mvn test
```

The project includes:

- Unit tests
- Service tests
- Controller tests
- MockMvc tests
- Security-related web tests
- Billing communication failure tests

The current test coverage is above the required 70% line coverage.

## Project Structure

```text
src/main/java/bg/softuni/parkzone
├── config
├── exception
├── model
│   ├── dto
│   └── entities
├── repository
├── scheduler
├── security
├── service
└── web
```

Layer responsibilities:

- `web` - Spring MVC controllers
- `service` - business logic
- `repository` - database access
- `model.entities` - JPA entities
- `model.dto` - request and response DTOs
- `security` - authentication-related classes
- `config` - application configuration
- `scheduler` - scheduled tasks
- `exception` - custom exceptions and global exception handling

## Related Repositories

- Main application: [ParkZone](https://github.com/dimitardimitrov1996/ParkZone)
- Billing microservice: [ParkZoneBillingService](https://github.com/dimitardimitrov1996/ParkZoneBillingService)

## Author

Dimitar Dimitrov

GitHub: [dimitardimitrov1996](https://github.com/dimitardimitrov1996)