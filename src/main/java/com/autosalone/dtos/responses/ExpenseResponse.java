package com.autosalone.dtos.responses;

import java.math.BigDecimal;
import java.util.UUID;

import com.autosalone.models.Transaction;

public record ExpenseResponse(
        UUID id,
        String reason,
        BigDecimal amount,
        String date,
        UUID vehicleId) {

    public static ExpenseResponse fromEntity(Transaction expense) {
        if (expense == null)
            return null;

        return new ExpenseResponse(
                expense.getId(),
                expense.getReason(),
                expense.getAmount(),
                expense.getDate() != null ? expense.getDate().toString() : null,
                expense.getVehicle().getId());
    }
}