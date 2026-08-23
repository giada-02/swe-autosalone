package com.autosalone.dtos.requests;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CatalogItemPriceUpdateRequest(
        @NotNull @PositiveOrZero BigDecimal newPrice) {
}
