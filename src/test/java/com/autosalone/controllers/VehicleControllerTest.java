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
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autosalone.dtos.requests.DeadlineRequest;
import com.autosalone.dtos.requests.ExpenseRequest;
import com.autosalone.dtos.requests.VehicleRequest;
import com.autosalone.dtos.requests.VehicleWithdrawRequest;
import com.autosalone.dtos.responses.DeadlineResponse;
import com.autosalone.dtos.responses.ExpenseResponse;
import com.autosalone.dtos.responses.VehicleResponse;
import com.autosalone.enums.VehicleCondition;
import com.autosalone.enums.VehicleStatus;
import com.autosalone.services.DeadlineService;
import com.autosalone.services.TransactionService;
import com.autosalone.services.VehicleService;

import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class VehicleControllerTest {

    @Mock
    private VehicleService vehicleService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private DeadlineService deadlineService;

    @InjectMocks
    private VehicleController vehicleController;

    private LocalDate now;

    private UUID vehicleId;
    private VehicleRequest vehicleRequest;
    private VehicleResponse vehicleResponse;

    private UUID deadlineId;
    private DeadlineResponse deadlineResponse;
    private UUID expenseId;
    private ExpenseResponse expenseResponse;

    @BeforeEach
    void setUp() {
        now = LocalDate.now();
        vehicleId = UUID.randomUUID();

        vehicleRequest = new VehicleRequest("Fiat", "Panda", "Bianco", VehicleCondition.NEW,
                null, null, new BigDecimal("45000"), null, null, null, null, true);

        vehicleResponse = new VehicleResponse(vehicleId, "Fiat", "Panda", "Bianco", VehicleCondition.NEW,
                null, new BigDecimal("45000"), null, null, null, null, true, VehicleStatus.AVAILABLE, null);

        deadlineId = UUID.randomUUID();
        deadlineResponse = new DeadlineResponse(deadlineId, "Revisione", now.plusDays(10), vehicleId, null,
                false, false, null, null, false);

        expenseId = UUID.randomUUID();
        expenseResponse = new ExpenseResponse(expenseId, "Carrozziere", new BigDecimal("500"), now, vehicleId);
    }

    @Test
    void getVehicles_Returns200AndList() {
        when(vehicleService.getVehicles("keyword", "Fiat", VehicleCondition.NEW, new BigDecimal("50000"), true,
                List.of(VehicleStatus.AVAILABLE))).thenReturn(List.of(vehicleResponse));

        Response response = vehicleController.getVehicles("keyword", "Fiat", VehicleCondition.NEW,
                new BigDecimal("50000"), true, List.of(VehicleStatus.AVAILABLE));

        assertEquals(200, response.getStatus());
        assertEquals(1, ((List<?>) response.getEntity()).size());
        verify(vehicleService).getVehicles("keyword", "Fiat", VehicleCondition.NEW, new BigDecimal("50000"), true,
                List.of(VehicleStatus.AVAILABLE));
    }

    @Test
    void getBrands_Returns200AndList() {
        when(vehicleService.getAllBrands()).thenReturn(List.of("Fiat", "BMW"));

        Response response = vehicleController.getBrands();

        assertEquals(200, response.getStatus());
        assertEquals(2, ((List<?>) response.getEntity()).size());
        verify(vehicleService).getAllBrands();
    }

    @Test
    void getVehicleById_Returns200AndVehicle() {
        when(vehicleService.getVehicleResponseById(vehicleId)).thenReturn(vehicleResponse);

        Response response = vehicleController.getVehicleById(vehicleId);

        assertEquals(200, response.getStatus());
        assertEquals(vehicleResponse, response.getEntity());
        verify(vehicleService).getVehicleResponseById(vehicleId);
    }

    @Test
    void addVehicle_Returns201AndLocationHeaderWithBody() {
        when(vehicleService.addVehicle(vehicleRequest)).thenReturn(vehicleResponse);

        Response response = vehicleController.addVehicle(vehicleRequest);

        assertEquals(201, response.getStatus());
        assertEquals(vehicleResponse, response.getEntity());

        URI location = response.getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith("/vehicles/" + vehicleId));

        assertEquals(vehicleResponse, response.getEntity());
    }

    @Test
    void updateVehicle_Returns200AndUpdatedVehicle() {
        when(vehicleService.updateVehicle(vehicleId, vehicleRequest)).thenReturn(vehicleResponse);

        Response response = vehicleController.updateVehicle(vehicleId, vehicleRequest);

        assertEquals(200, response.getStatus());
        assertEquals(vehicleResponse, response.getEntity());
        verify(vehicleService).updateVehicle(vehicleId, vehicleRequest);
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
    void getDeadlines_Returns200AndList() {
        when(deadlineService.getDeadlinesByVehicleId(vehicleId, false)).thenReturn(List.of(deadlineResponse));

        Response response = vehicleController.getDeadlines(vehicleId, false);

        assertEquals(200, response.getStatus());
        assertEquals(1, ((List<?>) response.getEntity()).size());
        verify(deadlineService).getDeadlinesByVehicleId(vehicleId, false);
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

    @Test
    void removeDeadline_Returns204NoContent() {
        Response response = vehicleController.removeDeadline(vehicleId, deadlineId);

        assertEquals(204, response.getStatus());
        verify(vehicleService).removeDeadline(vehicleId, deadlineId);
    }
}