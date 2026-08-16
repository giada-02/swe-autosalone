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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autosalone.dtos.requests.DeadlineRequest;
import com.autosalone.dtos.requests.PurchaseTransactionRequest;
import com.autosalone.dtos.requests.VehicleCreateRequest;
import com.autosalone.dtos.requests.VehicleUpdateRequest;
import com.autosalone.dtos.responses.DeadlineResponse;
import com.autosalone.dtos.responses.ExpenseResponse;
import com.autosalone.dtos.responses.VehicleResponse;
import com.autosalone.enums.ContractStatus;
import com.autosalone.enums.VehicleCondition;
import com.autosalone.enums.VehicleStatus;
import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.models.Contract;
import com.autosalone.models.Deadline;
import com.autosalone.models.Quotation;
import com.autosalone.models.Transaction;
import com.autosalone.models.Vehicle;
import com.autosalone.repositories.ContractRepository;
import com.autosalone.repositories.DeadlineRepository;
import com.autosalone.repositories.QuotationRepository;
import com.autosalone.repositories.TransactionRepository;
import com.autosalone.repositories.VehicleRepository;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DeadlineRepository deadlineRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private QuotationRepository quotationRepository;

    @Mock
    private ContractRepository contractRepository;

    @InjectMocks
    private VehicleService vehicleService;

    private UUID vehicleId;
    private Vehicle mockVehicle;

    @BeforeEach
    void setUp() {
        vehicleId = UUID.randomUUID();
        mockVehicle = mock(Vehicle.class);
    }

    // read

    @Test
    void getVehicleById_Success() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(mockVehicle));
        Vehicle response = vehicleService.getVehicleById(vehicleId);
        assertNotNull(response);
        assertEquals(mockVehicle, response);
    }

    @Test
    void getVehicleById_NotFound() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            vehicleService.getVehicleById(vehicleId);
        });
    }

    @Test
    void getVehicleResponseById_Success() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(mockVehicle));
        when(mockVehicle.getId()).thenReturn(vehicleId);

        VehicleResponse response = vehicleService.getVehicleResponseById(vehicleId);

        assertNotNull(response);
        assertEquals(vehicleId, response.id());
    }

    @Test
    void getVehicleResponseById_NotFound() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            vehicleService.getVehicleResponseById(vehicleId);
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

        when(mockVehicle.getId()).thenReturn(vehicleId);

        when(vehicleRepository.findVehicles(keyword, brand, condition, maxPrice, isInShowroom, statusList))
                .thenReturn(List.of(mockVehicle));

        List<VehicleResponse> responses = vehicleService.getVehicles(
                keyword, brand, condition, maxPrice, isInShowroom, statusList);

        assertEquals(1, responses.size());
        assertEquals(vehicleId, responses.get(0).id());
        verify(vehicleRepository).findVehicles(
                keyword, brand, condition, maxPrice, isInShowroom, statusList);
    }

    @Test
    void getAllBrands_Success() {
        List<String> mockBrands = List.of("Audi", "BMW", "Fiat");

        when(vehicleRepository.findAllBrands()).thenReturn(mockBrands);

        List<String> responses = vehicleService.getAllBrands();

        assertEquals(3, responses.size());
        assertTrue(responses.contains("BMW"));
        verify(vehicleRepository).findAllBrands();
    }

    // write

    @Test
    void addVehicle_Success_WithPurchaseTransaction() {
        VehicleCreateRequest request = new VehicleCreateRequest("Fiat", "Panda", "Rosso", VehicleCondition.NEW,
                BigDecimal.valueOf(12000), null, "AB123CD", LocalDate.now(), 0.0, true,
                new PurchaseTransactionRequest(
                        BigDecimal.valueOf(10000),
                        LocalDate.now()));

        VehicleResponse response = vehicleService.addVehicle(request);

        assertNotNull(response);
        assertNotNull(response.purchaseTransaction());
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void addVehicle_Success_WithoutPurchaseTransaction() {
        VehicleCreateRequest requestNoPurchase = new VehicleCreateRequest(
                "BMW", "X5", "Nero", VehicleCondition.SECONDHAND, BigDecimal.valueOf(30000), null,
                "ZA999ZZ", LocalDate.now(), 50000.0, true, null);

        VehicleResponse response = vehicleService.addVehicle(requestNoPurchase);

        assertNotNull(response);
        assertNull(response.purchaseTransaction());
        verify(vehicleRepository).save(any(Vehicle.class));
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

        ExpenseResponse response = vehicleService.addExpense(vehicleId, "Cambio gomme", BigDecimal.valueOf(400),
                LocalDate.now());

        assertNotNull(response);
        verify(mockVehicle).addExpense(any(Transaction.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void generateStandardInspection_FromRegistration() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(mockVehicle));

        Deadline mockDeadline = mock(Deadline.class);
        when(mockVehicle.generateStandardInspectionDeadline()).thenReturn(mockDeadline);
        when(mockDeadline.getVehicle()).thenReturn(mockVehicle);

        DeadlineResponse response = vehicleService.generateStandardInspection(vehicleId, null);

        assertNotNull(response);
        verify(mockVehicle).generateStandardInspectionDeadline();
        verify(vehicleRepository).save(mockVehicle);
    }

    @Test
    void generateStandardInspection_FromLastInspection() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(mockVehicle));

        Deadline mockDeadline = mock(Deadline.class);
        LocalDate lastDate = LocalDate.now().minusYears(2);
        when(mockVehicle.generateInspectionFromLastDate(lastDate)).thenReturn(mockDeadline);
        when(mockDeadline.getVehicle()).thenReturn(mockVehicle);

        DeadlineResponse response = vehicleService.generateStandardInspection(vehicleId, lastDate);

        assertNotNull(response);
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
        when(mockDeadline.getVehicle()).thenReturn(mockVehicle);

        DeadlineResponse response = vehicleService.addDeadline(vehicleId, request);

        assertNotNull(response);
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
    void addPurchaseTransaction_Success() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(mockVehicle));
        when(mockVehicle.getPurchaseTransaction()).thenReturn(null);

        PurchaseTransactionRequest request = new PurchaseTransactionRequest(BigDecimal.valueOf(10000), LocalDate.now());

        assertDoesNotThrow(() -> vehicleService.addPurchaseTransaction(vehicleId, request));

        verify(mockVehicle).setPurchaseTransaction(any(Transaction.class));
        verify(vehicleRepository).save(mockVehicle);
    }

    @Test
    void addPurchaseTransaction_AlreadyExists_ThrowsException() {
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(mockVehicle));

        Transaction existingTransaction = mock(Transaction.class);
        when(mockVehicle.getPurchaseTransaction()).thenReturn(existingTransaction);

        PurchaseTransactionRequest request = new PurchaseTransactionRequest(BigDecimal.valueOf(10000), LocalDate.now());

        assertThrows(IllegalStateException.class, () -> vehicleService.addPurchaseTransaction(vehicleId, request));

        verify(mockVehicle, never()).setPurchaseTransaction(any());
    }

    @Test
    void updateVehicle_Success_OnlyUpdatesBaseFields() {
        VehicleUpdateRequest request = new VehicleUpdateRequest("Fiat", "Panda", "Rosso", VehicleCondition.NEW,
                BigDecimal.valueOf(12000), null, "AB123CD", LocalDate.now(), 0.0, true);

        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(mockVehicle));

        assertDoesNotThrow(() -> vehicleService.updateVehicle(vehicleId, request));

        verify(mockVehicle).setBrand("Fiat");
        verify(mockVehicle).setSellingPrice(BigDecimal.valueOf(12000));

        verify(mockVehicle, never()).setPurchaseTransaction(any());
        verify(vehicleRepository).save(mockVehicle);
    }
}