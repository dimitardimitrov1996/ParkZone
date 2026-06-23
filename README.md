# ParkZone

ParkZone is a Spring MVC web application for managing parking vehicles, parking spots, and parking reservations. The application supports regular users and administrators, includes session-based authentication, and applies business rules for vehicle ownership, parking spot availability, reservation periods, and user status management.

This project was developed as an individual project for the SoftUni Spring Fundamentals course, May 2026.

## Tech Stack

* Java 21
* Spring Boot 3.4.0
* Spring MVC
* Thymeleaf
* Spring Data JPA
* Hibernate
* MySQL
* Maven
* Jakarta Bean Validation
* Lombok
* BCrypt password hashing through Spring Security Crypto
* HTML, CSS, JavaScript

## Application Roles

The application has three access levels:

* Guest

    * Can access the landing page, register page, and login page.
* User

    * Can manage profile data.
    * Can create, edit, and delete personal vehicles.
    * Can create, edit, and cancel reservations.
    * Can view reservation history.
* Admin

    * Can access the admin dashboard.
    * Can manage users.
    * Can manage parking spots.
    * Can manage vehicles.
    * Can cancel reservations.

## Main Domain Entities

### User

Represents an application user. A user can be either a regular user or an administrator.

Main fields:

* UUID id
* username
* email
* password
* firstName
* lastName
* phoneNumber
* role
* active status

### Vehicle

Represents a vehicle owned by a user.

Main fields:

* UUID id
* registrationNumber
* brand
* model
* vehicleType
* engineType
* disabledParkingRequired
* owner
* active status

### ParkingLot

Represents a parking lot in the system.

Main fields:

* UUID id
* name
* parkingType
* capacity
* disabledParkingSpots
* electricChargingSpots
* dailyPrice
* monthlyPrice
* yearlyPrice

### ParkingSpot

Represents a specific parking spot inside a parking lot.

Main fields:

* UUID id
* spotNumber
* disabledSpot
* electricChargingSpot
* active status
* parkingLot

### Reservation

Represents a parking reservation made by a user.

Main fields:

* UUID id
* user
* vehicle
* parkingLot
* parkingSpot
* reservationType
* reservationStatus
* startDate
* endDate
* totalPrice
* createdOn
* disabledParkingSpotRequired
* electricChargingRequired

## Main Functionalities

### Authentication and Access Control

* User registration with server-side validation.
* User login with session-based authentication.
* Passwords are stored hashed with BCrypt.
* The authenticated user's id is stored in the HTTP session.
* Guests can access only public pages.
* Logged users can access user pages.
* Admin pages are protected with role checks.
* Inactive users are automatically redirected to login.

### User Profile Management

Users can update their personal profile information:

* first name
* last name
* phone number

Username and email are displayed in the profile page but are not edited from there.

### Vehicle Management

Users can:

* create vehicles
* view their active vehicles
* edit their vehicles
* delete vehicles

Vehicle rules:

* Registration numbers must be unique.
* A user can edit or delete only vehicles owned by that user.
* Deleting a vehicle performs a soft delete by setting the vehicle as inactive.
* Deleting a vehicle cancels all active reservations for that vehicle.
* If a vehicle has an active reservation for an electric charging spot, the engine type cannot be changed from electric to another type.
* If a vehicle has an active reservation for a disabled spot, the disabled parking requirement cannot be removed.
* If a vehicle has an active indoor reservation, it cannot be changed to VAN.

### Reservation Management

Users can:

* create reservations
* view reservation history
* edit active reservations
* cancel active reservations

Reservation rules:

* A user can manage only their own reservations.
* Only active reservations can be edited or cancelled.
* A vehicle must be active to be used in a reservation.
* A parking spot must be active to be reserved.
* The selected parking spot must belong to the selected parking lot.
* Vans cannot reserve indoor parking spots.
* Only electric vehicles can reserve electric charging spots.
* Only vehicles marked as requiring disabled parking can reserve disabled parking spots.
* A parking spot cannot be reserved if it is already taken for the selected period.
* A vehicle cannot have another active reservation for the same period.
* Daily reservations must be at least one full day.
* Daily reservations are priced by number of reserved days.
* Monthly reservations must be exactly one full month.
* Yearly reservations must be exactly one full year.
* Expired active reservations are automatically marked as completed.
* Completed reservations are kept in the reservation history and do not block future reservations.
* Started reservations can still be edited, but only the vehicle, parking lot, and parking spot can be changed. Dates and reservation type cannot be changed after the reservation has started.

### Admin User Management

Admins can:

* view all users
* activate or deactivate users

Admin user rules:

* An admin cannot deactivate their own admin account.
* When a user is deactivated, all user vehicles become inactive.
* When a user is deactivated, all active user reservations are cancelled.
* When a user is reactivated, their vehicles become active again.
* Cancelled reservations remain cancelled and are not automatically restored.

### Admin Vehicle Management

Admins can:

* view all vehicles
* deactivate vehicles
* reactivate vehicles

Admin vehicle rules:

* Deactivating a vehicle cancels all active reservations for that vehicle.
* A vehicle cannot be activated if its owner is inactive.
* Reactivating a vehicle does not restore cancelled reservations.

### Admin Reservation Management

Admins can:

* view all reservations
* cancel active reservations

Only active reservations can be cancelled.

### Admin Parking Spot Management

Admins can:

* view parking lots
* view parking spots by parking lot
* change a parking spot to normal
* change a parking spot to disabled
* change a parking spot to electric charging
* activate or deactivate parking spots

Parking spot rules:

* A parking spot with an active reservation cannot be changed or deactivated.
* Parking lot disabled and electric spot counters are updated when spot types are changed.

## Web Pages

The application contains the following Thymeleaf pages:

### Public Pages

* Landing page
* Login page
* Register page

### User Pages

* Home page
* User profile page
* Vehicle list page
* Vehicle create page
* Vehicle edit page
* Reservation list page
* Reservation create page
* Reservation edit page

### Admin Pages

* Admin dashboard
* User management page
* Vehicle management page
* Reservation management page
* Parking lot management page
* Parking spot management page

## Validation and Error Handling

The project uses server-side validation with Jakarta Bean Validation. Invalid form submissions are returned to the same form with field-specific error messages.

Examples of validated input:

* username length and uniqueness
* email format and uniqueness
* password length
* vehicle registration number
* vehicle brand and model
* selected vehicle, parking lot, and parking spot
* reservation start and end dates
* reservation type
* phone number format

Business rule violations are handled in the service layer and returned to the user through controller-level error handling.

## Default Data

The application initializes default data when the database is empty.

### Default Admin

* Email: [admin@abv.bg](mailto:admin@abv.bg)
* Password is set from the application.properties file with the property `app.admin.password`.

### Default User

* Email: [user@abv.bg](mailto:user@abv.bg)
* Password is set from the application.properties file with the property `app.user.password`.

### Default Parking Lots

Outdoor Parking:

* Capacity: 30 spots
* Disabled spots: 5
* Electric charging spots: 0
* Daily price: 5 EUR
* Monthly price: 120 EUR
* Yearly price: 1200 EUR

Indoor Parking:

* Capacity: 30 spots
* Disabled spots: 5
* Electric charging spots: 5
* Daily price: 10 EUR
* Monthly price: 240 EUR
* Yearly price: 2400 EUR

### Default Vehicles

The default user receives several vehicles for testing:

* Diesel car
* Electric car
* Van
* Car requiring disabled parking

## Running the Application

### Prerequisites

* Java 21 or compatible Java 17+ version
* Maven
* MySQL Server

### Database Configuration

The application uses MySQL. The default configuration is in:

```text
src/main/resources/application.properties
```

Default database settings:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/park-zone-application?createDatabaseIfNotExist=true
spring.datasource.username=//insert-your-username-here//
spring.datasource.password=//insert-your-password-here//
spring.jpa.hibernate.ddl-auto=update
```

Before running the application, update the MySQL username and password.

### Start the Application

From the project root directory, run:

```bash
mvn spring-boot:run
```

Then open:

```text
http://localhost:8080
```

## Security

The project uses custom session-based authentication.

* The user id is stored in the HTTP session after successful login.
* Protected endpoints are checked by a Spring MVC interceptor.
* Admin endpoints require the ADMIN role.
* Passwords are hashed with BCrypt.
* Inactive users cannot access protected pages.

## Author

Created as an individual project for the SoftUni Spring Fundamentals course, May 2026.
