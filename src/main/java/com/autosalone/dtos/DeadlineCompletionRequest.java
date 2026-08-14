package com.autosalone.dtos;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeadlineCompletionRequest(
        @NotNull LocalDate completionDate,
        @Size(max = 500, message = "cannot exceed 500 characters") String notes) {
}