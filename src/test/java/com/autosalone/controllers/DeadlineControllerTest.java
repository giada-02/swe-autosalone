package com.autosalone.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.autosalone.dtos.DeadlineCompletionRequest;
import com.autosalone.dtos.DeadlineRequest;
import com.autosalone.models.Deadline;
import com.autosalone.services.DeadlineService;

import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
class DeadlineControllerTest {

    @Mock
    private DeadlineService deadlineService;

    @InjectMocks
    private DeadlineController deadlineController;

    private UUID deadlineId;
    private Deadline mockDeadline;
    private DeadlineRequest deadlineRequest;
    private DeadlineCompletionRequest completionRequest;

    @BeforeEach
    void setUp() {
        deadlineId = UUID.randomUUID();
        mockDeadline = mock(Deadline.class);
        deadlineRequest = new DeadlineRequest("Revisione", LocalDate.now().plusDays(10), null, false);
        completionRequest = new DeadlineCompletionRequest(LocalDate.now(), null);
    }

    @Test
    void getUrgentDeadlines_WithDate_Returns200AndList() {
        LocalDate upToDate = LocalDate.now().plusDays(15);
        String upToDateString = upToDate.toString();
        when(deadlineService.getUrgentDeadlines(upToDate)).thenReturn(List.of(mockDeadline));

        Response response = deadlineController.getUrgentDeadlines(upToDateString);

        assertEquals(200, response.getStatus());
        assertEquals(1, ((List<?>) response.getEntity()).size());
        verify(deadlineService).getUrgentDeadlines(upToDate);
    }

    @Test
    void getUrgentDeadlines_WithNullDate_UsesDefaultAndReturns200() {
        when(deadlineService.getUrgentDeadlines(any(LocalDate.class))).thenReturn(List.of(mockDeadline));

        Response response = deadlineController.getUrgentDeadlines(null);

        assertEquals(200, response.getStatus());

        // verifica che abbia calcolato il default di 30 giorni
        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(deadlineService).getUrgentDeadlines(dateCaptor.capture());
        assertEquals(LocalDate.now().plusDays(30), dateCaptor.getValue());
    }

    @Test
    void updateDeadline_Returns200AndUpdatedDeadline() {
        when(deadlineService.updateDeadline(deadlineId, deadlineRequest)).thenReturn(mockDeadline);

        Response response = deadlineController.updateDeadline(deadlineId, deadlineRequest);

        assertEquals(200, response.getStatus());
        assertEquals(mockDeadline, response.getEntity());
        verify(deadlineService).updateDeadline(deadlineId, deadlineRequest);
    }

    @Test
    void completeDeadline_Returns200Ok() {
        Response response = deadlineController.completeDeadline(deadlineId, completionRequest);

        assertEquals(200, response.getStatus());
        verify(deadlineService).completeDeadline(deadlineId, completionRequest.completionDate(),
                completionRequest.notes());
    }
}