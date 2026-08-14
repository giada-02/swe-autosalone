package com.autosalone.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AccessoryRequest(
        @NotBlank @Size(max = 255, message = "cannot exceed 255 characters") String name,
        @Size(max = 255, message = "cannot exceed 255 characters") String description,
        @NotNull @PositiveOrZero BigDecimal basePrice) {
}