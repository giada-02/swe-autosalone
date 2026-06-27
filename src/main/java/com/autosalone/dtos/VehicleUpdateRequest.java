package com.autosalone.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.autosalone.enums.VehicleCondition;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record VehicleUpdateRequest(
        String brand,
        String model,
        String color,
        VehicleCondition condition,
        @Positive BigDecimal purchaseTransactionAmount,
        LocalDate purchaseTransactionDate,
        @PositiveOrZero BigDecimal sellingPrice,
        LocalDate handoverDate,
        String licensePlate,
        LocalDate registrationDate,
        @PositiveOrZero Double kilometers,
        Boolean inShowroom) {

    public VehicleUpdateRequest {
        if ((purchaseTransactionAmount != null && purchaseTransactionDate == null) ||
                (purchaseTransactionAmount == null && purchaseTransactionDate != null)) {
            throw new IllegalArgumentException(
                    "If the purchase amount is provided, the purchase date must also be provided (and vice versa)");
        }

        if (brand != null && brand.trim().isEmpty()) {
            throw new IllegalArgumentException("Brand cannot be empty");
        }
        if (model != null && model.trim().isEmpty()) {
            throw new IllegalArgumentException("Model cannot be empty");
        }
        if (color != null && color.trim().isEmpty()) {
            throw new IllegalArgumentException("Color cannot be empty");
        }
        if (licensePlate != null && licensePlate.trim().isEmpty()) {
            throw new IllegalArgumentException("License plate cannot be empty");
        }
    }
}