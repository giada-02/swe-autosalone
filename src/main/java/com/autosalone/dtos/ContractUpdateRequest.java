package com.autosalone.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.autosalone.enums.DiscountType;

import jakarta.validation.constraints.PositiveOrZero;

public record ContractUpdateRequest(
        LocalDate estimatedHandoverDate,
        LocalDate date,
        UUID vehicleId,
        UUID customerId,
        @PositiveOrZero BigDecimal additionalFees,
        String publicNotes,
        String internalNotes,
        @PositiveOrZero BigDecimal vehicleSellingPrice,
        DiscountType discountType,
        @PositiveOrZero BigDecimal discountValue) {
}
