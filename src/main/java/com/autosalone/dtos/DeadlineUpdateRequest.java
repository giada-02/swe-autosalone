package com.autosalone.dtos;

import java.time.LocalDate;
import java.time.Period;

public record DeadlineUpdateRequest(
        String reason,
        LocalDate dueDate,
        Period recurrence,
        Boolean recalculateFromCompletion) {

    public DeadlineUpdateRequest {
        if (reason != null && reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason cannot be empty");
        }
    }
}
