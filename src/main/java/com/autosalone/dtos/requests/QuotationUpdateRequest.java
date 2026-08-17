package com.autosalone.dtos.requests;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.autosalone.enums.DiscountType;
import com.autosalone.enums.ExpirationPolicy;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record QuotationUpdateRequest(
        LocalDate expirationDate,
        @NotNull ExpirationPolicy expirationPolicy,
        @NotNull LocalDate date,
        @NotNull UUID vehicleId,
        @NotNull UUID customerId,
        @PositiveOrZero BigDecimal additionalFees,
        @Size(max = 1000, message = "cannot exceed 1000 characters") String publicNotes,
        @Size(max = 1000, message = "cannot exceed 1000 characters") String internalNotes,
        @PositiveOrZero BigDecimal vehicleSellingPrice,
        DiscountType discountType,
        @PositiveOrZero BigDecimal discountValue) {

    public QuotationUpdateRequest {
        if (discountType == null && discountValue != null) {
            throw new IllegalArgumentException(
                    "Cannot apply a discount value without specifying a discount type");
        }

        if (discountType != null && discountValue == null) {
            throw new IllegalArgumentException(
                    "A discount type was specified, but the discount value is missing");
        }

        if (expirationPolicy == ExpirationPolicy.CUSTOM && expirationDate == null) {
            throw new IllegalArgumentException(
                    "Cannot apply a CUSTOM expiration policy without providing an explicit expiration date");
        }
    }
}