package com.autosalone.dtos;

import jakarta.validation.constraints.NotBlank;

public record PasswordUpdateRequest(
        @NotBlank String newPassword) {
}