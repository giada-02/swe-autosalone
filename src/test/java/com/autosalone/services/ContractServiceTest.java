package com.autosalone.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autosalone.dtos.ContractUpdateRequest;
import com.autosalone.dtos.SalesDocumentCreateRequest;
import com.autosalone.enums.VehicleCondition;
import com.autosalone.enums.VehicleStatus;
import com.autosalone.models.Contract;
import com.autosalone.models.Customer;
import com.autosalone.models.Quotation;
import com.autosalone.models.Transaction;
import com.autosalone.models.Vehicle;
import com.autosalone.models.catalog.AppliedItem;
import com.autosalone.models.catalog.PurchasableItem;
import com.autosalone.repositories.ContractRepository;
import com.autosalone.repositories.QuotationRepository;
import com.autosalone.repositories.VehicleRepository;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private QuotationRepository quotationRepository;
    @Mock
    private QuotationService quotationService;

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private VehicleService vehicleService;

    @Mock
    private CustomerService customerService;

    @Mock
    private CatalogService catalogService;

    @InjectMocks
    private ContractService contractService;

    private UUID contractId;
    private UUID quotationId;
    private UUID vehicleId;
    private UUID customerId;

    private Vehicle mockVehicle;
    private Customer mockCustomer;
    private Contract mockContract;

    @BeforeEach
    void setUp() {
        contractId = UUID.randomUUID();
        quotationId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        mockVehicle = mock(Vehicle.class);
        mockCustomer = mock(Customer.class);
        mockContract = mock(Contract.class);
    }

    // read

    @Test
    void getContractById_Success() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(mockContract));
        Contract result = contractService.getContractById(contractId);
        assertNotNull(result);
        assertEquals(mockContract, result);
    }

    @Test
    void getContractById_NotFound() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> contractService.getContractById(contractId));
    }

    @Test
    void getContracts_Success() {
        when(contractRepository.findContracts(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(mockContract));

        List<Contract> results = contractService.getContracts(null, null, null, null, null, null);
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        verify(contractRepository, times(1)).findContracts(null, null, null, null, null, null);
    }

    @Test
    void getVisibleContractsForCustomer_Success() {
        when(contractRepository.findVisibleContractsByCustomerId(customerId))
                .thenReturn(List.of(mockContract));

        List<Contract> results = contractService.getVisibleContractsForCustomer(customerId);
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        verify(contractRepository, times(1)).findVisibleContractsByCustomerId(customerId);
    }

    // write

    @Test
    void addContract_Success() {
        SalesDocumentCreateRequest request = new SalesDocumentCreateRequest(vehicleId, customerId);

        when(vehicleService.getVehicleById(vehicleId)).thenReturn(mockVehicle);
        when(customerService.getCustomerById(customerId)).thenReturn(mockCustomer);
        when(mockVehicle.getSellingPrice()).thenReturn(BigDecimal.valueOf(10000));

        UUID resultId = contractService.addContract(request);
        assertNull(resultId);
        verify(contractRepository, times(1)).save(any(Contract.class));
    }

    @Test
    void createContractFromQuotation_Success() {
        when(mockVehicle.getSellingPrice()).thenReturn(BigDecimal.valueOf(10000));
        Quotation sourceQuotation = new Quotation(mockVehicle, mockCustomer);
        sourceQuotation.updateExpiration(LocalDate.now().plusDays(10));
        sourceQuotation.issue();

        when(quotationService.getQuotationById(quotationId)).thenReturn(sourceQuotation);

        contractService.createContractFromQuotation(quotationId);

        verify(contractRepository, times(1)).save(any(Contract.class));
    }

    @Test
    void confirmContract_WithConflictsAndQuotation_Success() {
        Quotation linkedQuotation = mock(Quotation.class);
        Quotation conflictQuotation = mock(Quotation.class);
        Contract conflictContract = mock(Contract.class);

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(mockContract));
        when(mockContract.getQuotationReference()).thenReturn(linkedQuotation);
        when(mockContract.getVehicle()).thenReturn(mockVehicle);

        when(linkedQuotation.getId()).thenReturn(quotationId);
        when(mockVehicle.getId()).thenReturn(vehicleId);

        when(quotationRepository.findConflictingQuotationsForVehicle(vehicleId, quotationId))
                .thenReturn(List.of(conflictQuotation));
        when(contractRepository.findConflictingContractsForVehicle(vehicleId, contractId))
                .thenReturn(List.of(conflictContract));

        when(mockContract.getCustomer()).thenReturn(mockCustomer);
        when(mockCustomer.getFirstName()).thenReturn("Mario");
        when(mockCustomer.getLastName()).thenReturn("Rossi");

        contractService.confirmContract(contractId, BigDecimal.TEN, LocalDate.now());

        verify(mockContract, times(1)).confirm(any());
        verify(linkedQuotation, times(1)).accept();
        verify(quotationRepository, times(1)).save(linkedQuotation);

        verify(mockVehicle, times(1)).setStatus(VehicleStatus.RESERVED);
        verify(vehicleRepository, times(1)).save(mockVehicle);

        verify(conflictQuotation, times(1)).voidDocument();
        verify(quotationRepository, times(1)).save(conflictQuotation);

        verify(conflictContract, times(1)).voidDocument();
        verify(contractRepository, times(1)).save(conflictContract);

        verify(contractRepository, times(1)).save(mockContract);
    }

    @Test
    void confirmContract_WithoutQuotation_Success() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(mockContract));
        when(mockContract.getQuotationReference()).thenReturn(null);
        when(mockContract.getVehicle()).thenReturn(mockVehicle);
        when(mockVehicle.getId()).thenReturn(vehicleId);

        when(quotationRepository.findConflictingQuotationsForVehicle(vehicleId, null))
                .thenReturn(Collections.emptyList());
        when(contractRepository.findConflictingContractsForVehicle(vehicleId, contractId))
                .thenReturn(Collections.emptyList());

        contractService.confirmContract(contractId, null, null);

        verify(mockContract, times(1)).confirm(null);
        verify(quotationRepository, never()).save(any(Quotation.class));
        verify(contractRepository, times(1)).save(mockContract);
    }

    @Test
    void completeContract_Success() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(mockContract));
        when(mockContract.getVehicle()).thenReturn(mockVehicle);
        when(mockVehicle.getCondition()).thenReturn(VehicleCondition.NEW);

        contractService.completeContract(contractId);

        verify(mockContract, times(1)).complete();
        verify(mockVehicle, times(1)).setIsInShowroom(false);
        verify(mockVehicle, times(1)).generateStandardInspectionDeadline();
        verify(mockVehicle, times(1)).setStatus(VehicleStatus.SOLD);

        verify(vehicleRepository, times(1)).save(mockVehicle);
        verify(contractRepository, times(1)).save(mockContract);
    }

    @Test
    void cancelContract_Success() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(mockContract));
        when(mockContract.getVehicle()).thenReturn(mockVehicle);

        contractService.cancelContract(contractId, "Motivo cancellazione");

        verify(mockContract, times(1)).cancel("Motivo cancellazione");
        verify(mockVehicle, times(1)).setStatus(VehicleStatus.AVAILABLE);

        verify(vehicleRepository, times(1)).save(mockVehicle);
        verify(contractRepository, times(1)).save(mockContract);
    }

    @Test
    void archiveContract_Success() {
        Contract contract = getContract();
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        contractService.archiveContract(contractId);

        assertTrue(contract.isArchived());
        verify(contractRepository, times(1)).save(contract);
    }

    @Test
    void unarchiveContract_Success() {
        Contract contract = getContract();
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        contractService.unarchiveContract(contractId);

        assertFalse(contract.isArchived());
        verify(contractRepository, times(1)).save(contract);
    }

    @Test
    void addPaymentToContract_Success() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(mockContract));

        when(mockContract.getCustomer()).thenReturn(mockCustomer);
        when(mockCustomer.getFirstName()).thenReturn("Mario");
        when(mockCustomer.getLastName()).thenReturn("Rossi");
        when(mockContract.getVehicle()).thenReturn(mockVehicle);

        contractService.addPaymentToContract(contractId, "Bonifico", BigDecimal.TEN, LocalDate.now());

        verify(mockContract, times(1)).registerPayment(any(Transaction.class));
        verify(contractRepository, times(1)).save(mockContract);
    }

    @Test
    void addRefundToContract_Success() {
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(mockContract));

        when(mockContract.getCustomer()).thenReturn(mockCustomer);
        when(mockCustomer.getFirstName()).thenReturn("Mario");
        when(mockCustomer.getLastName()).thenReturn("Rossi");
        when(mockContract.getVehicle()).thenReturn(mockVehicle);

        contractService.addRefundToContract(contractId, "Rimborso", BigDecimal.TEN, LocalDate.now());

        verify(mockContract, times(1)).registerRefund(any(Transaction.class));
        verify(contractRepository, times(1)).save(mockContract);
    }

    @Test
    void addItemsToContract_Success() {
        Contract contract = getContract();
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        UUID catalogItemId = UUID.randomUUID();
        PurchasableItem mockItem = mock(PurchasableItem.class);
        when(mockItem.getPrice()).thenReturn(BigDecimal.TEN);
        when(catalogService.getItemById(catalogItemId)).thenReturn(mockItem);

        contractService.addItemsToContract(contractId, Set.of(catalogItemId));

        assertFalse(contract.getItems().isEmpty());
        verify(contractRepository, times(1)).save(contract);
    }

    @Test
    void addItemsToContract_NullOrEmptySet_DoesNothing() {
        contractService.addItemsToContract(contractId, null);
        contractService.addItemsToContract(contractId, Collections.emptySet());

        verify(contractRepository, never()).findById(any());
        verify(contractRepository, never()).save(any());
    }

    @Test
    void removeItemsFromContract_Success() {
        Contract contract = getContract();
        UUID catalogItemId = UUID.randomUUID();
        PurchasableItem mockItem = mock(PurchasableItem.class);
        when(mockItem.getId()).thenReturn(catalogItemId);
        when(mockItem.getPrice()).thenReturn(BigDecimal.TEN);

        contract.addItem(new AppliedItem(mockItem));
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        contractService.removeItemsFromContract(contractId, Set.of(catalogItemId));

        assertTrue(contract.getItems().isEmpty());
        verify(contractRepository, times(1)).save(contract);
    }

    @Test
    void removeItemsFromContract_NullOrEmptySet_DoesNothing() {
        contractService.removeItemsFromContract(contractId, null);
        contractService.removeItemsFromContract(contractId, Collections.emptySet());

        verify(contractRepository, never()).findById(any());
        verify(contractRepository, never()).save(any());
    }

    @Test
    void updateAppliedItemPrice_Success() {
        Contract contract = getContract();
        UUID catalogItemId = UUID.randomUUID();
        PurchasableItem mockItem = mock(PurchasableItem.class);
        when(mockItem.getId()).thenReturn(catalogItemId);
        when(mockItem.getPrice()).thenReturn(BigDecimal.TEN);

        contract.addItem(new AppliedItem(mockItem));
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        BigDecimal newPrice = BigDecimal.valueOf(50);
        contractService.updateAppliedItemPrice(contractId, catalogItemId, newPrice);

        assertEquals(newPrice, contract.getItems().get(0).getAppliedPrice());
        verify(contractRepository, times(1)).save(contract);
    }

    @Test
    void updateAppliedItemPrice_ItemNotFound() {
        Contract contract = getContract();
        UUID wrongCatalogItemId = UUID.randomUUID();
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

        assertThrows(IllegalArgumentException.class, () -> {
            contractService.updateAppliedItemPrice(contractId, wrongCatalogItemId, BigDecimal.TEN);
        });

        verify(contractRepository, never()).save(any());
    }

    @Test
    void updateContract_WithAllNullFields_DoesNotCrash() {
        Contract contract = getContract();
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        ContractUpdateRequest request = mock(ContractUpdateRequest.class);
        when(request.internalNotes()).thenReturn("Nuove note interne");
        when(request.date()).thenReturn(LocalDate.now().plusDays(2));

        assertDoesNotThrow(() -> contractService.updateContract(contractId, request));

        assertEquals("Nuove note interne", contract.getInternalNotes());
        assertEquals(LocalDate.now().plusDays(2), contract.getDate());
        verify(contractRepository, times(1)).save(contract);
    }

    // helper
    private Contract getContract() {
        when(mockVehicle.getSellingPrice()).thenReturn(BigDecimal.valueOf(10000));
        return new Contract(mockVehicle, mockCustomer);
    }
}