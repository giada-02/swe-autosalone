package com.autosalone.dtos;

import java.time.LocalDate;
import java.time.Period;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeadlineCreateRequest(
        @NotBlank String reason,
        @NotNull LocalDate dueDate,
        Period recurrence,
        boolean recalculateFromCompletion) {

    public DeadlineCreateRequest {
        if (recurrence == null && recalculateFromCompletion) {
            throw new IllegalArgumentException(
                    "Cannot recalculate from completion if there is no recurrence");
        }
    }
}
