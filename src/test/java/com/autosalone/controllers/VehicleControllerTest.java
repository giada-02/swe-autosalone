package com.autosalone.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
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

import com.autosalone.dtos.DeadlineRequest;
import com.autosalone.dtos.VehicleRequest;
import com.autosalone.dtos.VehicleWithdrawRequest;
import com.autosalone.enums.VehicleCondition;
import com.autosalone.enums.VehicleStatus;
import com.autosalone.models.Deadline;
import com.autosalone.models.Transaction;
import com.autosalone.models.Vehicle;
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

    private UUID vehicleId;
    private Vehicle mockVehicle;
    private VehicleRequest vehicleRequest;
    private UUID deadlineId;
    private Deadline mockDeadline;
    private UUID expenseId;
    private Transaction mockExpense;

    @BeforeEach
    void setUp() {
        vehicleId = UUID.randomUUID();
        mockVehicle = mock(Vehicle.class);

        vehicleRequest = new VehicleRequest("Fiat", "Panda", "Bianco", VehicleCondition.NEW,
                null, null, new BigDecimal("45000"), null, null, null, null, true);

        deadlineId = UUID.randomUUID();
        mockDeadline = mock(Deadline.class);
        expenseId = UUID.randomUUID();
        mockExpense = mock(Transaction.class);
    }

    @Test
    void getVehicles_Returns200AndList() {
        when(vehicleService.getVehicles("keyword", "Fiat", VehicleCondition.NEW, new BigDecimal("50000"), true,
                List.of(VehicleStatus.AVAILABLE)))
                .thenReturn(List.of(mockVehicle));

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
        when(vehicleService.getVehicleById(vehicleId)).thenReturn(mockVehicle);

        Response response = vehicleController.getVehicleById(vehicleId);

        assertEquals(200, response.getStatus());
        assertEquals(mockVehicle, response.getEntity());
        verify(vehicleService).getVehicleById(vehicleId);
    }

    @Test
    void addVehicle_Returns201AndLocationHeaderWithBody() {
        when(mockVehicle.getId()).thenReturn(vehicleId);
        when(vehicleService.addVehicle(vehicleRequest)).thenReturn(mockVehicle);

        Response response = vehicleController.addVehicle(vehicleRequest);

        assertEquals(201, response.getStatus());
        assertEquals(mockVehicle, response.getEntity());

        URI location = response.getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith("/vehicles/" + vehicleId));

        assertEquals(mockVehicle, response.getEntity());
    }

    @Test
    void updateVehicle_Returns200AndUpdatedVehicle() {
        when(vehicleService.updateVehicle(vehicleId, vehicleRequest)).thenReturn(mockVehicle);

        Response response = vehicleController.updateVehicle(vehicleId, vehicleRequest);

        assertEquals(200, response.getStatus());
        assertEquals(mockVehicle, response.getEntity());
        verify(vehicleService).updateVehicle(vehicleId, vehicleRequest);
    }

    @Test
    void withdrawVehicle_Returns204NoContent() {
        VehicleWithdrawRequest withdrawRequest = new VehicleWithdrawRequest("Danneggiata");

        Response response = vehicleController.withdrawVehicle(vehicleId, withdrawRequest);

        assertEquals(204, response.getStatus());
        verify(vehicleService).withdrawVehicle(vehicleId, "Danneggiata");
    }

    // expenses

    @Test
    void getExpenses_Returns200AndList() {
        when(transactionService.getExpensesByVehicleId(vehicleId)).thenReturn(List.of(mockExpense));

        Response response = vehicleController.getExpenses(vehicleId);

        assertEquals(200, response.getStatus());
        assertEquals(1, ((List<?>) response.getEntity()).size());
        verify(transactionService).getExpensesByVehicleId(vehicleId);
    }

    @Test
    void addExpense_Returns201AndLocationHeaderWithBody() {
        when(mockExpense.getId()).thenReturn(expenseId);

        LocalDate date = LocalDate.now();
        when(vehicleService.addExpense(vehicleId, "Carrozziere", new BigDecimal("500"), date)).thenReturn(mockExpense);

        Response response = vehicleController.addExpense(vehicleId, "Carrozziere", new BigDecimal("500"), date);

        assertEquals(201, response.getStatus());
        assertEquals(mockExpense, response.getEntity());

        URI location = response.getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith("/vehicles/" + vehicleId + "/expenses/" + expenseId));

        assertEquals(mockExpense, response.getEntity());
    }

    // inspections and deadlines

    @Test
    void generateStandardInspection_Returns201AndLocationHeaderWithBody() {
        when(mockDeadline.getId()).thenReturn(deadlineId);

        LocalDate lastInspection = LocalDate.now().minusYears(1);
        when(vehicleService.generateStandardInspection(vehicleId, lastInspection)).thenReturn(mockDeadline);

        Response response = vehicleController.generateStandardInspection(vehicleId, lastInspection);

        assertEquals(201, response.getStatus());
        assertEquals(mockDeadline, response.getEntity());

        URI location = response.getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith("/vehicles/" + vehicleId + "/deadlines/" + deadlineId));

        assertEquals(mockDeadline, response.getEntity());
    }

    @Test
    void getDeadlines_Returns200AndList() {
        when(deadlineService.getDeadlinesByVehicleId(vehicleId, false)).thenReturn(List.of(mockDeadline));

        Response response = vehicleController.getDeadlines(vehicleId, false);

        assertEquals(200, response.getStatus());
        assertEquals(1, ((List<?>) response.getEntity()).size());
        verify(deadlineService).getDeadlinesByVehicleId(vehicleId, false);
    }

    @Test
    void addDeadline_Returns201AndLocationHeaderWithBody() {
        when(mockDeadline.getId()).thenReturn(deadlineId);

        DeadlineRequest deadlineRequest = new DeadlineRequest("Tagliando", LocalDate.now(), null, false);
        when(vehicleService.addDeadline(vehicleId, deadlineRequest)).thenReturn(mockDeadline);

        Response response = vehicleController.addDeadline(vehicleId, deadlineRequest);

        assertEquals(201, response.getStatus());
        assertEquals(mockDeadline, response.getEntity());

        URI location = response.getLocation();
        assertNotNull(location);
        assertTrue(location.toString().endsWith("/vehicles/" + vehicleId + "/deadlines/" + deadlineId));

        assertEquals(mockDeadline, response.getEntity());
    }

    @Test
    void removeDeadline_Returns204NoContent() {
        Response response = vehicleController.removeDeadline(vehicleId, deadlineId);

        assertEquals(204, response.getStatus());
        verify(vehicleService).removeDeadline(vehicleId, deadlineId);
    }
}