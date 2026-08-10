package bg.softuni.parkzone.service.reservation;

import bg.softuni.parkzone.exception.BusinessRuleException;
import bg.softuni.parkzone.exception.billing.BillingServiceUnavailableException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private ParkingLotRepository parkingLotRepository;

    @Mock
    private ParkingSpotRepository parkingSpotRepository;

    @Mock
    private BillingClient billingClient;

    @InjectMocks
    private ReservationService reservationService;

    private UUID userId;
    private UUID vehicleId;
    private UUID parkingLotId;
    private UUID parkingSpotId;

    private User user;
    private Vehicle vehicle;
    private ParkingLot parkingLot;
    private ParkingSpot parkingSpot;
    private ReservationCreateRequestDTO createRequestDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();
        parkingLotId = UUID.randomUUID();
        parkingSpotId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .username("user1")
                .isActive(true)
                .build();

        vehicle = Vehicle.builder()
                .id(vehicleId)
                .owner(user)
                .registrationNumber("EA2222EV")
                .brand("Tesla")
                .model("Model 3")
                .vehicleType(VehicleType.CAR)
                .engineType(EngineType.ELECTRIC)
                .disabledParkingRequired(false)
                .active(true)
                .build();

        parkingLot = ParkingLot.builder()
                .id(parkingLotId)
                .name("Outdoor Parking")
                .parkingType(ParkingType.OUTDOOR)
                .capacity(30)
                .disabledParkingSpots(5)
                .electricChargingSpots(0)
                .dailyPrice(BigDecimal.valueOf(5))
                .monthlyPrice(BigDecimal.valueOf(120))
                .yearlyPrice(BigDecimal.valueOf(1200))
                .build();

        parkingSpot = ParkingSpot.builder()
                .id(parkingSpotId)
                .parkingLot(parkingLot)
                .spotNumber(1)
                .active(true)
                .electricChargingSpot(false)
                .disabledSpot(false)
                .build();

        createRequestDTO = ReservationCreateRequestDTO.builder()
                .vehicleId(vehicleId)
                .parkingLotId(parkingLotId)
                .parkingSpotId(parkingSpotId)
                .reservationType(ReservationType.DAILY)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .build();
    }

    @Test
    void createReservation_whenDataIsValid_shouldCreatePendingPaymentReservationAndInvoice() {
        mockCreateReservationDependencies();

        when(reservationRepository.existsByParkingSpotIdAndStatusInAndStartDateBeforeAndEndDateAfter(
                eq(parkingSpotId), anyList(), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(false);

        when(reservationRepository.existsByVehicleIdAndStatusInAndStartDateBeforeAndEndDateAfter(
                eq(vehicleId), anyList(), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(false);

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> {
                    Reservation reservation = invocation.getArgument(0);
                    reservation.setId(UUID.randomUUID());
                    return reservation;
                });

        reservationService.createReservation(createRequestDTO, userId);

        verify(reservationRepository).save(argThat(reservation ->
                reservation.getStatus() == ReservationStatus.PENDING_PAYMENT
                        && reservation.getUser().getId().equals(userId)
                        && reservation.getVehicle().getId().equals(vehicleId)
                        && reservation.getParkingLot().getId().equals(parkingLotId)
                        && reservation.getParkingSpot().getId().equals(parkingSpotId)
                        && reservation.getTotalPrice().compareTo(BigDecimal.valueOf(5)) == 0
        ));

        verify(billingClient).createInvoice(argThat(request ->
                request.getUserId().equals(userId)
                        && request.getAmount().compareTo(BigDecimal.valueOf(5)) == 0
                        && request.getCurrency().equals("EUR")
        ));
    }

    @Test
    void createReservation_whenParkingSpotIsAlreadyTaken_shouldThrowBusinessRuleException() {
        mockCreateReservationDependencies();

        when(reservationRepository.existsByParkingSpotIdAndStatusInAndStartDateBeforeAndEndDateAfter(
                eq(parkingSpotId), anyList(), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> reservationService.createReservation(createRequestDTO, userId));

        verify(reservationRepository, never()).save(any());
        verify(billingClient, never()).createInvoice(any());
    }

    @Test
    void createReservation_whenVehicleIsAlreadyReserved_shouldThrowBusinessRuleException() {
        mockCreateReservationDependencies();

        when(reservationRepository.existsByParkingSpotIdAndStatusInAndStartDateBeforeAndEndDateAfter(
                eq(parkingSpotId), anyList(), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(false);

        when(reservationRepository.existsByVehicleIdAndStatusInAndStartDateBeforeAndEndDateAfter(
                eq(vehicleId), anyList(), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> reservationService.createReservation(createRequestDTO, userId));

        verify(reservationRepository, never()).save(any());
        verify(billingClient, never()).createInvoice(any());
    }

    @Test
    void createReservation_whenBillingServiceFails_shouldThrowBillingServiceUnavailableException() {
        mockCreateReservationDependencies();

        when(reservationRepository.existsByParkingSpotIdAndStatusInAndStartDateBeforeAndEndDateAfter(
                eq(parkingSpotId), anyList(), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(false);

        when(reservationRepository.existsByVehicleIdAndStatusInAndStartDateBeforeAndEndDateAfter(
                eq(vehicleId), anyList(), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(false);

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> {
                    Reservation reservation = invocation.getArgument(0);
                    reservation.setId(UUID.randomUUID());
                    return reservation;
                });

        when(billingClient.createInvoice(any(CreateInvoiceRequest.class)))
                .thenThrow(mock(FeignException.class));

        assertThrows(BillingServiceUnavailableException.class,
                () -> reservationService.createReservation(createRequestDTO, userId));
    }

    @Test
    void createReservation_whenVanTriesToReserveIndoorParking_shouldThrowBusinessRuleException() {
        vehicle.setVehicleType(VehicleType.VAN);
        parkingLot.setParkingType(ParkingType.INDOOR);

        mockCreateReservationDependencies();

        assertThrows(BusinessRuleException.class,
                () -> reservationService.createReservation(createRequestDTO, userId));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_whenNonElectricVehicleTriesToReserveElectricSpot_shouldThrowBusinessRuleException() {
        vehicle.setEngineType(EngineType.DIESEL);
        parkingSpot.setElectricChargingSpot(true);

        mockCreateReservationDependencies();

        assertThrows(BusinessRuleException.class,
                () -> reservationService.createReservation(createRequestDTO, userId));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_whenVehicleDoesNotNeedDisabledSpotButSpotIsDisabled_shouldThrowBusinessRuleException() {
        vehicle.setDisabledParkingRequired(false);
        parkingSpot.setDisabledSpot(true);

        mockCreateReservationDependencies();

        assertThrows(BusinessRuleException.class,
                () -> reservationService.createReservation(createRequestDTO, userId));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_whenVehicleIsInactive_shouldThrowBusinessRuleException() {
        vehicle.setActive(false);

        mockCreateReservationDependencies();

        assertThrows(BusinessRuleException.class,
                () -> reservationService.createReservation(createRequestDTO, userId));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_whenParkingSpotIsInactive_shouldThrowBusinessRuleException() {
        parkingSpot.setActive(false);

        mockCreateReservationDependencies();

        assertThrows(BusinessRuleException.class,
                () -> reservationService.createReservation(createRequestDTO, userId));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_whenVehicleDoesNotBelongToUser_shouldThrowBusinessRuleException() {
        User otherUser = User.builder()
                .id(UUID.randomUUID())
                .isActive(true)
                .build();

        vehicle.setOwner(otherUser);

        mockCreateReservationDependencies();

        assertThrows(BusinessRuleException.class,
                () -> reservationService.createReservation(createRequestDTO, userId));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_whenParkingSpotDoesNotBelongToParkingLot_shouldThrowBusinessRuleException() {
        ParkingLot otherParkingLot = ParkingLot.builder()
                .id(UUID.randomUUID())
                .name("Indoor Parking")
                .parkingType(ParkingType.INDOOR)
                .capacity(30)
                .disabledParkingSpots(5)
                .electricChargingSpots(5)
                .dailyPrice(BigDecimal.valueOf(10))
                .monthlyPrice(BigDecimal.valueOf(240))
                .yearlyPrice(BigDecimal.valueOf(2400))
                .build();

        parkingSpot.setParkingLot(otherParkingLot);

        mockCreateReservationDependencies();

        assertThrows(BusinessRuleException.class,
                () -> reservationService.createReservation(createRequestDTO, userId));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_whenEndDateIsBeforeStartDate_shouldThrowBusinessRuleException() {
        createRequestDTO.setEndDate(createRequestDTO.getStartDate().minusHours(1));

        mockCreateReservationDependencies();

        assertThrows(BusinessRuleException.class,
                () -> reservationService.createReservation(createRequestDTO, userId));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_whenMonthlyReservationIsNotExactlyOneMonth_shouldThrowBusinessRuleException() {
        createRequestDTO.setReservationType(ReservationType.MONTHLY);
        createRequestDTO.setEndDate(createRequestDTO.getStartDate().plusDays(20));

        mockCreateReservationDependencies();

        assertThrows(BusinessRuleException.class,
                () -> reservationService.createReservation(createRequestDTO, userId));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_whenYearlyReservationIsNotExactlyOneYear_shouldThrowBusinessRuleException() {
        createRequestDTO.setReservationType(ReservationType.YEARLY);
        createRequestDTO.setEndDate(createRequestDTO.getStartDate().plusMonths(11));

        mockCreateReservationDependencies();

        assertThrows(BusinessRuleException.class,
                () -> reservationService.createReservation(createRequestDTO, userId));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void getAllReservations_shouldReturnReservationsOrderedByCreatedOnDesc() {
        Reservation reservation = createReservation(ReservationStatus.ACTIVE);

        when(reservationRepository.findAllByOrderByCreatedOnDesc())
                .thenReturn(List.of(reservation));

        List<Reservation> result = reservationService.getAllReservations();

        assertEquals(1, result.size());
        assertEquals(reservation, result.get(0));
    }

    @Test
    void cancelReservationByUser_whenReservationIsPendingPayment_shouldCancelReservationAndInvoice() {
        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.cancelReservationByUser(reservation.getId(), userId);

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());

        verify(reservationRepository).save(reservation);
        verify(billingClient).cancelInvoiceByReservationId(reservation.getId());
    }

    @Test
    void cancelReservationByUser_whenReservationBelongsToAnotherUser_shouldThrowBusinessRuleException() {
        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        assertThrows(BusinessRuleException.class,
                () -> reservationService.cancelReservationByUser(reservation.getId(), UUID.randomUUID()));

        verify(reservationRepository, never()).save(any());
        verify(billingClient, never()).cancelInvoiceByReservationId(any());
    }

    @Test
    void cancelReservationByUser_whenReservationIsCompleted_shouldThrowBusinessRuleException() {
        Reservation reservation = createReservation(ReservationStatus.COMPLETED);

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        assertThrows(BusinessRuleException.class,
                () -> reservationService.cancelReservationByUser(reservation.getId(), userId));

        verify(reservationRepository, never()).save(any());
        verify(billingClient, never()).cancelInvoiceByReservationId(any());
    }

    @Test
    void cancelReservationByUser_whenBillingServiceFails_shouldThrowBillingServiceUnavailableException() {
        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(billingClient.cancelInvoiceByReservationId(reservation.getId()))
                .thenThrow(mock(FeignException.class));

        assertThrows(BillingServiceUnavailableException.class,
                () -> reservationService.cancelReservationByUser(reservation.getId(), userId));
    }

    @Test
    void cancelReservationByAdmin_whenReservationIsActive_shouldCancelReservationAndInvoice() {
        Reservation reservation = createReservation(ReservationStatus.ACTIVE);

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.cancelReservationByAdmin(reservation.getId());

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());

        verify(reservationRepository).save(reservation);
        verify(billingClient).cancelInvoiceByReservationId(reservation.getId());
    }

    @Test
    void cancelReservationByAdmin_whenReservationIsCompleted_shouldThrowBusinessRuleException() {
        Reservation reservation = createReservation(ReservationStatus.COMPLETED);

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        assertThrows(BusinessRuleException.class,
                () -> reservationService.cancelReservationByAdmin(reservation.getId()));

        verify(reservationRepository, never()).save(any());
        verify(billingClient, never()).cancelInvoiceByReservationId(any());
    }

    @Test
    void cancelReservationByAdmin_whenBillingServiceFails_shouldThrowBillingServiceUnavailableException() {
        Reservation reservation = createReservation(ReservationStatus.ACTIVE);

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(billingClient.cancelInvoiceByReservationId(reservation.getId()))
                .thenThrow(mock(FeignException.class));

        assertThrows(BillingServiceUnavailableException.class,
                () -> reservationService.cancelReservationByAdmin(reservation.getId()));
    }

    @Test
    void getReservationForEdit_whenReservationIsPendingPayment_shouldReturnEditDto() {
        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        ReservationEditRequestDTO result =
                reservationService.getReservationForEdit(reservation.getId(), userId);

        assertEquals(vehicleId, result.getVehicleId());
        assertEquals(parkingLotId, result.getParkingLotId());
        assertEquals(parkingSpotId, result.getParkingSpotId());
        assertEquals(reservation.getReservationType(), result.getReservationType());
    }

    @Test
    void getReservationForEdit_whenReservationBelongsToAnotherUser_shouldThrowBusinessRuleException() {
        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        assertThrows(BusinessRuleException.class,
                () -> reservationService.getReservationForEdit(reservation.getId(), UUID.randomUUID()));
    }

    @Test
    void getReservationForEdit_whenReservationIsCompleted_shouldThrowBusinessRuleException() {
        Reservation reservation = createReservation(ReservationStatus.COMPLETED);

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        assertThrows(BusinessRuleException.class,
                () -> reservationService.getReservationForEdit(reservation.getId(), userId));
    }

    @Test
    void getReservationForEdit_whenActiveReservationAlreadyStarted_shouldThrowBusinessRuleException() {
        Reservation reservation = createStartedReservation();

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        assertThrows(BusinessRuleException.class,
                () -> reservationService.getReservationForEdit(reservation.getId(), userId));
    }

    @Test
    void editReservation_whenPendingPaymentAndDataIsValid_shouldUpdateReservationAndInvoice() {
        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);
        ReservationEditRequestDTO editRequestDTO = createEditRequestDTO();

        mockEditReservationDependencies(reservation, editRequestDTO);

        when(reservationRepository.existsByParkingSpotIdAndStatusInAndIdNotAndStartDateBeforeAndEndDateAfter(
                eq(parkingSpotId), anyList(), eq(reservation.getId()), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(false);

        when(reservationRepository.existsByVehicleIdAndStatusInAndIdNotAndStartDateBeforeAndEndDateAfter(
                eq(vehicleId), anyList(), eq(reservation.getId()), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(false);

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.editReservation(editRequestDTO, reservation.getId(), userId);

        assertEquals(editRequestDTO.getVehicleId(), reservation.getVehicle().getId());
        assertEquals(editRequestDTO.getParkingLotId(), reservation.getParkingLot().getId());
        assertEquals(editRequestDTO.getParkingSpotId(), reservation.getParkingSpot().getId());
        assertEquals(editRequestDTO.getReservationType(), reservation.getReservationType());
        assertEquals(editRequestDTO.getStartDate(), reservation.getStartDate());
        assertEquals(editRequestDTO.getEndDate(), reservation.getEndDate());

        verify(reservationRepository).save(reservation);
        verify(billingClient).updateInvoiceByReservationId(
                eq(reservation.getId()),
                any(UpdateInvoiceRequest.class)
        );
    }

    @Test
    void editReservation_whenActiveBeforeStartAndVehicleAndSpotAreChanged_shouldUpdateWithoutInvoiceUpdate() {
        Reservation reservation = createReservation(ReservationStatus.ACTIVE);

        UUID newVehicleId = UUID.randomUUID();
        Vehicle newVehicle = Vehicle.builder()
                .id(newVehicleId)
                .owner(user)
                .vehicleType(VehicleType.CAR)
                .engineType(EngineType.ELECTRIC)
                .disabledParkingRequired(false)
                .active(true)
                .build();

        UUID newParkingSpotId = UUID.randomUUID();
        ParkingSpot newParkingSpot = ParkingSpot.builder()
                .id(newParkingSpotId)
                .parkingLot(parkingLot)
                .spotNumber(2)
                .active(true)
                .electricChargingSpot(false)
                .disabledSpot(false)
                .build();

        ReservationEditRequestDTO editRequestDTO = ReservationEditRequestDTO.builder()
                .vehicleId(newVehicleId)
                .parkingLotId(parkingLotId)
                .parkingSpotId(newParkingSpotId)
                .reservationType(reservation.getReservationType())
                .startDate(reservation.getStartDate())
                .endDate(reservation.getEndDate())
                .build();

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        when(vehicleRepository.findById(newVehicleId)).thenReturn(Optional.of(newVehicle));
        when(parkingLotRepository.findById(parkingLotId)).thenReturn(Optional.of(parkingLot));
        when(parkingSpotRepository.findById(newParkingSpotId)).thenReturn(Optional.of(newParkingSpot));

        when(reservationRepository.existsByParkingSpotIdAndStatusInAndIdNotAndStartDateBeforeAndEndDateAfter(
                eq(newParkingSpotId), anyList(), eq(reservation.getId()), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(false);

        when(reservationRepository.existsByVehicleIdAndStatusInAndIdNotAndStartDateBeforeAndEndDateAfter(
                eq(newVehicleId), anyList(), eq(reservation.getId()), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(false);

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.editReservation(editRequestDTO, reservation.getId(), userId);

        assertEquals(newVehicleId, reservation.getVehicle().getId());
        assertEquals(newParkingSpotId, reservation.getParkingSpot().getId());
        assertEquals(ReservationStatus.ACTIVE, reservation.getStatus());

        verify(reservationRepository).save(reservation);
        verify(billingClient, never()).updateInvoiceByReservationId(any(), any());
    }

    @Test
    void editReservation_whenActiveBeforeStartAndParkingLotIsChanged_shouldThrowBusinessRuleException() {
        Reservation reservation = createReservation(ReservationStatus.ACTIVE);

        ReservationEditRequestDTO editRequestDTO = ReservationEditRequestDTO.builder()
                .vehicleId(vehicleId)
                .parkingLotId(UUID.randomUUID())
                .parkingSpotId(parkingSpotId)
                .reservationType(reservation.getReservationType())
                .startDate(reservation.getStartDate())
                .endDate(reservation.getEndDate())
                .build();

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        assertThrows(BusinessRuleException.class,
                () -> reservationService.editReservation(editRequestDTO, reservation.getId(), userId));

        verify(reservationRepository, never()).save(any());
        verify(billingClient, never()).updateInvoiceByReservationId(any(), any());
    }

    @Test
    void editReservation_whenActiveBeforeStartAndReservationTypeIsChanged_shouldThrowBusinessRuleException() {
        Reservation reservation = createReservation(ReservationStatus.ACTIVE);

        ReservationEditRequestDTO editRequestDTO = ReservationEditRequestDTO.builder()
                .vehicleId(vehicleId)
                .parkingLotId(parkingLotId)
                .parkingSpotId(parkingSpotId)
                .reservationType(ReservationType.MONTHLY)
                .startDate(reservation.getStartDate())
                .endDate(reservation.getEndDate())
                .build();

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        assertThrows(BusinessRuleException.class,
                () -> reservationService.editReservation(editRequestDTO, reservation.getId(), userId));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void editReservation_whenActiveBeforeStartAndStartDateIsChanged_shouldThrowBusinessRuleException() {
        Reservation reservation = createReservation(ReservationStatus.ACTIVE);

        ReservationEditRequestDTO editRequestDTO = ReservationEditRequestDTO.builder()
                .vehicleId(vehicleId)
                .parkingLotId(parkingLotId)
                .parkingSpotId(parkingSpotId)
                .reservationType(reservation.getReservationType())
                .startDate(reservation.getStartDate().plusHours(1))
                .endDate(reservation.getEndDate())
                .build();

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        assertThrows(BusinessRuleException.class,
                () -> reservationService.editReservation(editRequestDTO, reservation.getId(), userId));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void editReservation_whenActiveBeforeStartAndEndDateIsChanged_shouldThrowBusinessRuleException() {
        Reservation reservation = createReservation(ReservationStatus.ACTIVE);

        ReservationEditRequestDTO editRequestDTO = ReservationEditRequestDTO.builder()
                .vehicleId(vehicleId)
                .parkingLotId(parkingLotId)
                .parkingSpotId(parkingSpotId)
                .reservationType(reservation.getReservationType())
                .startDate(reservation.getStartDate())
                .endDate(reservation.getEndDate().plusHours(1))
                .build();

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        assertThrows(BusinessRuleException.class,
                () -> reservationService.editReservation(editRequestDTO, reservation.getId(), userId));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void editReservation_whenActiveReservationAlreadyStarted_shouldThrowBusinessRuleException() {
        Reservation reservation = createStartedReservation();
        ReservationEditRequestDTO editRequestDTO = createEditRequestDTO();

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        assertThrows(BusinessRuleException.class,
                () -> reservationService.editReservation(editRequestDTO, reservation.getId(), userId));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void editReservation_whenPendingPaymentInvoiceUpdateFails_shouldThrowBillingServiceUnavailableException() {
        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);
        ReservationEditRequestDTO editRequestDTO = createEditRequestDTO();

        mockEditReservationDependencies(reservation, editRequestDTO);

        when(reservationRepository.existsByParkingSpotIdAndStatusInAndIdNotAndStartDateBeforeAndEndDateAfter(
                eq(parkingSpotId), anyList(), eq(reservation.getId()), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(false);

        when(reservationRepository.existsByVehicleIdAndStatusInAndIdNotAndStartDateBeforeAndEndDateAfter(
                eq(vehicleId), anyList(), eq(reservation.getId()), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(false);

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(billingClient.updateInvoiceByReservationId(eq(reservation.getId()), any(UpdateInvoiceRequest.class)))
                .thenThrow(mock(FeignException.class));

        assertThrows(BillingServiceUnavailableException.class,
                () -> reservationService.editReservation(editRequestDTO, reservation.getId(), userId));
    }

    @Test
    void editReservation_whenReservationBelongsToAnotherUser_shouldThrowBusinessRuleException() {
        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);
        ReservationEditRequestDTO editRequestDTO = createEditRequestDTO();

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        assertThrows(BusinessRuleException.class,
                () -> reservationService.editReservation(editRequestDTO, reservation.getId(), UUID.randomUUID()));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void editReservation_whenReservationIsCompleted_shouldThrowBusinessRuleException() {
        Reservation reservation = createReservation(ReservationStatus.COMPLETED);
        ReservationEditRequestDTO editRequestDTO = createEditRequestDTO();

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        assertThrows(BusinessRuleException.class,
                () -> reservationService.editReservation(editRequestDTO, reservation.getId(), userId));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void editReservation_whenStartDateIsNotInFutureForPendingPayment_shouldThrowBusinessRuleException() {
        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);

        ReservationEditRequestDTO editRequestDTO = ReservationEditRequestDTO.builder()
                .vehicleId(vehicleId)
                .parkingLotId(parkingLotId)
                .parkingSpotId(parkingSpotId)
                .reservationType(ReservationType.DAILY)
                .startDate(LocalDateTime.now().minusHours(1))
                .endDate(LocalDateTime.now().plusDays(1))
                .build();

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        assertThrows(BusinessRuleException.class,
                () -> reservationService.editReservation(editRequestDTO, reservation.getId(), userId));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void editReservation_whenParkingSpotIsTaken_shouldThrowBusinessRuleException() {
        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);
        ReservationEditRequestDTO editRequestDTO = createEditRequestDTO();

        mockEditReservationDependencies(reservation, editRequestDTO);

        when(reservationRepository.existsByParkingSpotIdAndStatusInAndIdNotAndStartDateBeforeAndEndDateAfter(
                eq(parkingSpotId), anyList(), eq(reservation.getId()), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> reservationService.editReservation(editRequestDTO, reservation.getId(), userId));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void editReservation_whenVehicleIsAlreadyReserved_shouldThrowBusinessRuleException() {
        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);
        ReservationEditRequestDTO editRequestDTO = createEditRequestDTO();

        mockEditReservationDependencies(reservation, editRequestDTO);

        when(reservationRepository.existsByParkingSpotIdAndStatusInAndIdNotAndStartDateBeforeAndEndDateAfter(
                eq(parkingSpotId), anyList(), eq(reservation.getId()), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(false);

        when(reservationRepository.existsByVehicleIdAndStatusInAndIdNotAndStartDateBeforeAndEndDateAfter(
                eq(vehicleId), anyList(), eq(reservation.getId()), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> reservationService.editReservation(editRequestDTO, reservation.getId(), userId));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void isReservationStarted_whenReservationHasStarted_shouldReturnTrue() {
        Reservation reservation = createStartedReservation();

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        boolean result = reservationService.isReservationStarted(reservation.getId(), userId);

        assertTrue(result);
    }

    @Test
    void isReservationStarted_whenReservationHasNotStarted_shouldReturnFalse() {
        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        boolean result = reservationService.isReservationStarted(reservation.getId(), userId);

        assertFalse(result);
    }

    @Test
    void isReservationStarted_whenReservationBelongsToAnotherUser_shouldThrowBusinessRuleException() {
        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        assertThrows(BusinessRuleException.class,
                () -> reservationService.isReservationStarted(reservation.getId(), UUID.randomUUID()));
    }

    @Test
    void getReservationStatus_whenReservationBelongsToUser_shouldReturnStatus() {
        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        ReservationStatus result = reservationService.getReservationStatus(reservation.getId(), userId);

        assertEquals(ReservationStatus.PENDING_PAYMENT, result);
    }

    @Test
    void getReservationStatus_whenReservationBelongsToAnotherUser_shouldThrowBusinessRuleException() {
        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        assertThrows(BusinessRuleException.class,
                () -> reservationService.getReservationStatus(reservation.getId(), UUID.randomUUID()));
    }

    @Test
    void completeExpiredReservations_whenExpiredActiveReservationsExist_shouldMarkThemCompleted() {
        Reservation firstReservation = createReservation(ReservationStatus.ACTIVE);
        Reservation secondReservation = createReservation(ReservationStatus.ACTIVE);

        when(reservationRepository.findAllByStatusAndEndDateBefore(
                eq(ReservationStatus.ACTIVE),
                any(LocalDateTime.class)
        )).thenReturn(List.of(firstReservation, secondReservation));

        reservationService.completeExpiredReservations();

        assertEquals(ReservationStatus.COMPLETED, firstReservation.getStatus());
        assertEquals(ReservationStatus.COMPLETED, secondReservation.getStatus());

        verify(reservationRepository).saveAll(List.of(firstReservation, secondReservation));
    }

    @Test
    void cancelExpiredPendingPaymentReservations_whenExpiredUnpaidReservationsExist_shouldCancelThemAndInvoices() {
        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);

        when(reservationRepository.findAllByStatusAndStartDateBefore(
                eq(ReservationStatus.PENDING_PAYMENT),
                any(LocalDateTime.class)
        )).thenReturn(List.of(reservation));

        reservationService.cancelExpiredPendingPaymentReservations();

        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());

        verify(billingClient).cancelInvoiceByReservationId(reservation.getId());
        verify(reservationRepository).saveAll(List.of(reservation));
    }

    @Test
    void cancelExpiredPendingPaymentReservations_whenBillingServiceFails_shouldThrowBillingServiceUnavailableException() {
        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);

        when(reservationRepository.findAllByStatusAndStartDateBefore(
                eq(ReservationStatus.PENDING_PAYMENT),
                any(LocalDateTime.class)
        )).thenReturn(List.of(reservation));

        when(billingClient.cancelInvoiceByReservationId(reservation.getId()))
                .thenThrow(mock(FeignException.class));

        assertThrows(BillingServiceUnavailableException.class,
                () -> reservationService.cancelExpiredPendingPaymentReservations());

        verify(reservationRepository, never()).saveAll(anyList());
    }

    @Test
    void getReservationViewsByUserId_whenBillingServiceReturnsInvoice_shouldReturnViewWithInvoiceStatusAndCanEdit() {
        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);

        InvoiceResponse invoice = InvoiceResponse.builder()
                .id(UUID.randomUUID())
                .reservationId(reservation.getId())
                .userId(userId)
                .amount(BigDecimal.valueOf(5))
                .currency("EUR")
                .status("PENDING")
                .build();

        when(reservationRepository.findAllByUserIdOrderByCreatedOnDesc(userId))
                .thenReturn(List.of(reservation));

        when(billingClient.getInvoiceByReservationId(reservation.getId()))
                .thenReturn(invoice);

        List<ReservationViewDTO> result =
                reservationService.getReservationViewsByUserId(userId);

        assertEquals(1, result.size());
        assertEquals(reservation, result.get(0).getReservation());
        assertEquals(invoice.getId(), result.get(0).getInvoiceId());
        assertEquals("PENDING", result.get(0).getInvoiceStatus());
        assertTrue(result.get(0).isCanEdit());
    }

    @Test
    void getReservationViewsByUserId_whenBillingServiceFails_shouldReturnUnavailableInvoiceStatusAndCanEdit() {
        Reservation reservation = createReservation(ReservationStatus.PENDING_PAYMENT);

        when(reservationRepository.findAllByUserIdOrderByCreatedOnDesc(userId))
                .thenReturn(List.of(reservation));

        when(billingClient.getInvoiceByReservationId(reservation.getId()))
                .thenThrow(mock(FeignException.class));

        List<ReservationViewDTO> result =
                reservationService.getReservationViewsByUserId(userId);

        assertEquals(1, result.size());
        assertEquals(reservation, result.get(0).getReservation());
        assertNull(result.get(0).getInvoiceId());
        assertEquals("UNAVAILABLE", result.get(0).getInvoiceStatus());
        assertTrue(result.get(0).isCanEdit());
    }

    @Test
    void getReservationViewsByUserId_whenReservationIsCompleted_shouldReturnCanEditFalse() {
        Reservation reservation = createReservation(ReservationStatus.COMPLETED);

        InvoiceResponse invoice = InvoiceResponse.builder()
                .id(UUID.randomUUID())
                .reservationId(reservation.getId())
                .userId(userId)
                .amount(BigDecimal.valueOf(5))
                .currency("EUR")
                .status("PAID")
                .build();

        when(reservationRepository.findAllByUserIdOrderByCreatedOnDesc(userId))
                .thenReturn(List.of(reservation));

        when(billingClient.getInvoiceByReservationId(reservation.getId()))
                .thenReturn(invoice);

        List<ReservationViewDTO> result =
                reservationService.getReservationViewsByUserId(userId);

        assertFalse(result.get(0).isCanEdit());
    }

    private void mockCreateReservationDependencies() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(parkingLotRepository.findById(parkingLotId)).thenReturn(Optional.of(parkingLot));
        when(parkingSpotRepository.findById(parkingSpotId)).thenReturn(Optional.of(parkingSpot));
    }

    private void mockEditReservationDependencies(Reservation reservation,
                                                 ReservationEditRequestDTO editRequestDTO) {
        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        when(vehicleRepository.findById(editRequestDTO.getVehicleId()))
                .thenReturn(Optional.of(vehicle));

        when(parkingLotRepository.findById(editRequestDTO.getParkingLotId()))
                .thenReturn(Optional.of(parkingLot));

        when(parkingSpotRepository.findById(editRequestDTO.getParkingSpotId()))
                .thenReturn(Optional.of(parkingSpot));
    }

    private Reservation createReservation(ReservationStatus status) {
        return Reservation.builder()
                .id(UUID.randomUUID())
                .user(user)
                .vehicle(vehicle)
                .parkingLot(parkingLot)
                .parkingSpot(parkingSpot)
                .reservationType(createRequestDTO.getReservationType())
                .startDate(createRequestDTO.getStartDate())
                .endDate(createRequestDTO.getEndDate())
                .disabledParkingSpotRequired(parkingSpot.isDisabledSpot())
                .electricChargingRequired(parkingSpot.isElectricChargingSpot())
                .status(status)
                .totalPrice(BigDecimal.valueOf(5))
                .createdOn(LocalDateTime.now())
                .build();
    }

    private ReservationEditRequestDTO createEditRequestDTO() {
        return ReservationEditRequestDTO.builder()
                .vehicleId(vehicleId)
                .parkingLotId(parkingLotId)
                .parkingSpotId(parkingSpotId)
                .reservationType(ReservationType.DAILY)
                .startDate(LocalDateTime.now().plusDays(3))
                .endDate(LocalDateTime.now().plusDays(4))
                .build();
    }

    private Reservation createStartedReservation() {
        return Reservation.builder()
                .id(UUID.randomUUID())
                .user(user)
                .vehicle(vehicle)
                .parkingLot(parkingLot)
                .parkingSpot(parkingSpot)
                .reservationType(ReservationType.DAILY)
                .startDate(LocalDateTime.now().minusHours(1))
                .endDate(LocalDateTime.now().plusDays(1))
                .disabledParkingSpotRequired(parkingSpot.isDisabledSpot())
                .electricChargingRequired(parkingSpot.isElectricChargingSpot())
                .status(ReservationStatus.ACTIVE)
                .totalPrice(BigDecimal.valueOf(5))
                .createdOn(LocalDateTime.now().minusDays(1))
                .build();
    }
}