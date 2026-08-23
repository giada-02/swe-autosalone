package com.autosalone.dtos.requests;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Positive;

public record ContractConfirmRequest(
        @Positive BigDecimal depositAmount,
        LocalDate depositDate) {

    public ContractConfirmRequest {
        if (depositAmount == null && depositDate != null) {
            throw new IllegalArgumentException(
                    "Cannot confirm contract: a deposit amount was specified, but the date is missing.");
        }

        if (depositAmount != null && depositDate == null) {
            throw new IllegalArgumentException(
                    "Cannot confirm contract: a deposit date was specified, but the value is missing.");
        }
    }
}
