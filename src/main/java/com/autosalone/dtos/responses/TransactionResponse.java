package com.autosalone.dtos.responses;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.autosalone.enums.TransactionType;
import com.autosalone.models.Transaction;

public record TransactionResponse(
        UUID id,
        String reason,
        BigDecimal amount,
        LocalDate date,
        TransactionType type,
        UUID vehicleId,
        UUID contractId) {

    public static TransactionResponse fromEntity(Transaction transaction) {
        if (transaction == null)
            return null;

        UUID vehicleId = transaction.getVehicle() != null ? transaction.getVehicle().getId() : null;
        UUID contractId = transaction.getContract() != null ? transaction.getContract().getId() : null;

        return new TransactionResponse(
                transaction.getId(),
                transaction.getReason(),
                transaction.getAmount(),
                transaction.getDate(),
                transaction.getType(),
                vehicleId,
                contractId);
    }
}