package com.autosalone.dtos.responses;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

import com.autosalone.models.Deadline;

public record DeadlineResponse(
        UUID id,
        String reason,
        LocalDate dueDate,
        UUID vehicleId,
        Period recurrence,
        boolean recalculateFromCompletion,
        boolean isCompleted,
        LocalDate completionDate,
        String notes,
        boolean isExpired) {

    public static DeadlineResponse fromEntity(Deadline deadline) {
        if (deadline == null)
            return null;

        return new DeadlineResponse(
                deadline.getId(),
                deadline.getReason(),
                deadline.getDueDate(),
                deadline.getVehicle().getId(),
                deadline.getRecurrence(),
                deadline.isRecalculatedFromCompletion(),
                deadline.isCompleted(),
                deadline.getCompletionDate(),
                deadline.getNotes(),
                deadline.isExpired());
    }
}