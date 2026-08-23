package com.autosalone.dtos.requests;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ExpenseRequest(
        @Size(max = 255, message = "cannot exceed 255 characters") String description,
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDate date) {
}
