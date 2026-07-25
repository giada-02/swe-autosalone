package com.autosalone.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record AccessoryRequest(
        @NotBlank String name,
        String description,
        @NotNull @PositiveOrZero BigDecimal basePrice) {
}