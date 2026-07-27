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

import com.autosalone.dtos.QuotationUpdateRequest;
import com.autosalone.dtos.SalesDocumentCreateRequest;
import com.autosalone.enums.ExpirationPolicy;
import com.autosalone.enums.QuotationStatus;
import com.autosalone.enums.VehicleStatus;
import com.autosalone.exceptions.ResourceNotFoundException;
import com.autosalone.models.Contract;
import com.autosalone.models.Customer;
import com.autosalone.models.Quotation;
import com.autosalone.models.Vehicle;
import com.autosalone.models.catalog.AppliedItem;
import com.autosalone.models.catalog.PurchasableItem;
import com.autosalone.repositories.ContractRepository;
import com.autosalone.repositories.QuotationRepository;
import com.autosalone.repositories.VehicleRepository;

@ExtendWith(MockitoExtension.class)
class QuotationServiceTest {

    @Mock
    private QuotationRepository quotationRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private VehicleService vehicleService;

    @Mock
    private CustomerService customerService;

    @Mock
    private CatalogService catalogService;

    @InjectMocks
    private QuotationService quotationService;

    private Vehicle mockVehicle;
    private Customer mockCustomer;
    private Quotation quotation;

    private UUID quotationId;
    private UUID vehicleId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        quotationId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        mockVehicle = mock(Vehicle.class);
        mockCustomer = mock(Customer.class);

        quotation = new Quotation(mockVehicle, mockCustomer);
    }

    // read

    @Test
    void getQuotationById_Success() {
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(quotation));
        Quotation result = quotationService.getQuotationById(quotationId);
        assertNotNull(result);
        assertEquals(quotation, result);
    }

    @Test
    void getQuotationById_NotFound() {
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> {
            quotationService.getQuotationById(quotationId);
        });
    }

    @Test
    void getQuotations_Success() {
        when(quotationRepository.findQuotations(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(quotation));

        List<Quotation> results = quotationService.getQuotations(null, null, null, null, null, null);
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        verify(quotationRepository).findQuotations(null, null, null, null, null, null);
    }

    @Test
    void getVisibleQuotationsForCustomer_Success() {
        when(quotationRepository.findVisibleQuotationsByCustomerId(customerId))
                .thenReturn(List.of(quotation));

        List<Quotation> results = quotationService.getVisibleQuotationsForCustomer(customerId);
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        verify(quotationRepository).findVisibleQuotationsByCustomerId(customerId);
    }

    // write

    @Test
    void addQuotation_Success() {
        SalesDocumentCreateRequest request = new SalesDocumentCreateRequest(vehicleId, customerId);

        when(vehicleService.getVehicleById(vehicleId)).thenReturn(mockVehicle);
        when(customerService.getCustomerById(customerId)).thenReturn(mockCustomer);

        UUID resultId = quotationService.addQuotation(request);
        assertNull(resultId);
        verify(quotationRepository).save(any(Quotation.class));
    }

    @Test
    void cloneQuotation_Success() {
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(quotation));
        when(mockVehicle.getSellingPrice()).thenReturn(new BigDecimal("10000"));

        UUID resultId = quotationService.cloneQuotation(quotationId);
        assertNull(resultId);
        verify(quotationRepository).save(any(Quotation.class));
    }

    @Test
    void issueQuotation_Success() {
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(quotation));
        when(mockVehicle.getStatus()).thenReturn(VehicleStatus.AVAILABLE);

        quotation.updateExpiration(LocalDate.now().plusDays(10));

        quotationService.issueQuotation(quotationId);

        assertEquals(QuotationStatus.ISSUED, quotation.getStatus());
        verify(mockVehicle).setStatus(VehicleStatus.QUOTED);
        verify(vehicleRepository).save(mockVehicle);
        verify(quotationRepository).save(quotation);
    }

    @Test
    void issueQuotationForQuotedVehicle_Success() {
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(quotation));
        when(mockVehicle.getStatus()).thenReturn(VehicleStatus.QUOTED);

        quotation.updateExpiration(LocalDate.now().plusDays(10));

        quotationService.issueQuotation(quotationId);

        assertEquals(QuotationStatus.ISSUED, quotation.getStatus());
        verify(mockVehicle, never()).setStatus(VehicleStatus.QUOTED);
        verify(vehicleRepository, never()).save(any());
        verify(quotationRepository).save(quotation);
    }

    @Test
    void issueQuotation_FailsWithoutExpirationDate() {
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(quotation));

        assertThrows(IllegalStateException.class, () -> {
            quotationService.issueQuotation(quotationId);
        });
        verify(quotationRepository, never()).save(any());
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void archiveQuotation_Success() {
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(quotation));

        quotationService.archiveQuotation(quotationId);

        assertTrue(quotation.isArchived());
        verify(quotationRepository).save(quotation);
    }

    @Test
    void unarchiveQuotation_Success() {
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(quotation));
        quotation.archive();

        quotationService.unarchiveQuotation(quotationId);

        assertFalse(quotation.isArchived());
        verify(quotationRepository).save(quotation);
    }

    @Test
    void expireOutdatedQuotations_Success() {
        Quotation expiredMock = mock(Quotation.class);
        Contract draftContractMock = mock(Contract.class);

        when(expiredMock.getId()).thenReturn(quotationId);
        when(expiredMock.getVehicle()).thenReturn(mockVehicle);
        when(mockVehicle.getId()).thenReturn(vehicleId);

        when(quotationRepository.findExpiredQuotations(any(LocalDate.class))).thenReturn(List.of(expiredMock));
        when(contractRepository.findDraftContractsBySourceQuotation(quotationId))
                .thenReturn(List.of(draftContractMock));
        when(quotationRepository.findConflictingQuotationsForVehicle(vehicleId, null))
                .thenReturn(Collections.emptyList());
        when(contractRepository.findConflictingContractsForVehicle(vehicleId, null))
                .thenReturn(Collections.emptyList());

        quotationService.expireOutdatedQuotations();

        verify(expiredMock).expire();
        verify(quotationRepository).save(expiredMock);
        verify(draftContractMock).voidDocument();
        verify(contractRepository).save(draftContractMock);

        verify(mockVehicle).setStatus(VehicleStatus.AVAILABLE);
        verify(vehicleRepository).save(mockVehicle);
    }

    @Test
    void expireOutdatedQuotations_WithActiveConflictingQuotation_VehicleStatusRemainsQuoted() {
        Quotation expiredMock = mock(Quotation.class);
        Quotation activeConflictingMock = mock(Quotation.class);

        when(expiredMock.getId()).thenReturn(quotationId);
        when(expiredMock.getVehicle()).thenReturn(mockVehicle);
        when(mockVehicle.getId()).thenReturn(vehicleId);

        when(quotationRepository.findExpiredQuotations(any(LocalDate.class))).thenReturn(List.of(expiredMock));
        when(contractRepository.findDraftContractsBySourceQuotation(quotationId)).thenReturn(Collections.emptyList());

        when(quotationRepository.findConflictingQuotationsForVehicle(vehicleId, null))
                .thenReturn(List.of(activeConflictingMock));

        quotationService.expireOutdatedQuotations();

        verify(expiredMock).expire();
        verify(quotationRepository).save(expiredMock);

        verify(mockVehicle, never()).setStatus(VehicleStatus.AVAILABLE);
        verify(vehicleRepository, never()).save(mockVehicle);
    }

    @Test
    void expireOutdatedQuotations_NoExpiredQuotations_DoesNothing() {
        when(quotationRepository.findExpiredQuotations(any(LocalDate.class))).thenReturn(Collections.emptyList());

        quotationService.expireOutdatedQuotations();

        verify(contractRepository, never()).findDraftContractsBySourceQuotation(any());
        verify(vehicleRepository, never()).save(any());
        verify(quotationRepository, never()).save(any());
    }

    @Test
    void addItemsToQuotation_Success() {
        UUID catalogItemId = UUID.randomUUID();
        PurchasableItem mockItem = mock(PurchasableItem.class);
        when(mockItem.getPrice()).thenReturn(BigDecimal.TEN);

        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(quotation));
        when(catalogService.getItemById(catalogItemId)).thenReturn(mockItem);

        quotationService.addItemsToQuotation(quotationId, Set.of(catalogItemId));

        assertFalse(quotation.getItems().isEmpty());
        verify(quotationRepository).save(quotation);
    }

    @Test
    void addItemsToQuotation_NullOrEmptySet_DoesNothing() {
        quotationService.addItemsToQuotation(quotationId, null);
        quotationService.addItemsToQuotation(quotationId, Collections.emptySet());

        verify(quotationRepository, never()).findById(any());
        verify(quotationRepository, never()).save(any());
        verify(catalogService, never()).getItemById(any());
    }

    @Test
    void removeItemsFromQuotation_Success() {
        UUID catalogItemId = UUID.randomUUID();
        PurchasableItem mockItem = mock(PurchasableItem.class);
        when(mockItem.getId()).thenReturn(catalogItemId);
        when(mockItem.getPrice()).thenReturn(BigDecimal.TEN);

        quotation.addItem(new AppliedItem(mockItem));
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(quotation));

        quotationService.removeItemsFromQuotation(quotationId, Set.of(catalogItemId));

        assertTrue(quotation.getItems().isEmpty());
        verify(quotationRepository).save(quotation);
    }

    @Test
    void removeItemsFromQuotation_NullOrEmptySet_DoesNothing() {
        quotationService.removeItemsFromQuotation(quotationId, null);
        quotationService.removeItemsFromQuotation(quotationId, Collections.emptySet());

        verify(quotationRepository, never()).findById(any());
        verify(quotationRepository, never()).save(any());
    }

    @Test
    void updateAppliedItemPrice_Success() {
        UUID catalogItemId = UUID.randomUUID();
        PurchasableItem mockItem = mock(PurchasableItem.class);
        when(mockItem.getId()).thenReturn(catalogItemId);
        when(mockItem.getPrice()).thenReturn(BigDecimal.TEN);

        quotation.addItem(new AppliedItem(mockItem));
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(quotation));

        BigDecimal newPrice = BigDecimal.valueOf(50);
        quotationService.updateAppliedItemPrice(quotationId, catalogItemId, newPrice);

        assertEquals(newPrice, quotation.getItems().get(0).getAppliedPrice());
        verify(quotationRepository).save(quotation);
    }

    @Test
    void updateAppliedItemPrice_ItemNotFound() {
        UUID wrongCatalogItemId = UUID.randomUUID();
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(quotation));

        assertThrows(ResourceNotFoundException.class, () -> {
            quotationService.updateAppliedItemPrice(quotationId, wrongCatalogItemId, BigDecimal.TEN);
        });

        verify(quotationRepository, never()).save(any());
    }

    @Test
    void updateQuotation_Success_SameVehicleAndCustomer_NoExtraQueries() {
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(quotation));

        when(mockVehicle.getId()).thenReturn(vehicleId);
        when(mockCustomer.getId()).thenReturn(customerId);

        QuotationUpdateRequest request = new QuotationUpdateRequest(
                LocalDate.now().plusDays(7),
                ExpirationPolicy.CUSTOM,
                LocalDate.now(),
                vehicleId,
                customerId,
                BigDecimal.ZERO,
                "Note pubbliche",
                "Note interne",
                BigDecimal.valueOf(10000),
                null,
                null);

        assertDoesNotThrow(() -> quotationService.updateQuotation(quotationId, request));

        assertEquals("Note pubbliche", quotation.getPublicNotes());
        assertEquals(LocalDate.now(), quotation.getDate());

        verify(vehicleService, never()).getVehicleById(any());
        verify(customerService, never()).getCustomerById(any());

        verify(quotationRepository).save(quotation);
    }

    @Test
    void updateQuotation_Success_DifferentVehicleAndCustomer_WithQueries() {
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(quotation));

        when(mockVehicle.getId()).thenReturn(vehicleId);
        when(mockCustomer.getId()).thenReturn(customerId);

        UUID newVehicleId = UUID.randomUUID();
        UUID newCustomerId = UUID.randomUUID();
        Vehicle newMockVehicle = mock(Vehicle.class);
        Customer newMockCustomer = mock(Customer.class);

        when(vehicleService.getVehicleById(newVehicleId)).thenReturn(newMockVehicle);
        when(customerService.getCustomerById(newCustomerId)).thenReturn(newMockCustomer);

        QuotationUpdateRequest request = new QuotationUpdateRequest(
                LocalDate.now().plusDays(7),
                ExpirationPolicy.CUSTOM,
                LocalDate.now(),
                newVehicleId,
                newCustomerId,
                BigDecimal.ZERO,
                "Cambio veicolo",
                null,
                BigDecimal.valueOf(15000),
                null,
                null);

        assertDoesNotThrow(() -> quotationService.updateQuotation(quotationId, request));

        assertEquals(newMockVehicle, quotation.getVehicle());
        assertEquals(newMockCustomer, quotation.getCustomer());

        verify(vehicleService).getVehicleById(newVehicleId);
        verify(customerService).getCustomerById(newCustomerId);

        verify(quotationRepository).save(quotation);
    }

}