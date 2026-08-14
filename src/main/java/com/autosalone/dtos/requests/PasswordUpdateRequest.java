package com.autosalone.dtos.requests;

import jakarta.validation.constraints.NotBlank;

public record PasswordUpdateRequest(
        @NotBlank String currentPassword,
        @NotBlank String newPassword) {
}