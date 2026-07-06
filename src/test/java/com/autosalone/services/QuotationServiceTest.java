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
import com.autosalone.enums.QuotationStatus;
import com.autosalone.enums.VehicleStatus;
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
    private Quotation mockQuotation;

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

        mockQuotation = new Quotation(mockVehicle, mockCustomer);
    }

    // read

    @Test
    void getQuotationById_Success() {
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(mockQuotation));
        Quotation result = quotationService.getQuotationById(quotationId);
        assertNotNull(result);
        assertEquals(mockQuotation, result);
    }

    @Test
    void getQuotationById_NotFound() {
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> {
            quotationService.getQuotationById(quotationId);
        });
    }

    @Test
    void getQuotations_Success() {
        when(quotationRepository.findQuotations(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(mockQuotation));

        List<Quotation> results = quotationService.getQuotations(null, null, null, null, null, null);
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        verify(quotationRepository, times(1)).findQuotations(null, null, null, null, null, null);
    }

    @Test
    void getVisibleQuotationsForCustomer_Success() {
        when(quotationRepository.findVisibleQuotationsByCustomerId(customerId))
                .thenReturn(List.of(mockQuotation));

        List<Quotation> results = quotationService.getVisibleQuotationsForCustomer(customerId);
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        verify(quotationRepository, times(1)).findVisibleQuotationsByCustomerId(customerId);
    }

    // write

    @Test
    void addQuotation_Success() {
        SalesDocumentCreateRequest request = new SalesDocumentCreateRequest(vehicleId, customerId);

        when(vehicleService.getVehicleById(request.vehicleId())).thenReturn(mockVehicle);
        when(customerService.getCustomerById(request.customerId())).thenReturn(mockCustomer);

        UUID resultId = quotationService.addQuotation(request);
        assertNull(resultId);
        verify(quotationRepository, times(1)).save(any(Quotation.class));
    }

    @Test
    void cloneQuotation_Success() {
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(mockQuotation));
        when(mockVehicle.getSellingPrice()).thenReturn(new BigDecimal("10000"));

        UUID resultId = quotationService.cloneQuotation(quotationId);
        assertNull(resultId);
        verify(quotationRepository, times(1)).save(any(Quotation.class));
    }

    @Test
    void issueQuotation_Success() {
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(mockQuotation));
        when(mockVehicle.getStatus()).thenReturn(VehicleStatus.AVAILABLE);

        mockQuotation.updateExpiration(LocalDate.now().plusDays(10));

        quotationService.issueQuotation(quotationId);

        assertEquals(QuotationStatus.ISSUED, mockQuotation.getStatus());
        verify(mockVehicle, times(1)).setStatus(VehicleStatus.QUOTED);
        verify(vehicleRepository, times(1)).save(mockVehicle);
        verify(quotationRepository, times(1)).save(mockQuotation);
    }

    @Test
    void issueQuotationForQuotedVehicle_Success() {
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(mockQuotation));
        when(mockVehicle.getStatus()).thenReturn(VehicleStatus.QUOTED);

        mockQuotation.updateExpiration(LocalDate.now().plusDays(10));

        quotationService.issueQuotation(quotationId);

        assertEquals(QuotationStatus.ISSUED, mockQuotation.getStatus());
        verify(mockVehicle, never()).setStatus(VehicleStatus.QUOTED);
        verify(vehicleRepository, never()).save(any());
        verify(quotationRepository, times(1)).save(mockQuotation);
    }

    @Test
    void issueQuotation_FailsWithoutExpirationDate() {
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(mockQuotation));

        assertThrows(IllegalStateException.class, () -> {
            quotationService.issueQuotation(quotationId);
        });
        verify(quotationRepository, never()).save(any());
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void archiveQuotation_Success() {
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(mockQuotation));

        quotationService.archiveQuotation(quotationId);

        assertTrue(mockQuotation.isArchived());
        verify(quotationRepository, times(1)).save(mockQuotation);
    }

    @Test
    void unarchiveQuotation_Success() {
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(mockQuotation));
        mockQuotation.archive();

        quotationService.unarchiveQuotation(quotationId);

        assertFalse(mockQuotation.isArchived());
        verify(quotationRepository, times(1)).save(mockQuotation);
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

        verify(expiredMock, times(1)).expire();
        verify(quotationRepository, times(1)).save(expiredMock);
        verify(draftContractMock, times(1)).voidDocument();
        verify(contractRepository, times(1)).save(draftContractMock);

        verify(mockVehicle, times(1)).setStatus(VehicleStatus.AVAILABLE);
        verify(vehicleRepository, times(1)).save(mockVehicle);
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

        verify(expiredMock, times(1)).expire();
        verify(quotationRepository, times(1)).save(expiredMock);

        verify(mockVehicle, never()).setStatus(VehicleStatus.AVAILABLE);
        verify(vehicleRepository, never()).save(mockVehicle);
    }

    @Test
    void addItemsToQuotation_Success() {
        UUID catalogItemId = UUID.randomUUID();
        PurchasableItem mockItem = mock(PurchasableItem.class);
        when(mockItem.getPrice()).thenReturn(BigDecimal.TEN);

        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(mockQuotation));
        when(catalogService.getItemById(catalogItemId)).thenReturn(mockItem);

        quotationService.addItemsToQuotation(quotationId, Set.of(catalogItemId));

        assertFalse(mockQuotation.getItems().isEmpty());
        verify(quotationRepository, times(1)).save(mockQuotation);
    }

    @Test
    void removeItemsFromQuotation_Success() {
        UUID catalogItemId = UUID.randomUUID();
        PurchasableItem mockItem = mock(PurchasableItem.class);
        when(mockItem.getId()).thenReturn(catalogItemId);
        when(mockItem.getPrice()).thenReturn(BigDecimal.TEN);

        AppliedItem appliedItem = new AppliedItem(mockItem);
        mockQuotation.addItem(appliedItem);

        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(mockQuotation));

        quotationService.removeItemsFromQuotation(quotationId, Set.of(catalogItemId));

        assertTrue(mockQuotation.getItems().isEmpty());
        verify(quotationRepository, times(1)).save(mockQuotation);
    }

    @Test
    void updateAppliedItemPrice_Success() {
        UUID catalogItemId = UUID.randomUUID();
        PurchasableItem mockItem = mock(PurchasableItem.class);
        when(mockItem.getId()).thenReturn(catalogItemId);
        when(mockItem.getPrice()).thenReturn(BigDecimal.TEN);

        AppliedItem appliedItem = new AppliedItem(mockItem);
        mockQuotation.addItem(appliedItem);

        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(mockQuotation));

        BigDecimal newPrice = BigDecimal.valueOf(99.99);
        quotationService.updateAppliedItemPrice(quotationId, catalogItemId, newPrice);

        assertEquals(newPrice, mockQuotation.getItems().get(0).getAppliedPrice());
        verify(quotationRepository, times(1)).save(mockQuotation);
    }

    @Test
    void updateAppliedItemPrice_ItemNotFound() {
        UUID wrongCatalogItemId = UUID.randomUUID();
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(mockQuotation));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            quotationService.updateAppliedItemPrice(quotationId, wrongCatalogItemId, BigDecimal.TEN);
        });

        assertTrue(ex.getMessage().contains("Accessory of id"));
        verify(quotationRepository, never()).save(any());
    }

    @Test
    void updateQuotation_Success() {
        when(quotationRepository.findById(quotationId)).thenReturn(Optional.of(mockQuotation));

        QuotationUpdateRequest request = mock(QuotationUpdateRequest.class);
        when(request.publicNotes()).thenReturn("Nuove note pubbliche");
        when(request.date()).thenReturn(LocalDate.now().plusDays(1));

        quotationService.updateQuotation(quotationId, request);

        assertEquals("Nuove note pubbliche", mockQuotation.getPublicNotes());
        assertEquals(LocalDate.now().plusDays(1), mockQuotation.getDate());
        verify(quotationRepository, times(1)).save(mockQuotation);
    }

}