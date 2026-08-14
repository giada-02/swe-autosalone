package com.autosalone.dtos.requests;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.autosalone.enums.VehicleCondition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;

public record VehicleRequest(
        @NotBlank String brand,
        @NotBlank String model,
        @NotBlank String color,
        @NotNull VehicleCondition condition,
        @Positive BigDecimal purchaseTransactionAmount,
        LocalDate purchaseTransactionDate,
        @PositiveOrZero BigDecimal sellingPrice,
        LocalDate handoverDate,
        @Size(max = 20, message = "cannot exceed 20 characters") String licensePlate,
        LocalDate registrationDate,
        @PositiveOrZero Double kilometers,
        @NotNull boolean inShowroom) {

    public VehicleRequest {
        if ((purchaseTransactionAmount != null && purchaseTransactionDate == null) ||
                (purchaseTransactionAmount == null && purchaseTransactionDate != null)) {
            throw new IllegalArgumentException(
                    "If the purchase amount is provided, the purchase date must also be provided (and vice versa)");
        }
    }
}
