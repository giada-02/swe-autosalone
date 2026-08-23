package com.autosalone.dtos.responses;

import java.util.UUID;

import com.autosalone.models.Deadline;

public record DeadlineResponse(
        UUID id,
        String reason,
        String dueDate,
        UUID vehicleId,
        String recurrence,
        boolean recalculateFromCompletion,
        boolean isCompleted,
        String completionDate,
        String notes,
        boolean isExpired) {

    public static DeadlineResponse fromEntity(Deadline deadline) {
        if (deadline == null)
            return null;

        return new DeadlineResponse(
                deadline.getId(),
                deadline.getReason(),
                deadline.getDueDate() != null ? deadline.getDueDate().toString() : null,
                deadline.getVehicle().getId(),
                deadline.getRecurrence() != null ? deadline.getRecurrence().toString() : null,
                deadline.isRecalculatedFromCompletion(),
                deadline.isCompleted(),
                deadline.getCompletionDate() != null ? deadline.getCompletionDate().toString() : null,
                deadline.getNotes(),
                deadline.isExpired());
    }
}