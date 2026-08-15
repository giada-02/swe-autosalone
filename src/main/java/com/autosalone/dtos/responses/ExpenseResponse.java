package com.autosalone.dtos.responses;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.autosalone.models.Transaction;

public record ExpenseResponse(
        UUID id,
        String reason,
        BigDecimal amount,
        LocalDate date,
        UUID vehicleId) {

    public static ExpenseResponse fromEntity(Transaction expense) {
        if (expense == null)
            return null;

        return new ExpenseResponse(
                expense.getId(),
                expense.getReason(),
                expense.getAmount(),
                expense.getDate(),
                expense.getVehicle().getId());
    }
}