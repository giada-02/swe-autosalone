package com.autosalone.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autosalone.dtos.requests.CatalogItemIdsRequest;
import com.autosalone.dtos.requests.CatalogItemPriceUpdateRequest;
import com.autosalone.dtos.requests.QuotationUpdateRequest;
import com.autosalone.dtos.requests.SalesDocumentCreateRequest;
import com.autosalone.dtos.responses.QuotationResponse;
import com.autosalone.enums.ExpirationPolicy;
import com.autosalone.enums.QuotationStatus;
import com.autosalone.services.QuotationService;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@ExtendWith(MockitoExtension.class)
class QuotationControllerTest {

    @Mock
    private QuotationService quotationService;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private QuotationController quotationController;

    private UUID quotationId;
    private UUID vehicleId;
    private UUID customerId;
    private UUID itemId;
    private LocalDate now;

    private QuotationResponse quotationResponse;

    @BeforeEach
    void setUp() {
        now = LocalDate.now();
        quotationId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        itemId = UUID.randomUUID();

        quotationResponse = new QuotationResponse(quotationId, null, null, null, null, null, null, false, null,
                null, null, null, null, null, null, null, null, null);
    }

    @Test
    void getQuotations_Returns200AndList() {
        when(quotationService.getQuotations(now, now, false, vehicleId, customerId, List.of(QuotationStatus.DRAFT)))
                .thenReturn(List.of(quotationResponse));

        Response response = quotationController.getQuotations(now.toString(), now.toString(), false, vehicleId,
                customerId, List.of(QuotationStatus.DRAFT));

        assertEquals(200, response.getStatus());
        assertEquals(1, ((List<?>) response.getEntity()).size());
    }

    @Test
    void getQuotationById_Returns200AndQuotation() {
        when(quotationService.getQuotationResponseById(quotationId)).thenReturn(quotationResponse);

        Response response = quotationController.getQuotationById(quotationId);

        assertEquals(200, response.getStatus());
        assertEquals(quotationResponse, response.getEntity());
    }

    @Test
    void addQuotation_Returns201AndLocationHeader() {
        SalesDocumentCreateRequest request = new SalesDocumentCreateRequest(vehicleId, customerId);
        when(quotationService.addQuotation(request)).thenReturn(quotationResponse);

        Response response = quotationController.addQuotation(request);

        assertEquals(201, response.getStatus());
        URI location = response.getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith("/quotations/" + quotationId));
    }

    @Test
    void updateQuotation_Returns200() {
        QuotationUpdateRequest request = new QuotationUpdateRequest(now, ExpirationPolicy.CUSTOM, now, vehicleId,
                customerId, BigDecimal.ZERO, "Pub", "Int", BigDecimal.TEN, null, null);
        when(quotationService.updateQuotation(quotationId, request)).thenReturn(quotationResponse);

        Response response = quotationController.updateQuotation(quotationId, request);

        assertEquals(200, response.getStatus());
        verify(quotationService).updateQuotation(quotationId, request);
    }

    @Test
    void cloneQuotation_Returns201AndLocationHeader() {
        when(quotationService.cloneQuotation(quotationId)).thenReturn(quotationResponse);

        Response response = quotationController.cloneQuotation(quotationId);

        assertEquals(201, response.getStatus());
        assertTrue(response.getLocation().toString().endsWith("/quotations/" + quotationId));
    }

    @Test
    void issueQuotation_Returns200() {
        when(quotationService.issueQuotation(quotationId)).thenReturn(quotationResponse);
        Response response = quotationController.issueQuotation(quotationId);
        assertEquals(200, response.getStatus());
    }

    @Test
    void archiveQuotation_Returns200() {
        when(quotationService.archiveQuotation(quotationId)).thenReturn(quotationResponse);
        Response response = quotationController.archiveQuotation(quotationId);
        assertEquals(200, response.getStatus());
    }

    @Test
    void unarchiveQuotation_Returns200() {
        when(quotationService.unarchiveQuotation(quotationId)).thenReturn(quotationResponse);
        Response response = quotationController.unarchiveQuotation(quotationId);
        assertEquals(200, response.getStatus());
    }

    // items

    @Test
    void addItemsToQuotation_Returns200() {
        CatalogItemIdsRequest request = new CatalogItemIdsRequest(Set.of(itemId));
        when(quotationService.addItemsToQuotation(quotationId, Set.of(itemId))).thenReturn(quotationResponse);

        Response response = quotationController.addItemsToQuotation(quotationId, request);

        assertEquals(200, response.getStatus());
        verify(quotationService).addItemsToQuotation(quotationId, Set.of(itemId));
    }

    @Test
    void updateAppliedItemPrice_Returns200() {
        CatalogItemPriceUpdateRequest request = new CatalogItemPriceUpdateRequest(BigDecimal.TEN);
        when(quotationService.updateAppliedItemPrice(quotationId, itemId, BigDecimal.TEN))
                .thenReturn(quotationResponse);

        Response response = quotationController.updateAppliedItemPrice(quotationId, itemId, request);

        assertEquals(200, response.getStatus());
        verify(quotationService).updateAppliedItemPrice(quotationId, itemId, BigDecimal.TEN);
    }

    @Test
    void removeItemsFromQuotation_Returns200() {
        CatalogItemIdsRequest request = new CatalogItemIdsRequest(Set.of(itemId));
        when(quotationService.removeItemsFromQuotation(quotationId, Set.of(itemId))).thenReturn(quotationResponse);

        Response response = quotationController.removeItemsFromQuotation(quotationId, request);

        assertEquals(200, response.getStatus());
        verify(quotationService).removeItemsFromQuotation(quotationId, Set.of(itemId));
    }
}