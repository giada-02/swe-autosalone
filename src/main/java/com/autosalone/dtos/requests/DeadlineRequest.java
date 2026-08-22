package com.autosalone.dtos.requests;

import java.time.LocalDate;
import java.time.Period;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeadlineRequest(
        @NotBlank @Size(max = 255, message = "cannot exceed 255 characters") String reason,
        @NotNull LocalDate dueDate,
        Period recurrence,
        boolean recalculateFromCompletion) {

    public DeadlineRequest {
        if (recurrence == null && recalculateFromCompletion) {
            throw new IllegalArgumentException(
                    "Cannot recalculate from completion if there is no recurrence");
        }
    }
}