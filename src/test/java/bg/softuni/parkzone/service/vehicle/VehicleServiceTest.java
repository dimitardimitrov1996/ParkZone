package bg.softuni.parkzone.service.vehicle;

import bg.softuni.parkzone.exception.BusinessRuleException;
import bg.softuni.parkzone.exception.vehicle.VehicleNotFoundException;
import bg.softuni.parkzone.model.dto.vehicle.VehicleCreateRequestDTO;
import bg.softuni.parkzone.model.dto.vehicle.VehicleEditDTO;
import bg.softuni.parkzone.model.entities.parkinglot.ParkingLot;
import bg.softuni.parkzone.model.entities.parkinglot.ParkingType;
import bg.softuni.parkzone.model.entities.parkingspot.ParkingSpot;
import bg.softuni.parkzone.model.entities.reservation.Reservation;
import bg.softuni.parkzone.model.entities.reservation.ReservationStatus;
import bg.softuni.parkzone.model.entities.user.User;
import bg.softuni.parkzone.model.entities.vehicle.EngineType;
import bg.softuni.parkzone.model.entities.vehicle.Vehicle;
import bg.softuni.parkzone.model.entities.vehicle.VehicleType;
import bg.softuni.parkzone.repository.reservation.ReservationRepository;
import bg.softuni.parkzone.repository.user.UserRepository;
import bg.softuni.parkzone.repository.vehicle.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private VehicleService vehicleService;

    private UUID userId;
    private UUID vehicleId;
    private UUID otherUserId;

    private User user;
    private Vehicle vehicle;
    private VehicleCreateRequestDTO createRequestDTO;
    private VehicleEditDTO editDTO;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .username("user")
                .isActive(true)
                .build();

        vehicle = Vehicle.builder()
                .id(vehicleId)
                .registrationNumber("CA1234AA")
                .brand("Toyota")
                .model("Corolla")
                .vehicleType(VehicleType.CAR)
                .engineType(EngineType.ELECTRIC)
                .disabledParkingRequired(true)
                .owner(user)
                .active(true)
                .build();

        createRequestDTO = VehicleCreateRequestDTO.builder()
                .registrationNumber("CA1234AA")
                .brand("Toyota")
                .model("Corolla")
                .vehicleType(VehicleType.CAR)
                .engineType(EngineType.ELECTRIC)
                .disabledParkingRequired(true)
                .build();

        editDTO = VehicleEditDTO.builder()
                .registrationNumber("CB5678BB")
                .brand("Honda")
                .model("Civic")
                .vehicleType(VehicleType.CAR)
                .engineType(EngineType.ELECTRIC)
                .disabledParkingRequired(true)
                .build();
    }

    @Test
    void createVehicle_whenDataIsValid_shouldSaveVehicle() {
        when(vehicleRepository.existsByRegistrationNumber(createRequestDTO.getRegistrationNumber()))
                .thenReturn(false);
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        vehicleService.createVehicle(createRequestDTO, userId);

        verify(vehicleRepository).save(argThat(savedVehicle ->
                savedVehicle.getRegistrationNumber().equals(createRequestDTO.getRegistrationNumber())
                        && savedVehicle.getBrand().equals(createRequestDTO.getBrand())
                        && savedVehicle.getModel().equals(createRequestDTO.getModel())
                        && savedVehicle.getVehicleType() == createRequestDTO.getVehicleType()
                        && savedVehicle.getEngineType() == createRequestDTO.getEngineType()
                        && savedVehicle.isDisabledParkingRequired() == createRequestDTO.isDisabledParkingRequired()
                        && savedVehicle.getOwner().equals(user)
                        && savedVehicle.isActive()
        ));
    }

    @Test
    void createVehicle_whenRegistrationNumberExists_shouldThrowException() {
        when(vehicleRepository.existsByRegistrationNumber(createRequestDTO.getRegistrationNumber()))
                .thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> vehicleService.createVehicle(createRequestDTO, userId));

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void createVehicle_whenUserDoesNotExist_shouldThrowException() {
        when(vehicleRepository.existsByRegistrationNumber(createRequestDTO.getRegistrationNumber()))
                .thenReturn(false);
        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> vehicleService.createVehicle(createRequestDTO, userId));

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void getVehiclesByOwner_shouldReturnOnlyActiveOwnerVehicles() {
        when(vehicleRepository.findAllByOwnerIdAndActiveTrue(userId))
                .thenReturn(List.of(vehicle));

        List<Vehicle> result = vehicleService.getVehiclesByOwner(userId);

        assertEquals(1, result.size());
        assertEquals(vehicle, result.get(0));
    }

    @Test
    void findById_whenVehicleExists_shouldReturnVehicle() {
        when(vehicleRepository.findById(vehicleId))
                .thenReturn(Optional.of(vehicle));

        Vehicle result = vehicleService.findById(vehicleId);

        assertEquals(vehicle, result);
    }

    @Test
    void findById_whenVehicleDoesNotExist_shouldThrowException() {
        when(vehicleRepository.findById(vehicleId))
                .thenReturn(Optional.empty());

        assertThrows(VehicleNotFoundException.class,
                () -> vehicleService.findById(vehicleId));
    }

    @Test
    void getVehicleForEdit_whenVehicleBelongsToUser_shouldReturnEditDTO() {
        when(vehicleRepository.findById(vehicleId))
                .thenReturn(Optional.of(vehicle));

        VehicleEditDTO result = vehicleService.getVehicleForEdit(vehicleId, userId);

        assertEquals(vehicle.getRegistrationNumber(), result.getRegistrationNumber());
        assertEquals(vehicle.getBrand(), result.getBrand());
        assertEquals(vehicle.getModel(), result.getModel());
        assertEquals(vehicle.getVehicleType(), result.getVehicleType());
        assertEquals(vehicle.getEngineType(), result.getEngineType());
        assertEquals(vehicle.isDisabledParkingRequired(), result.isDisabledParkingRequired());
    }

    @Test
    void getVehicleForEdit_whenVehicleDoesNotBelongToUser_shouldThrowException() {
        when(vehicleRepository.findById(vehicleId))
                .thenReturn(Optional.of(vehicle));

        assertThrows(BusinessRuleException.class,
                () -> vehicleService.getVehicleForEdit(vehicleId, otherUserId));
    }

    @Test
    void editVehicle_whenDataIsValid_shouldUpdateVehicle() {
        when(vehicleRepository.findById(vehicleId))
                .thenReturn(Optional.of(vehicle));
        when(vehicleRepository.existsByRegistrationNumberAndIdNot(editDTO.getRegistrationNumber(), vehicleId))
                .thenReturn(false);
        when(reservationRepository.findAllByVehicleIdAndStatus(vehicleId, ReservationStatus.ACTIVE))
                .thenReturn(List.of());

        vehicleService.editVehicle(editDTO, vehicleId, userId);

        assertEquals(editDTO.getRegistrationNumber(), vehicle.getRegistrationNumber());
        assertEquals(editDTO.getBrand(), vehicle.getBrand());
        assertEquals(editDTO.getModel(), vehicle.getModel());
        assertEquals(editDTO.getVehicleType(), vehicle.getVehicleType());
        assertEquals(editDTO.getEngineType(), vehicle.getEngineType());
        assertEquals(editDTO.isDisabledParkingRequired(), vehicle.isDisabledParkingRequired());

        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void editVehicle_whenVehicleDoesNotBelongToUser_shouldThrowException() {
        when(vehicleRepository.findById(vehicleId))
                .thenReturn(Optional.of(vehicle));

        assertThrows(BusinessRuleException.class,
                () -> vehicleService.editVehicle(editDTO, vehicleId, otherUserId));

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void editVehicle_whenRegistrationNumberExists_shouldThrowException() {
        when(vehicleRepository.findById(vehicleId))
                .thenReturn(Optional.of(vehicle));
        when(vehicleRepository.existsByRegistrationNumberAndIdNot(editDTO.getRegistrationNumber(), vehicleId))
                .thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> vehicleService.editVehicle(editDTO, vehicleId, userId));

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void editVehicle_whenVehicleHasActiveElectricReservationAndEngineChangedToNonElectric_shouldThrowException() {
        Reservation reservation = createReservationWithSpot(true, false, ParkingType.OUTDOOR);

        editDTO.setEngineType(EngineType.PETROL);

        when(vehicleRepository.findById(vehicleId))
                .thenReturn(Optional.of(vehicle));
        when(vehicleRepository.existsByRegistrationNumberAndIdNot(editDTO.getRegistrationNumber(), vehicleId))
                .thenReturn(false);
        when(reservationRepository.findAllByVehicleIdAndStatus(vehicleId, ReservationStatus.ACTIVE))
                .thenReturn(List.of(reservation));

        assertThrows(BusinessRuleException.class,
                () -> vehicleService.editVehicle(editDTO, vehicleId, userId));

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void editVehicle_whenVehicleHasActiveDisabledReservationAndDisabledFlagRemoved_shouldThrowException() {
        Reservation reservation = createReservationWithSpot(false, true, ParkingType.OUTDOOR);

        editDTO.setDisabledParkingRequired(false);

        when(vehicleRepository.findById(vehicleId))
                .thenReturn(Optional.of(vehicle));
        when(vehicleRepository.existsByRegistrationNumberAndIdNot(editDTO.getRegistrationNumber(), vehicleId))
                .thenReturn(false);
        when(reservationRepository.findAllByVehicleIdAndStatus(vehicleId, ReservationStatus.ACTIVE))
                .thenReturn(List.of(reservation));

        assertThrows(BusinessRuleException.class,
                () -> vehicleService.editVehicle(editDTO, vehicleId, userId));

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void editVehicle_whenVehicleHasActiveIndoorReservationAndChangedToVan_shouldThrowException() {
        Reservation reservation = createReservationWithSpot(false, false, ParkingType.INDOOR);

        editDTO.setVehicleType(VehicleType.VAN);

        when(vehicleRepository.findById(vehicleId))
                .thenReturn(Optional.of(vehicle));
        when(vehicleRepository.existsByRegistrationNumberAndIdNot(editDTO.getRegistrationNumber(), vehicleId))
                .thenReturn(false);
        when(reservationRepository.findAllByVehicleIdAndStatus(vehicleId, ReservationStatus.ACTIVE))
                .thenReturn(List.of(reservation));

        assertThrows(BusinessRuleException.class,
                () -> vehicleService.editVehicle(editDTO, vehicleId, userId));

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void deleteVehicle_whenVehicleBelongsToUser_shouldDeactivateVehicleAndCancelActiveReservations() {
        Reservation reservation = Reservation.builder()
                .status(ReservationStatus.ACTIVE)
                .build();

        when(vehicleRepository.findById(vehicleId))
                .thenReturn(Optional.of(vehicle));
        when(reservationRepository.findAllByVehicleIdAndStatus(vehicleId, ReservationStatus.ACTIVE))
                .thenReturn(List.of(reservation));

        vehicleService.deleteVehicle(vehicleId, userId);

        assertFalse(vehicle.isActive());
        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());

        verify(reservationRepository).saveAll(List.of(reservation));
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void deleteVehicle_whenVehicleDoesNotBelongToUser_shouldThrowException() {
        when(vehicleRepository.findById(vehicleId))
                .thenReturn(Optional.of(vehicle));

        assertThrows(BusinessRuleException.class,
                () -> vehicleService.deleteVehicle(vehicleId, otherUserId));

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void deleteVehicle_whenVehicleAlreadyDeleted_shouldThrowException() {
        vehicle.setActive(false);

        when(vehicleRepository.findById(vehicleId))
                .thenReturn(Optional.of(vehicle));

        assertThrows(BusinessRuleException.class,
                () -> vehicleService.deleteVehicle(vehicleId, userId));

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void getAllVehicles_shouldReturnAllVehiclesOrderedByRegistrationNumber() {
        when(vehicleRepository.findAllByOrderByRegistrationNumberAsc())
                .thenReturn(List.of(vehicle));

        List<Vehicle> result = vehicleService.getAllVehicles();

        assertEquals(1, result.size());
        assertEquals(vehicle, result.get(0));
    }

    @Test
    void deleteVehicleByAdmin_whenVehicleIsActive_shouldDeactivateVehicleAndCancelActiveReservations() {
        Reservation reservation = Reservation.builder()
                .status(ReservationStatus.ACTIVE)
                .build();

        when(vehicleRepository.findById(vehicleId))
                .thenReturn(Optional.of(vehicle));
        when(reservationRepository.findAllByVehicleIdAndStatus(vehicleId, ReservationStatus.ACTIVE))
                .thenReturn(List.of(reservation));

        vehicleService.deleteVehicleByAdmin(vehicleId);

        assertFalse(vehicle.isActive());
        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());

        verify(reservationRepository).saveAll(List.of(reservation));
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void deleteVehicleByAdmin_whenVehicleAlreadyDeleted_shouldThrowException() {
        vehicle.setActive(false);

        when(vehicleRepository.findById(vehicleId))
                .thenReturn(Optional.of(vehicle));

        assertThrows(BusinessRuleException.class,
                () -> vehicleService.deleteVehicleByAdmin(vehicleId));

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void activateVehicleByAdmin_whenVehicleIsInactiveAndOwnerIsActive_shouldActivateVehicle() {
        vehicle.setActive(false);
        vehicle.setOwner(user);

        when(vehicleRepository.findById(vehicleId))
                .thenReturn(Optional.of(vehicle));

        vehicleService.activateVehicleByAdmin(vehicleId);

        assertTrue(vehicle.isActive());
        verify(vehicleRepository).save(vehicle);
    }

    @Test
    void activateVehicleByAdmin_whenVehicleAlreadyActive_shouldThrowException() {
        when(vehicleRepository.findById(vehicleId))
                .thenReturn(Optional.of(vehicle));

        assertThrows(BusinessRuleException.class,
                () -> vehicleService.activateVehicleByAdmin(vehicleId));

        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void activateVehicleByAdmin_whenOwnerIsInactive_shouldThrowException() {
        user.setActive(false);
        vehicle.setActive(false);
        vehicle.setOwner(user);

        when(vehicleRepository.findById(vehicleId))
                .thenReturn(Optional.of(vehicle));

        assertThrows(BusinessRuleException.class,
                () -> vehicleService.activateVehicleByAdmin(vehicleId));

        verify(vehicleRepository, never()).save(any());
    }

    private Reservation createReservationWithSpot(boolean electricSpot,
                                                  boolean disabledSpot,
                                                  ParkingType parkingType) {
        ParkingLot parkingLot = ParkingLot.builder()
                .parkingType(parkingType)
                .build();

        ParkingSpot parkingSpot = ParkingSpot.builder()
                .electricChargingSpot(electricSpot)
                .disabledSpot(disabledSpot)
                .build();

        return Reservation.builder()
                .parkingLot(parkingLot)
                .parkingSpot(parkingSpot)
                .status(ReservationStatus.ACTIVE)
                .build();
    }
}
