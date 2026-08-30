package com.autosalone.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autosalone.dtos.requests.DeadlineCompletionRequest;
import com.autosalone.dtos.requests.DeadlineRequest;
import com.autosalone.dtos.responses.DeadlineResponse;
import com.autosalone.enums.VehicleStatus;
import com.autosalone.models.Deadline;
import com.autosalone.models.Vehicle;
import com.autosalone.services.DeadlineService;
import com.autosalone.services.VehicleService;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@ExtendWith(MockitoExtension.class)
class DeadlineControllerTest {

    @Mock
    private DeadlineService deadlineService;

    @Mock
    private VehicleService vehicleService;

    @Mock
    private SecurityContext securityContext;
    @Mock
    private Principal principal;

    @InjectMocks
    private DeadlineController deadlineController;

    private UUID deadlineId;
    private DeadlineRequest deadlineRequest;
    private DeadlineCompletionRequest completionRequest;
    private DeadlineResponse deadlineResponse;

    @BeforeEach
    void setUp() {
        deadlineId = UUID.randomUUID();

        deadlineRequest = new DeadlineRequest("Revisione", LocalDate.now().plusDays(10), null, false);
        completionRequest = new DeadlineCompletionRequest(LocalDate.now(), null);

        UUID vehicleId = UUID.randomUUID();
        deadlineResponse = new DeadlineResponse(deadlineId, "Revisione", LocalDate.now().plusDays(10).toString(),
                vehicleId, null, false, false, null, null, false);
    }

    @Test
    void getUrgentDeadlines_WithDate_Returns200AndList() {
        LocalDate upToDate = LocalDate.now().plusDays(15);
        String upToDateString = upToDate.toString();
        when(deadlineService.getUrgentDeadlines(upToDate)).thenReturn(List.of(deadlineResponse));

        Response response = deadlineController.getUrgentDeadlines(upToDateString);

        assertEquals(200, response.getStatus());
        assertEquals(1, ((List<?>) response.getEntity()).size());
        verify(deadlineService).getUrgentDeadlines(upToDate);
    }

    @Test
    void getUrgentDeadlines_WithNullDate_UsesDefaultAndReturns200() {
        when(deadlineService.getUrgentDeadlines(any(LocalDate.class))).thenReturn(List.of(deadlineResponse));

        Response response = deadlineController.getUrgentDeadlines(null);

        assertEquals(200, response.getStatus());

        // verifica che abbia calcolato il default di 30 giorni
        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(deadlineService).getUrgentDeadlines(dateCaptor.capture());
        assertEquals(LocalDate.now().plusDays(30), dateCaptor.getValue());
    }

    @Test
    void updateDeadline_Returns200AndUpdatedDeadline() {
        when(deadlineService.updateDeadline(deadlineId, deadlineRequest)).thenReturn(deadlineResponse);

        Response response = deadlineController.updateDeadline(deadlineId, deadlineRequest);

        assertEquals(200, response.getStatus());
        assertEquals(deadlineResponse, response.getEntity());
        verify(deadlineService).updateDeadline(deadlineId, deadlineRequest);
    }

    @Test
    void completeDeadline_AsOwner_Returns200() {
        Deadline mockDeadline = mock(Deadline.class);
        Vehicle mockVehicle = mock(Vehicle.class);
        when(securityContext.isUserInRole("CUSTOMER")).thenReturn(false);
        when(deadlineService.getDeadlineById(deadlineId)).thenReturn(mockDeadline);
        when(mockDeadline.getVehicle()).thenReturn(mockVehicle);
        when(mockVehicle.getStatus()).thenReturn(VehicleStatus.AVAILABLE);

        Response response = deadlineController.completeDeadline(deadlineId, completionRequest);

        assertEquals(200, response.getStatus());
        verify(deadlineService).completeDeadline(deadlineId, completionRequest.completionDate(),
                completionRequest.notes());
    }

    @Test
    void completeDeadline_AsOwner_Returns403WhenOwnedBySoldVehicle() {
        Deadline mockDeadline = mock(Deadline.class);
        Vehicle mockVehicle = mock(Vehicle.class);
        when(securityContext.isUserInRole("CUSTOMER")).thenReturn(false);
        when(deadlineService.getDeadlineById(deadlineId)).thenReturn(mockDeadline);
        when(mockDeadline.getVehicle()).thenReturn(mockVehicle);
        when(mockVehicle.getStatus()).thenReturn(VehicleStatus.SOLD);

        Response response = deadlineController.completeDeadline(deadlineId, completionRequest);

        assertEquals(403, response.getStatus());
        verify(deadlineService, never()).completeDeadline(deadlineId, completionRequest.completionDate(),
                completionRequest.notes());
    }

    @Test
    void completeDeadline_AsOwner_Returns403WhenOwnedByWithdrawnVehicle() {
        Deadline mockDeadline = mock(Deadline.class);
        Vehicle mockVehicle = mock(Vehicle.class);
        when(securityContext.isUserInRole("CUSTOMER")).thenReturn(false);
        when(deadlineService.getDeadlineById(deadlineId)).thenReturn(mockDeadline);
        when(mockDeadline.getVehicle()).thenReturn(mockVehicle);
        when(mockVehicle.getStatus()).thenReturn(VehicleStatus.WITHDRAWN);

        Response response = deadlineController.completeDeadline(deadlineId, completionRequest);

        assertEquals(403, response.getStatus());
        verify(deadlineService, never()).completeDeadline(deadlineId, completionRequest.completionDate(),
                completionRequest.notes());
    }

    @Test
    void completeDeadline_AsCustomer_Returns200() {
        UUID customerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Deadline mockDeadline = mock(Deadline.class);
        Vehicle mockVehicle = mock(Vehicle.class);
        when(securityContext.isUserInRole("CUSTOMER")).thenReturn(true);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(customerId.toString());
        when(deadlineService.getDeadlineById(deadlineId)).thenReturn(mockDeadline);
        when(mockDeadline.getVehicle()).thenReturn(mockVehicle);
        when(mockVehicle.getId()).thenReturn(vehicleId);
        when(vehicleService.isVehicleOwnedByCustomer(mockVehicle.getId(), customerId)).thenReturn(true);

        Response response = deadlineController.completeDeadline(deadlineId, completionRequest);

        assertEquals(200, response.getStatus());
        verify(deadlineService).completeDeadline(deadlineId, completionRequest.completionDate(),
                completionRequest.notes());
    }

    @Test
    void completeDeadline_AsCustomer_Returns403WhenVehicleIsNotOwnedByCustomer() {
        UUID customerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Deadline mockDeadline = mock(Deadline.class);
        Vehicle mockVehicle = mock(Vehicle.class);
        when(securityContext.isUserInRole("CUSTOMER")).thenReturn(true);
        when(securityContext.getUserPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn(customerId.toString());
        when(deadlineService.getDeadlineById(deadlineId)).thenReturn(mockDeadline);
        when(mockDeadline.getVehicle()).thenReturn(mockVehicle);
        when(mockVehicle.getId()).thenReturn(vehicleId);
        when(vehicleService.isVehicleOwnedByCustomer(mockVehicle.getId(), customerId)).thenReturn(false);

        Response response = deadlineController.completeDeadline(deadlineId, completionRequest);

        assertEquals(403, response.getStatus());
        verify(deadlineService, never()).completeDeadline(deadlineId, completionRequest.completionDate(),
                completionRequest.notes());
    }

    @Test
    void deleteDeadline_Returns204NoContent() {
        Response response = deadlineController.deleteDeadline(deadlineId);

        assertEquals(204, response.getStatus());
        verify(deadlineService).deleteDeadline(deadlineId);
    }
}