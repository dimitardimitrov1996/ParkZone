package bg.softuni.parkzone.service.reservation;

import bg.softuni.parkzone.model.dto.reservation.ReservationCreateRequestDTO;
import bg.softuni.parkzone.model.dto.reservation.ReservationEditRequestDTO;
import bg.softuni.parkzone.model.entities.parkinglot.ParkingLot;
import bg.softuni.parkzone.model.entities.parkinglot.ParkingType;
import bg.softuni.parkzone.model.entities.parkingspot.ParkingSpot;
import bg.softuni.parkzone.model.entities.reservation.Reservation;
import bg.softuni.parkzone.model.entities.reservation.ReservationStatus;
import bg.softuni.parkzone.model.entities.reservation.ReservationType;
import bg.softuni.parkzone.model.entities.user.User;
import bg.softuni.parkzone.model.entities.vehicle.EngineType;
import bg.softuni.parkzone.model.entities.vehicle.Vehicle;
import bg.softuni.parkzone.model.entities.vehicle.VehicleType;
import bg.softuni.parkzone.repository.parkinglot.ParkingLotRepository;
import bg.softuni.parkzone.repository.parkingspot.ParkingSpotRepository;
import bg.softuni.parkzone.repository.reservation.ReservationRepository;
import bg.softuni.parkzone.repository.user.UserRepository;
import bg.softuni.parkzone.repository.vehicle.VehicleRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final ParkingSpotRepository parkingSpotRepository;

    public ReservationService(ReservationRepository reservationRepository, UserRepository userRepository, VehicleRepository vehicleRepository, ParkingLotRepository parkingLotRepository, ParkingSpotRepository parkingSpotRepository) {
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.parkingLotRepository = parkingLotRepository;
        this.parkingSpotRepository = parkingSpotRepository;
    }


    public List<Reservation> getReservationsByUserId(UUID userId) {
        return reservationRepository.findAllByUserId(userId);
    }

    public void createReservation(ReservationCreateRequestDTO dto, UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        ParkingLot parkingLot = parkingLotRepository.findById(dto.getParkingLotId())
                .orElseThrow(() -> new IllegalArgumentException("Parking lot not found"));

        ParkingSpot parkingSpot = parkingSpotRepository.findById(dto.getParkingSpotId())
                .orElseThrow(() -> new IllegalArgumentException("Parking spot not found"));

        if (!vehicle.isActive()) {
            throw new IllegalArgumentException("This vehicle is no longer active");
        }

        if (!parkingSpot.isActive()) {
            throw new IllegalArgumentException("This parking spot is not active");
        }

        if (!vehicle.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot make a reservation with this vehicle");
        }

        if (vehicle.getVehicleType() == VehicleType.VAN
                && parkingLot.getParkingType() == ParkingType.INDOOR) {
            throw new IllegalArgumentException("Vans cannot reserve spots in the indoor parking lot");
        }

        validateReservationPeriod(
                dto.getStartDate(),
                dto.getEndDate(),
                dto.getReservationType()
        );

        boolean parkingSpotIsTaken =
                reservationRepository.existsByParkingSpotIdAndStatusAndStartDateBeforeAndEndDateAfter(
                        parkingSpot.getId(),
                        ReservationStatus.ACTIVE,
                        dto.getEndDate(),
                        dto.getStartDate()
                );

        if (parkingSpotIsTaken) {
            throw new IllegalArgumentException("This parking spot is already reserved for the selected period");
        }

        boolean vehicleAlreadyReserved =
                reservationRepository.existsByVehicleIdAndStatusAndStartDateBeforeAndEndDateAfter(
                        vehicle.getId(),
                        ReservationStatus.ACTIVE,
                        dto.getEndDate(),
                        dto.getStartDate()
                );

        if (vehicleAlreadyReserved) {
            throw new IllegalArgumentException("This vehicle already has an active reservation for the selected period");
        }

        if (!parkingSpot.getParkingLot().getId().equals(parkingLot.getId())) {
            throw new IllegalArgumentException("Selected parking spot does not belong to the selected parking lot");
        }

        if (parkingSpot.isDisabledSpot() && !vehicle.isDisabledParkingRequired()) {
            throw new IllegalArgumentException("Only vehicles marked as requiring disabled parking can reserve disabled parking spots");
        }

        if (parkingSpot.isElectricChargingSpot() && vehicle.getEngineType() != EngineType.ELECTRIC) {
            throw new IllegalArgumentException("Only electric vehicles can reserve electric charging spots");
        }

        Reservation reservation = Reservation.builder()
                .user(user)
                .vehicle(vehicle)
                .parkingLot(parkingLot)
                .parkingSpot(parkingSpot)
                .reservationType(dto.getReservationType())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .disabledParkingSpotRequired(parkingSpot.isDisabledSpot())
                .electricChargingRequired(parkingSpot.isElectricChargingSpot())
                .status(ReservationStatus.ACTIVE)
                .totalPrice(calculatePrice(
                        dto.getReservationType(),
                        dto.getStartDate(),
                        dto.getEndDate(),
                        parkingLot
                ))
                .createdOn(LocalDateTime.now())
                .build();

        reservationRepository.save(reservation);
    }

    private void validateReservationPeriod(LocalDateTime startDate,
                                           LocalDateTime endDate,
                                           ReservationType reservationType) {

        if (startDate == null || endDate == null || reservationType == null) {
            return;
        }

        if (endDate.isBefore(startDate) || endDate.isEqual(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        switch (reservationType) {
            case DAILY -> {
            }
            case MONTHLY -> {
                LocalDateTime expectedEndDate = startDate.plusMonths(1);

                if (!endDate.isEqual(expectedEndDate)) {
                    throw new IllegalArgumentException("Monthly reservation must be exactly 1 full month");
                }
            }
            case YEARLY -> {
                LocalDateTime expectedEndDate = startDate.plusYears(1);

                if (!endDate.isEqual(expectedEndDate)) {
                    throw new IllegalArgumentException("Yearly reservation must be exactly 1 full year");
                }
            }
        }
    }

    private BigDecimal calculatePrice(ReservationType reservationType,
                                      LocalDateTime startDate,
                                      LocalDateTime endDate,
                                      ParkingLot parkingLot) {

        return switch (reservationType) {
            case DAILY -> {
                long days = calculateDays(startDate, endDate);
                yield parkingLot.getDailyPrice().multiply(BigDecimal.valueOf(days));
            }
            case MONTHLY -> parkingLot.getMonthlyPrice();
            case YEARLY -> parkingLot.getYearlyPrice();
        };
    }

    private long calculateDays(LocalDateTime startDate, LocalDateTime endDate) {

        long hours = ChronoUnit.HOURS.between(startDate, endDate);

        if (hours <= 24) {
            return 1;
        }

        long days = hours / 24;

        if (hours % 24 != 0) {
            days++;
        }

        return days;
    }

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    public void cancelReservationByAdmin(UUID reservationId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active reservations can be cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        reservationRepository.save(reservation);
    }

    public void cancelReservationByUser(UUID reservationId, UUID userId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (!reservation.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot cancel this reservation");
        }

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active reservations can be cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        reservationRepository.save(reservation);
    }

    public ReservationEditRequestDTO getReservationForEdit(UUID reservationId, UUID userId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (!reservation.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot edit this reservation");
        }

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active reservations can be edited");
        }

        return ReservationEditRequestDTO.builder()
                .vehicleId(reservation.getVehicle().getId())
                .parkingLotId(reservation.getParkingLot().getId())
                .parkingSpotId(reservation.getParkingSpot().getId())
                .reservationType(reservation.getReservationType())
                .startDate(reservation.getStartDate())
                .endDate(reservation.getEndDate())
                .build();
    }

    public void editReservation(ReservationEditRequestDTO dto, UUID reservationId, UUID userId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (!reservation.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot edit this reservation");
        }

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active reservations can be edited");
        }

        LocalDateTime now = LocalDateTime.now();

        boolean reservationAlreadyStarted = !reservation.getStartDate().isAfter(now);

        if (reservationAlreadyStarted) {

            if (!dto.getStartDate().isEqual(reservation.getStartDate())) {
                throw new IllegalArgumentException("Start date cannot be changed after reservation has started");
            }

            if (!dto.getEndDate().isEqual(reservation.getEndDate())) {
                throw new IllegalArgumentException("End date cannot be changed after reservation has started");
            }

            if (dto.getReservationType() != reservation.getReservationType()) {
                throw new IllegalArgumentException("Reservation type cannot be changed after reservation has started");
            }

        } else {

            if (!dto.getStartDate().isAfter(now)) {
                throw new IllegalArgumentException("Start date must be in the future");
            }

            validateReservationPeriod(
                    dto.getStartDate(),
                    dto.getEndDate(),
                    dto.getReservationType()
            );
        }

        Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        if (!vehicle.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot use this vehicle");
        }

        if (!vehicle.isActive()) {
            throw new IllegalArgumentException("This vehicle is not active");
        }

        ParkingLot parkingLot = parkingLotRepository.findById(dto.getParkingLotId())
                .orElseThrow(() -> new IllegalArgumentException("Parking lot not found"));

        ParkingSpot parkingSpot = parkingSpotRepository.findById(dto.getParkingSpotId())
                .orElseThrow(() -> new IllegalArgumentException("Parking spot not found"));

        if (!parkingSpot.isActive()) {
            throw new IllegalArgumentException("This parking spot is not active");
        }

        if (!parkingSpot.getParkingLot().getId().equals(parkingLot.getId())) {
            throw new IllegalArgumentException("Selected parking spot does not belong to the selected parking lot");
        }

        if (vehicle.getVehicleType() == VehicleType.VAN
                && parkingLot.getParkingType() == ParkingType.INDOOR) {
            throw new IllegalArgumentException("Vans cannot reserve spots in the indoor parking lot");
        }

        boolean parkingSpotIsTaken =
                reservationRepository.existsByParkingSpotIdAndStatusAndIdNotAndStartDateBeforeAndEndDateAfter(
                        parkingSpot.getId(),
                        ReservationStatus.ACTIVE,
                        reservationId,
                        dto.getEndDate(),
                        dto.getStartDate()
                );

        if (parkingSpotIsTaken) {
            throw new IllegalArgumentException("This parking spot is already reserved for the selected period");
        }

        boolean vehicleAlreadyReserved =
                reservationRepository.existsByVehicleIdAndStatusAndIdNotAndStartDateBeforeAndEndDateAfter(
                        vehicle.getId(),
                        ReservationStatus.ACTIVE,
                        reservationId,
                        dto.getEndDate(),
                        dto.getStartDate()
                );

        if (vehicleAlreadyReserved) {
            throw new IllegalArgumentException("This vehicle already has an active reservation for the selected period");
        }

        if (parkingSpot.isDisabledSpot() && !vehicle.isDisabledParkingRequired()) {
            throw new IllegalArgumentException("Only vehicles marked as requiring disabled parking can reserve disabled parking spots");
        }

        if (parkingSpot.isElectricChargingSpot() && vehicle.getEngineType() != EngineType.ELECTRIC) {
            throw new IllegalArgumentException("Only electric vehicles can reserve electric charging spots");
        }

        reservation.setVehicle(vehicle);
        reservation.setParkingLot(parkingLot);
        reservation.setParkingSpot(parkingSpot);

        if (!reservationAlreadyStarted) {
            reservation.setReservationType(dto.getReservationType());
            reservation.setStartDate(dto.getStartDate());
            reservation.setEndDate(dto.getEndDate());
        }

        reservation.setDisabledParkingSpotRequired(parkingSpot.isDisabledSpot());
        reservation.setElectricChargingRequired(parkingSpot.isElectricChargingSpot());

        reservation.setTotalPrice(calculatePrice(
                reservation.getReservationType(),
                reservation.getStartDate(),
                reservation.getEndDate(),
                parkingLot
        ));

        reservationRepository.save(reservation);
    }

    public boolean isReservationStarted(UUID reservationId, UUID userId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

        if (!reservation.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot edit this reservation");
        }

        return !reservation.getStartDate().isAfter(LocalDateTime.now());
    }

    @Transactional
    public void completeExpiredReservations() {

        List<Reservation> expiredReservations =
                reservationRepository.findAllByStatusAndEndDateBefore(
                        ReservationStatus.ACTIVE,
                        LocalDateTime.now()
                );

        for (Reservation reservation : expiredReservations) {
            reservation.setStatus(ReservationStatus.COMPLETED);
        }

        reservationRepository.saveAll(expiredReservations);
    }

}
