package bg.softuni.parkzone.service.reservation;

import bg.softuni.parkzone.exception.BusinessRuleException;
import bg.softuni.parkzone.exception.billing.BillingServiceUnavailableException;
import bg.softuni.parkzone.exception.reservation.ReservationNotFoundException;
import bg.softuni.parkzone.exception.vehicle.VehicleNotFoundException;
import bg.softuni.parkzone.model.dto.billing.CreateInvoiceRequest;
import bg.softuni.parkzone.model.dto.billing.InvoiceResponse;
import bg.softuni.parkzone.model.dto.billing.UpdateInvoiceRequest;
import bg.softuni.parkzone.model.dto.reservation.ReservationCreateRequestDTO;
import bg.softuni.parkzone.model.dto.reservation.ReservationEditRequestDTO;
import bg.softuni.parkzone.model.dto.reservation.ReservationViewDTO;
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
import bg.softuni.parkzone.service.billing.client.BillingClient;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final ParkingSpotRepository parkingSpotRepository;
    private final BillingClient billingClient;

    public ReservationService(ReservationRepository reservationRepository,
                              UserRepository userRepository,
                              VehicleRepository vehicleRepository,
                              ParkingLotRepository parkingLotRepository,
                              ParkingSpotRepository parkingSpotRepository,
                              BillingClient billingClient) {
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.parkingLotRepository = parkingLotRepository;
        this.parkingSpotRepository = parkingSpotRepository;
        this.billingClient = billingClient;
    }

    @Transactional
    public void createReservation(ReservationCreateRequestDTO dto, UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessRuleException("User not found"));

        Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
                .orElseThrow(() -> new VehicleNotFoundException(dto.getVehicleId()));

        ParkingLot parkingLot = parkingLotRepository.findById(dto.getParkingLotId())
                .orElseThrow(() -> new BusinessRuleException("Parking lot not found"));

        ParkingSpot parkingSpot = parkingSpotRepository.findById(dto.getParkingSpotId())
                .orElseThrow(() -> new BusinessRuleException("Parking spot not found"));

        validateReservationCreation(dto, userId, vehicle, parkingLot, parkingSpot);

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
                .status(ReservationStatus.PENDING_PAYMENT)
                .totalPrice(calculatePrice(
                        dto.getReservationType(),
                        dto.getStartDate(),
                        dto.getEndDate(),
                        parkingLot
                ))
                .createdOn(LocalDateTime.now())
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);

        log.info("Reservation [{}] created for user [{}] with status [{}]",
                savedReservation.getId(), userId, savedReservation.getStatus());

        createInvoice(savedReservation, user);
    }

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAllByOrderByCreatedOnDesc();
    }

    @Transactional
    public void cancelReservationByAdmin(UUID reservationId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        if (!canManageReservation(reservation)) {
            log.warn("Admin cancellation rejected for reservation [{}] with status [{}]",
                    reservationId, reservation.getStatus());

            throw new BusinessRuleException("Only active or pending payment reservations can be cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        Reservation savedReservation = reservationRepository.save(reservation);

        log.info("Admin cancelled reservation [{}]", savedReservation.getId());

        try {
            billingClient.cancelInvoiceByReservationId(savedReservation.getId());
        } catch (FeignException e) {
            log.error("Billing service failed while cancelling invoice for reservation [{}]",
                    savedReservation.getId(), e);

            throw new BillingServiceUnavailableException();
        }
    }

    @Transactional
    public void cancelReservationByUser(UUID reservationId, UUID userId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        if (!reservation.getUser().getId().equals(userId)) {
            log.warn("User [{}] tried to cancel reservation [{}] owned by another user",
                    userId, reservationId);

            throw new BusinessRuleException("You cannot cancel this reservation");
        }

        if (!canManageReservation(reservation)) {
            log.warn("User [{}] cancellation rejected for reservation [{}] with status [{}]",
                    userId, reservationId, reservation.getStatus());

            throw new BusinessRuleException("Only active or pending payment reservations can be cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        Reservation savedReservation = reservationRepository.save(reservation);

        log.info("User [{}] cancelled reservation [{}]", userId, savedReservation.getId());

        try {
            billingClient.cancelInvoiceByReservationId(savedReservation.getId());
        } catch (FeignException e) {
            log.error("Billing service failed while cancelling invoice for reservation [{}]",
                    savedReservation.getId(), e);

            throw new BillingServiceUnavailableException();
        }
    }

    public ReservationEditRequestDTO getReservationForEdit(UUID reservationId, UUID userId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        if (!reservation.getUser().getId().equals(userId)) {
            throw new BusinessRuleException("You cannot edit this reservation");
        }

        if (!canManageReservation(reservation)) {
            throw new BusinessRuleException("Only active or pending payment reservations can be edited");
        }

        if (reservation.getStatus() == ReservationStatus.ACTIVE
                && !reservation.getStartDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Started reservations cannot be edited");
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

    @Transactional
    public void editReservation(ReservationEditRequestDTO dto, UUID reservationId, UUID userId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        if (!reservation.getUser().getId().equals(userId)) {
            log.warn("User [{}] tried to edit reservation [{}] owned by another user",
                    userId, reservationId);

            throw new BusinessRuleException("You cannot edit this reservation");
        }

        validateReservationCanBeEdited(reservation, dto);

        Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
                .orElseThrow(() -> new VehicleNotFoundException(dto.getVehicleId()));

        ParkingLot parkingLot = parkingLotRepository.findById(dto.getParkingLotId())
                .orElseThrow(() -> new BusinessRuleException("Parking lot not found"));

        ParkingSpot parkingSpot = parkingSpotRepository.findById(dto.getParkingSpotId())
                .orElseThrow(() -> new BusinessRuleException("Parking spot not found"));

        validateReservationEdit(dto, userId, reservation, vehicle, parkingLot, parkingSpot);

        reservation.setVehicle(vehicle);
        reservation.setParkingLot(parkingLot);
        reservation.setParkingSpot(parkingSpot);
        reservation.setDisabledParkingSpotRequired(parkingSpot.isDisabledSpot());
        reservation.setElectricChargingRequired(parkingSpot.isElectricChargingSpot());

        if (reservation.getStatus() == ReservationStatus.PENDING_PAYMENT) {
            reservation.setReservationType(dto.getReservationType());
            reservation.setStartDate(dto.getStartDate());
            reservation.setEndDate(dto.getEndDate());
        }

        reservation.setTotalPrice(calculatePrice(
                reservation.getReservationType(),
                reservation.getStartDate(),
                reservation.getEndDate(),
                parkingLot
        ));

        reservationRepository.save(reservation);

        if (reservation.getStatus() == ReservationStatus.PENDING_PAYMENT) {
            updatePendingInvoice(reservation);
        }

        log.info("Reservation [{}] edited by user [{}]. Status [{}], total price [{}]",
                reservation.getId(),
                userId,
                reservation.getStatus(),
                reservation.getTotalPrice());
    }

    public boolean isReservationStarted(UUID reservationId, UUID userId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        if (!reservation.getUser().getId().equals(userId)) {
            throw new BusinessRuleException("You cannot edit this reservation");
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

        log.info("Completed [{}] expired reservations", expiredReservations.size());
    }

    public List<ReservationViewDTO> getReservationViewsByUserId(UUID userId) {

        return reservationRepository.findAllByUserIdOrderByCreatedOnDesc(userId)
                .stream()
                .map(reservation -> {
                    try {
                        InvoiceResponse invoice = billingClient.getInvoiceByReservationId(reservation.getId());

                        return ReservationViewDTO.builder()
                                .reservation(reservation)
                                .invoiceId(invoice.getId())
                                .invoiceStatus(invoice.getStatus())
                                .canEdit(canEditReservation(reservation))
                                .build();

                    } catch (FeignException e) {
                        log.warn("Invoice data unavailable for reservation [{}]", reservation.getId());

                        return ReservationViewDTO.builder()
                                .reservation(reservation)
                                .invoiceId(null)
                                .invoiceStatus("UNAVAILABLE")
                                .canEdit(canEditReservation(reservation))
                                .build();
                    }
                })
                .toList();
    }

    @Transactional
    public void cancelExpiredPendingPaymentReservations() {

        List<Reservation> unpaidReservations =
                reservationRepository.findAllByStatusAndStartDateBefore(
                        ReservationStatus.PENDING_PAYMENT,
                        LocalDateTime.now()
                );

        for (Reservation reservation : unpaidReservations) {
            reservation.setStatus(ReservationStatus.CANCELLED);

            try {
                billingClient.cancelInvoiceByReservationId(reservation.getId());
            } catch (FeignException e) {
                log.error("Billing service failed while cancelling invoice for expired reservation [{}]",
                        reservation.getId(), e);

                throw new BillingServiceUnavailableException();
            }
        }

        reservationRepository.saveAll(unpaidReservations);

        log.info("Cancelled [{}] expired pending payment reservations", unpaidReservations.size());
    }

    private void validateReservationCreation(ReservationCreateRequestDTO dto,
                                             UUID userId,
                                             Vehicle vehicle,
                                             ParkingLot parkingLot,
                                             ParkingSpot parkingSpot) {

        if (!vehicle.isActive()) {
            throw new BusinessRuleException("This vehicle is no longer active");
        }

        if (!parkingSpot.isActive()) {
            throw new BusinessRuleException("This parking spot is not active");
        }

        if (!vehicle.getOwner().getId().equals(userId)) {
            throw new BusinessRuleException("You cannot make a reservation with this vehicle");
        }

        validateVehicleAndParkingLot(vehicle, parkingLot);
        validateReservationPeriod(dto.getStartDate(), dto.getEndDate(), dto.getReservationType());
        validateParkingSpotBelongsToParkingLot(parkingSpot, parkingLot);
        validateParkingSpotRequirements(vehicle, parkingSpot);
        validateParkingSpotAvailability(parkingSpot.getId(), dto.getEndDate(), dto.getStartDate());
        validateVehicleAvailability(vehicle.getId(), dto.getEndDate(), dto.getStartDate());
    }

    private void validateReservationEdit(ReservationEditRequestDTO dto,
                                         UUID userId,
                                         Reservation reservation,
                                         Vehicle vehicle,
                                         ParkingLot parkingLot,
                                         ParkingSpot parkingSpot) {

        if (!vehicle.getOwner().getId().equals(userId)) {
            throw new BusinessRuleException("You cannot use this vehicle");
        }

        if (!vehicle.isActive()) {
            throw new BusinessRuleException("This vehicle is not active");
        }

        if (!parkingSpot.isActive()) {
            throw new BusinessRuleException("This parking spot is not active");
        }

        validateParkingSpotBelongsToParkingLot(parkingSpot, parkingLot);
        validateActiveReservationParkingSpotChange(reservation, parkingSpot);
        validateVehicleAndParkingLot(vehicle, parkingLot);
        validateParkingSpotRequirements(vehicle, parkingSpot);

        validateParkingSpotAvailabilityForEdit(
                parkingSpot.getId(),
                reservation.getId(),
                dto.getEndDate(),
                dto.getStartDate()
        );

        validateVehicleAvailabilityForEdit(
                vehicle.getId(),
                reservation.getId(),
                dto.getEndDate(),
                dto.getStartDate()
        );
    }

    private void validateReservationCanBeEdited(Reservation reservation,
                                                ReservationEditRequestDTO dto) {

        if (!canManageReservation(reservation)) {
            log.warn("Reservation [{}] edit rejected because status is [{}]",
                    reservation.getId(), reservation.getStatus());

            throw new BusinessRuleException("Only active or pending payment reservations can be edited");
        }

        if (reservation.getStatus() == ReservationStatus.PENDING_PAYMENT) {

            if (!dto.getStartDate().isAfter(LocalDateTime.now())) {
                throw new BusinessRuleException("Start date must be in the future");
            }

            validateReservationPeriod(
                    dto.getStartDate(),
                    dto.getEndDate(),
                    dto.getReservationType()
            );

            return;
        }

        if (reservation.getStatus() == ReservationStatus.ACTIVE) {

            if (!reservation.getStartDate().isAfter(LocalDateTime.now())) {
                log.warn("Reservation [{}] edit rejected because it already started", reservation.getId());

                throw new BusinessRuleException("Started reservations cannot be edited");
            }

            boolean paidReservationPriceFieldsAreNotChanged =
                    reservation.getParkingLot().getId().equals(dto.getParkingLotId())
                            && reservation.getReservationType() == dto.getReservationType()
                            && reservation.getStartDate().isEqual(dto.getStartDate())
                            && reservation.getEndDate().isEqual(dto.getEndDate());

            if (!paidReservationPriceFieldsAreNotChanged) {
                log.warn("Reservation [{}] edit rejected because paid price fields were changed",
                        reservation.getId());

                throw new BusinessRuleException(
                        "Paid reservations can only change vehicle or parking spot before start time"
                );
            }
        }
    }

    private void validateActiveReservationParkingSpotChange(Reservation reservation,
                                                            ParkingSpot newParkingSpot) {

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            return;
        }

        if (!newParkingSpot.getParkingLot().getId().equals(reservation.getParkingLot().getId())) {
            log.warn("Reservation [{}] edit rejected because paid reservation parking lot was changed",
                    reservation.getId());

            throw new BusinessRuleException(
                    "Paid reservations can only change parking spot in the same parking lot"
            );
        }
    }

    private void validateVehicleAndParkingLot(Vehicle vehicle, ParkingLot parkingLot) {

        if (vehicle.getVehicleType() == VehicleType.VAN
                && parkingLot.getParkingType() == ParkingType.INDOOR) {
            throw new BusinessRuleException("Vans cannot reserve spots in the indoor parking lot");
        }
    }

    private void validateParkingSpotBelongsToParkingLot(ParkingSpot parkingSpot, ParkingLot parkingLot) {

        if (!parkingSpot.getParkingLot().getId().equals(parkingLot.getId())) {
            throw new BusinessRuleException("Selected parking spot does not belong to the selected parking lot");
        }
    }

    private void validateParkingSpotRequirements(Vehicle vehicle, ParkingSpot parkingSpot) {

        if (parkingSpot.isDisabledSpot() && !vehicle.isDisabledParkingRequired()) {
            throw new BusinessRuleException("Only vehicles marked as requiring disabled parking can reserve disabled parking spots");
        }

        if (parkingSpot.isElectricChargingSpot() && vehicle.getEngineType() != EngineType.ELECTRIC) {
            throw new BusinessRuleException("Only electric vehicles can reserve electric charging spots");
        }
    }

    private void validateParkingSpotAvailability(UUID parkingSpotId,
                                                 LocalDateTime endDate,
                                                 LocalDateTime startDate) {

        boolean parkingSpotIsTaken =
                reservationRepository.existsByParkingSpotIdAndStatusInAndStartDateBeforeAndEndDateAfter(
                        parkingSpotId,
                        getOccupyingStatuses(),
                        endDate,
                        startDate
                );

        if (parkingSpotIsTaken) {
            throw new BusinessRuleException("This parking spot is already reserved for the selected period");
        }
    }

    private void validateVehicleAvailability(UUID vehicleId,
                                             LocalDateTime endDate,
                                             LocalDateTime startDate) {

        boolean vehicleAlreadyReserved =
                reservationRepository.existsByVehicleIdAndStatusInAndStartDateBeforeAndEndDateAfter(
                        vehicleId,
                        getOccupyingStatuses(),
                        endDate,
                        startDate
                );

        if (vehicleAlreadyReserved) {
            throw new BusinessRuleException("This vehicle already has an active reservation for the selected period");
        }
    }

    private void validateParkingSpotAvailabilityForEdit(UUID parkingSpotId,
                                                        UUID reservationId,
                                                        LocalDateTime endDate,
                                                        LocalDateTime startDate) {

        boolean parkingSpotIsTaken =
                reservationRepository.existsByParkingSpotIdAndStatusInAndIdNotAndStartDateBeforeAndEndDateAfter(
                        parkingSpotId,
                        getOccupyingStatuses(),
                        reservationId,
                        endDate,
                        startDate
                );

        if (parkingSpotIsTaken) {
            throw new BusinessRuleException("This parking spot is already reserved for the selected period");
        }
    }

    private void validateVehicleAvailabilityForEdit(UUID vehicleId,
                                                    UUID reservationId,
                                                    LocalDateTime endDate,
                                                    LocalDateTime startDate) {

        boolean vehicleAlreadyReserved =
                reservationRepository.existsByVehicleIdAndStatusInAndIdNotAndStartDateBeforeAndEndDateAfter(
                        vehicleId,
                        getOccupyingStatuses(),
                        reservationId,
                        endDate,
                        startDate
                );

        if (vehicleAlreadyReserved) {
            throw new BusinessRuleException("This vehicle already has an active reservation for the selected period");
        }
    }

    private void validateReservationPeriod(LocalDateTime startDate,
                                           LocalDateTime endDate,
                                           ReservationType reservationType) {

        if (startDate == null || endDate == null || reservationType == null) {
            return;
        }

        if (endDate.isBefore(startDate) || endDate.isEqual(startDate)) {
            throw new BusinessRuleException("End date must be after start date");
        }

        switch (reservationType) {
            case DAILY -> {
                LocalDateTime minimumEndDate = startDate.plusDays(1);

                if (endDate.isBefore(minimumEndDate)) {
                    throw new BusinessRuleException("Daily reservation must be at least 1 full day");
                }
            }
            case MONTHLY -> {
                LocalDateTime expectedEndDate = startDate.plusMonths(1);

                if (!endDate.isEqual(expectedEndDate)) {
                    throw new BusinessRuleException("Monthly reservation must be exactly 1 full month");
                }
            }
            case YEARLY -> {
                LocalDateTime expectedEndDate = startDate.plusYears(1);

                if (!endDate.isEqual(expectedEndDate)) {
                    throw new BusinessRuleException("Yearly reservation must be exactly 1 full year");
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

    private void createInvoice(Reservation reservation, User user) {

        CreateInvoiceRequest invoiceRequest = CreateInvoiceRequest.builder()
                .reservationId(reservation.getId())
                .userId(user.getId())
                .amount(reservation.getTotalPrice())
                .currency("EUR")
                .build();

        try {
            billingClient.createInvoice(invoiceRequest);

            log.info("Invoice creation requested for reservation [{}] with amount [{}] EUR",
                    reservation.getId(), reservation.getTotalPrice());

        } catch (FeignException e) {
            log.error("Billing service failed while creating invoice for reservation [{}]",
                    reservation.getId(), e);

            throw new BillingServiceUnavailableException();
        }
    }

    private void updatePendingInvoice(Reservation reservation) {

        UpdateInvoiceRequest request = UpdateInvoiceRequest.builder()
                .amount(reservation.getTotalPrice())
                .currency("EUR")
                .build();

        try {
            billingClient.updateInvoiceByReservationId(reservation.getId(), request);

            log.info("Invoice for reservation [{}] updated with amount [{}] EUR",
                    reservation.getId(), reservation.getTotalPrice());

        } catch (FeignException e) {
            log.error("Billing service failed while updating invoice for reservation [{}]",
                    reservation.getId(), e);

            throw new BillingServiceUnavailableException();
        }
    }

    private boolean canManageReservation(Reservation reservation) {
        return reservation.getStatus() == ReservationStatus.ACTIVE
                || reservation.getStatus() == ReservationStatus.PENDING_PAYMENT;
    }

    private List<ReservationStatus> getOccupyingStatuses() {
        return List.of(
                ReservationStatus.ACTIVE,
                ReservationStatus.PENDING_PAYMENT
        );
    }

    private boolean canEditReservation(Reservation reservation) {

        if (reservation.getStatus() == ReservationStatus.PENDING_PAYMENT) {
            return true;
        }

        return reservation.getStatus() == ReservationStatus.ACTIVE
                && reservation.getStartDate().isAfter(LocalDateTime.now());
    }

    public ReservationStatus getReservationStatus(UUID reservationId, UUID userId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationNotFoundException(reservationId));

        if (!reservation.getUser().getId().equals(userId)) {
            throw new BusinessRuleException("You cannot access this reservation");
        }

        return reservation.getStatus();
    }

}