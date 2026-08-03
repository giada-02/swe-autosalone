package com.autosalone.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AccessoryRequest(
        @NotBlank String name,
        @Size(min = 1, message = "cannot be blank") String description,
        @NotNull @PositiveOrZero BigDecimal basePrice) {
}