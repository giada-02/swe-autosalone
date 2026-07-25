package com.autosalone.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

import com.autosalone.dtos.DeadlineRequest;
import com.autosalone.models.Deadline;
import com.autosalone.models.Vehicle;
import com.autosalone.repositories.DeadlineRepository;

@ExtendWith(MockitoExtension.class)
class DeadlineServiceTest {

    @Mock
    private DeadlineRepository deadlineRepository;

    @InjectMocks
    private DeadlineService deadlineService;

    private UUID deadlineId;
    private UUID vehicleId;
    private Deadline mockDeadline;
    private Vehicle mockVehicle;

    @BeforeEach
    void setUp() {
        deadlineId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();
        mockDeadline = mock(Deadline.class);
        mockVehicle = mock(Vehicle.class);
    }

    // read

    @Test
    void getDeadlineById_Success() {
        when(deadlineRepository.findById(deadlineId)).thenReturn(Optional.of(mockDeadline));
        Deadline result = deadlineService.getDeadlineById(deadlineId);
        assertNotNull(result);
        assertEquals(mockDeadline, result);
    }

    @Test
    void getDeadlineById_NotFound() {
        when(deadlineRepository.findById(deadlineId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> {
            deadlineService.getDeadlineById(deadlineId);
        });
    }

    @Test
    void getDeadlinesByVehicleId_Completed() {
        when(deadlineRepository.findHistoryByVehicleId(vehicleId)).thenReturn(List.of(mockDeadline));

        List<Deadline> results = deadlineService.getDeadlinesByVehicleId(vehicleId, true);

        assertEquals(1, results.size());
        verify(deadlineRepository).findHistoryByVehicleId(vehicleId);
        verify(deadlineRepository, never()).findPendingByVehicleId(any());
    }

    @Test
    void getDeadlinesByVehicleId_Pending() {
        when(deadlineRepository.findPendingByVehicleId(vehicleId)).thenReturn(List.of(mockDeadline));

        List<Deadline> results = deadlineService.getDeadlinesByVehicleId(vehicleId, false);

        assertEquals(1, results.size());
        verify(deadlineRepository).findPendingByVehicleId(vehicleId);
        verify(deadlineRepository, never()).findHistoryByVehicleId(any());
    }

    @Test
    void getUrgentDeadlines_Success() {
        LocalDate upToDate = LocalDate.now().plusDays(7);
        when(deadlineRepository.findUrgentDeadlines(upToDate)).thenReturn(List.of(mockDeadline));

        List<Deadline> results = deadlineService.getUrgentDeadlines(upToDate);

        assertEquals(1, results.size());
        verify(deadlineRepository).findUrgentDeadlines(upToDate);
    }

    // write

    @Test
    void completeDeadline_NoRecurrence_SavesOnlyCurrent() {
        when(deadlineRepository.findById(deadlineId)).thenReturn(Optional.of(mockDeadline));
        LocalDate completionDate = LocalDate.now();

        when(mockDeadline.complete(completionDate, "Tagliando completato")).thenReturn(null);

        deadlineService.completeDeadline(deadlineId, completionDate, "Tagliando completato");

        verify(deadlineRepository).save(mockDeadline);
        verify(deadlineRepository).save(any(Deadline.class));
    }

    @Test
    void completeDeadline_WithRecurrence_SavesBoth() {
        when(deadlineRepository.findById(deadlineId)).thenReturn(Optional.of(mockDeadline));
        LocalDate completionDate = LocalDate.now();

        Deadline mockNextRecurrence = mock(Deadline.class);

        when(mockDeadline.complete(completionDate, "Revisione fatta")).thenReturn(mockNextRecurrence);

        deadlineService.completeDeadline(deadlineId, completionDate, "Revisione fatta");

        verify(deadlineRepository).save(mockDeadline);
        verify(deadlineRepository).save(mockNextRecurrence);
    }

    @Test
    void updateDeadline_Success() {
        when(deadlineRepository.findById(deadlineId)).thenReturn(Optional.of(mockDeadline));

        DeadlineRequest request = new DeadlineRequest(
                "Bollo",
                LocalDate.now().plusMonths(1),
                Period.ofYears(1),
                false);

        assertDoesNotThrow(() -> deadlineService.updateDeadline(deadlineId, request));

        verify(mockDeadline).setReason("Bollo");
        verify(mockDeadline).setDueDate(request.dueDate());
        verify(mockDeadline).setRecurrence(Period.ofYears(1));
        verify(mockDeadline).setRecalculateFromCompletion(false);

        verify(deadlineRepository).save(mockDeadline);
    }

    @Test
    void deleteDeadline_Success() {
        when(deadlineRepository.findById(deadlineId)).thenReturn(Optional.of(mockDeadline));
        when(mockDeadline.getVehicle()).thenReturn(mockVehicle);

        deadlineService.deleteDeadline(deadlineId);

        verify(mockVehicle).removeDeadline(mockDeadline);
        verify(deadlineRepository).delete(mockDeadline);
    }
}