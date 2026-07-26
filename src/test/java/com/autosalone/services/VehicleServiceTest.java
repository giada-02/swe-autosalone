package com.autosalone.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autosalone.dtos.DeadlineRequest;
import com.autosalone.dtos.VehicleRequest;
import com.autosalone.enums.ContractStatus;
import com.autosalone.enums.VehicleCondition;
import com.autosalone.enums.VehicleStatus;
import com.autosalone.models.Contract;
import com.autosalone.models.Deadline;
import com.autosalone.models.Quotation;
import com.autosalone.models.Transaction;
import com.autosalone.models.Vehicle;
import com.autosalone.repositories.ContractRepository;
import com.autosalone.repositories.DeadlineRepository;
import com.autosalone.repositories.QuotationRepository;
import com.autosalone.repositories.VehicleRepository;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DeadlineRepository deadlineRepository;

    @Mock
    private QuotationRepository quotationRepository;

    @Mock
    private ContractRepository contractRepository;

    @InjectMocks
    private VehicleService vehicleService;

    private UUID vehicleId;
    private Vehicle mockVehicle;
    private VehicleRequest validRequest;

    @BeforeEach
    void setUp() {
        vehicleId = UUID.randomUUID();
        mockVehicle = mock(Vehicle.class);

        validRequest = new VehicleRequest(
                "Fiat",
                "Panda",
                "Rosso",
                VehicleCondition.NEW,
                BigDecimal.valueOf(10000),
                LocalDate.now(),
                BigDecimal.valueOf(12000),
                null,
                "AB123CD",
                LocalDate.now(),
                0.0,
                true);
    }

    // read

    @Test
    void getVehicleById_Success() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(mockVehicle));
        Vehicle result = vehicleService.getVehicleById(vehicleId);
        assertNotNull(result);
        assertEquals(mockVehicle, result);
    }

    @Test
    void getVehicleById_NotFound() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> {
            vehicleService.getVehicleById(vehicleId);
        });
    }

    @Test
    void getVehicles_Success() {
        String keyword = "Panda";
        String brand = "Fiat";
        VehicleCondition condition = VehicleCondition.NEW;
        BigDecimal maxPrice = BigDecimal.valueOf(15000);
        Boolean isInShowroom = true;
        List<VehicleStatus> statusList = List.of(VehicleStatus.AVAILABLE);

        when(vehicleRepository.findVehicles(keyword, brand, condition, maxPrice, isInShowroom, statusList))
                .thenReturn(List.of(mockVehicle));

        List<Vehicle> results = vehicleService.getVehicles(
                keyword, brand, condition, maxPrice, isInShowroom, statusList);

        assertEquals(1, results.size());
        verify(vehicleRepository).findVehicles(
                keyword, brand, condition, maxPrice, isInShowroom, statusList);
    }

    @Test
    void getAllBrands_Success() {
        List<String> mockBrands = List.of("Audi", "BMW", "Fiat");

        when(vehicleRepository.findAllBrands()).thenReturn(mockBrands);

        List<String> results = vehicleService.getAllBrands();

        assertEquals(3, results.size());
        assertTrue(results.contains("BMW"));
        verify(vehicleRepository).findAllBrands();
    }

    // write

    @Test
    void addVehicle_Success_WithPurchaseTransaction() {
        vehicleService.addVehicle(validRequest);

        ArgumentCaptor<Vehicle> captor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(captor.capture());

        Vehicle savedVehicle = captor.getValue();
        assertEquals("Fiat", savedVehicle.getBrand());
        assertEquals(VehicleCondition.NEW, savedVehicle.getCondition());
        assertNotNull(savedVehicle.getPurchaseTransaction());
    }

    @Test
    void addVehicle_Success_WithoutPurchaseTransaction() {
        VehicleRequest requestNoPurchase = new VehicleRequest(
                "BMW", "X5", "Nero", VehicleCondition.SECONDHAND, null, null,
                BigDecimal.valueOf(30000), null, "ZA999ZZ", LocalDate.now(), 50000.0, true);

        vehicleService.addVehicle(requestNoPurchase);

        ArgumentCaptor<Vehicle> captor = ArgumentCaptor.forClass(Vehicle.class);
        verify(vehicleRepository).save(captor.capture());

        Vehicle savedVehicle = captor.getValue();
        assertNull(savedVehicle.getPurchaseTransaction());
    }

    @Test
    void withdrawVehicle_Success_VoidsQuotationsAndCancelsContracts() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(mockVehicle));

        Quotation mockQuotation = mock(Quotation.class);
        Contract mockDraftContract = mock(Contract.class);
        Contract mockConfirmedContract = mock(Contract.class);

        when(mockDraftContract.getStatus()).thenReturn(ContractStatus.DRAFT);
        when(mockConfirmedContract.getStatus()).thenReturn(ContractStatus.CONFIRMED);

        when(quotationRepository.findQuotations(any(), any(), anyBoolean(), eq(vehicleId), any(), any()))
                .thenReturn(List.of(mockQuotation));

        when(contractRepository.findContracts(any(), any(), anyBoolean(), eq(vehicleId), any(), any()))
                .thenReturn(List.of(mockDraftContract, mockConfirmedContract));

        vehicleService.withdrawVehicle(vehicleId, "Danno irreparabile");

        verify(mockVehicle).withdraw("Danno irreparabile");
        verify(vehicleRepository).save(mockVehicle);

        // verifica preventivi
        verify(mockQuotation).voidDocument();
        verify(quotationRepository).save(mockQuotation);

        // verifica contratti
        verify(mockDraftContract).voidDocument(); // Draft -> Void
        verify(mockConfirmedContract).cancel("Danno irreparabile"); // Confirmed -> Cancel
        verify(contractRepository).save(mockDraftContract);
        verify(contractRepository).save(mockConfirmedContract);
    }

    @Test
    void addExpense_Success() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(mockVehicle));
        when(mockVehicle.getBrand()).thenReturn("Audi");
        when(mockVehicle.getModel()).thenReturn("A3");

        vehicleService.addExpense(vehicleId, "Cambio gomme", BigDecimal.valueOf(400), LocalDate.now());

        verify(mockVehicle).addExpense(any(Transaction.class));
        verify(vehicleRepository).save(mockVehicle);
    }

    @Test
    void generateStandardInspection_FromRegistration() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(mockVehicle));

        Deadline mockDeadline = mock(Deadline.class);
        when(mockVehicle.generateStandardInspectionDeadline()).thenReturn(mockDeadline);

        vehicleService.generateStandardInspection(vehicleId, null);

        verify(mockVehicle).generateStandardInspectionDeadline();
        verify(vehicleRepository).save(mockVehicle);
    }

    @Test
    void generateStandardInspection_FromLastInspection() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(mockVehicle));

        Deadline mockDeadline = mock(Deadline.class);
        LocalDate lastDate = LocalDate.now().minusYears(2);
        when(mockVehicle.generateInspectionFromLastDate(lastDate)).thenReturn(mockDeadline);

        vehicleService.generateStandardInspection(vehicleId, lastDate);

        verify(mockVehicle).generateInspectionFromLastDate(lastDate);
        verify(vehicleRepository).save(mockVehicle);
    }

    @Test
    void addDeadline_Success() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(mockVehicle));
        Deadline mockDeadline = mock(Deadline.class);

        DeadlineRequest request = new DeadlineRequest("Bollo", LocalDate.now(), Period.ofYears(1), false);

        when(mockVehicle.addDeadline(anyString(), any(LocalDate.class), any(Period.class), anyBoolean()))
                .thenReturn(mockDeadline);

        vehicleService.addDeadline(vehicleId, request);

        verify(mockVehicle).addDeadline(request.reason(), request.dueDate(), request.recurrence(),
                request.recalculateFromCompletion());
        verify(vehicleRepository).save(mockVehicle);
    }

    @Test
    void removeDeadline_Success() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(mockVehicle));

        UUID deadlineId = UUID.randomUUID();
        Deadline mockDeadline = mock(Deadline.class);
        when(deadlineRepository.findById(deadlineId)).thenReturn(Optional.of(mockDeadline));

        vehicleService.removeDeadline(vehicleId, deadlineId);

        verify(mockVehicle).removeDeadline(mockDeadline);
        verify(vehicleRepository).save(mockVehicle);
    }

    @Test
    void updateVehicle_Success_WithoutOverwritingExistingPurchase() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(mockVehicle));

        Transaction existingPurchase = mock(Transaction.class);
        when(mockVehicle.getPurchaseTransaction()).thenReturn(existingPurchase);

        assertDoesNotThrow(() -> vehicleService.updateVehicle(vehicleId, validRequest));

        verify(mockVehicle).setBrand("Fiat");
        verify(mockVehicle).setSellingPrice(BigDecimal.valueOf(12000));

        verify(mockVehicle, never()).setPurchaseTransaction(any());
        verify(vehicleRepository).save(mockVehicle);
    }

    @Test
    void updateVehicle_Success_AddingNewPurchaseTransaction() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(mockVehicle));

        when(mockVehicle.getPurchaseTransaction()).thenReturn(null);

        assertDoesNotThrow(() -> vehicleService.updateVehicle(vehicleId, validRequest));

        verify(mockVehicle).setPurchaseTransaction(any(Transaction.class));
        verify(vehicleRepository).save(mockVehicle);
    }
}