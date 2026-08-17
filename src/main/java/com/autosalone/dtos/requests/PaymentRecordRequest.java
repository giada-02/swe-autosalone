package com.autosalone.dtos.requests;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentRecordRequest(
        String description,
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDate date) {
}
