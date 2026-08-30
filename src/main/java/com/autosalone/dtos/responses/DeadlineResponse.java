package com.autosalone.dtos.responses;

import java.util.UUID;
import org.hibernate.proxy.HibernateProxy;
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

        UUID extractedVehicleId = null;

        if (deadline.getVehicle() != null) {
            if (deadline.getVehicle() instanceof HibernateProxy proxy) {
                extractedVehicleId = (UUID) proxy.getHibernateLazyInitializer().getIdentifier();
            } else {
                extractedVehicleId = deadline.getVehicle().getId();
            }
        }

        return new DeadlineResponse(
                deadline.getId(),
                deadline.getReason(),
                deadline.getDueDate() != null ? deadline.getDueDate().toString() : null,
                extractedVehicleId,
                deadline.getRecurrence() != null ? deadline.getRecurrence().toString() : null,
                deadline.isRecalculatedFromCompletion(),
                deadline.isCompleted(),
                deadline.getCompletionDate() != null ? deadline.getCompletionDate().toString() : null,
                deadline.getNotes(),
                deadline.isExpired());
    }
}