package com.autosalone.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.URI;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autosalone.dtos.requests.DeadlineRequest;
import com.autosalone.dtos.requests.ExpenseRequest;
import com.autosalone.dtos.requests.PurchaseTransactionRequest;
import com.autosalone.dtos.requests.VehicleCreateRequest;
import com.autosalone.dtos.requests.VehicleUpdateRequest;
import com.autosalone.dtos.requests.VehicleWithdrawRequest;
import com.autosalone.dtos.responses.DeadlineResponse;
import com.autosalone.dtos.responses.ExpenseResponse;
import com.autosalone.dtos.responses.VehicleCustomerResponse;
import com.autosalone.dtos.responses.VehicleResponse;
import com.autosalone.enums.VehicleCondition;
import com.autosalone.enums.VehicleStatus;
import com.autosalone.services.DeadlineService;
import com.autosalone.services.TransactionService;
import com.autosalone.services.VehicleService;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@ExtendWith(MockitoExtension.class)
class VehicleControllerTest {

    @Mock
    private VehicleService vehicleService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private DeadlineService deadlineService;

    @Mock
    private SecurityContext securityContext;
    @Mock
    private Principal principal;

    @InjectMocks
    private VehicleController vehicleController;

    private LocalDate now;

    private UUID vehicleId;
    private VehicleCreateRequest vehicleCreateRequest;
    private VehicleUpdateRequest vehicleUpdateRequest;
    private VehicleResponse vehicleResponse;
    private VehicleCustomerResponse vehicleCustomerResponse;

    private UUID deadlineId;
    private DeadlineResponse deadlineResponse;
    private UUID expenseId;
    private ExpenseResponse expenseResponse;

    @BeforeEach
    void setUp() {
        now = LocalDate.now();
        vehicleId = UUID.randomUUID();

        vehicleCreateRequest = buildCreateRequest();
        vehicleUpdateRequest = buildUpdateRequest();
        vehicleResponse = buildVehicleResponse(vehicleId);
        vehicleCustomerResponse = buildVehicleCustomerResponse(vehicleId);

        deadlineId = UUID.randomUUID();
        deadlineResponse = new DeadlineResponse(deadlineId, "Revisione", now.plusDays(10).toString(), vehicleId, null,
                false, false, null, null, false);

        expenseId = UUID.randomUUID();
        expenseResponse = new ExpenseResponse(expenseId, "Carrozziere", new BigDecimal("500"), now.toString(),
                vehicleId);
    }

    @Test
    void getVehicles_AsOwner_Returns200AndList() {
        when(securityContext.isUserInRole("CUSTOMER")).thenReturn(false);
        when(vehicleService.getVehiclesForOwner("keyword", "Fiat", VehicleCondition.NEW, new BigDecimal("50000"), true,
                List.of(VehicleStatus.AVAILABLE))).thenReturn(List.of(vehicleResponse));

        Response response = vehicleController.getVehicles("keyword", "Fiat", VehicleCondition.NEW,
                new BigDecimal("50000"), true, List.of(VehicleStatus.AVAILABLE));

        assertEquals(200, response.getStatus());
        assertEquals(1, ((List<?>) response.getEntity()).size());
    }

    @Test
    void getVehicles_AsCustomer_Returns200AndOverridesFilters() {
        UUID customerId = UUID.randomUUID();
        when(securityContext.isUserInRole("CUSTOMER")).thenReturn(true);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(customerId.toString());

        when(vehicleService.getVehiclesForCustomer(customerId)).thenReturn(List.of(vehicleCustomerResponse));

        Response response = vehicleController.getVehicles(null, null, null, null, null, null);

        assertEquals(200, response.getStatus());

        verify(vehicleService).getVehiclesForCustomer(customerId);
    }

    @Test
    void getBrands_Returns200AndList() {
        when(vehicleService.getAllBrands()).thenReturn(List.of("Fiat", "BMW"));

        Response response = vehicleController.getBrands();

        assertEquals(200, response.getStatus());
        assertEquals(2, ((List<?>) response.getEntity()).size());
    }

    @Test
    void getVehicleById_AsOwner_Returns200AndVehicle() {
        when(securityContext.isUserInRole("CUSTOMER")).thenReturn(false);
        when(vehicleService.getVehicleResponseById(vehicleId)).thenReturn(vehicleResponse);

        Response response = vehicleController.getVehicleById(vehicleId);

        assertEquals(200, response.getStatus());
        assertEquals(vehicleResponse, response.getEntity());
        verify(vehicleService).getVehicleResponseById(vehicleId);
    }

    @Test
    void getVehicleById_AsCustomer_Returns200AndVehicle() {
        UUID customerId = UUID.randomUUID();
        when(securityContext.isUserInRole("CUSTOMER")).thenReturn(true);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(customerId.toString());

        when(vehicleService.isVehicleOwnedByCustomer(vehicleId, customerId)).thenReturn(true);
        when(vehicleService.getVehicleCustomerResponseById(vehicleId)).thenReturn(vehicleCustomerResponse);

        Response response = vehicleController.getVehicleById(vehicleId);

        assertEquals(200, response.getStatus());
        assertEquals(vehicleCustomerResponse, response.getEntity());
        verify(vehicleService).getVehicleCustomerResponseById(vehicleId);
    }

    @Test
    void getVehicleById_AsCustomer_Returns403WhenNotOwned() {
        UUID customerId = UUID.randomUUID();
        when(securityContext.isUserInRole("CUSTOMER")).thenReturn(true);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(customerId.toString());

        when(vehicleService.isVehicleOwnedByCustomer(vehicleId, customerId)).thenReturn(false);

        Response response = vehicleController.getVehicleById(vehicleId);

        assertEquals(403, response.getStatus());
        verify(vehicleService, never()).getVehicleCustomerResponseById(any());
        verify(vehicleService, never()).getVehicleResponseById(any());
    }

    @Test
    void addVehicle_Returns201AndLocationHeaderWithBody() {
        when(vehicleService.addVehicle(vehicleCreateRequest)).thenReturn(vehicleResponse);

        Response response = vehicleController.addVehicle(vehicleCreateRequest);

        assertEquals(201, response.getStatus());
        assertEquals(vehicleResponse, response.getEntity());

        URI location = response.getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith("/vehicles/" + vehicleId));

        assertEquals(vehicleResponse, response.getEntity());
    }

    @Test
    void addPurchaseTransaction_Returns200AndVehicle() {
        PurchaseTransactionRequest purchaseRequest = new PurchaseTransactionRequest(new BigDecimal("10000"), now);
        when(vehicleService.addPurchaseTransaction(vehicleId, purchaseRequest)).thenReturn(vehicleResponse);

        Response response = vehicleController.addPurchaseTransaction(vehicleId, purchaseRequest);

        assertEquals(200, response.getStatus());
        assertEquals(vehicleResponse, response.getEntity());
        verify(vehicleService).addPurchaseTransaction(vehicleId, purchaseRequest);
    }

    @Test
    void updateVehicle_Returns200AndUpdatedVehicle() {
        when(vehicleService.updateVehicle(vehicleId, vehicleUpdateRequest)).thenReturn(vehicleResponse);

        Response response = vehicleController.updateVehicle(vehicleId, vehicleUpdateRequest);

        assertEquals(200, response.getStatus());
        assertEquals(vehicleResponse, response.getEntity());
        verify(vehicleService).updateVehicle(vehicleId, vehicleUpdateRequest);
    }

    @Test
    void withdrawVehicle_Returns204NoContent() {
        VehicleWithdrawRequest withdrawRequest = new VehicleWithdrawRequest("Danneggiata");
        when(vehicleService.withdrawVehicle(vehicleId, withdrawRequest.reason())).thenReturn(vehicleResponse);

        Response response = vehicleController.withdrawVehicle(vehicleId, withdrawRequest);

        assertEquals(200, response.getStatus());
        assertEquals(vehicleResponse, response.getEntity());
        verify(vehicleService).withdrawVehicle(vehicleId, "Danneggiata");
    }

    // expenses

    @Test
    void getExpenses_Returns200AndList() {
        when(transactionService.getExpensesByVehicleId(vehicleId)).thenReturn(List.of(expenseResponse));

        Response response = vehicleController.getExpenses(vehicleId);

        assertEquals(200, response.getStatus());
        assertEquals(1, ((List<?>) response.getEntity()).size());
        verify(transactionService).getExpensesByVehicleId(vehicleId);
    }

    @Test
    void addExpense_Returns201AndLocationHeaderWithBody() {
        ExpenseRequest expenseRequest = new ExpenseRequest("Carrozziere", new BigDecimal("500"), now);
        when(vehicleService.addExpense(vehicleId, "Carrozziere", new BigDecimal("500"), now))
                .thenReturn(expenseResponse);

        Response response = vehicleController.addExpense(vehicleId, expenseRequest);

        assertEquals(201, response.getStatus());
        assertEquals(expenseResponse, response.getEntity());

        URI location = response.getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith("/vehicles/" + vehicleId + "/expenses/" + expenseId));
    }

    // inspections and deadlines

    @Test
    void generateStandardInspection_Returns201AndLocationHeaderWithBody() {
        LocalDate lastInspection = now.minusYears(1);
        String lastInspectionDateString = lastInspection.toString();
        when(vehicleService.generateStandardInspection(vehicleId, lastInspection)).thenReturn(deadlineResponse);

        Response response = vehicleController.generateStandardInspection(vehicleId, lastInspectionDateString);

        assertEquals(201, response.getStatus());
        assertEquals(deadlineResponse, response.getEntity());

        URI location = response.getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith("/vehicles/" + vehicleId + "/deadlines/" + deadlineId));
    }

    @Test
    void getDeadlines_AsOwner_Returns200AndList() {
        UUID ownerId = UUID.randomUUID();
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(ownerId.toString());
        when(securityContext.isUserInRole("OWNER")).thenReturn(true);
        when(deadlineService.getDeadlinesByVehicleId(vehicleId, false, ownerId, true))
                .thenReturn(List.of(deadlineResponse));

        Response response = vehicleController.getDeadlines(vehicleId, false);

        assertEquals(200, response.getStatus());
        assertEquals(1, ((List<?>) response.getEntity()).size());
        verify(deadlineService).getDeadlinesByVehicleId(vehicleId, false, ownerId, true);
    }

    @Test
    void getDeadlines_AsCustomer_Returns200AndList() {
        UUID customerId = UUID.randomUUID();
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(customerId.toString());
        when(securityContext.isUserInRole("OWNER")).thenReturn(false);

        when(vehicleService.isVehicleOwnedByCustomer(vehicleId, customerId)).thenReturn(true);

        when(deadlineService.getDeadlinesByVehicleId(vehicleId, true, customerId, false))
                .thenReturn(List.of(deadlineResponse));

        Response response = vehicleController.getDeadlines(vehicleId, true);

        assertEquals(200, response.getStatus());
        assertEquals(List.of(deadlineResponse), response.getEntity());
        verify(deadlineService).getDeadlinesByVehicleId(vehicleId, true, customerId, false);
    }

    @Test
    void getDeadlines_AsCustomer_Returns403WhenNotOwned() {
        UUID customerId = UUID.randomUUID();
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(customerId.toString());
        when(securityContext.isUserInRole("OWNER")).thenReturn(false);

        when(vehicleService.isVehicleOwnedByCustomer(vehicleId, customerId)).thenReturn(false);

        Response response = vehicleController.getDeadlines(vehicleId, false);

        assertEquals(403, response.getStatus());

        verify(deadlineService, never()).getDeadlinesByVehicleId(any(), eq(false), any(), eq(false));
    }

    @Test
    void addDeadline_Returns201AndLocationHeaderWithBody() {
        DeadlineRequest deadlineRequest = new DeadlineRequest("Tagliando", now, null, false);
        when(vehicleService.addDeadline(vehicleId, deadlineRequest)).thenReturn(deadlineResponse);

        Response response = vehicleController.addDeadline(vehicleId, deadlineRequest);

        assertEquals(201, response.getStatus());
        assertEquals(deadlineResponse, response.getEntity());

        URI location = response.getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith("/vehicles/" + vehicleId + "/deadlines/" + deadlineId));
    }

    // helper methods

    private VehicleCreateRequest buildCreateRequest() {
        return new VehicleCreateRequest("Fiat", "Panda", "Bianco", VehicleCondition.NEW,
                new BigDecimal("45000"), null, null, null, null, true, null);
    }

    private VehicleUpdateRequest buildUpdateRequest() {
        return new VehicleUpdateRequest("Fiat", "Panda", "Bianco", VehicleCondition.NEW,
                new BigDecimal("45000"), null, null, null, null, true);
    }

    private VehicleResponse buildVehicleResponse(UUID id) {
        return new VehicleResponse(id, "Fiat", "Panda", "Bianco", VehicleCondition.NEW,
                new BigDecimal("45000"), null, null, null, null, true, VehicleStatus.AVAILABLE, null, null);
    }

    private VehicleCustomerResponse buildVehicleCustomerResponse(UUID id) {
        return new VehicleCustomerResponse(id, "Fiat", "Panda", "Bianco",
                VehicleCondition.NEW, new BigDecimal("45000"), null, null, null, null);
    }
}